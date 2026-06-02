package com.example.snapmind.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snapmind.core.result.AppResult
import com.example.snapmind.data.model.MemoryCategory
import com.example.snapmind.data.repository.MemoryRepository
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository,
) : ViewModel() {
    private val selectedTag = MutableStateFlow<String?>(null)
    private val selectedCategory = MutableStateFlow<MemoryCategory?>(null)

    val uiState = combine(
        memoryRepository.memories,
        selectedTag,
        selectedCategory,
    ) { memories, tag, category ->
        val activeMemories = memories.filterNot { it.isDeleted }
            .sortedByDescending { it.createdAtMillis }
        val filteredForHome = activeMemories.filter { item ->
            (category == null || item.category == category) &&
                (tag == null || item.tags.any { it.equalsTag(tag) })
        }
        val tagItems = activeMemories.filter { item ->
            tag == null || item.tags.any { it.equalsTag(tag) }
        }

        MainUiState(
            memories = activeMemories,
            homeItems = filteredForHome,
            favoriteItems = activeMemories.filter { it.isFavorite },
            tagItems = tagItems,
            tags = memoryRepository.tags(),
            topTags = memoryRepository.topTags(),
            categories = memoryRepository.categoryCounts(),
            selectedTag = tag,
            selectedCategory = category,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    fun applyTagFilter(tagName: String) {
        selectedTag.value = tagName.removePrefix("#")
        selectedCategory.value = null
    }

    fun applyCategoryFilter(category: MemoryCategory) {
        selectedCategory.value = category
        selectedTag.value = null
    }

    fun clearFilters() {
        selectedTag.value = null
        selectedCategory.value = null
    }

    fun toggleFavorite(memoryId: Long) {
        memoryRepository.toggleFavorite(memoryId)
    }

    fun softDelete(memoryIds: List<Long>) {
        memoryIds.forEach { memoryRepository.softDelete(it) }
    }

    suspend fun exportToPdf(memoryIds: List<Long>): AppResult<Uri> =
        memoryRepository.exportToPdf(memoryIds)

    private fun String.equalsTag(other: String): Boolean =
        removePrefix("#").equals(other.removePrefix("#"), ignoreCase = true)
}
