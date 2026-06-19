package com.example.snapmind.data.remote.dto

data class YoutubeVideosResponseDto(
    val items: List<YoutubeVideoItemDto> = emptyList(),
)

data class YoutubeVideoItemDto(
    val id: String? = null,
    val snippet: YoutubeVideoSnippetDto? = null,
)

data class YoutubeVideoSnippetDto(
    val title: String? = null,
    val description: String? = null,
    val channelTitle: String? = null,
    val thumbnails: YoutubeVideoThumbnailsDto? = null,
)

data class YoutubeVideoThumbnailsDto(
    val maxres: YoutubeVideoThumbnailDto? = null,
    val standard: YoutubeVideoThumbnailDto? = null,
    val high: YoutubeVideoThumbnailDto? = null,
    val medium: YoutubeVideoThumbnailDto? = null,
    val default: YoutubeVideoThumbnailDto? = null,
)

data class YoutubeVideoThumbnailDto(
    val url: String? = null,
)
