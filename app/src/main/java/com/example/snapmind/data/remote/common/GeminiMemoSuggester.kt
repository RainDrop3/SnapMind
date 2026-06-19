package com.example.snapmind.data.remote.common

import androidx.core.net.toUri
import com.example.snapmind.core.image.RemoteImageEncoder
import com.example.snapmind.core.result.AppError
import com.example.snapmind.core.result.AppResult
import com.example.snapmind.core.settings.AppPreferences
import com.example.snapmind.data.local.dao.MemoryItemDao
import com.example.snapmind.data.local.entity.GeminiMemoStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 상세 화면 버튼에서 호출하는 온디맨드 Gemini 메모 추천.
 * 네트워크 호출과 RUNNING/FAILED/SKIPPED 상태 전이만 담당하고, 성공 시 추천 텍스트를 반환한다.
 * 성공 결과를 어떻게 반영할지(칩 노출 vs 메모 자동 적용)는 호출자(Repository)가 화면 노출 여부에 따라 결정한다.
 * (import 시 자동 추천은 비활성화되어 있고, 이 경로로만 추천이 생성된다.)
 */
@Singleton
class GeminiMemoSuggester @Inject constructor(
    private val memoryItemDao: MemoryItemDao,
    private val remoteRepository: RemoteEnrichmentRepository,
    private val prefs: AppPreferences,
    private val imageEncoder: RemoteImageEncoder,
) {
    suspend fun suggest(memoryId: Long): AppResult<String> {
        val flags = prefs.current()
        if (!flags.geminiEnabled || flags.geminiApiKey.isBlank()) {
            return AppResult.Error(AppError.RemoteFeatureDisabled)
        }
        val entity = memoryItemDao.getById(memoryId) ?: return AppResult.Error(AppError.FileNotFound)
        val now = { System.currentTimeMillis() }

        memoryItemDao.setGeminiMemoStatus(memoryId, GeminiMemoStatus.RUNNING, now())
        val base64Jpeg = imageEncoder.encodeBase64Jpeg(entity.imageUri.toUri())
        if (base64Jpeg == null) {
            memoryItemDao.setGeminiMemoStatus(memoryId, GeminiMemoStatus.FAILED, now())
            return AppResult.Error(AppError.FileNotFound)
        }

        return when (val result = remoteRepository.suggestMemo(base64Jpeg, flags.geminiApiKey)) {
            is AppResult.Success -> {
                val suggestion = sanitizeGeminiMemoSuggestion(result.data.text)
                if (suggestion.isBlank()) {
                    memoryItemDao.setGeminiMemoStatus(memoryId, GeminiMemoStatus.SKIPPED, now())
                    AppResult.Error(AppError.Unknown("추천할 내용을 찾지 못했어요."))
                } else {
                    // 상태(SUGGESTED/ACCEPTED)와 영속화는 호출자가 화면 노출 여부에 따라 마무리한다.
                    AppResult.Success(suggestion)
                }
            }
            is AppResult.Error -> {
                memoryItemDao.setGeminiMemoStatus(memoryId, GeminiMemoStatus.FAILED, now())
                result
            }
        }
    }
}

internal fun sanitizeGeminiMemoSuggestion(raw: String): String {
    var cleaned = raw
        .replace("\r", "\n")
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(separator = " ")
        .trim()
    cleaned = cleaned.replace(Regex("^[-*\\d.)\\s]+"), "")
    cleaned = cleaned.replace(Regex("^(추천|메모|저장 이유)\\s*[:：]\\s*"), "")
    cleaned = cleaned.replace(Regex("\\s*[\\(（]\\s*(?:약\\s*)?\\d+\\s*자\\s*[\\)）]\\s*$"), "")
    cleaned = cleaned.trim().trimSurroundingQuotes()
    cleaned = cleaned.replace(Regex("[*_`]+"), "").trim()
    cleaned = cleaned.replace(Regex("\\s*[\\(（]\\s*(?:약\\s*)?\\d+\\s*자\\s*[\\)）]\\s*$"), "")
    return cleaned.trim().trimSurroundingQuotes()
}

private fun String.trimSurroundingQuotes(): String =
    trim()
        .removeSurrounding("\"")
        .removeSurrounding("'")
        .removeSurrounding("“", "”")
        .removeSurrounding("‘", "’")
