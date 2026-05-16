package com.tiktok.stickermaker.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

object StickerStorage {
    private const val STICKERS_DIR = "stickers"

    fun getStickersDirectory(context: Context): File {
        val dir = File(context.filesDir, STICKERS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getAllStickers(context: Context): List<File> {
        val dir = getStickersDirectory(context)
        return dir.listFiles { file -> file.extension == "webp" }?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun saveVideoToGallery(context: Context, videoFile: File): Uri? {
        val filename = "TikTokSticker_${System.currentTimeMillis()}.mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
            }
        }

        val contentResolver = context.contentResolver
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                videoFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        return uri
    }
}
