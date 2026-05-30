package com.example.snapmind.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.snapmind.data.local.entity.OcrTextEntity

@Dao
interface OcrTextDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(ocrText: OcrTextEntity)

    @Query("SELECT * FROM ocr_texts WHERE memoryId = :memoryId LIMIT 1")
    suspend fun getByMemoryId(memoryId: Long): OcrTextEntity?

    @Query("DELETE FROM ocr_texts WHERE memoryId = :memoryId")
    suspend fun deleteByMemoryId(memoryId: Long): Int
}
