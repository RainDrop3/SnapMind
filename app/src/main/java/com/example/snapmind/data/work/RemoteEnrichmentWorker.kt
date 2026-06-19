package com.example.snapmind.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.snapmind.core.link.UrlExtractor
import com.example.snapmind.core.link.YoutubeLinkHelper
import com.example.snapmind.core.result.AppResult
import com.example.snapmind.core.settings.AppPreferences
import com.example.snapmind.core.settings.RemoteFeatureFlags
import com.example.snapmind.data.local.dao.LinkPreviewDao
import com.example.snapmind.data.local.dao.MemoryItemDao
import com.example.snapmind.data.local.dao.MemorySearchDao
import com.example.snapmind.data.local.dao.OcrTextDao
import com.example.snapmind.data.model.LinkSafetyStatus
import com.example.snapmind.data.local.entity.GeminiMemoStatus
import com.example.snapmind.data.local.entity.LinkPreviewEntity
import com.example.snapmind.data.local.entity.OptionalRemoteProcessingStatus
import com.example.snapmind.data.remote.common.RemoteEnrichmentRepository
import com.example.snapmind.data.remote.common.RemoteLinkSafety
import com.example.snapmind.data.remote.common.RemoteLinkPreview
import com.example.snapmind.data.repository.MemoryAggregateBuilder
import com.example.snapmind.data.repository.refreshFtsRow
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val LINK_ACCESS_FAILED_DESCRIPTION =
    "페이지 접근을 확인하지 못했습니다. URL 자동 인식이 실패했을 수 있어요."

@HiltWorker
class RemoteEnrichmentWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val memoryItemDao: MemoryItemDao,
    private val ocrTextDao: OcrTextDao,
    private val linkPreviewDao: LinkPreviewDao,
    private val memorySearchDao: MemorySearchDao,
    private val remoteRepository: RemoteEnrichmentRepository,
    private val prefs: AppPreferences,
    private val aggregateBuilder: MemoryAggregateBuilder,
    private val urlExtractor: UrlExtractor,
    private val youtubeLinkHelper: YoutubeLinkHelper,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val memoryId = inputData.getLong(LocalMemoryProcessingWorker.KEY_MEMORY_ID, -1L)
        if (memoryId <= 0L) return Result.failure()
        if (memoryItemDao.getById(memoryId) == null) return Result.success()
        val now = System.currentTimeMillis()
        val flags = prefs.current()

        // Google Vision API 기반 라벨링은 링크 프리뷰 수집으로 대체한다.
        memoryItemDao.setVisionLabelStatus(memoryId, OptionalRemoteProcessingStatus.SKIPPED, now)
        // Gemini 메모 추천은 자동 실행하지 않고, 상세 화면 버튼(GeminiMemoSuggester)에서 온디맨드로만 호출한다.
        memoryItemDao.setGeminiMemoStatus(memoryId, GeminiMemoStatus.SKIPPED, now)

        enrichLinkPreview(memoryId, flags)
        refreshFtsRow(memoryId, memoryItemDao, aggregateBuilder, memorySearchDao)
        enqueueAutoTagging(memoryId)
        return Result.success()
    }

    private suspend fun enrichLinkPreview(
        memoryId: Long,
        flags: RemoteFeatureFlags,
    ) {
        val now = { System.currentTimeMillis() }
        if (!flags.linkPreviewEnabled) {
            linkPreviewDao.deleteByMemoryId(memoryId)
            memoryItemDao.setLinkPreviewStatus(memoryId, OptionalRemoteProcessingStatus.SKIPPED, now())
            return
        }

        val ocrText = ocrTextDao.getByMemoryId(memoryId)?.fullText.orEmpty()
        val url = urlExtractor.firstUrl(ocrText)
        if (url == null) {
            linkPreviewDao.deleteByMemoryId(memoryId)
            memoryItemDao.setLinkPreviewStatus(memoryId, OptionalRemoteProcessingStatus.SKIPPED, now())
            return
        }

        memoryItemDao.setLinkPreviewStatus(memoryId, OptionalRemoteProcessingStatus.RUNNING, now())
        val safety = checkSafety(url, flags.safeBrowsingEnabled, flags.safeBrowsingApiKey)
        val fallback = fallbackPreview(url)
        val enrichment = if (safety.status == LinkSafetyStatus.UNSAFE) {
            null
        } else {
            enrichPreview(url, flags.youtubeEnabled, flags.youtubeApiKey)
        }
        val preview = when {
            safety.status == LinkSafetyStatus.UNSAFE -> fallback.copy(
                title = "위험 가능성이 있는 링크",
                description = safety.threatTypes.joinToString(separator = ", "),
            )
            enrichment?.preview != null -> enrichment.preview
            else -> fallback.copy(description = LINK_ACCESS_FAILED_DESCRIPTION)
        }.withSafety(safety.withAccessFailure(enrichment?.accessFailed == true))

        linkPreviewDao.upsert(preview.toEntity(memoryId, now()))
        memoryItemDao.setLinkPreviewStatus(memoryId, OptionalRemoteProcessingStatus.SUCCESS, now())
    }

    private suspend fun checkSafety(
        url: String,
        enabled: Boolean,
        apiKey: String,
    ): RemoteLinkSafety {
        if (!enabled || apiKey.isBlank()) {
            return RemoteLinkSafety(status = LinkSafetyStatus.UNCHECKED)
        }
        return when (val result = remoteRepository.checkLinkSafety(url, apiKey)) {
            is AppResult.Success -> result.data
            is AppResult.Error -> RemoteLinkSafety(status = LinkSafetyStatus.CHECK_FAILED)
        }
    }

    private suspend fun enrichPreview(
        url: String,
        youtubeEnabled: Boolean,
        youtubeApiKey: String,
    ): PreviewResult {
        val videoId = youtubeLinkHelper.videoId(url)
        if (videoId != null && youtubeEnabled && youtubeApiKey.isNotBlank()) {
            when (val result = remoteRepository.fetchYoutubeVideo(videoId, youtubeApiKey)) {
                is AppResult.Success -> result.data
                    ?.withImageFallback(url)
                    ?.withOpenUrlFallback(url)
                    ?.let { return PreviewResult(preview = it, accessFailed = false) }
                is AppResult.Error -> Unit
            }
        }
        return when (val result = remoteRepository.fetchLinkPreview(url)) {
            is AppResult.Success -> PreviewResult(
                preview = result.data.withImageFallback(url).withOpenUrlFallback(url),
                accessFailed = false,
            )
            is AppResult.Error -> PreviewResult(preview = null, accessFailed = true)
        }
    }

    private fun fallbackPreview(url: String): RemoteLinkPreview =
        RemoteLinkPreview(
            url = url,
            title = urlExtractor.hostLabel(url),
            description = null,
            imageUrl = null,
            siteName = urlExtractor.hostLabel(url),
        )

    private fun RemoteLinkPreview.withImageFallback(sourceUrl: String): RemoteLinkPreview =
        if (!imageUrl.isNullOrBlank()) this else copy(imageUrl = youtubeLinkHelper.thumbnailUrl(sourceUrl))

    private fun RemoteLinkPreview.withOpenUrlFallback(sourceUrl: String): RemoteLinkPreview =
        copy(url = youtubeLinkHelper.watchUrl(sourceUrl) ?: url)

    private fun RemoteLinkSafety.withAccessFailure(accessFailed: Boolean): RemoteLinkSafety =
        if (!accessFailed || status == LinkSafetyStatus.UNSAFE) {
            this
        } else {
            copy(status = LinkSafetyStatus.ACCESS_FAILED)
        }

    private fun RemoteLinkPreview.withSafety(safety: RemoteLinkSafety): RemoteLinkPreviewWithSafety =
        RemoteLinkPreviewWithSafety(
            preview = this,
            safety = safety,
        )

    private fun RemoteLinkPreviewWithSafety.toEntity(memoryId: Long, createdAt: Long): LinkPreviewEntity =
        LinkPreviewEntity(
            memoryId = memoryId,
            legacyId = preview.url,
            title = preview.title,
            url = preview.url,
            description = preview.description,
            imageUrl = preview.imageUrl,
            siteName = preview.siteName,
            safetyStatus = safety.status,
            safetyThreatTypes = safety.threatTypes.joinToString(separator = ", ").ifBlank { null },
            safetyCheckedAt = if (
                safety.status == LinkSafetyStatus.UNCHECKED ||
                safety.status == LinkSafetyStatus.ACCESS_FAILED
            ) null else createdAt,
            createdAt = createdAt,
        )

    private data class RemoteLinkPreviewWithSafety(
        val preview: RemoteLinkPreview,
        val safety: RemoteLinkSafety,
    )

    private data class PreviewResult(
        val preview: RemoteLinkPreview?,
        val accessFailed: Boolean,
    )

    private fun enqueueAutoTagging(memoryId: Long) {
        WorkManager.getInstance(applicationContext).enqueue(
            OneTimeWorkRequestBuilder<AutoTaggingWorker>()
                .setInputData(workDataOf(LocalMemoryProcessingWorker.KEY_MEMORY_ID to memoryId))
                .build(),
        )
    }
}
