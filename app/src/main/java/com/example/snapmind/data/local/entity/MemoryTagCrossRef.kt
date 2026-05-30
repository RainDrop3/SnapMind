package com.example.snapmind.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "memory_tag_cross_refs",
    primaryKeys = ["memoryId", "tagId"],
    indices = [
        Index(value = ["tagId", "removedAt"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = MemoryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["memoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class MemoryTagCrossRef(
    val memoryId: Long,
    val tagId: Long,
    val assignedBy: TagAssignedBy,
    val sourceTypes: String,
    val createdAt: Long,
    val removedAt: Long? = null,
)
