package com.example.snapmind.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(notIndexed = ["memoryId"])
@Entity(tableName = "memory_search_fts")
data class MemorySearchFts(
    val memoryId: Long,
    val ocrText: String,
    val memoBody: String,
    val tagText: String,
    val categoryText: String,
    val youtubeTitle: String,
)
