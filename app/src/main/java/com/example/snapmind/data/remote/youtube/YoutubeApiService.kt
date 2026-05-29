package com.example.snapmind.data.remote.youtube

import com.example.snapmind.data.remote.dto.YoutubeSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface YoutubeApiService {
    @GET("youtube/v3/search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 1,
        @Query("key") apiKey: String,
    ): YoutubeSearchResponseDto
}
