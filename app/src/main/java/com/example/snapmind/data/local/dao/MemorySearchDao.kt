package com.example.snapmind.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.snapmind.data.local.entity.MemoryItemEntity
import com.example.snapmind.data.local.entity.MemorySearchFts
import kotlinx.coroutines.flow.Flow

@Dao
interface MemorySearchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIndex(row: MemorySearchFts)

    @Query("DELETE FROM memory_search_fts WHERE memoryId = :memoryId")
    suspend fun deleteIndex(memoryId: Long): Int

    @Query(
        """
        SELECT memory_items.* FROM memory_items
        JOIN memory_search_fts ON memory_search_fts.memoryId = memory_items.id
        WHERE memory_search_fts MATCH :query
          AND memory_items.deletedAt IS NULL
        ORDER BY memory_items.createdAt DESC
        """,
    )
    suspend fun search(query: String): List<MemoryItemEntity>

    @Query(
        """
        SELECT memory_items.id FROM memory_items
        JOIN memory_search_fts ON memory_search_fts.memoryId = memory_items.id
        WHERE memory_search_fts MATCH :query
          AND memory_items.deletedAt IS NULL
        """,
    )
    suspend fun searchIds(query: String): List<Long>

    @Query(
        """
        SELECT memory_items.* FROM memory_items
        JOIN memory_search_fts ON memory_search_fts.memoryId = memory_items.id
        WHERE memory_search_fts MATCH :query
          AND memory_items.deletedAt IS NULL
        ORDER BY memory_items.createdAt DESC
        """,
    )
    fun observeSearch(query: String): Flow<List<MemoryItemEntity>>
}
