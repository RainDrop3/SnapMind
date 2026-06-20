package com.example.snapmind.data.local.entity

import androidx.room.ColumnInfo
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
data class LinkPreviewEntity(
    @PrimaryKey
    val memoryId: Long,
    @ColumnInfo(name = "videoId")
    val legacyId: String,
    val title: String? = null,
    val url: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val siteName: String? = null,
    @ColumnInfo(defaultValue = "'UNCHECKED'")
    val safetyStatus: String = "UNCHECKED",
    val safetyThreatTypes: String? = null,
    val safetyCheckedAt: Long? = null,
    val createdAt: Long,
)
