package com.example.snapmind.data.remote.dto

data class VisionAnnotateRequestDto(
    val requests: List<VisionImageRequestDto>,
)

data class VisionImageRequestDto(
    val image: VisionImageDto,
    val features: List<VisionFeatureDto>,
)

data class VisionImageDto(
    val content: String,
)

data class VisionFeatureDto(
    val type: String,
    val maxResults: Int,
)

data class VisionAnnotateResponseDto(
    val responses: List<VisionImageResponseDto> = emptyList(),
)

data class VisionImageResponseDto(
    val labelAnnotations: List<VisionLabelAnnotationDto> = emptyList(),
)

data class VisionLabelAnnotationDto(
    val description: String? = null,
    val score: Float? = null,
)
