package com.example.snapmind.data.remote.image

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ClipdropImageUpscaleApiService {
    @Multipart
    @POST("image-upscaling/v1/upscale")
    suspend fun upscale(
        @Header("x-api-key") apiKey: String,
        @Part imageFile: MultipartBody.Part,
        @Part("target_width") targetWidth: RequestBody,
        @Part("target_height") targetHeight: RequestBody,
    ): Response<ResponseBody>
}
