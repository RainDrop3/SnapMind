package com.example.snapmind.data.remote.common

import com.example.snapmind.core.coroutine.DispatcherProvider
import com.example.snapmind.core.result.AppError
import com.example.snapmind.core.result.AppResult
import com.example.snapmind.data.model.LinkSafetyStatus
import com.example.snapmind.data.remote.dto.GeminiContentDto
import com.example.snapmind.data.remote.dto.GeminiGenerateContentRequestDto
import com.example.snapmind.data.remote.dto.GeminiInlineDataDto
import com.example.snapmind.data.remote.dto.GeminiPartDto
import com.example.snapmind.data.remote.dto.SafeBrowsingClientDto
import com.example.snapmind.data.remote.dto.SafeBrowsingFindThreatMatchesRequestDto
import com.example.snapmind.data.remote.dto.SafeBrowsingThreatEntryDto
import com.example.snapmind.data.remote.dto.SafeBrowsingThreatInfoDto
import com.example.snapmind.data.remote.gemini.GeminiApiService
import com.example.snapmind.data.remote.safebrowsing.SafeBrowsingApiService
import com.example.snapmind.data.remote.youtube.YoutubeApiService
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import retrofit2.HttpException

@Singleton
class RemoteEnrichmentRepository @Inject constructor(
    private val geminiApiService: GeminiApiService,
    private val youtubeApiService: YoutubeApiService,
    private val safeBrowsingApiService: SafeBrowsingApiService,
    private val okHttpClient: OkHttpClient,
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend fun fetchYoutubeVideo(
        videoId: String,
        apiKey: String,
    ): AppResult<RemoteLinkPreview?> = runRemote(apiKey) {
        val item = youtubeApiService.getVideos(id = videoId, apiKey = apiKey)
            .items
            .firstOrNull()
            ?: return@runRemote null
        val snippet = item.snippet
        RemoteLinkPreview(
            url = "https://www.youtube.com/watch?v=${item.id ?: videoId}",
            title = snippet?.title,
            description = snippet?.description,
            imageUrl = snippet?.thumbnails?.bestUrl(),
            siteName = snippet?.channelTitle ?: "YouTube",
        )
    }

    suspend fun checkLinkSafety(
        url: String,
        apiKey: String,
    ): AppResult<RemoteLinkSafety> = runRemote(apiKey) {
        val response = safeBrowsingApiService.findThreatMatches(
            apiKey = apiKey,
            request = SafeBrowsingFindThreatMatchesRequestDto(
                client = SafeBrowsingClientDto(
                    clientId = SAFE_BROWSING_CLIENT_ID,
                    clientVersion = SAFE_BROWSING_CLIENT_VERSION,
                ),
                threatInfo = SafeBrowsingThreatInfoDto(
                    threatTypes = SAFE_BROWSING_THREAT_TYPES,
                    platformTypes = listOf("ANY_PLATFORM"),
                    threatEntryTypes = listOf("URL"),
                    threatEntries = listOf(SafeBrowsingThreatEntryDto(url = url)),
                ),
            ),
        )
        val threatTypes = response.matches.mapNotNull { it.threatType }.distinct()
        if (threatTypes.isEmpty()) {
            RemoteLinkSafety(status = LinkSafetyStatus.SAFE)
        } else {
            RemoteLinkSafety(status = LinkSafetyStatus.UNSAFE, threatTypes = threatTypes)
        }
    }

    suspend fun fetchLinkPreview(url: String): AppResult<RemoteLinkPreview> = runRemote {
        withContext(dispatcherProvider.io) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml")
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw HttpStatusException(response.code, response.message)
                }
                val finalUrl = response.request.url.toString()
                val body = response.body ?: return@withContext finalUrl.minimalPreview()
                val mediaType = body.contentType()
                if (mediaType != null && mediaType.type != "text" && !mediaType.subtype.contains("html")) {
                    return@withContext finalUrl.minimalPreview()
                }
                val html = body.readLimitedString(MAX_HTML_BYTES)
                if (html.isBlank()) return@withContext finalUrl.minimalPreview()
                Jsoup.parse(html, finalUrl).toLinkPreview(openUrl = url, finalUrl = finalUrl)
            }
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

    private suspend fun <T> runRemote(
        apiKey: String,
        block: suspend () -> T,
    ): AppResult<T> {
        if (apiKey.isBlank()) {
            return AppResult.Error(AppError.RemoteFeatureDisabled)
        }
        return runRemote(block)
    }

    private suspend fun <T> runRemote(block: suspend () -> T): AppResult<T> =
        runCatching { block() }
            .fold(
                onSuccess = { AppResult.Success(it) },
                onFailure = { AppResult.Error(it.toAppError()) },
            )

    private fun okhttp3.ResponseBody.readLimitedString(maxBytes: Long): String {
        val charset = contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
        val source = source()
        source.request(maxBytes)
        val buffer = source.buffer.clone()
        val byteCount = minOf(buffer.size, maxBytes)
        return buffer.readString(byteCount, charset)
    }

    private fun Document.toLinkPreview(openUrl: String, finalUrl: String): RemoteLinkPreview {
        val canonicalUrl = selectFirst("link[rel=canonical]")
            ?.absUrl("href")
            ?.clean(MAX_URL_LENGTH)
            ?: finalUrl

        return RemoteLinkPreview(
            url = openUrl,
            title = firstMetaContent(
                MAX_TITLE_LENGTH,
                "meta[property=og:title]",
                "meta[name=twitter:title]",
            ) ?: title().clean(MAX_TITLE_LENGTH) ?: canonicalUrl.hostLabel(),
            description = firstMetaContent(
                MAX_DESCRIPTION_LENGTH,
                "meta[property=og:description]",
                "meta[name=description]",
                "meta[name=twitter:description]",
            ),
            imageUrl = firstMetaUrl(
                "meta[property=og:image]",
                "meta[property=og:image:url]",
                "meta[name=twitter:image]",
                "link[rel=image_src]",
            ),
            siteName = firstMetaContent(MAX_TITLE_LENGTH, "meta[property=og:site_name]") ?: canonicalUrl.hostLabel(),
        )
    }

    private fun Document.firstMetaContent(maxLength: Int, vararg selectors: String): String? =
        selectors.firstNotNullOfOrNull { selector ->
            selectFirst(selector)
                ?.attr("content")
                ?.clean(maxLength)
        }

    private fun Document.firstMetaUrl(vararg selectors: String): String? =
        selectors.firstNotNullOfOrNull { selector ->
            val element = selectFirst(selector) ?: return@firstNotNullOfOrNull null
            element.attrUrl().clean(MAX_URL_LENGTH)
        }

    private fun Element.attrUrl(): String {
        val attr = if (tagName() == "link") "href" else "content"
        return absUrl(attr).ifBlank { attr(attr) }
    }

    private fun com.example.snapmind.data.remote.dto.YoutubeVideoThumbnailsDto.bestUrl(): String? =
        maxres?.url ?: standard?.url ?: high?.url ?: medium?.url ?: default?.url

    private fun String.minimalPreview(): RemoteLinkPreview =
        RemoteLinkPreview(
            url = this,
            title = hostLabel(),
            description = null,
            imageUrl = null,
            siteName = hostLabel(),
        )

    private fun String.hostLabel(): String? =
        toHttpUrlOrNull()
            ?.host
            ?.removePrefix("www.")
            ?.clean(MAX_TITLE_LENGTH)

    private fun String?.clean(maxLength: Int): String? {
        val cleaned = this
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return cleaned.take(maxLength)
    }

    private fun Throwable.toAppError(): AppError =
        when (this) {
            is SocketTimeoutException -> AppError.ApiTimeout
            is HttpException -> when (code()) {
                401, 403 -> AppError.ApiUnauthorized
                429 -> AppError.ApiQuotaExceeded
                else -> AppError.Http(code(), response()?.errorBody()?.string())
            }
            is HttpStatusException -> when (code) {
                401, 403 -> AppError.ApiUnauthorized
                429 -> AppError.ApiQuotaExceeded
                else -> AppError.Http(code, message)
            }
            is IOException -> AppError.NetworkUnavailable
            else -> AppError.Unknown(message.orEmpty())
        }

    private class HttpStatusException(
        val code: Int,
        override val message: String?,
    ) : IOException(message)

    private companion object {
        const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"
        const val GEMINI_MEMO_PROMPT = "이 이미지를 저장한 이유를 한 문장(50자 이내 한국어)으로 추천해 주세요."
        const val MAX_HTML_BYTES = 512_000L
        const val MAX_TITLE_LENGTH = 180
        const val MAX_DESCRIPTION_LENGTH = 300
        const val MAX_URL_LENGTH = 2_048
        const val USER_AGENT = "SnapMind/1.0 link-preview"
        const val SAFE_BROWSING_CLIENT_ID = "snapmind"
        const val SAFE_BROWSING_CLIENT_VERSION = "1.0"
        val SAFE_BROWSING_THREAT_TYPES = listOf(
            "MALWARE",
            "SOCIAL_ENGINEERING",
            "UNWANTED_SOFTWARE",
            "POTENTIALLY_HARMFUL_APPLICATION",
        )
    }
}
