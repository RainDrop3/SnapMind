package com.example.snapmind.core.link

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Singleton
class YoutubeLinkHelper @Inject constructor() {

    fun thumbnailUrl(url: String): String? {
        val videoId = videoId(url) ?: return null
        return "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
    }

    fun watchUrl(url: String): String? {
        val videoId = videoId(url) ?: return null
        return "https://www.youtube.com/watch?v=$videoId"
    }

    fun videoId(url: String): String? {
        val parsed = url.toHttpUrlOrNull() ?: return null
        val host = parsed.host.removePrefix("www.").removePrefix("m.")
        return when {
            host == "youtu.be" -> parsed.pathSegments.firstOrNull()
            host == "youtube.com" || host.endsWith(".youtube.com") -> {
                parsed.queryParameter("v")
                    ?: parsed.pathSegments.videoIdAfter("shorts")
                    ?: parsed.pathSegments.videoIdAfter("embed")
                    ?: parsed.pathSegments.videoIdAfter("live")
            }
            else -> null
        }?.takeIf { it.matches(YOUTUBE_VIDEO_ID_REGEX) }
    }

    private fun List<String>.videoIdAfter(segment: String): String? {
        val index = indexOf(segment)
        return if (index >= 0) getOrNull(index + 1) else null
    }

    private companion object {
        val YOUTUBE_VIDEO_ID_REGEX = Regex("[A-Za-z0-9_-]{11}")
    }
}
