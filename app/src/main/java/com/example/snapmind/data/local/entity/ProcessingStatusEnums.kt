package com.example.snapmind.data.local.entity

enum class StandardProcessingStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
}

enum class OptionalRemoteProcessingStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    SKIPPED,
}

enum class GeminiMemoStatus {
    PENDING,
    RUNNING,
    SUGGESTED,
    ACCEPTED,
    DISMISSED,
    FAILED,
    SKIPPED,
}

enum class TagAssignmentSource {
    OCR,
    TFLITE,
    VISION,
    USER,
    SYSTEM,
}

enum class TagAssignedBy {
    AUTO,
    USER,
}
