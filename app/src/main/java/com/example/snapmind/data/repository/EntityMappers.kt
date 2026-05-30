package com.example.snapmind.data.repository

import com.example.snapmind.data.local.entity.ClassificationEntity
import com.example.snapmind.data.local.entity.GeminiMemoStatus
import com.example.snapmind.data.local.entity.MemoEntity
import com.example.snapmind.data.local.entity.MemoryItemEntity
import com.example.snapmind.data.local.entity.MemorySearchFts
import com.example.snapmind.data.local.entity.OcrTextEntity
import com.example.snapmind.data.local.entity.OptionalRemoteProcessingStatus
import com.example.snapmind.data.local.entity.StandardProcessingStatus
import com.example.snapmind.data.local.entity.TagEntity
import com.example.snapmind.data.local.entity.YoutubeLinkEntity
import com.example.snapmind.data.model.MemoryCategory
import com.example.snapmind.data.model.MemoryItem
import com.example.snapmind.data.model.ProcessingStatus

data class MemoryAggregate(
    val item: MemoryItemEntity,
    val ocr: OcrTextEntity?,
    val memo: MemoEntity?,
    val tags: List<TagEntity>,
    val topClassification: ClassificationEntity?,
    val youtubeLink: YoutubeLinkEntity?,
)

fun MemoryAggregate.toDomain(): MemoryItem = MemoryItem(
    id = item.id,
    imageUri = item.imageUri,
    sourceLabel = SOURCE_LABEL,
    category = topClassification?.label.toMemoryCategory(),
    memo = memo?.body.orEmpty(),
    ocrText = ocr?.fullText.orEmpty(),
    tags = tags.map { "#${it.displayName}" },
    createdAtMillis = item.createdAt,
    processingStatus = item.composeProcessingStatus(),
    isFavorite = item.isFavorite,
    geminiSuggestion = memo?.geminiSuggestion,
    youtubeTitle = youtubeLink?.title,
    youtubeUrl = youtubeLink?.url,
    deletedAtMillis = item.deletedAt,
)

fun MemoryItemEntity.composeProcessingStatus(): ProcessingStatus {
    val anyFailed = ocrStatus == StandardProcessingStatus.FAILED ||
        classificationStatus == StandardProcessingStatus.FAILED ||
        taggingStatus == StandardProcessingStatus.FAILED ||
        visionLabelStatus == OptionalRemoteProcessingStatus.FAILED ||
        youtubeLinkStatus == OptionalRemoteProcessingStatus.FAILED ||
        geminiMemoStatus == GeminiMemoStatus.FAILED
    if (anyFailed) return ProcessingStatus.ERROR

    val allDone = ocrStatus.isDone() &&
        classificationStatus.isDone() &&
        taggingStatus.isDone() &&
        visionLabelStatus.isDone() &&
        youtubeLinkStatus.isDone() &&
        geminiMemoStatus.isDone()
    return if (allDone) ProcessingStatus.DONE else ProcessingStatus.PROCESSING
}

private fun StandardProcessingStatus.isDone(): Boolean = this == StandardProcessingStatus.SUCCESS

private fun OptionalRemoteProcessingStatus.isDone(): Boolean =
    this == OptionalRemoteProcessingStatus.SUCCESS ||
        this == OptionalRemoteProcessingStatus.SKIPPED

private fun GeminiMemoStatus.isDone(): Boolean = this == GeminiMemoStatus.SUGGESTED ||
    this == GeminiMemoStatus.ACCEPTED ||
    this == GeminiMemoStatus.DISMISSED ||
    this == GeminiMemoStatus.SKIPPED

fun String?.toMemoryCategory(): MemoryCategory {
    if (this.isNullOrBlank()) return MemoryCategory.UNKNOWN
    return runCatching { MemoryCategory.valueOf(this.uppercase()) }
        .getOrDefault(MemoryCategory.UNKNOWN)
}

fun buildFtsRow(aggregate: MemoryAggregate): MemorySearchFts = MemorySearchFts(
    memoryId = aggregate.item.id,
    ocrText = aggregate.ocr?.fullText.orEmpty(),
    memoBody = aggregate.memo?.body.orEmpty(),
    tagText = aggregate.tags.joinToString(separator = " ") { it.displayName },
    categoryText = aggregate.topClassification?.label.orEmpty(),
    youtubeTitle = aggregate.youtubeLink?.title.orEmpty(),
)

private const val SOURCE_LABEL = "SnapMind"
