package com.example.snapmind.core.link

import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Singleton
class UrlExtractor @Inject constructor() {

    fun firstUrl(text: String): String? =
        sequenceOf(text.collapseOcrUrlBreaks(), text)
            .distinct()
            .flatMap { searchableText -> urlRegex.findAll(searchableText) }
            .mapNotNull { match -> match.value.cleanCandidate().normalizeUrl() }
            .firstOrNull()

    fun hostLabel(url: String): String? =
        url.toHttpUrlOrNull()
            ?.host
            ?.removePrefix("www.")
            ?.takeIf { it.isNotBlank() }

    private fun String.cleanCandidate(): String =
        trim()
            .trim(*TRAILING_PUNCTUATION)
            .trimStart('(', '[', '{', '<', '"', '\'')
            .trim(*TRAILING_PUNCTUATION)

    private fun String.normalizeUrl(): String? {
        if (isBlank()) return null
        val withScheme = if (startsWith("http://", ignoreCase = true) ||
            startsWith("https://", ignoreCase = true)
        ) {
            this
        } else {
            "https://$this"
        }
        withScheme.repairNaverSearchUrlCandidate()?.let { return it }
        val parsed = withScheme.toHttpUrlOrNull() ?: return null
        return if (parsed.host.contains('.')) parsed.toString() else null
    }

    private fun String.repairNaverSearchUrlCandidate(): String? {
        val rawQuery = NAVER_SEARCH_QUERY_VALUE_REGEX.find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.cleanCandidate()
            ?.takeIf { it.isNotBlank() && !it.contains("=") }
            ?: return null
        val query = rawQuery
            .replace("%620", "%20")
            .decodeUrlQueryValue()
            .replace(Regex("\\s+"), " ")
            .trim()
            .takeIf { value -> value.any { it.isLetterOrDigit() } }
            ?: return null
        return NAVER_SEARCH_BASE
            .toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("query", query)
            ?.build()
            ?.toString()
    }

    private fun String.decodeUrlQueryValue(): String =
        runCatching { URLDecoder.decode(this, Charsets.UTF_8.name()) }.getOrDefault(this)

    private fun String.collapseOcrUrlBreaks(): String {
        var current = this
        while (true) {
            val collapsed = current
                .replace(URL_DOT_OCR_WHITESPACE, ".")
                .replace(URL_STRUCTURAL_LINE_BREAK, "")
                .replace(YOUTUBE_VIDEO_ID_LINE_BREAK) { match ->
                    match.groupValues[1] + match.groupValues[2]
                }
            if (collapsed == current) return current
            current = collapsed
        }
    }

    private companion object {
        val urlRegex = Regex(
            pattern = """(?i)\b((?:https?://|www\.)[^\s<>"']+|(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,24}(?:/[^\s<>"']*)?)""",
        )
        val URL_STRUCTURAL_LINE_BREAK = Regex(
            pattern = """(?<=[/:?#&=%+_~-])\s*\R\s*(?=[\p{L}\p{N}/:?#&=%+_~.\-])|(?<=[\p{L}\p{N}])\s*\R\s*(?=[/:?#&=%+_~.\-])""",
        )
        val URL_DOT_OCR_WHITESPACE = Regex(
            pattern = """(?<=[A-Za-z0-9])\.\s+(?=[A-Za-z0-9-]+(?:[./?#&=%:_~-]))""",
        )
        val NAVER_SEARCH_QUERY_VALUE_REGEX = Regex(
            pattern = """(?i)^https?://search\.naver\.com/search\.naver\?(.+)$""",
        )
        val YOUTUBE_VIDEO_ID_LINE_BREAK = Regex(
            pattern = """(?i)(((?:https?://)?(?:www\.|m\.)?(?:youtu\.be/|youtube\.com/(?:shorts/|embed/|live/)|youtube\.com/watch\?[^\s<>"']*?v=)[A-Za-z0-9_-]{1,10}))\s*\R\s*([A-Za-z0-9_-]{1,10})""",
        )
        const val NAVER_SEARCH_BASE = "https://search.naver.com/search.naver"
        val TRAILING_PUNCTUATION = charArrayOf(
            '.', ',', ';', ':', '!', '?', ')', ']', '}', '>', '"', '\'',
            '…', '。', '、', '，', '；', '：', '！', '？',
        )
    }
}
