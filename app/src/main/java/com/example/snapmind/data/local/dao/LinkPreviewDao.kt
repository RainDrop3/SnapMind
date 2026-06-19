package com.example.snapmind.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.snapmind.data.local.entity.LinkPreviewEntity

@Dao
interface LinkPreviewDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(link: LinkPreviewEntity)

    @Query("SELECT * FROM youtube_links WHERE memoryId = :memoryId LIMIT 1")
    suspend fun getByMemoryId(memoryId: Long): LinkPreviewEntity?

    @Query("DELETE FROM youtube_links WHERE memoryId = :memoryId")
    suspend fun deleteByMemoryId(memoryId: Long): Int
}
