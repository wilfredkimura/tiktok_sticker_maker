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
    data class Downloaded(val videoFile: File) : AppState()
    data class Editor(val videoFile: File) : AppState()
    data class Exporting(val progress: Float) : AppState()
    data class Success(val stickerFile: File, val videoFile: File) : AppState()
    data class Error(val message: String) : AppState()
    object Logs : AppState()
    object Library : AppState()
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
                AppLogger.log("Resolved raw video URL: ${response.video_url}")
                
                val finalUrl = if (response.video_url.startsWith("/")) {
                    RENDER_URL + response.video_url
                } else {
                    response.video_url
                }
                
                AppLogger.log("Final download URL: $finalUrl")
                
                AppLogger.log("Starting download...")
                val videoFile = downloader.downloadVideo(finalUrl, "temp_video")
                if (videoFile != null) {
                    AppLogger.log("Download complete: ${videoFile.length()} bytes")
                    state = AppState.Downloaded(videoFile)
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

    fun startEditing(videoFile: File) {
        state = AppState.Editor(videoFile)
    }

    fun saveVideoToGallery(videoFile: File) {
        val uri = com.tiktok.stickermaker.utils.StickerStorage.saveVideoToGallery(getApplication(), videoFile)
        if (uri != null) {
            AppLogger.log("Video saved to gallery: $uri")
            android.widget.Toast.makeText(getApplication(), "Video saved to Gallery!", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(getApplication(), "Failed to save video", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun createSticker(videoFile: File, startTime: Float, duration: Float, text: String) {
        AppLogger.log("Creating sticker: start=${startTime}s, duration=${duration}s, text='$text'")
        viewModelScope.launch {
            state = AppState.Exporting(0f)
            try {
                val stickerFile = processor.convertToWebP(
                    videoFile.absolutePath,
                    "sticker_${System.currentTimeMillis()}",
                    startTime,
                    duration,
                    text
                )
                if (stickerFile != null) {
                    AppLogger.log("Sticker created: ${stickerFile.absolutePath}")
                    
                    // Move to permanent stickers directory
                    val permanentFile = File(com.tiktok.stickermaker.utils.StickerStorage.getStickersDirectory(getApplication()), stickerFile.name)
                    stickerFile.renameTo(permanentFile)
                    
                    StickerPackManager.addSticker(getApplication(), permanentFile.name)
                    processor.generateTrayIcon(permanentFile.absolutePath)
                    state = AppState.Success(permanentFile, videoFile)
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

    fun addToWhatsApp() {
        StickerPackManager.addStickerPackToWhatsApp(getApplication())
    }

    fun showLibrary() {
        state = AppState.Library
    }

    fun showLogs() {
        state = AppState.Logs
    }

    fun reset() {
        state = AppState.Home
    }
}
