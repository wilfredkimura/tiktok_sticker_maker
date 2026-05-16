package com.tiktok.stickermaker.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tiktok.stickermaker.downloader.VideoDownloader
import com.tiktok.stickermaker.ffmpeg.FFmpegProcessor
import com.tiktok.stickermaker.network.ResolveRequest
import com.tiktok.stickermaker.network.TikTokApi
import com.tiktok.stickermaker.stickers.StickerPackManager
import com.tiktok.stickermaker.utils.AppLogger
import com.tiktok.stickermaker.utils.LogLevel
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

sealed class AppState {
    object Home : AppState()
    object Loading : AppState()
    data class Editor(val videoFile: File) : AppState()
    data class Exporting(val progress: Float) : AppState()
    data class Success(val stickerFile: File) : AppState()
    data class Error(val message: String) : AppState()
    object Logs : AppState()
}

class StickerViewModel(application: Application) : AndroidViewModel(application) {
    var state by mutableStateOf<AppState>(AppState.Home)
        private set

    var isBackendConnected by mutableStateOf(false)
        private set

    private val downloader = VideoDownloader(application)
    private val processor = FFmpegProcessor(application)

    // TODO: Replace with your actual Render backend URL after deployment
    private val RENDER_URL = "https://tiktok-resolver-ly7d.onrender.com"
    private val LOCAL_URL = "http://10.0.2.2:8000"

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val api = Retrofit.Builder()
        .baseUrl(RENDER_URL) // Using Render URL as requested
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TikTokApi::class.java)

    init {
        checkBackendConnection()
    }

    fun checkBackendConnection() {
        AppLogger.log("Checking backend connection to $RENDER_URL")
        viewModelScope.launch {
            try {
                val response = api.checkHealth()
                isBackendConnected = response["status"] == "ok"
                AppLogger.log("Backend status: ${response["status"]}")
            } catch (e: Exception) {
                isBackendConnected = false
                AppLogger.log("Backend connection failed: ${e.message}", LogLevel.ERROR)
            }
        }
    }

    fun processVideo(url: String) {
        AppLogger.log("Processing video URL: $url")
        viewModelScope.launch {
            state = AppState.Loading
            try {
                val response = api.resolveVideo(ResolveRequest(url))
                AppLogger.log("Resolved video title: ${response.title}")
                AppLogger.log("Resolved video URL: ${response.video_url}")
                
                AppLogger.log("Starting download...")
                val videoFile = downloader.downloadVideo(response.video_url, "temp_video")
                if (videoFile != null) {
                    AppLogger.log("Download complete: ${videoFile.length()} bytes")
                    state = AppState.Editor(videoFile)
                } else {
                    AppLogger.log("Download failed", LogLevel.ERROR)
                    state = AppState.Error("Failed to download video")
                }
            } catch (e: Exception) {
                AppLogger.log("Resolution failed: ${e.message}", LogLevel.ERROR)
                state = AppState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun createSticker(videoFile: File, startTime: Float, duration: Float) {
        AppLogger.log("Creating sticker: start=${startTime}s, duration=${duration}s")
        viewModelScope.launch {
            state = AppState.Exporting(0f)
            try {
                val stickerFile = processor.convertToWebP(
                    videoFile.absolutePath,
                    "sticker_${System.currentTimeMillis()}",
                    startTime,
                    duration
                )
                if (stickerFile != null) {
                    AppLogger.log("Sticker created: ${stickerFile.absolutePath}")
                    StickerPackManager.addSticker(getApplication(), stickerFile.name)
                    state = AppState.Success(stickerFile)
                } else {
                    AppLogger.log("FFmpeg processing failed", LogLevel.ERROR)
                    state = AppState.Error("FFmpeg processing failed")
                }
            } catch (e: Exception) {
                AppLogger.log("Sticker creation error: ${e.message}", LogLevel.ERROR)
                state = AppState.Error(e.message ?: "Processing error")
            }
        }
    }

    fun showLogs() {
        state = AppState.Logs
    }

    fun reset() {
        state = AppState.Home
    }
}
