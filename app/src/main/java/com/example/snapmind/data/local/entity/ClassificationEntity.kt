package com.example.snapmind.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "classifications",
    indices = [
        Index(value = ["memoryId", "label"]),
        Index(value = ["label"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = MemoryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["memoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ClassificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val memoryId: Long,
    val label: String,
    val confidence: Float,
    val modelVersion: String,
    val rank: Int,
    val createdAt: Long,
)
