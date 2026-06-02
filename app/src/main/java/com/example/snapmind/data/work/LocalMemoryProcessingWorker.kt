package com.example.snapmind.data.work

import android.content.Context
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.snapmind.core.ai.ImageClassifier
import com.example.snapmind.core.ai.ModelUnavailableException
import com.example.snapmind.core.ai.OcrExtractor
import com.example.snapmind.data.local.dao.ClassificationDao
import com.example.snapmind.data.local.dao.MemoryItemDao
import com.example.snapmind.data.local.dao.MemorySearchDao
import com.example.snapmind.data.local.dao.OcrTextDao
import com.example.snapmind.data.local.entity.ClassificationEntity
import com.example.snapmind.data.local.entity.OcrTextEntity
import com.example.snapmind.data.local.entity.StandardProcessingStatus
import com.example.snapmind.data.repository.MemoryAggregateBuilder
import com.example.snapmind.data.repository.refreshFtsRow
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class LocalMemoryProcessingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val memoryItemDao: MemoryItemDao,
    private val ocrTextDao: OcrTextDao,
    private val classificationDao: ClassificationDao,
    private val memorySearchDao: MemorySearchDao,
    private val ocrExtractor: OcrExtractor,
    private val imageClassifier: ImageClassifier,
    private val aggregateBuilder: MemoryAggregateBuilder,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val memoryId = inputData.getLong(KEY_MEMORY_ID, -1L)
        if (memoryId <= 0L) return Result.failure()
        val entity = memoryItemDao.getById(memoryId) ?: return Result.success()
        val now = { System.currentTimeMillis() }

        memoryItemDao.setOcrStatus(memoryId, StandardProcessingStatus.RUNNING, now())
        ocrExtractor.extract(entity.imageUri.toUri())
            .onSuccess { ocr ->
                ocrTextDao.upsert(
                    OcrTextEntity(
                        memoryId = memoryId,
                        fullText = ocr.fullText,
                        rawText = ocr.rawText.takeIf { it != ocr.fullText },
                        createdAt = now(),
                    ),
                )
                memoryItemDao.setOcrStatus(memoryId, StandardProcessingStatus.SUCCESS, now())
            }
            .onFailure {
                memoryItemDao.setOcrStatus(memoryId, StandardProcessingStatus.FAILED, now())
            }

        memoryItemDao.setClassificationStatus(memoryId, StandardProcessingStatus.RUNNING, now())
        imageClassifier.classify(entity.imageUri.toUri())
            .onSuccess { result ->
                val createdAt = now()
                val rows = result.predictions.map { prediction ->
                    ClassificationEntity(
                        memoryId = memoryId,
                        label = prediction.label,
                        confidence = prediction.confidence,
                        modelVersion = result.modelVersion,
                        rank = prediction.rank,
                        createdAt = createdAt,
                    )
                }
                classificationDao.deleteByMemoryId(memoryId)
                if (rows.isNotEmpty()) classificationDao.insertAll(rows)
                memoryItemDao.setClassificationStatus(memoryId, StandardProcessingStatus.SUCCESS, now())
            }
            .onFailure { error ->
                if (error is ModelUnavailableException) {
                    memoryItemDao.setClassificationStatus(memoryId, StandardProcessingStatus.FAILED, now())
                } else {
                    memoryItemDao.setClassificationStatus(memoryId, StandardProcessingStatus.FAILED, now())
                }
            }

        refreshFtsRow(memoryId, memoryItemDao, aggregateBuilder, memorySearchDao)

        WorkManager.getInstance(applicationContext).enqueue(
            OneTimeWorkRequestBuilder<RemoteEnrichmentWorker>()
                .setInputData(workDataOf(KEY_MEMORY_ID to memoryId))
                .build(),
        )
        return Result.success()
    }

    companion object {
        const val KEY_MEMORY_ID = "memoryId"
    }
}
