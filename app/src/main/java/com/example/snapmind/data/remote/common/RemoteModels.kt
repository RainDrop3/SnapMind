package com.example.snapmind.data.remote.common

data class RemoteVisionLabel(
    val label: String,
    val score: Float,
)

data class GeminiMemoSuggestion(
    val text: String,
)

data class YoutubeVideoLink(
    val videoId: String,
    val title: String?,
    val url: String,
)
