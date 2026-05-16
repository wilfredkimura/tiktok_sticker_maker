package com.tiktok.stickermaker.ffmpeg

import android.content.Context
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FFmpegProcessor(private val context: Context) {

    suspend fun convertToWebP(
        videoPath: String,
        outputName: String,
        startTime: Float,
        duration: Float
    ): File? = withContext(Dispatchers.IO) {
        val outputFile = File(context.filesDir, "$outputName.webp")
        
        // WhatsApp animated sticker requirements:
        // - 512x512 exactly (padded with transparency)
        // - 15 fps
        // - Loop 0 (infinite)
        // - Under 512KB
        val command = "-y -ss $startTime -t $duration -i \"$videoPath\" " +
                "-vf \"scale=512:512:force_original_aspect_ratio=decrease,pad=512:512:(ow-iw)/2:(oh-ih)/2:color=#00000000,fps=15\" " +
                "-vcodec libwebp -lossless 0 -compression_level 4 -q:v 50 -loop 0 -preset default -an -vsync 0 " +
                "\"${outputFile.absolutePath}\""

        Log.d("FFmpegProcessor", "Executing command: $command")
        
        val session = FFmpegKit.execute(command)
        if (ReturnCode.isSuccess(session.returnCode)) {
            // Also generate a tray icon if it doesn't exist (PNG 96x96)
            generateTrayIcon(videoPath)
            outputFile
        } else {
            Log.e("FFmpegProcessor", "FFmpeg failed: ${session.allLogsAsString}")
            null
        }
    }

    private fun generateTrayIcon(videoPath: String) {
        val trayFile = File(context.filesDir, "tray_icon.png")
        if (!trayFile.exists()) {
            val command = "-y -i \"$videoPath\" -ss 0 -vframes 1 -vf \"scale=96:96:force_original_aspect_ratio=decrease,pad=96:96:(ow-iw)/2:(oh-ih)/2:color=#00000000\" \"${trayFile.absolutePath}\""
            FFmpegKit.execute(command)
        }
    }
}
