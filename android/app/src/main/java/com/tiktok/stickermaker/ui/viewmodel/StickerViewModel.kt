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
}

class StickerViewModel(application: Application) : AndroidViewModel(application) {
    var state by mutableStateOf<AppState>(AppState.Home)
        private set

    private val downloader = VideoDownloader(application)
    private val processor = FFmpegProcessor(application)

    // Change this to your Render backend URL
    private val api = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8000") // 10.0.2.2 is localhost for Android Emulator
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TikTokApi::class.java)

    fun processVideo(url: String) {
        viewModelScope.launch {
            state = AppState.Loading
            try {
                val response = api.resolveVideo(ResolveRequest(url))
                val videoFile = downloader.downloadVideo(response.video_url, "temp_video")
                if (videoFile != null) {
                    state = AppState.Editor(videoFile)
                } else {
                    state = AppState.Error("Failed to download video")
                }
            } catch (e: Exception) {
                state = AppState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun createSticker(videoFile: File, startTime: Float, duration: Float) {
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
                    StickerPackManager.addSticker(getApplication(), stickerFile.name)
                    state = AppState.Success(stickerFile)
                } else {
                    state = AppState.Error("FFmpeg processing failed")
                }
            } catch (e: Exception) {
                state = AppState.Error(e.message ?: "Processing error")
            }
        }
    }

    fun reset() {
        state = AppState.Home
    }
}
