package com.example.snapmind.core.link

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Singleton
class UrlExtractor @Inject constructor() {

    fun firstUrl(text: String): String? =
        urlRegex.findAll(text)
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
        val parsed = withScheme.toHttpUrlOrNull() ?: return null
        return if (parsed.host.contains('.')) parsed.toString() else null
    }

    private companion object {
        val urlRegex = Regex(
            pattern = """(?i)\b((?:https?://|www\.)[^\s<>"']+|(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,24}(?:/[^\s<>"']*)?)""",
        )
        val TRAILING_PUNCTUATION = charArrayOf(
            '.', ',', ';', ':', '!', '?', ')', ']', '}', '>', '"', '\'',
            '…', '。', '、', '，', '；', '：', '！', '？',
        )
    }
}
