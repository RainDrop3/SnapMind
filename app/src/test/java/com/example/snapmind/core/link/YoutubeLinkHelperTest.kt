package com.example.snapmind.core.link

import org.junit.Assert.assertEquals
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
}
