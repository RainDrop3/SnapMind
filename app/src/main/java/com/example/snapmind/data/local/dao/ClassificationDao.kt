package com.example.snapmind.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.snapmind.data.local.entity.ClassificationEntity

@Dao
interface ClassificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<ClassificationEntity>): List<Long>

    @Query("SELECT * FROM classifications WHERE memoryId = :memoryId ORDER BY rank ASC")
    suspend fun getByMemoryId(memoryId: Long): List<ClassificationEntity>

    @Query("SELECT * FROM classifications WHERE memoryId = :memoryId AND rank = 1 LIMIT 1")
    suspend fun getTopByMemoryId(memoryId: Long): ClassificationEntity?

    @Query("DELETE FROM classifications WHERE memoryId = :memoryId")
    suspend fun deleteByMemoryId(memoryId: Long): Int

    @Query(
        """
        SELECT classifications.label AS label, COUNT(DISTINCT classifications.memoryId) AS count
        FROM classifications
        JOIN memory_items ON memory_items.id = classifications.memoryId
        WHERE classifications.rank = 1
          AND memory_items.deletedAt IS NULL
        GROUP BY classifications.label
        ORDER BY count DESC, classifications.label ASC
        """,
    )
    suspend fun categoryCounts(): List<CategoryCountRow>
}

data class CategoryCountRow(
    val label: String,
    val count: Int,
)
