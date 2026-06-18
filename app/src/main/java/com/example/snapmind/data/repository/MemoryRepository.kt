package com.example.snapmind.data.repository

import android.net.Uri
import com.example.snapmind.core.result.AppResult
import com.example.snapmind.data.model.CategoryCount
import com.example.snapmind.data.model.MemoryCategory
import com.example.snapmind.data.model.MemoryItem
import com.example.snapmind.data.model.TagCount
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** 온디맨드 Gemini 추천 1회 완료 이벤트(상세 화면 토스트용). */
data class GeminiSuggestionEvent(val memoryId: Long, val result: AppResult<Unit>)

interface MemoryRepository {
    val memories: StateFlow<List<MemoryItem>>

    /** 현재 추천 요청이 진행 중인 메모리 id 집합(버튼 비활성·로딩 표시용). 화면 재생성과 무관하게 유지된다. */
    val geminiInProgress: StateFlow<Set<Long>>

    /** 추천 완료(성공/실패) 이벤트 스트림. 화면에 떠 있는 구독자만 소비한다. */
    val geminiEvents: SharedFlow<GeminiSuggestionEvent>

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
        initialMemo: String = "",
        initialTags: List<String> = emptyList(),
    ): AppResult<MemoryItem>

    fun toggleFavorite(memoryId: Long)
    fun updateMemo(memoryId: Long, memo: String)

    /** 상세 화면에서 메모리에 태그를 추가/삭제. */
    suspend fun addTagToMemory(memoryId: Long, tagName: String)
    suspend fun removeTagFromMemory(memoryId: Long, tagName: String)

    /** 상세 화면 "저장" 시 호출: 목표 태그 목록과 현재 상태의 차이만 반영한다. */
    fun updateTags(memoryId: Long, tags: List<String>)

    /** 상세 화면 "저장" 시 호출: 사용자가 지정한 카테고리(최대 2개)로 분류를 덮어쓴다. */
    fun updateCategories(memoryId: Long, categories: List<MemoryCategory>)

    /** 태그 선택·관리 화면용 전체 태그 목록(아카이브 제외, 미사용 태그 포함). */
    suspend fun listAllTags(): List<TagCount>

    /** 태그 관리 화면에서 전역 태그를 생성/삭제. */
    suspend fun createTag(tagName: String): Boolean
    suspend fun deleteTag(tagName: String)

    /** 상세 화면 버튼에서 호출하는 온디맨드 Gemini 추천. 화면을 나가도 백그라운드에서 끝까지 처리된다. */
    fun requestGeminiSuggestion(memoryId: Long)

    /** 현재 상세 화면에 떠 있는 메모리 id(없으면 null). 추천 완료 시 칩 vs 자동 적용 결정에 사용. */
    fun setViewingMemory(memoryId: Long?)

    fun acceptGeminiSuggestion(memoryId: Long)
    fun dismissGeminiSuggestion(memoryId: Long)
    fun softDelete(memoryId: Long)
    fun restore(memoryId: Long)
    suspend fun permanentDelete(memoryId: Long): AppResult<Unit>
    suspend fun searchFts(query: String): List<MemoryItem>
    suspend fun exportToPdf(memoryIds: List<Long>): AppResult<Uri>
}
