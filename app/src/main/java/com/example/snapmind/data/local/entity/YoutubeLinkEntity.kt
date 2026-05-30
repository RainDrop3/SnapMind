package com.example.snapmind.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "youtube_links",
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
data class YoutubeLinkEntity(
    @PrimaryKey
    val memoryId: Long,
    val videoId: String,
    val title: String? = null,
    val url: String,
    val createdAt: Long,
)
