package com.example.snapmind.data.remote.youtube

import com.example.snapmind.data.remote.dto.YoutubeVideosResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface YoutubeApiService {
    @GET("youtube/v3/videos")
    suspend fun getVideos(
        @Query("part") part: String = "snippet",
        @Query("id") id: String,
        @Query("key") apiKey: String,
    ): YoutubeVideosResponseDto
}
