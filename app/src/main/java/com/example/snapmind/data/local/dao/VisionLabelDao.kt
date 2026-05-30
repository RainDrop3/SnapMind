package com.example.snapmind.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.snapmind.data.local.entity.VisionLabelEntity

@Dao
interface VisionLabelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<VisionLabelEntity>): List<Long>

    @Query("SELECT * FROM vision_labels WHERE memoryId = :memoryId ORDER BY score DESC")
    suspend fun getByMemoryId(memoryId: Long): List<VisionLabelEntity>

    @Query("DELETE FROM vision_labels WHERE memoryId = :memoryId")
    suspend fun deleteByMemoryId(memoryId: Long): Int
}
