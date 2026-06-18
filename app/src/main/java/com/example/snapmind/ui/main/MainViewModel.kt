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

    // '태그별' 페이지 전용 필터 상태 (홈/즐겨찾기와 분리).
    private val tagBrowseTag = MutableStateFlow<String?>(null)
    private val tagBrowseCategory = MutableStateFlow<MemoryCategory?>(null)

    val uiState = combine(
        memoryRepository.memories,
        selectedTag,
        selectedCategory,
        tagBrowseTag,
        tagBrowseCategory,
    ) { memories, tag, category, browseTag, browseCategory ->
        val activeMemories = memories.filterNot { it.isDeleted }
            .sortedByDescending { it.createdAtMillis }
        val filteredForHome = activeMemories.filter { item ->
            (category == null || category in item.categories) &&
                (tag == null || item.tags.any { it.equalsTag(tag) })
        }
        val tagBrowseItems = activeMemories.filter { item ->
            (browseCategory == null || browseCategory in item.categories) &&
                (browseTag == null || item.tags.any { it.equalsTag(browseTag) })
        }

        MainUiState(
            memories = activeMemories,
            homeItems = filteredForHome,
            favoriteItems = activeMemories.filter { it.isFavorite },
            tags = memoryRepository.tags(),
            topTags = memoryRepository.topTags(),
            categories = memoryRepository.categoryCounts(),
            selectedTag = tag,
            selectedCategory = category,
            tagBrowseItems = tagBrowseItems,
            tagBrowseSelectedTag = browseTag,
            tagBrowseSelectedCategory = browseCategory,
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

    // --- '태그별' 페이지 전용 필터 (홈/즐겨찾기 미영향) ---

    fun applyTagBrowseTag(tagName: String) {
        tagBrowseTag.value = tagName.removePrefix("#")
        tagBrowseCategory.value = null
    }

    fun applyTagBrowseCategory(category: MemoryCategory) {
        tagBrowseCategory.value = category
        tagBrowseTag.value = null
    }

    fun clearTagBrowseFilters() {
        tagBrowseTag.value = null
        tagBrowseCategory.value = null
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
