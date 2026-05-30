package com.example.snapmind.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memory_items",
    indices = [
        Index(value = ["imageUri"], unique = true),
        Index(value = ["contentHash"], unique = true),
        Index(value = ["createdAt"]),
        Index(value = ["deletedAt"]),
        Index(value = ["isFavorite"]),
    ],
)
data class MemoryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val imageUri: String,
    val sourceUri: String? = null,
    val mimeType: String? = null,
    val contentHash: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val ocrStatus: StandardProcessingStatus = StandardProcessingStatus.PENDING,
    val classificationStatus: StandardProcessingStatus = StandardProcessingStatus.PENDING,
    val visionLabelStatus: OptionalRemoteProcessingStatus = OptionalRemoteProcessingStatus.SKIPPED,
    val geminiMemoStatus: GeminiMemoStatus = GeminiMemoStatus.SKIPPED,
    val youtubeLinkStatus: OptionalRemoteProcessingStatus = OptionalRemoteProcessingStatus.SKIPPED,
    val taggingStatus: StandardProcessingStatus = StandardProcessingStatus.PENDING,
    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean = false,
    val deletedAt: Long? = null,
)
