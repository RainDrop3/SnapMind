package com.example.snapmind.data.remote.vision

import com.example.snapmind.data.remote.dto.VisionAnnotateRequestDto
import com.example.snapmind.data.remote.dto.VisionAnnotateResponseDto
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface VisionApiService {
    @POST("v1/images:annotate")
    suspend fun annotate(
        @Query("key") apiKey: String,
        @Body request: VisionAnnotateRequestDto,
    ): VisionAnnotateResponseDto
}
