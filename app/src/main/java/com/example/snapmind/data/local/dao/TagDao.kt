package com.example.snapmind.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.snapmind.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    @Update
    suspend fun update(tag: TagEntity): Int

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): TagEntity?

    @Query("SELECT * FROM tags WHERE id = :tagId LIMIT 1")
    suspend fun findById(tagId: Long): TagEntity?

    @Query("SELECT * FROM tags WHERE isArchived = 0 ORDER BY displayName ASC")
    fun observeActive(): Flow<List<TagEntity>>

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteById(tagId: Long): Int

    @Query("UPDATE tags SET isArchived = :archived, updatedAt = :updatedAt WHERE id = :tagId")
    suspend fun setArchived(tagId: Long, archived: Boolean, updatedAt: Long): Int

    @Query(
        """
        SELECT tags.id AS tagId, tags.name AS name, tags.displayName AS displayName,
               COUNT(memory_tag_cross_refs.memoryId) AS count
        FROM tags
        JOIN memory_tag_cross_refs ON tags.id = memory_tag_cross_refs.tagId
        JOIN memory_items ON memory_items.id = memory_tag_cross_refs.memoryId
        WHERE tags.isArchived = 0
          AND memory_tag_cross_refs.removedAt IS NULL
          AND memory_items.deletedAt IS NULL
        GROUP BY tags.id
        ORDER BY count DESC, tags.displayName ASC
        LIMIT :limit
        """,
    )
    suspend fun popularTags(limit: Int): List<TagCountRow>

    @Query(
        """
        SELECT tags.id AS tagId, tags.name AS name, tags.displayName AS displayName,
               COUNT(memory_tag_cross_refs.memoryId) AS count
        FROM tags
        JOIN memory_tag_cross_refs ON tags.id = memory_tag_cross_refs.tagId
        JOIN memory_items ON memory_items.id = memory_tag_cross_refs.memoryId
        WHERE tags.isArchived = 0
          AND memory_tag_cross_refs.removedAt IS NULL
          AND memory_items.deletedAt IS NULL
        GROUP BY tags.id
        ORDER BY count DESC, tags.displayName ASC
        """,
    )
    suspend fun allTagCounts(): List<TagCountRow>
}

data class TagCountRow(
    val tagId: Long,
    val name: String,
    val displayName: String,
    val count: Int,
)
