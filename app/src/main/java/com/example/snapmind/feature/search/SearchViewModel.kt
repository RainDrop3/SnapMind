package com.example.snapmind.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snapmind.data.model.CategoryCount
import com.example.snapmind.data.model.MemoryCategory
import com.example.snapmind.data.model.MemoryItem
import com.example.snapmind.data.model.TagCount
import com.example.snapmind.data.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

data class SearchUiState(
    val query: String = "",
    val tagName: String? = null,
    val category: MemoryCategory? = null,
    val results: List<MemoryItem> = emptyList(),
    val tags: List<TagCount> = emptyList(),
    val categories: List<CategoryCount> = emptyList(),
) {
    val isEmpty: Boolean get() = results.isEmpty()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MemoryRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _tagName = MutableStateFlow<String?>(null)
    private val _category = MutableStateFlow<MemoryCategory?>(null)

    val query: StateFlow<String> = _query.asStateFlow()
    val tagName: StateFlow<String?> = _tagName.asStateFlow()
    val category: StateFlow<MemoryCategory?> = _category.asStateFlow()

    private val debouncedQuery = _query
        .debounce(DEBOUNCE_MILLIS)
        .distinctUntilChanged()

    private val resultsFlow = combine(
        repository.memories,
        debouncedQuery,
        _tagName,
        _category,
    ) { _, q, t, c -> SearchInputs(q, t, c) }
        .mapLatest { (q, t, c) ->
            val base = repository.searchFts(q)
            base.filter { item ->
                (t == null || item.tags.any { matchesTag(it, t) }) &&
                    (c == null || item.category == c)
            }
        }

    val uiState: StateFlow<SearchUiState> = combine(
        resultsFlow,
        _query,
        _tagName,
        _category,
        repository.memories,
    ) { results, q, t, c, _ ->
        SearchUiState(
            query = q,
            tagName = t,
            category = c,
            results = results,
            tags = repository.tags(),
            categories = repository.categoryCounts(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChanged(value: String) { _query.value = value }
    fun onTagSelected(name: String?) {
        _tagName.value = name
        if (name != null) _category.value = null
    }

    fun onCategorySelected(value: MemoryCategory?) {
        _category.value = value
        if (value != null) _tagName.value = null
    }

    fun clearFilters() {
        _tagName.value = null
        _category.value = null
    }

    fun toggleFavorite(memoryId: Long) {
        repository.toggleFavorite(memoryId)
    }

    private fun matchesTag(tagDisplay: String, selected: String): Boolean {
        val left = tagDisplay.trim().removePrefix("#").lowercase()
        val right = selected.trim().removePrefix("#").lowercase()
        return left == right
    }

    private data class SearchInputs(
        val query: String,
        val tagName: String?,
        val category: MemoryCategory?,
    )

    companion object {
        private const val DEBOUNCE_MILLIS = 250L
    }
}
