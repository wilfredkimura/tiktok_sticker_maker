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
        inputPath: String,
        outputName: String,
        startTime: Float,
        duration: Float,
        text: String = ""
    ): File? = withContext(Dispatchers.IO) {
        val outputFile = File(context.filesDir, "$outputName.webp")
        
        // Base filters for WhatsApp: 512x512, padding, 15fps
        var videoFilter = "scale=512:512:force_original_aspect_ratio=decrease,pad=512:512:(ow-iw)/2:(oh-ih)/2:color=black@0,fps=15"
        
        // Add text overlay if provided
        if (text.isNotEmpty()) {
            // Simple drawtext: white text, bottom center, with a small shadow
            videoFilter += ",drawtext=text='$text':fontcolor=white:fontsize=48:x=(w-text_w)/2:y=h-text_h-40:shadowcolor=black@0.5:shadowx=2:shadowy=2"
        }

        val command = "-y -ss $startTime -t $duration -i \"$inputPath\" " +
                "-vf \"$videoFilter\" " +
                "-loop 0 -vcodec libwebp -lossless 0 -compression_level 6 -q:v 50 -preset default " +
                "-an -vsync 0 \"${outputFile.absolutePath}\""

        Log.d("FFmpegProcessor", "Executing command: $command")
        
        val session = FFmpegKit.execute(command)
        if (ReturnCode.isSuccess(session.returnCode)) {
            generateTrayIcon(outputFile.absolutePath)
            outputFile
        } else {
            Log.e("FFmpegProcessor", "FFmpeg failed: ${session.allLogsAsString}")
            null
        }
    }

    fun generateTrayIcon(videoPath: String) {
        val trayFile = File(context.filesDir, "tray_icon.png")
        if (!trayFile.exists()) {
            val command = "-y -i \"$videoPath\" -ss 0 -vframes 1 -vf \"scale=96:96:force_original_aspect_ratio=decrease,pad=96:96:(ow-iw)/2:(oh-ih)/2:color=#00000000\" \"${trayFile.absolutePath}\""
            FFmpegKit.execute(command)
        }
    }
}
