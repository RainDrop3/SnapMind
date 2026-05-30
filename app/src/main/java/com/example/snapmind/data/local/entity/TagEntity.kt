package com.example.snapmind.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["isUserManaged"]),
        Index(value = ["isArchived"]),
    ],
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val displayName: String,
    @ColumnInfo(defaultValue = "0")
    val isUserManaged: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    @ColumnInfo(defaultValue = "0")
    val isArchived: Boolean = false,
)
