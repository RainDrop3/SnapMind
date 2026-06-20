package com.example.snapmind.data.remote.dto

data class SafeBrowsingFindThreatMatchesRequestDto(
    val client: SafeBrowsingClientDto,
    val threatInfo: SafeBrowsingThreatInfoDto,
)

data class SafeBrowsingClientDto(
    val clientId: String,
    val clientVersion: String,
)

data class SafeBrowsingThreatInfoDto(
    val threatTypes: List<String>,
    val platformTypes: List<String>,
    val threatEntryTypes: List<String>,
    val threatEntries: List<SafeBrowsingThreatEntryDto>,
)

data class SafeBrowsingThreatEntryDto(
    val url: String,
)

data class SafeBrowsingFindThreatMatchesResponseDto(
    val matches: List<SafeBrowsingThreatMatchDto> = emptyList(),
)

data class SafeBrowsingThreatMatchDto(
    val threatType: String? = null,
    val platformType: String? = null,
    val threatEntryType: String? = null,
    val threat: SafeBrowsingThreatEntryDto? = null,
)
