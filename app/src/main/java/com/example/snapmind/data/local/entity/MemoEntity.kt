package com.example.snapmind.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memos",
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
data class MemoEntity(
    @PrimaryKey
    val memoryId: Long,
    val body: String,
    val geminiSuggestion: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
