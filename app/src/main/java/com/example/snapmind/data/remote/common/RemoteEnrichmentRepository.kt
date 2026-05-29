package com.example.snapmind.data.remote.common

import com.example.snapmind.core.result.AppError
import com.example.snapmind.core.result.AppResult
import com.example.snapmind.data.remote.dto.GeminiContentDto
import com.example.snapmind.data.remote.dto.GeminiGenerateContentRequestDto
import com.example.snapmind.data.remote.dto.GeminiInlineDataDto
import com.example.snapmind.data.remote.dto.GeminiPartDto
import com.example.snapmind.data.remote.dto.VisionAnnotateRequestDto
import com.example.snapmind.data.remote.dto.VisionFeatureDto
import com.example.snapmind.data.remote.dto.VisionImageDto
import com.example.snapmind.data.remote.dto.VisionImageRequestDto
import com.example.snapmind.data.remote.gemini.GeminiApiService
import com.example.snapmind.data.remote.vision.VisionApiService
import com.example.snapmind.data.remote.youtube.YoutubeApiService
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

@Singleton
class RemoteEnrichmentRepository @Inject constructor(
    private val visionApiService: VisionApiService,
    private val geminiApiService: GeminiApiService,
    private val youtubeApiService: YoutubeApiService,
) {
    suspend fun labelImage(
        base64Jpeg: String,
        apiKey: String,
    ): AppResult<List<RemoteVisionLabel>> = runRemote(apiKey) {
        val response = visionApiService.annotate(
            apiKey = apiKey,
            request = VisionAnnotateRequestDto(
                requests = listOf(
                    VisionImageRequestDto(
                        image = VisionImageDto(content = base64Jpeg),
                        features = listOf(VisionFeatureDto(type = "LABEL_DETECTION", maxResults = 10)),
                    ),
                ),
            ),
        )
        response.responses
            .firstOrNull()
            ?.labelAnnotations
            .orEmpty()
            .mapNotNull { label ->
                val description = label.description ?: return@mapNotNull null
                val score = label.score ?: return@mapNotNull null
                if (score >= VISION_LABEL_THRESHOLD) RemoteVisionLabel(description, score) else null
            }
    }

    suspend fun suggestMemo(
        base64Jpeg: String,
        apiKey: String,
        model: String = DEFAULT_GEMINI_MODEL,
    ): AppResult<GeminiMemoSuggestion> = runRemote(apiKey) {
        val response = geminiApiService.generateContent(
            apiKey = apiKey,
            model = model,
            request = GeminiGenerateContentRequestDto(
                contents = listOf(
                    GeminiContentDto(
                        parts = listOf(
                            GeminiPartDto(text = GEMINI_MEMO_PROMPT),
                            GeminiPartDto(
                                inlineData = GeminiInlineDataDto(
                                    mimeType = "image/jpeg",
                                    data = base64Jpeg,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val text = response.candidates
            .firstOrNull()
            ?.content
            ?.parts
            ?.firstNotNullOfOrNull { it.text }
            ?.trim()
            .orEmpty()
        GeminiMemoSuggestion(text)
    }

    suspend fun findYoutubeVideo(
        title: String,
        apiKey: String,
    ): AppResult<YoutubeVideoLink?> = runRemote(apiKey) {
        val item = youtubeApiService.searchVideos(query = title, apiKey = apiKey)
            .items
            .firstOrNull()
        val videoId = item?.id?.videoId ?: return@runRemote null
        YoutubeVideoLink(
            videoId = videoId,
            title = item.snippet?.title,
            url = "https://www.youtube.com/watch?v=$videoId",
        )
    }

    private suspend fun <T> runRemote(
        apiKey: String,
        block: suspend () -> T,
    ): AppResult<T> {
        if (apiKey.isBlank()) {
            return AppResult.Error(AppError.RemoteFeatureDisabled)
        }
        return runCatching { block() }
            .fold(
                onSuccess = { AppResult.Success(it) },
                onFailure = { AppResult.Error(it.toAppError()) },
            )
    }

    private fun Throwable.toAppError(): AppError =
        when (this) {
            is SocketTimeoutException -> AppError.ApiTimeout
            is IOException -> AppError.NetworkUnavailable
            is HttpException -> when (code()) {
                401, 403 -> AppError.ApiUnauthorized
                429 -> AppError.ApiQuotaExceeded
                else -> AppError.Http(code(), response()?.errorBody()?.string())
            }
            else -> AppError.Unknown(message.orEmpty())
        }

    private companion object {
        const val VISION_LABEL_THRESHOLD = 0.80f
        const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"
        const val GEMINI_MEMO_PROMPT = "이 이미지를 저장한 이유를 한 문장(50자 이내 한국어)으로 추천해 주세요."
    }
}
