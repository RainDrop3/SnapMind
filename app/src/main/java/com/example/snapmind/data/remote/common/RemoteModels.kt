package com.example.snapmind.data.remote.common

data class GeminiMemoSuggestion(
    val text: String,
)

data class RemoteLinkPreview(
    val url: String,
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val siteName: String?,
)

data class RemoteLinkSafety(
    val status: String,
    val threatTypes: List<String> = emptyList(),
)
