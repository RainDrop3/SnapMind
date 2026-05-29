package com.example.snapmind.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.example.snapmind.core.coroutine.DispatcherProvider
import com.example.snapmind.core.result.AppError
import com.example.snapmind.core.result.AppResult
import com.example.snapmind.data.model.CategoryCount
import com.example.snapmind.data.model.MemoryCategory
import com.example.snapmind.data.model.MemoryItem
import com.example.snapmind.data.model.ProcessingStatus
import com.example.snapmind.data.model.TagCount
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryMemoryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
) : MemoryRepository {
    private val _memories = MutableStateFlow(seedMemories())
    override val memories: StateFlow<List<MemoryItem>> = _memories.asStateFlow()

    override fun getMemory(memoryId: Long): MemoryItem? =
        _memories.value.firstOrNull { it.id == memoryId }

    override fun activeMemories(): List<MemoryItem> =
        _memories.value.filterNot { it.isDeleted }.sortedByDescending { it.createdAtMillis }

    override fun favoriteMemories(): List<MemoryItem> =
        activeMemories().filter { it.isFavorite }

    override fun trashedMemories(): List<MemoryItem> =
        _memories.value.filter { it.isDeleted }.sortedByDescending { it.deletedAtMillis ?: 0L }

    override fun topTags(limit: Int): List<TagCount> =
        activeMemories()
            .flatMap { it.tags }
            .groupingBy { it.normalizeTag() }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(limit)
            .map { TagCount(name = it.key, displayName = "#${it.key}", count = it.value) }

    override fun categoryCounts(): List<CategoryCount> =
        activeMemories()
            .groupingBy { it.category }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<MemoryCategory, Int>> { it.value }.thenBy { it.key.displayName })
            .map { CategoryCount(category = it.key, count = it.value) }

    override fun tags(): List<TagCount> = topTags(Int.MAX_VALUE)

    override fun searchMemories(
        query: String,
        tagName: String?,
        category: MemoryCategory?,
    ): List<MemoryItem> {
        val normalizedQuery = query.trim()
        val normalizedTag = tagName?.normalizeTag()
        return activeMemories().filter { memory ->
            val matchesQuery = normalizedQuery.isBlank() ||
                memory.memo.contains(normalizedQuery, ignoreCase = true) ||
                memory.ocrText.contains(normalizedQuery, ignoreCase = true) ||
                memory.category.displayName.contains(normalizedQuery, ignoreCase = true) ||
                memory.tags.any { it.contains(normalizedQuery, ignoreCase = true) } ||
                memory.youtubeTitle?.contains(normalizedQuery, ignoreCase = true) == true
            val matchesTag = normalizedTag == null || memory.tags.any { it.normalizeTag() == normalizedTag }
            val matchesCategory = category == null || memory.category == category
            matchesQuery && matchesTag && matchesCategory
        }
    }

    override fun filterByTag(tagName: String?): List<MemoryItem> {
        val normalized = tagName?.normalizeTag()
        return activeMemories().filter { memory ->
            normalized == null || memory.tags.any { it.normalizeTag() == normalized }
        }
    }

    override fun filterByCategory(category: MemoryCategory?): List<MemoryItem> =
        activeMemories().filter { category == null || it.category == category }

    override suspend fun importImage(
        sourceUri: Uri,
        mimeType: String?,
        sourceLabel: String,
    ): AppResult<MemoryItem> = withContext(dispatcherProvider.io) {
        val resolvedMimeType = mimeType ?: context.contentResolver.getType(sourceUri)
        if (resolvedMimeType?.startsWith("image/") != true) {
            return@withContext AppResult.Error(AppError.UnsupportedImageType)
        }

        val nextId = ((_memories.value.maxOfOrNull { it.id } ?: 0L) + 1L)
        val importedDir = File(context.filesDir, "imported").apply { mkdirs() }
        val targetFile = File(importedDir, "memory_${System.currentTimeMillis()}_$nextId.${resolvedMimeType.fileExtension()}")

        runCatching {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext AppResult.Error(AppError.FileNotFound)
        }.onFailure { error ->
            return@withContext AppResult.Error(AppError.Unknown(error.message.orEmpty()))
        }

        val imported = MemoryItem(
            id = nextId,
            imageUri = targetFile.toUri().toString(),
            sourceLabel = sourceLabel,
            category = MemoryCategory.UNKNOWN,
            memo = "새 이미지 분석을 준비 중입니다.",
            ocrText = "",
            tags = listOf("#Imported"),
            createdAtMillis = System.currentTimeMillis(),
            processingStatus = ProcessingStatus.PROCESSING,
        )
        _memories.value = listOf(imported) + _memories.value
        AppResult.Success(imported)
    }

    override fun toggleFavorite(memoryId: Long) {
        _memories.value = _memories.value.map { item ->
            if (item.id == memoryId) item.copy(isFavorite = !item.isFavorite) else item
        }
    }

    override fun updateMemo(memoryId: Long, memo: String) {
        updateMemory(memoryId) { it.copy(memo = memo) }
    }

    override fun acceptGeminiSuggestion(memoryId: Long) {
        updateMemory(memoryId) { item ->
            val suggestion = item.geminiSuggestion ?: return@updateMemory item
            item.copy(memo = suggestion, geminiSuggestion = null)
        }
    }

    override fun dismissGeminiSuggestion(memoryId: Long) {
        updateMemory(memoryId) { it.copy(geminiSuggestion = null) }
    }

    override fun softDelete(memoryId: Long) {
        updateMemory(memoryId) { it.copy(deletedAtMillis = System.currentTimeMillis()) }
    }

    override fun restore(memoryId: Long) {
        updateMemory(memoryId) { it.copy(deletedAtMillis = null) }
    }

    private fun updateMemory(memoryId: Long, transform: (MemoryItem) -> MemoryItem) {
        _memories.value = _memories.value.map { item ->
            if (item.id == memoryId) transform(item) else item
        }
    }

    private fun String.normalizeTag(): String = trim().removePrefix("#").lowercase()

    private fun String.fileExtension(): String =
        when (substringAfter("/", missingDelimiterValue = "jpeg").lowercase()) {
            "png" -> "png"
            "webp" -> "webp"
            else -> "jpg"
        }

    private fun seedMemories(): List<MemoryItem> {
        val now = System.currentTimeMillis()
        return listOf(
            MemoryItem(
                id = 1,
                category = MemoryCategory.CODE,
                memo = "React hydration error - 아침 회의 전에 원인 확인",
                ocrText = "Hydration failed because the initial UI does not match.",
                tags = listOf("#React_Error", "#BugFix"),
                createdAtMillis = now - 2 * HOUR,
                processingStatus = ProcessingStatus.PROCESSING,
                isFavorite = true,
                geminiSuggestion = "내일 회의 전 수정할 React 오류를 저장했습니다.",
            ),
            MemoryItem(
                id = 2,
                category = MemoryCategory.SHOPPING,
                memo = "흰색 운동화 사이즈 280 재입고 확인",
                ocrText = "White sneakers size 10 availability",
                tags = listOf("#To_buy_later"),
                createdAtMillis = now - 5 * HOUR,
                processingStatus = ProcessingStatus.DONE,
            ),
            MemoryItem(
                id = 3,
                category = MemoryCategory.MAP,
                memo = "강남 카페 위치 - 주말에 방문",
                ocrText = "Gangnam coffee shop",
                tags = listOf("#Weekend", "#Cafe"),
                createdAtMillis = now - 26 * HOUR,
                processingStatus = ProcessingStatus.DONE,
                isFavorite = true,
            ),
            MemoryItem(
                id = 4,
                category = MemoryCategory.RECEIPT,
                memo = "점심 영수증 48,500원 - 비용 처리",
                ocrText = "Business lunch receipt 48,500 KRW",
                tags = listOf("#Expense", "#Receipt"),
                createdAtMillis = now - 30 * HOUR,
                processingStatus = ProcessingStatus.ERROR,
            ),
            MemoryItem(
                id = 5,
                category = MemoryCategory.CHAT,
                memo = "프로젝트 제안 답장 금요일까지",
                ocrText = "Project proposal reply by Friday",
                tags = listOf("#Action_Required"),
                createdAtMillis = now - 2 * DAY,
                processingStatus = ProcessingStatus.DONE,
                isFavorite = true,
            ),
            MemoryItem(
                id = 6,
                category = MemoryCategory.YOUTUBE,
                memo = "Compose 애니메이션 강의 다시 보기",
                ocrText = "Jetpack Compose animation tutorial",
                tags = listOf("#Study", "#Android"),
                createdAtMillis = now - 3 * DAY,
                processingStatus = ProcessingStatus.DONE,
                youtubeTitle = "Jetpack Compose animation tutorial",
                youtubeUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            ),
        )
    }

    private companion object {
        const val HOUR = 60 * 60 * 1000L
        const val DAY = 24 * HOUR
    }
}
