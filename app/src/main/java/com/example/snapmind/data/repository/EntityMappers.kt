package com.example.snapmind.data.repository

import com.example.snapmind.data.local.entity.ClassificationEntity
import com.example.snapmind.data.local.entity.GeminiMemoStatus
import com.example.snapmind.data.local.entity.LinkPreviewEntity
import com.example.snapmind.data.local.entity.MemoEntity
import com.example.snapmind.data.local.entity.MemoryItemEntity
import com.example.snapmind.data.local.entity.MemorySearchFts
import com.example.snapmind.data.local.entity.OcrTextEntity
import com.example.snapmind.data.local.entity.OptionalRemoteProcessingStatus
import com.example.snapmind.data.local.entity.StandardProcessingStatus
import com.example.snapmind.data.local.entity.TagEntity
import com.example.snapmind.data.model.LinkPreview
import com.example.snapmind.data.model.MemoryCategory
import com.example.snapmind.data.model.MemoryItem
import com.example.snapmind.data.model.ProcessingStatus

data class MemoryAggregate(
    val item: MemoryItemEntity,
    val ocr: OcrTextEntity?,
    val memo: MemoEntity?,
    val tags: List<TagEntity>,
    /** rank 오름차순으로 정렬된 상위 카테고리 분류(최대 2개). */
    val classifications: List<ClassificationEntity>,
    val linkPreview: LinkPreviewEntity?,
) {
    val topClassification: ClassificationEntity? get() = classifications.firstOrNull()
}

fun MemoryAggregate.toDomain(): MemoryItem = MemoryItem(
    id = item.id,
    imageUri = item.imageUri,
    originalImageUri = item.originalImageUri,
    sourceLabel = SOURCE_LABEL,
    categories = classifications.toMemoryCategories(),
    memo = memo?.body.orEmpty(),
    ocrText = ocr?.fullText.orEmpty(),
    tags = tags.map { "#${it.displayName}" },
    createdAtMillis = item.createdAt,
    updatedAtMillis = item.updatedAt,
    processingStatus = item.composeProcessingStatus(),
    isFavorite = item.isFavorite,
    geminiSuggestion = memo?.geminiSuggestion,
    linkPreview = linkPreview?.toDomain(),
    imageEnhancementStatus = item.imageEnhancementStatus.name,
    imageEnhancementProvider = item.imageEnhancementProvider,
    imageEnhancedAtMillis = item.imageEnhancedAt,
    deletedAtMillis = item.deletedAt,
)

private fun LinkPreviewEntity.toDomain(): LinkPreview = LinkPreview(
    url = url,
    title = title,
    description = description,
    imageUrl = imageUrl,
    siteName = siteName,
    safetyStatus = safetyStatus,
    safetyThreatTypes = safetyThreatTypes,
    safetyCheckedAtMillis = safetyCheckedAt,
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
    if (this.isNullOrBlank()) return MemoryCategory.OTHERS
    return runCatching { MemoryCategory.valueOf(this.uppercase()) }
        .getOrDefault(MemoryCategory.OTHERS)
}

/**
 * rank 순 분류 목록을 도메인 카테고리 리스트로 변환한다.
 * 실제 카테고리(OTHERS 제외)만 중복 없이 최대 2개 취하고, 하나도 없으면 [OTHERS] 한 개를 반환한다.
 */
fun List<ClassificationEntity>.toMemoryCategories(): List<MemoryCategory> {
    val resolved = sortedBy { it.rank }
        .map { it.label.toMemoryCategory() }
        .filter { it != MemoryCategory.OTHERS }
        .distinct()
        .take(MemoryCategory.MAX_PER_MEMORY)
    return resolved.ifEmpty { listOf(MemoryCategory.OTHERS) }
}

fun buildFtsRow(aggregate: MemoryAggregate): MemorySearchFts = MemorySearchFts(
    memoryId = aggregate.item.id,
    ocrText = aggregate.ocr?.fullText.orEmpty(),
    memoBody = aggregate.memo?.body.orEmpty(),
    tagText = aggregate.tags.joinToString(separator = " ") { it.displayName },
    categoryText = aggregate.topClassification?.label.orEmpty(),
    youtubeTitle = aggregate.linkPreview?.let { preview ->
        listOfNotNull(preview.title, preview.description, preview.siteName, preview.url)
            .joinToString(separator = " ")
    }.orEmpty(),
)

private const val SOURCE_LABEL = "SnapMind"
