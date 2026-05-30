package com.example.snapmind.data.model

enum class ProcessingStatus {
    PROCESSING,
    DONE,
    ERROR,
}

enum class MemoryCategory(val displayName: String, val glyph: String) {
    CHAT("Chat", "CHAT"),
    RECEIPT("Receipt", "RCPT"),
    CODE("Code", "</>"),
    SHOPPING("Shopping", "SHOP"),
    TRAVEL("Travel", "TRIP"),
    FOOD("Food", "FOOD"),
    DOCUMENT("Document", "DOC"),
    YOUTUBE("YouTube", "PLAY"),
    UNKNOWN("Unknown", "SNAP"),
}

data class MemoryItem(
    val id: Long,
    val imageUri: String? = null,
    val sourceLabel: String = "SnapMind",
    val category: MemoryCategory = MemoryCategory.UNKNOWN,
    val memo: String = "",
    val ocrText: String = "",
    val tags: List<String> = emptyList(),
    val createdAtMillis: Long,
    val processingStatus: ProcessingStatus = ProcessingStatus.PROCESSING,
    val isFavorite: Boolean = false,
    val geminiSuggestion: String? = null,
    val youtubeTitle: String? = null,
    val youtubeUrl: String? = null,
    val deletedAtMillis: Long? = null,
) {
    val isDeleted: Boolean = deletedAtMillis != null
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
