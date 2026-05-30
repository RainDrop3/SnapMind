package com.example.snapmind.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vision_labels",
    indices = [
        Index(value = ["memoryId"]),
        Index(value = ["label", "score"]),
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
data class VisionLabelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val memoryId: Long,
    val label: String,
    val score: Float,
    val createdAt: Long,
)
