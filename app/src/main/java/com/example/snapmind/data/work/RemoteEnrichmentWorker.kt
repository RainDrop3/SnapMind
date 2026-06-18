package com.example.snapmind.data.work

import android.content.Context
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.snapmind.core.image.RemoteImageEncoder
import com.example.snapmind.core.result.AppResult
import com.example.snapmind.core.settings.AppPreferences
import com.example.snapmind.data.local.dao.MemoDao
import com.example.snapmind.data.local.dao.MemoryItemDao
import com.example.snapmind.data.local.dao.MemorySearchDao
import com.example.snapmind.data.local.dao.OcrTextDao
import com.example.snapmind.data.local.dao.VisionLabelDao
import com.example.snapmind.data.local.dao.YoutubeLinkDao
import com.example.snapmind.data.local.entity.GeminiMemoStatus
import com.example.snapmind.data.local.entity.OptionalRemoteProcessingStatus
import com.example.snapmind.data.local.entity.VisionLabelEntity
import com.example.snapmind.data.local.entity.YoutubeLinkEntity
import com.example.snapmind.data.remote.common.RemoteEnrichmentRepository
import com.example.snapmind.data.repository.MemoryAggregateBuilder
import com.example.snapmind.data.repository.refreshFtsRow
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class RemoteEnrichmentWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val memoryItemDao: MemoryItemDao,
    private val ocrTextDao: OcrTextDao,
    private val memoDao: MemoDao,
    private val visionLabelDao: VisionLabelDao,
    private val youtubeLinkDao: YoutubeLinkDao,
    private val memorySearchDao: MemorySearchDao,
    private val remoteRepository: RemoteEnrichmentRepository,
    private val prefs: AppPreferences,
    private val aggregateBuilder: MemoryAggregateBuilder,
    private val imageEncoder: RemoteImageEncoder,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val memoryId = inputData.getLong(LocalMemoryProcessingWorker.KEY_MEMORY_ID, -1L)
        if (memoryId <= 0L) return Result.failure()
        val entity = memoryItemDao.getById(memoryId) ?: return Result.success()
        val flags = prefs.current()
        val needsImagePayload = flags.visionEnabled && flags.visionApiKey.isNotBlank()
        val base64Jpeg = if (needsImagePayload) imageEncoder.encodeBase64Jpeg(entity.imageUri.toUri()) else null

        enrichVision(memoryId, flags.visionEnabled, flags.visionApiKey, base64Jpeg)
        // Gemini 메모 추천은 자동 실행하지 않고, 상세 화면 버튼(GeminiMemoSuggester)에서 온디맨드로만 호출한다.
        memoryItemDao.setGeminiMemoStatus(memoryId, GeminiMemoStatus.SKIPPED, System.currentTimeMillis())
        enrichYoutube(memoryId, flags.youtubeEnabled, flags.youtubeApiKey)
        refreshFtsRow(memoryId, memoryItemDao, aggregateBuilder, memorySearchDao)
        enqueueAutoTagging(memoryId)
        return Result.success()
    }

    private suspend fun enrichVision(
        memoryId: Long,
        enabled: Boolean,
        apiKey: String,
        base64Jpeg: String?,
    ) {
        val now = { System.currentTimeMillis() }
        if (!enabled || apiKey.isBlank()) {
            memoryItemDao.setVisionLabelStatus(memoryId, OptionalRemoteProcessingStatus.SKIPPED, now())
            return
        }
        if (base64Jpeg == null) {
            memoryItemDao.setVisionLabelStatus(memoryId, OptionalRemoteProcessingStatus.FAILED, now())
            return
        }
        memoryItemDao.setVisionLabelStatus(memoryId, OptionalRemoteProcessingStatus.RUNNING, now())
        when (val result = remoteRepository.labelImage(base64Jpeg, apiKey)) {
            is AppResult.Success -> {
                visionLabelDao.deleteByMemoryId(memoryId)
                val createdAt = now()
                val rows = result.data.map {
                    VisionLabelEntity(
                        memoryId = memoryId,
                        label = it.label,
                        score = it.score,
                        createdAt = createdAt,
                    )
                }
                if (rows.isNotEmpty()) visionLabelDao.insertAll(rows)
                memoryItemDao.setVisionLabelStatus(memoryId, OptionalRemoteProcessingStatus.SUCCESS, now())
            }
            is AppResult.Error -> {
                memoryItemDao.setVisionLabelStatus(memoryId, OptionalRemoteProcessingStatus.FAILED, now())
            }
        }
    }

    private suspend fun enrichYoutube(
        memoryId: Long,
        enabled: Boolean,
        apiKey: String,
    ) {
        val now = { System.currentTimeMillis() }
        if (!enabled || apiKey.isBlank()) {
            memoryItemDao.setYoutubeLinkStatus(memoryId, OptionalRemoteProcessingStatus.SKIPPED, now())
            return
        }
        val query = youtubeQuery(memoryId)
        if (query.isBlank()) {
            memoryItemDao.setYoutubeLinkStatus(memoryId, OptionalRemoteProcessingStatus.SKIPPED, now())
            return
        }
        memoryItemDao.setYoutubeLinkStatus(memoryId, OptionalRemoteProcessingStatus.RUNNING, now())
        when (val result = remoteRepository.findYoutubeVideo(query, apiKey)) {
            is AppResult.Success -> {
                val link = result.data
                if (link == null) {
                    memoryItemDao.setYoutubeLinkStatus(memoryId, OptionalRemoteProcessingStatus.SKIPPED, now())
                } else {
                    youtubeLinkDao.upsert(
                        YoutubeLinkEntity(
                            memoryId = memoryId,
                            videoId = link.videoId,
                            title = link.title,
                            url = link.url,
                            createdAt = now(),
                        ),
                    )
                    memoryItemDao.setYoutubeLinkStatus(memoryId, OptionalRemoteProcessingStatus.SUCCESS, now())
                }
            }
            is AppResult.Error -> {
                memoryItemDao.setYoutubeLinkStatus(memoryId, OptionalRemoteProcessingStatus.FAILED, now())
            }
        }
    }

    private suspend fun youtubeQuery(memoryId: Long): String {
        val ocrLine = ocrTextDao.getByMemoryId(memoryId)
            ?.fullText
            ?.lineSequence()
            ?.firstOrNull { it.isNotBlank() }
            ?.trim()
        if (!ocrLine.isNullOrBlank()) return ocrLine.take(MAX_YOUTUBE_QUERY_LENGTH)
        return memoDao.getByMemoryId(memoryId)?.body.orEmpty().trim().take(MAX_YOUTUBE_QUERY_LENGTH)
    }

    private fun enqueueAutoTagging(memoryId: Long) {
        WorkManager.getInstance(applicationContext).enqueue(
            OneTimeWorkRequestBuilder<AutoTaggingWorker>()
                .setInputData(workDataOf(LocalMemoryProcessingWorker.KEY_MEMORY_ID to memoryId))
                .build(),
        )
    }

    private companion object {
        const val MAX_YOUTUBE_QUERY_LENGTH = 120
    }
}
