package com.tiktok.stickermaker.network

import retrofit2.http.Body
import retrofit2.http.POST

data class ResolveRequest(val url: String)
data class ResolveResponse(val video_url: String, val title: String)

interface TikTokApi {
    @POST("/resolve")
    suspend fun resolveVideo(@Body request: ResolveRequest): ResolveResponse

    @retrofit2.http.GET("/")
    suspend fun checkHealth(): Map<String, String>
}
