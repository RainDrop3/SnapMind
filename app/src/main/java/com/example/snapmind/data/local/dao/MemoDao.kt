package com.example.snapmind.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.snapmind.data.local.entity.MemoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(memo: MemoEntity)

    @Query("SELECT * FROM memos WHERE memoryId = :memoryId LIMIT 1")
    suspend fun getByMemoryId(memoryId: Long): MemoEntity?

    @Query("SELECT * FROM memos WHERE memoryId = :memoryId LIMIT 1")
    fun observeByMemoryId(memoryId: Long): Flow<MemoEntity?>

    @Query("UPDATE memos SET body = :body, updatedAt = :updatedAt WHERE memoryId = :memoryId")
    suspend fun updateBody(memoryId: Long, body: String, updatedAt: Long): Int

    @Query("UPDATE memos SET geminiSuggestion = :suggestion, updatedAt = :updatedAt WHERE memoryId = :memoryId")
    suspend fun updateGeminiSuggestion(memoryId: Long, suggestion: String?, updatedAt: Long): Int

    @Query("DELETE FROM memos WHERE memoryId = :memoryId")
    suspend fun deleteByMemoryId(memoryId: Long): Int
}
