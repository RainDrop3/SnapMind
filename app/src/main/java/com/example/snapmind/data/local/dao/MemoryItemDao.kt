package com.example.snapmind.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.snapmind.data.local.entity.MemoryItemEntity
import com.example.snapmind.data.local.entity.StandardProcessingStatus
import com.example.snapmind.data.local.entity.OptionalRemoteProcessingStatus
import com.example.snapmind.data.local.entity.GeminiMemoStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryItemDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: MemoryItemEntity): Long

    @Update
    suspend fun update(item: MemoryItemEntity): Int

    @Delete
    suspend fun delete(item: MemoryItemEntity): Int

    @Query("DELETE FROM memory_items WHERE id = :memoryId")
    suspend fun deleteById(memoryId: Long): Int

    @Query("SELECT * FROM memory_items WHERE id = :memoryId LIMIT 1")
    suspend fun getById(memoryId: Long): MemoryItemEntity?

    @Query("SELECT * FROM memory_items WHERE id = :memoryId LIMIT 1")
    fun observeById(memoryId: Long): Flow<MemoryItemEntity?>

    @Query("SELECT * FROM memory_items WHERE imageUri = :imageUri LIMIT 1")
    suspend fun findByImageUri(imageUri: String): MemoryItemEntity?

    @Query("SELECT * FROM memory_items WHERE contentHash = :contentHash LIMIT 1")
    suspend fun findByContentHash(contentHash: String): MemoryItemEntity?

    @Query("SELECT * FROM memory_items WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<MemoryItemEntity>>

    @Query("SELECT * FROM memory_items WHERE deletedAt IS NULL AND isFavorite = 1 ORDER BY createdAt DESC")
    fun observeFavorites(): Flow<List<MemoryItemEntity>>

    @Query("SELECT * FROM memory_items WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrashed(): Flow<List<MemoryItemEntity>>

    @Query("UPDATE memory_items SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :memoryId")
    suspend fun setFavorite(memoryId: Long, isFavorite: Boolean, updatedAt: Long): Int

    @Query("UPDATE memory_items SET deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :memoryId")
    suspend fun setDeletedAt(memoryId: Long, deletedAt: Long?, updatedAt: Long): Int

    @Query("UPDATE memory_items SET ocrStatus = :status, updatedAt = :updatedAt WHERE id = :memoryId")
    suspend fun setOcrStatus(memoryId: Long, status: StandardProcessingStatus, updatedAt: Long): Int

    @Query("UPDATE memory_items SET classificationStatus = :status, updatedAt = :updatedAt WHERE id = :memoryId")
    suspend fun setClassificationStatus(memoryId: Long, status: StandardProcessingStatus, updatedAt: Long): Int

    @Query("UPDATE memory_items SET visionLabelStatus = :status, updatedAt = :updatedAt WHERE id = :memoryId")
    suspend fun setVisionLabelStatus(memoryId: Long, status: OptionalRemoteProcessingStatus, updatedAt: Long): Int

    @Query("UPDATE memory_items SET geminiMemoStatus = :status, updatedAt = :updatedAt WHERE id = :memoryId")
    suspend fun setGeminiMemoStatus(memoryId: Long, status: GeminiMemoStatus, updatedAt: Long): Int

    @Query("UPDATE memory_items SET youtubeLinkStatus = :status, updatedAt = :updatedAt WHERE id = :memoryId")
    suspend fun setYoutubeLinkStatus(memoryId: Long, status: OptionalRemoteProcessingStatus, updatedAt: Long): Int

    @Query("UPDATE memory_items SET taggingStatus = :status, updatedAt = :updatedAt WHERE id = :memoryId")
    suspend fun setTaggingStatus(memoryId: Long, status: StandardProcessingStatus, updatedAt: Long): Int
}
