package com.example.snapmind.feature.memorydetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snapmind.core.result.AppError
import com.example.snapmind.core.result.AppResult
import com.example.snapmind.data.model.MemoryCategory
import com.example.snapmind.data.model.MemoryItem
import com.example.snapmind.data.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

data class DetailUiState(
    val memory: MemoryItem? = null,
    val memoDraft: String = "",
    val tags: List<String> = emptyList(),
    val categories: List<MemoryCategory> = emptyList(),
    val hasUnsavedMemo: Boolean = false,
    val gone: Boolean = false,
) {
    val isReady: Boolean get() = memory != null && !gone
}

/** "저장" 결과: 성공 또는 저장을 진행할 수 없는 사유. */
enum class SaveResult { SAVED, NO_CATEGORY, PROCESSING }

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: MemoryRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val memoryIdFlow: StateFlow<Long> =
        savedStateHandle.getStateFlow(KEY_MEMORY_ID, -1L)

    /** Gemini 추천 요청 진행 중 여부 (버튼 비활성·메모칸 잠금·로딩 표시용). */
    val geminiLoading: StateFlow<Boolean> =
        combine(memoryIdFlow, repository.geminiInProgress) { id, inProgress ->
            id > 0L && id in inProgress
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val imageEnhancementLoading: StateFlow<Boolean> =
        combine(memoryIdFlow, repository.imageEnhancementInProgress) { id, inProgress ->
            id > 0L && id in inProgress
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** 추천 결과/오류를 1회성 토스트로 알리기 위한 메시지 채널. */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val memoDraftFlow: StateFlow<String?> =
        savedStateHandle.getStateFlow<String?>(KEY_MEMO_DRAFT, null)

    private val tagsDraftFlow: StateFlow<ArrayList<String>?> =
        savedStateHandle.getStateFlow<ArrayList<String>?>(KEY_TAGS_DRAFT, null)

    private val categoriesDraftFlow: StateFlow<ArrayList<String>?> =
        savedStateHandle.getStateFlow<ArrayList<String>?>(KEY_CATEGORIES_DRAFT, null)

    val uiState: StateFlow<DetailUiState> = combine(
        repository.memories,
        memoryIdFlow,
        memoDraftFlow,
        tagsDraftFlow,
        categoriesDraftFlow,
    ) { memories, id, draft, tagsDraft, categoriesDraft ->
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
                    tags = tagsDraft ?: memory.tags,
                    categories = categoriesDraft?.toCategories() ?: memory.categories,
                    hasUnsavedMemo = draft != null && draft != savedBody,
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState(),
    )

    init {
        // 추천 완료 이벤트는 현재 화면이 보고 있는 메모리에 대해서만 토스트로 노출한다.
        repository.geminiEvents
            .filter { it.memoryId == memoryIdFlow.value }
            .onEach { event ->
                val message = when (val result = event.result) {
                    is AppResult.Success -> "Gemini 추천을 받았어요."
                    is AppResult.Error -> result.error.toUserMessage()
                }
                _messages.tryEmit(message)
            }
            .launchIn(viewModelScope)

        repository.imageEnhancementEvents
            .filter { it.memoryId == memoryIdFlow.value }
            .onEach { event ->
                val message = when (val result = event.result) {
                    is AppResult.Success -> "화질 업그레이드가 완료되어 갤러리에 저장됐어요."
                    is AppResult.Error -> result.error.toImageEnhancementMessage()
                }
                _messages.tryEmit(message)
            }
            .launchIn(viewModelScope)
    }

    fun bind(memoryId: Long) {
        if (memoryId <= 0L) return
        if (memoryIdFlow.value != memoryId) {
            savedStateHandle[KEY_MEMORY_ID] = memoryId
            savedStateHandle[KEY_MEMO_DRAFT] = null
            savedStateHandle[KEY_TAGS_DRAFT] = null
            savedStateHandle[KEY_CATEGORIES_DRAFT] = null
        }
    }

    /** 상세 화면 노출 여부를 Repository에 알린다 (추천 완료 시 칩 vs 자동 적용 결정에 사용). */
    fun setScreenVisible(visible: Boolean) {
        repository.setViewingMemory(if (visible) memoryIdFlow.value.takeIf { it > 0L } else null)
    }

    fun onMemoDraftChanged(text: String) {
        val memory = uiState.value.memory ?: return
        val newDraft = if (text == memory.memo) null else text
        savedStateHandle[KEY_MEMO_DRAFT] = newDraft
    }

    /**
     * "저장" 클릭: 미저장 메모/태그/카테고리 변경분을 영속화한다.
     * 카테고리가 하나도 없으면 저장하지 않고 [SaveResult.NO_CATEGORY] 를 반환한다.
     */
    fun save(): SaveResult {
        val id = memoryIdFlow.value
        val memory = uiState.value.memory ?: return SaveResult.NO_CATEGORY
        if (id <= 0L) return SaveResult.NO_CATEGORY

        // 원격 처리가 끝나기 전에 화면을 빠져나가 원본 이미지가 다시 보이는 것을 막는다.
        // UI의 StateFlow 반영보다 빠르게 저장을 누르는 경우도 막도록 Repository 원본 상태를 확인한다.
        if (
            id in repository.geminiInProgress.value ||
            id in repository.imageEnhancementInProgress.value
        ) {
            return SaveResult.PROCESSING
        }

        // 카테고리는 최소 1개 필수.
        if (effectiveCategories().isEmpty()) return SaveResult.NO_CATEGORY

        val draft = memoDraftFlow.value
        if (draft != null && draft != memory.memo) {
            repository.updateMemo(id, draft)
        }
        tagsDraftFlow.value?.let { repository.updateTags(id, it) }
        categoriesDraftFlow.value?.let { repository.updateCategories(id, it.toCategories()) }
        savedStateHandle[KEY_MEMO_DRAFT] = null
        savedStateHandle[KEY_TAGS_DRAFT] = null
        savedStateHandle[KEY_CATEGORIES_DRAFT] = null
        return SaveResult.SAVED
    }

    fun toggleFavorite() {
        val id = memoryIdFlow.value
        if (id > 0L) repository.toggleFavorite(id)
    }

    /** 태그 추가는 즉시 저장하지 않고 staged draft 에만 반영한다 ("저장" 시 일괄 반영). */
    fun onAddTag(displayName: String) {
        val name = displayName.trim()
        if (name.isBlank()) return
        val current = effectiveTags()
        if (current.any { it.normalizeTag() == name.normalizeTag() }) return
        savedStateHandle[KEY_TAGS_DRAFT] = ArrayList(current + ("#" + name.removePrefix("#")))
    }

    /** 태그 제거도 staged draft 에만 반영한다 ("저장" 시 일괄 반영). */
    fun onRemoveTag(displayName: String) {
        val current = effectiveTags()
        val updated = current.filterNot { it.normalizeTag() == displayName.normalizeTag() }
        if (updated.size == current.size) return
        savedStateHandle[KEY_TAGS_DRAFT] = ArrayList(updated)
    }

    private fun effectiveTags(): List<String> =
        tagsDraftFlow.value ?: uiState.value.memory?.tags ?: emptyList()

    private fun String.normalizeTag(): String = trim().removePrefix("#").lowercase()

    /**
     * 카테고리 추가도 즉시 저장하지 않고 staged draft 에만 반영한다.
     * 이미 최대 개수면 추가하지 않고 false 를 반환한다(상한 안내용).
     */
    fun onAddCategory(category: MemoryCategory): Boolean {
        val current = effectiveCategories()
        if (category in current) return true
        if (current.size >= MemoryCategory.MAX_PER_MEMORY) return false
        savedStateHandle[KEY_CATEGORIES_DRAFT] = ArrayList((current + category).map { it.name })
        return true
    }

    /** 카테고리 제거도 staged draft 에만 반영한다. */
    fun onRemoveCategory(category: MemoryCategory) {
        val current = effectiveCategories()
        if (category !in current) return
        savedStateHandle[KEY_CATEGORIES_DRAFT] = ArrayList((current - category).map { it.name })
    }

    private fun effectiveCategories(): List<MemoryCategory> =
        categoriesDraftFlow.value?.toCategories() ?: uiState.value.memory?.categories ?: emptyList()

    private fun ArrayList<String>.toCategories(): List<MemoryCategory> =
        mapNotNull { name -> runCatching { MemoryCategory.valueOf(name) }.getOrNull() }

    /** 태그 선택 다이얼로그용 전체 태그 표시 이름 목록(예: "#travel"). */
    suspend fun allTagNames(): List<String> = repository.listAllTags().map { it.displayName }

    fun softDelete() {
        val id = memoryIdFlow.value
        if (id > 0L) repository.softDelete(id)
    }

    /** 버튼 클릭 시 현재 메모리에 대해 Gemini 메모 추천을 온디맨드로 요청한다. */
    fun requestGeminiSuggestion() {
        val id = memoryIdFlow.value
        if (id <= 0L || geminiLoading.value) return
        repository.requestGeminiSuggestion(id)
    }

    fun requestImageEnhancement() {
        val id = memoryIdFlow.value
        val memory = uiState.value.memory ?: return
        if (id <= 0L || imageEnhancementLoading.value || memory.imageUri.isNullOrBlank()) return
        repository.requestImageEnhancement(id)
    }

    private fun AppError.toUserMessage(): String = when (this) {
        AppError.RemoteFeatureDisabled -> "Gemini가 꺼져 있거나 키가 없어 추천할 수 없어요. 설정에서 Gemini를 켜 주세요."
        AppError.NetworkUnavailable -> "네트워크 연결을 확인해 주세요."
        AppError.ApiTimeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요."
        AppError.ApiUnauthorized -> "API 키가 올바르지 않아요."
        AppError.ApiQuotaExceeded -> "API 사용 한도를 초과했어요. 잠시 후 다시 시도해 주세요."
        AppError.FileNotFound -> "이미지를 불러올 수 없어 추천에 실패했어요."
        is AppError.Unknown -> message.ifBlank { "추천에 실패했어요." }
        else -> "추천에 실패했어요."
    }

    private fun AppError.toImageEnhancementMessage(): String = when (this) {
        AppError.RemoteFeatureDisabled -> "화질 업그레이드 API가 꺼져 있거나 Clipdrop 키가 없어요. 설정을 확인해 주세요."
        AppError.NetworkUnavailable -> "네트워크 연결을 확인해 주세요."
        AppError.ApiTimeout -> "업그레이드 응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요."
        AppError.ApiUnauthorized -> "Clipdrop API 키가 올바르지 않아요."
        AppError.ApiQuotaExceeded -> "Clipdrop API 사용 한도를 초과했어요."
        AppError.FileNotFound -> "이미지를 불러올 수 없어 업그레이드에 실패했어요."
        AppError.UnsupportedImageType -> "이 이미지 형식은 업그레이드를 지원하지 않아요. JPG, PNG, WebP 이미지를 사용해 주세요."
        is AppError.Http -> "화질 업그레이드 API 오류가 발생했어요. (${code})"
        is AppError.Unknown -> message.ifBlank { "화질 업그레이드에 실패했어요." }
        else -> "화질 업그레이드에 실패했어요."
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
        private const val KEY_TAGS_DRAFT = "detail.tagsDraft"
        private const val KEY_CATEGORIES_DRAFT = "detail.categoriesDraft"
    }
}
