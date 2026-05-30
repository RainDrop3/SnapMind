package com.example.snapmind.feature.memorydetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snapmind.data.model.MemoryItem
import com.example.snapmind.data.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DetailUiState(
    val memory: MemoryItem? = null,
    val memoDraft: String = "",
    val hasUnsavedMemo: Boolean = false,
    val gone: Boolean = false,
) {
    val isReady: Boolean get() = memory != null && !gone
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: MemoryRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val memoryIdFlow: StateFlow<Long> =
        savedStateHandle.getStateFlow(KEY_MEMORY_ID, -1L)

    private val memoDraftFlow: StateFlow<String?> =
        savedStateHandle.getStateFlow<String?>(KEY_MEMO_DRAFT, null)

    val uiState: StateFlow<DetailUiState> = combine(
        repository.memories,
        memoryIdFlow,
        memoDraftFlow,
    ) { memories, id, draft ->
        if (id <= 0L) return@combine DetailUiState()
        val memory = memories.firstOrNull { it.id == id }
        when {
            memory == null -> DetailUiState(gone = true)
            memory.isDeleted -> DetailUiState(gone = true)
            else -> {
                val savedBody = memory.memo
                val effectiveDraft = draft ?: savedBody
                DetailUiState(
                    memory = memory,
                    memoDraft = effectiveDraft,
                    hasUnsavedMemo = draft != null && draft != savedBody,
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState(),
    )

    fun bind(memoryId: Long) {
        if (memoryId <= 0L) return
        if (memoryIdFlow.value != memoryId) {
            savedStateHandle[KEY_MEMORY_ID] = memoryId
            savedStateHandle[KEY_MEMO_DRAFT] = null
        }
    }

    fun onMemoDraftChanged(text: String) {
        val memory = uiState.value.memory ?: return
        val newDraft = if (text == memory.memo) null else text
        savedStateHandle[KEY_MEMO_DRAFT] = newDraft
    }

    fun saveMemo(): Boolean {
        val draft = memoDraftFlow.value ?: return false
        val id = memoryIdFlow.value
        if (id <= 0L) return false
        repository.updateMemo(id, draft)
        savedStateHandle[KEY_MEMO_DRAFT] = null
        return true
    }

    fun toggleFavorite() {
        val id = memoryIdFlow.value
        if (id > 0L) repository.toggleFavorite(id)
    }

    fun softDelete() {
        val id = memoryIdFlow.value
        if (id > 0L) repository.softDelete(id)
    }

    fun acceptGeminiSuggestion() {
        val id = memoryIdFlow.value
        if (id > 0L) {
            repository.acceptGeminiSuggestion(id)
            savedStateHandle[KEY_MEMO_DRAFT] = null
        }
    }

    fun dismissGeminiSuggestion() {
        val id = memoryIdFlow.value
        if (id > 0L) repository.dismissGeminiSuggestion(id)
    }

    companion object {
        private const val KEY_MEMORY_ID = "detail.memoryId"
        private const val KEY_MEMO_DRAFT = "detail.memoDraft"
    }
}
