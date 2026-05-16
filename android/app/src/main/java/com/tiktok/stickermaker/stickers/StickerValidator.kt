package com.tiktok.stickermaker.stickers

import android.content.Context
import android.graphics.BitmapFactory
import java.io.File

object StickerValidator {
    private const val STATIC_SIZE_LIMIT = 100 * 1024 // 100 KB
    private const val ANIMATED_SIZE_LIMIT = 500 * 1024 // 500 KB
    private const val DIMENSION = 512
    private const val TRAY_DIMENSION = 96

    fun validateSticker(file: File, isAnimated: Boolean): Boolean {
        if (!file.exists()) return false
        
        // Check file size
        val sizeLimit = if (isAnimated) ANIMATED_SIZE_LIMIT else STATIC_SIZE_LIMIT
        if (file.length() > sizeLimit) return false

        // Check dimensions
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        
        // For WebP/Animated WebP, BitmapFactory might not give accurate bounds if not supported by OS version,
        // but for 512x512 stickers, we generally trust our FFmpeg output.
        // Still, it's good practice to verify if possible.
        return options.outWidth == DIMENSION && options.outHeight == DIMENSION
    }

    fun validateTrayIcon(file: File): Boolean {
        if (!file.exists()) return false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return options.outWidth == TRAY_DIMENSION && options.outHeight == TRAY_DIMENSION
    }

    fun validatePack(pack: StickerPack): Boolean {
        if (pack.stickers.size < 3 || pack.stickers.size > 30) return false
        return true
    }
}
