package com.example.snapmind.data.remote.dto

data class GeminiGenerateContentRequestDto(
    val contents: List<GeminiContentDto>,
)

data class GeminiContentDto(
    val parts: List<GeminiPartDto>,
)

data class GeminiPartDto(
    val text: String? = null,
    val inlineData: GeminiInlineDataDto? = null,
)

data class GeminiInlineDataDto(
    val mimeType: String,
    val data: String,
)

data class GeminiGenerateContentResponseDto(
    val candidates: List<GeminiCandidateDto> = emptyList(),
)

data class GeminiCandidateDto(
    val content: GeminiContentDto? = null,
)
