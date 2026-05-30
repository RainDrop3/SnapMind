package com.example.snapmind.data.repository

import android.net.Uri
import com.example.snapmind.core.result.AppResult
import com.example.snapmind.data.model.CategoryCount
import com.example.snapmind.data.model.MemoryCategory
import com.example.snapmind.data.model.MemoryItem
import com.example.snapmind.data.model.TagCount
import kotlinx.coroutines.flow.StateFlow

interface MemoryRepository {
    val memories: StateFlow<List<MemoryItem>>

    fun getMemory(memoryId: Long): MemoryItem?
    fun activeMemories(): List<MemoryItem>
    fun favoriteMemories(): List<MemoryItem>
    fun trashedMemories(): List<MemoryItem>
    fun topTags(limit: Int = 3): List<TagCount>
    fun categoryCounts(): List<CategoryCount>
    fun tags(): List<TagCount>
    fun searchMemories(
        query: String,
        tagName: String? = null,
        category: MemoryCategory? = null,
    ): List<MemoryItem>

    fun filterByTag(tagName: String?): List<MemoryItem>
    fun filterByCategory(category: MemoryCategory?): List<MemoryItem>
    suspend fun importImage(
        sourceUri: Uri,
        mimeType: String?,
        sourceLabel: String,
    ): AppResult<MemoryItem>

    fun toggleFavorite(memoryId: Long)
    fun updateMemo(memoryId: Long, memo: String)
    fun acceptGeminiSuggestion(memoryId: Long)
    fun dismissGeminiSuggestion(memoryId: Long)
    fun softDelete(memoryId: Long)
    fun restore(memoryId: Long)
    suspend fun permanentDelete(memoryId: Long): AppResult<Unit>
    suspend fun searchFts(query: String): List<MemoryItem>
    suspend fun exportToPdf(memoryIds: List<Long>): AppResult<Uri>
}
