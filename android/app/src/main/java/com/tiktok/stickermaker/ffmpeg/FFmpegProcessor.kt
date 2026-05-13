package com.tiktok.stickermaker.ffmpeg

import android.content.Context
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FFmpegProcessor(private val context: Context) {

    /**
     * Converts a downloaded MP4 video into a WebP sticker format.
     * Dimensions: 512x512 (required by WhatsApp)
     * Framerate: 15 fps
     * Duration limit: 6 seconds (if needed to trim, handled by start/duration params)
     */
    suspend fun convertToWebP(
        inputVideoPath: String,
        outputFileName: String,
        startTimeSec: Float = 0f,
        durationSec: Float = 6f
    ): File? = withContext(Dispatchers.IO) {
        val outputFile = File(context.filesDir, "$outputFileName.webp")
        if (outputFile.exists()) {
            outputFile.delete()
        }

        // WhatsApp animated sticker requirements:
        // - 512x512 exactly
        // - WebP format
        // - Loop infinitely
        // - Under 500KB ideally
        
        // Construct the FFmpeg command
        // -ss start time
        // -t duration
        // -i input
        // -vf scale=512:512:force_original_aspect_ratio=decrease,pad=512:512:(ow-iw)/2:(oh-ih)/2,fps=15
        // -vcodec libwebp -lossless 0 -qscale 50 -preset default -loop 0 -an -vsync 0
        
        val command = "-y -ss $startTimeSec -t $durationSec -i \"$inputVideoPath\" " +
                "-vf \"scale=512:512:force_original_aspect_ratio=decrease,pad=512:512:(ow-iw)/2:(oh-ih)/2,fps=15\" " +
                "-vcodec libwebp -lossless 0 -compression_level 4 -q:v 50 -loop 0 -preset default -an -vsync 0 " +
                "\"${outputFile.absolutePath}\""

        Log.d("FFmpegProcessor", "Executing command: $command")
        
        val session = FFmpegKit.execute(command)
        val returnCode = session.returnCode

        if (ReturnCode.isSuccess(returnCode)) {
            Log.d("FFmpegProcessor", "Conversion successful: ${outputFile.absolutePath}")
            return@withContext outputFile
        } else {
            Log.e("FFmpegProcessor", "Conversion failed with return code: $returnCode")
            Log.e("FFmpegProcessor", "FFmpeg logs: ${session.allLogsAsString}")
            return@withContext null
        }
    }
}
