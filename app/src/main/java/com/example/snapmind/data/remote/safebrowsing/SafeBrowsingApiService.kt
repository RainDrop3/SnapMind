package com.example.snapmind.data.remote.safebrowsing

import com.example.snapmind.data.remote.dto.SafeBrowsingFindThreatMatchesRequestDto
import com.example.snapmind.data.remote.dto.SafeBrowsingFindThreatMatchesResponseDto
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface SafeBrowsingApiService {
    @POST("v4/threatMatches:find")
    suspend fun findThreatMatches(
        @Query("key") apiKey: String,
        @Body request: SafeBrowsingFindThreatMatchesRequestDto,
    ): SafeBrowsingFindThreatMatchesResponseDto
}
