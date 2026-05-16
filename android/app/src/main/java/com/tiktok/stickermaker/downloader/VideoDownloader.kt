package com.tiktok.stickermaker.downloader

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class VideoDownloader(private val context: Context) {

    suspend fun downloadVideo(videoUrl: String, fileName: String): File? = withContext(Dispatchers.IO) {
        try {
            com.tiktok.stickermaker.utils.AppLogger.log("Opening connection to CDN...")
            val url = URL(videoUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                setRequestProperty("Referer", "https://www.tiktok.com/")
            }
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                com.tiktok.stickermaker.utils.AppLogger.log("Download failed: HTTP ${connection.responseCode} ${connection.responseMessage}", com.tiktok.stickermaker.utils.LogLevel.ERROR)
                return@withContext null
            }

            val inputStream = connection.inputStream
            
            // Save to app's cache directory
            val outputFile = File(context.cacheDir, "$fileName.mp4")
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(4096)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            outputStream.close()
            inputStream.close()

            return@withContext outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}
