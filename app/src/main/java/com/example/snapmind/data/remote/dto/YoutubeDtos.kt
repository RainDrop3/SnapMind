package com.example.snapmind.data.remote.dto

data class YoutubeSearchResponseDto(
    val items: List<YoutubeSearchItemDto> = emptyList(),
)

data class YoutubeSearchItemDto(
    val id: YoutubeIdDto? = null,
    val snippet: YoutubeSnippetDto? = null,
)

data class YoutubeIdDto(
    val videoId: String? = null,
)

data class YoutubeSnippetDto(
    val title: String? = null,
    val thumbnails: YoutubeThumbnailsDto? = null,
)

data class YoutubeThumbnailsDto(
    val default: YoutubeThumbnailDto? = null,
)

data class YoutubeThumbnailDto(
    val url: String? = null,
)
