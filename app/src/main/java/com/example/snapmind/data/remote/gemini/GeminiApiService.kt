package com.example.snapmind.data.remote.gemini

import com.example.snapmind.data.remote.dto.GeminiGenerateContentRequestDto
import com.example.snapmind.data.remote.dto.GeminiGenerateContentResponseDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Header("x-goog-api-key") apiKey: String,
        @Path("model") model: String,
        @Body request: GeminiGenerateContentRequestDto,
    ): GeminiGenerateContentResponseDto
}
