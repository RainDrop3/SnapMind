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

    fun ocrConfusionCandidateVideoIds(
        videoId: String,
        maxCandidates: Int = MAX_OCR_CONFUSION_CANDIDATES,
    ): List<String> {
        if (!videoId.matches(YOUTUBE_VIDEO_ID_REGEX) || maxCandidates <= 0) return emptyList()
        val positions = videoId.mapIndexedNotNull { index, char ->
            val alternatives = OCR_CONFUSABLES[char]?.filterNot { it == char }.orEmpty()
            if (alternatives.isEmpty()) null else index to alternatives
        }
        if (positions.isEmpty()) return emptyList()

        val results = LinkedHashSet<String>()
        for (distance in 1..positions.size) {
            collectCandidates(
                source = videoId.toCharArray(),
                positions = positions,
                positionCursor = 0,
                selectedCount = 0,
                targetCount = distance,
                maxCandidates = maxCandidates,
                results = results,
            )
            if (results.size >= maxCandidates) break
        }
        return results.toList()
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

    private fun collectCandidates(
        source: CharArray,
        positions: List<Pair<Int, List<Char>>>,
        positionCursor: Int,
        selectedCount: Int,
        targetCount: Int,
        maxCandidates: Int,
        results: LinkedHashSet<String>,
    ) {
        if (results.size >= maxCandidates) return
        if (selectedCount == targetCount) {
            results += String(source)
            return
        }
        if (positionCursor >= positions.size) return

        val remaining = positions.size - positionCursor
        if (selectedCount + remaining <= targetCount) {
            val (index, alternatives) = positions[positionCursor]
            val original = source[index]
            alternatives.forEach { alternative ->
                source[index] = alternative
                collectCandidates(
                    source = source,
                    positions = positions,
                    positionCursor = positionCursor + 1,
                    selectedCount = selectedCount + 1,
                    targetCount = targetCount,
                    maxCandidates = maxCandidates,
                    results = results,
                )
            }
            source[index] = original
            return
        }

        val (index, alternatives) = positions[positionCursor]
        val original = source[index]
        alternatives.forEach { alternative ->
            if (results.size >= maxCandidates) return@forEach
            source[index] = alternative
            collectCandidates(
                source = source,
                positions = positions,
                positionCursor = positionCursor + 1,
                selectedCount = selectedCount + 1,
                targetCount = targetCount,
                maxCandidates = maxCandidates,
                results = results,
            )
        }
        source[index] = original
        collectCandidates(
            source = source,
            positions = positions,
            positionCursor = positionCursor + 1,
            selectedCount = selectedCount,
            targetCount = targetCount,
            maxCandidates = maxCandidates,
            results = results,
        )
    }

    private companion object {
        val YOUTUBE_VIDEO_ID_REGEX = Regex("[A-Za-z0-9_-]{11}")
        val OCR_CONFUSABLES = mapOf(
            'l' to listOf('I', '1'),
            'I' to listOf('l', '1'),
            '1' to listOf('l', 'I'),
        )
        const val MAX_OCR_CONFUSION_CANDIDATES = 24
    }
}
