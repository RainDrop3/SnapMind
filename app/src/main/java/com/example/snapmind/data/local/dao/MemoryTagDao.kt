package com.example.snapmind.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.snapmind.data.local.entity.MemoryItemEntity
import com.example.snapmind.data.local.entity.MemoryTagCrossRef

@Dao
interface MemoryTagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(crossRef: MemoryTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(crossRefs: List<MemoryTagCrossRef>)

    @Update
    suspend fun update(crossRef: MemoryTagCrossRef): Int

    @Query("SELECT * FROM memory_tag_cross_refs WHERE memoryId = :memoryId AND removedAt IS NULL")
    suspend fun activeTagsForMemory(memoryId: Long): List<MemoryTagCrossRef>

    @Query("SELECT * FROM memory_tag_cross_refs WHERE memoryId = :memoryId")
    suspend fun allTagsForMemory(memoryId: Long): List<MemoryTagCrossRef>

    @Query(
        """
        UPDATE memory_tag_cross_refs
        SET removedAt = :removedAt
        WHERE memoryId = :memoryId AND tagId = :tagId
        """,
    )
    suspend fun setRemovedAt(memoryId: Long, tagId: Long, removedAt: Long?): Int

    @Query("DELETE FROM memory_tag_cross_refs WHERE memoryId = :memoryId")
    suspend fun deleteByMemoryId(memoryId: Long): Int

    @Query(
        """
        SELECT memory_items.* FROM memory_items
        JOIN memory_tag_cross_refs ON memory_items.id = memory_tag_cross_refs.memoryId
        WHERE memory_tag_cross_refs.tagId = :tagId
          AND memory_tag_cross_refs.removedAt IS NULL
          AND memory_items.deletedAt IS NULL
        ORDER BY memory_items.createdAt DESC
        """,
    )
    suspend fun memoriesForTag(tagId: Long): List<MemoryItemEntity>
}
