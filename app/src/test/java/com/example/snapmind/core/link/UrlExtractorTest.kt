package com.example.snapmind.core.link

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlExtractorTest {

    private val extractor = UrlExtractor()

    @Test
    fun firstUrl_normalizesBareDomainAndTrimsPunctuation() {
        val text = "참고 링크는 (example.com/path?q=1). 입니다"

        assertEquals("https://example.com/path?q=1", extractor.firstUrl(text))
    }

    @Test
    fun firstUrl_keepsHttpsUrl() {
        val text = "Docs: https://developer.android.com/guide/topics/ui"

        assertEquals("https://developer.android.com/guide/topics/ui", extractor.firstUrl(text))
    }

    @Test
    fun hostLabel_removesCommonWwwPrefix() {
        assertEquals("example.com", extractor.hostLabel("https://www.example.com/article"))
    }

    @Test
    fun firstUrl_supportsYoutubeShortHost() {
        val text = "다시 보기 youtu.be/abcDEF_1234"

        assertEquals("https://youtu.be/abcDEF_1234", extractor.firstUrl(text))
    }

    @Test
    fun firstUrl_joinsLineBreakAfterUrlSeparator() {
        val text = "다시 보기 https://youtu.be/\nabcDEF_1234"

        assertEquals("https://youtu.be/abcDEF_1234", extractor.firstUrl(text))
    }

    @Test
    fun firstUrl_joinsYoutubeVideoIdSplitAcrossLines() {
        val text = "영상 링크 https://www.youtube.com/watch?v=abcDEF_\n1234"

        assertEquals("https://www.youtube.com/watch?v=abcDEF_1234", extractor.firstUrl(text))
    }

    @Test
    fun firstUrl_joinsOcrSpaceInsideNaverSearchPath() {
        val url = "https://search.naver.com/search.naver?where=nexearch&sm=top_clk.splogo&query=%EB%8C%80%ED%95%9C%EB%AF%BC%EA%B5%AD%20%EB%A9%95%EC%8B%9C%EC%BD%94%20%EC%9B%94%EB%93%9C%EC%BB%B5"
        val text = "네이버 검색 https://search.naver.com/search. naver?where=nexearch&sm=top_clk.splogo&query=%EB%8C%80%ED%95%9C%EB%AF%BC%EA%B5%AD%20%EB%A9%95%EC%8B%9C%EC%BD%94%20%EC%9B%94%EB%93%9C%EC%BB%B5"

        assertEquals(url, extractor.firstUrl(text))
    }

    @Test
    fun firstUrl_joinsLineBreakAfterQuestionMarkBeforeKoreanQuery() {
        val text = "네이버 검색 https://search.naver.com/search.naver?\n대한민국멕시코월드컵"

        assertEquals(
            "https://search.naver.com/search.naver?query=%EB%8C%80%ED%95%9C%EB%AF%BC%EA%B5%AD%EB%A9%95%EC%8B%9C%EC%BD%94%EC%9B%94%EB%93%9C%EC%BB%B5",
            extractor.firstUrl(text),
        )
    }

    @Test
    fun firstUrl_joinsLineBreakAfterQueryEqualsBeforeKoreanQuery() {
        val text = "네이버 검색 https://search.naver.com/search.naver?query=\n대한민국멕시코월드컵"

        assertEquals(
            "https://search.naver.com/search.naver?query=%EB%8C%80%ED%95%9C%EB%AF%BC%EA%B5%AD%EB%A9%95%EC%8B%9C%EC%BD%94%EC%9B%94%EB%93%9C%EC%BB%B5",
            extractor.firstUrl(text),
        )
    }

    @Test
    fun firstUrl_repairsOcrFragmentedNaverSearchUrl() {
        val text = """
            11:47
            2:15 / 10:03
            댓글 1.2천
            민준
            성훈
            푸 채널 이름님이 고정함
            @footbal_kr . 2일 전
            대한민국 멕시코전 풀경기 다시 보기
            https://search. naver.com/search. naver?
            대한민국%620멕시코%20월드컵
            142
            wherenexearch &sm top_clk.splogo&query 그
            답글 7개
            @user-mj8xq7sj9d . 1일 전
            정말 잊지 못할 경기였습니다...
        """.trimIndent()

        assertEquals(
            "https://search.naver.com/search.naver?query=%EB%8C%80%ED%95%9C%EB%AF%BC%EA%B5%AD%20%EB%A9%95%EC%8B%9C%EC%BD%94%20%EC%9B%94%EB%93%9C%EC%BB%B5",
            extractor.firstUrl(text),
        )
    }

    @Test
    fun firstUrl_doesNotSwallowNextSentenceAfterTerminalPeriod() {
        val text = "참고 링크 https://example.com.\nNext sentence"

        assertEquals("https://example.com/", extractor.firstUrl(text))
    }

    @Test
    fun firstUrl_doesNotSwallowNextSentenceAfterTerminalPeriodAndSpace() {
        val text = "참고 링크 https://example.com. Next sentence"

        assertEquals("https://example.com/", extractor.firstUrl(text))
    }
}
