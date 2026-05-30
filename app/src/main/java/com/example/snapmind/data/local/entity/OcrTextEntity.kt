package com.example.snapmind.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ocr_texts",
    indices = [Index(value = ["memoryId"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = MemoryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["memoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class OcrTextEntity(
    @PrimaryKey
    val memoryId: Long,
    val fullText: String,
    val rawText: String? = null,
    val createdAt: Long,
)
