package com.example.snapmind.data.remote.common

import org.junit.Assert.assertEquals
import org.junit.Test

class GeminiMemoSuggesterTest {
    @Test
    fun `sanitizeGeminiMemoSuggestion removes markdown and character count`() {
        val raw = "**재미있는 영상을 나중에 보려고 저장했습니다.** (25자)"

        assertEquals(
            "재미있는 영상을 나중에 보려고 저장했습니다.",
            sanitizeGeminiMemoSuggestion(raw),
        )
    }

    @Test
    fun `sanitizeGeminiMemoSuggestion removes labels and quotes`() {
        val raw = "추천: \"나중에 참고하려고 저장했습니다.\""

        assertEquals(
            "나중에 참고하려고 저장했습니다.",
            sanitizeGeminiMemoSuggestion(raw),
        )
    }
}
