package com.example.snapmind.core.link

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeLinkHelperTest {

    private val helper = YoutubeLinkHelper()

    @Test
    fun thumbnailUrl_supportsWatchUrl() {
        val url = "https://www.youtube.com/watch?v=abcDEF_1234"

        assertEquals(
            "https://img.youtube.com/vi/abcDEF_1234/hqdefault.jpg",
            helper.thumbnailUrl(url),
        )
    }

    @Test
    fun thumbnailUrl_supportsShortUrl() {
        val url = "https://youtu.be/abcDEF_1234?si=test"

        assertEquals(
            "https://img.youtube.com/vi/abcDEF_1234/hqdefault.jpg",
            helper.thumbnailUrl(url),
        )
    }

    @Test
    fun thumbnailUrl_supportsShortsUrl() {
        val url = "https://youtube.com/shorts/abcDEF_1234"

        assertEquals(
            "https://img.youtube.com/vi/abcDEF_1234/hqdefault.jpg",
            helper.thumbnailUrl(url),
        )
    }

    @Test
    fun watchUrl_normalizesShortUrl() {
        val url = "https://youtu.be/abcDEF_1234?si=test"

        assertEquals("https://www.youtube.com/watch?v=abcDEF_1234", helper.watchUrl(url))
    }

    @Test
    fun ocrConfusionCandidateVideoIds_replacesCommonVerticalCharacters() {
        val candidates = helper.ocrConfusionCandidateVideoIds("abcDEF_lI1Z", maxCandidates = 12)

        assertFalse(candidates.contains("abcDEF_lI1Z"))
        assertTrue(candidates.contains("abcDEF_II1Z"))
        assertTrue(candidates.contains("abcDEF_ll1Z"))
        assertTrue(candidates.contains("abcDEF_lIIZ"))
    }

    @Test
    fun ocrConfusionCandidateVideoIds_respectsCandidateLimit() {
        val candidates = helper.ocrConfusionCandidateVideoIds("lI1lI1lI1lI", maxCandidates = 4)

        assertEquals(4, candidates.size)
    }

    @Test
    fun ocrConfusionCandidateVideoIds_ignoresInvalidVideoId() {
        val candidates = helper.ocrConfusionCandidateVideoIds("short", maxCandidates = 4)

        assertEquals(emptyList<String>(), candidates)
    }
}
