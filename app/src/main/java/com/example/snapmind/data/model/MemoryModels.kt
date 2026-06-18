package com.example.snapmind.data.model

enum class ProcessingStatus {
    PROCESSING,
    DONE,
    ERROR,
}

enum class MemoryCategory(val displayName: String, val glyph: String) {
    CHAT("Chat", "CHAT"),
    RECEIPT("Receipt", "RCPT"),
    TRAVEL("Travel", "TRIP"),
    FOOD("Food", "FOOD"),
    DOCUMENT("Document", "DOC"),
    YOUTUBE("YouTube", "PLAY"),
    OTHERS("Others", "SNAP"),
    ;

    companion object {
        /** 한 메모리에 붙일 수 있는 최대 카테고리 수. */
        const val MAX_PER_MEMORY = 2

        /** 사용자가 상세 화면에서 직접 고를 수 있는 카테고리(임계값 미달 fallback인 OTHERS는 제외). */
        val selectable: List<MemoryCategory> =
            entries.filter { it != OTHERS }
    }
}

data class MemoryItem(
    val id: Long,
    val imageUri: String? = null,
    val sourceLabel: String = "SnapMind",
    /** 이 메모리에 부여된 카테고리(최대 2개, rank 순). 비어 있으면 OTHERS로 간주. */
    val categories: List<MemoryCategory> = emptyList(),
    val memo: String = "",
    val ocrText: String = "",
    val tags: List<String> = emptyList(),
    val createdAtMillis: Long,
    val updatedAtMillis: Long = createdAtMillis,
    val processingStatus: ProcessingStatus = ProcessingStatus.PROCESSING,
    val isFavorite: Boolean = false,
    val geminiSuggestion: String? = null,
    val youtubeTitle: String? = null,
    val youtubeUrl: String? = null,
    val deletedAtMillis: Long? = null,
) {
    val isDeleted: Boolean = deletedAtMillis != null

    /** 대표 카테고리(썸네일 배경·제목·단일 카테고리 필터용). */
    val category: MemoryCategory get() = categories.firstOrNull() ?: MemoryCategory.OTHERS
}

data class TagCount(
    val name: String,
    val displayName: String,
    val count: Int,
)

data class CategoryCount(
    val category: MemoryCategory,
    val count: Int,
)
