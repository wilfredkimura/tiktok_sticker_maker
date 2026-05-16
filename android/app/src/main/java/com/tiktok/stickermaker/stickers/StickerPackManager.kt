package com.tiktok.stickermaker.stickers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import java.io.File

object StickerPackManager {
    private const val PACK_FILE = "pack_info.json"
    private const val TRAY_ICON_NAME = "tray_icon.png"
    
    private var cachedPack: StickerPack? = null

    fun getDynamicPack(context: Context): StickerPack {
        if (cachedPack != null) return cachedPack!!

        val file = File(context.filesDir, PACK_FILE)
        if (file.exists()) {
            try {
                val json = file.readText()
                cachedPack = Gson().fromJson(json, StickerPack::class.java)
                return cachedPack!!
            } catch (e: Exception) {
                Log.e("StickerPackManager", "Failed to load pack", e)
            }
        }

        // Default pack if none exists
        val newPack = StickerPack(
            identifier = "tiktok_stickers_1",
            name = "TikTok Hits",
            publisher = "TikTok Sticker Maker",
            trayImageFile = TRAY_ICON_NAME,
            publisherEmail = "contact@example.com",
            publisherWebsite = "https://example.com",
            privacyPolicyWebsite = "https://example.com/privacy",
            licenseAgreementWebsite = "https://example.com/license",
            imageDataVersion = "1",
            avoidCache = false,
            animatedStickerPack = true,
            stickers = mutableListOf()
        )
        savePack(context, newPack)
        return newPack
    }

    private fun savePack(context: Context, pack: StickerPack) {
        cachedPack = pack
        val file = File(context.filesDir, PACK_FILE)
        file.writeText(Gson().toJson(pack))
    }

    fun addSticker(context: Context, webpFileName: String, emojis: List<String> = listOf("✨", "😂")) {
        val pack = getDynamicPack(context)
        val currentStickers = pack.stickers.toMutableList()
        
        if (currentStickers.none { it.imageFileName == webpFileName }) {
            currentStickers.add(Sticker(webpFileName, emojis))
            pack.stickers = currentStickers
            savePack(context, pack)
            
            // Ensure tray icon exists (WhatsApp requirement: 96x96 PNG)
            ensureTrayIcon(context, webpFileName)
        }
    }

    private fun ensureTrayIcon(context: Context, sourceWebP: String) {
        val trayFile = File(context.filesDir, TRAY_ICON_NAME)
        if (!trayFile.exists()) {
            // We'll use the first sticker's first frame and resize it to 96x96 PNG
            // This is handled by FFmpegProcessor in the background
            Log.d("StickerPackManager", "Requesting tray icon generation from $sourceWebP")
        }
    }

    fun addStickerPackToWhatsApp(context: Context) {
        val pack = getDynamicPack(context)
        
        // Expert Constraint: Pack must have 3-30 stickers
        if (!StickerValidator.validatePack(pack)) {
            val msg = if (pack.stickers.size < 3) 
                "WhatsApp Error: Pack has only ${pack.stickers.size} stickers. Minimum is 3." 
            else 
                "WhatsApp Error: Pack has ${pack.stickers.size} stickers. Maximum is 30."
            
            com.tiktok.stickermaker.utils.AppLogger.log(msg, com.tiktok.stickermaker.utils.LogLevel.ERROR)
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            return
        }

        com.tiktok.stickermaker.utils.AppLogger.log("Firing WhatsApp Import Intent for pack: ${pack.name}")
        val intent = Intent()
        intent.action = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"
        
        // Requested Keys: launch_pool_id, launch_pool_name, launch_pool_authority
        // Note: WhatsApp also recognizes sticker_pack_id etc. We provide both for maximum compatibility.
        intent.putExtra("launch_pool_id", pack.identifier)
        intent.putExtra("launch_pool_name", pack.name)
        intent.putExtra("launch_pool_authority", StickerContentProvider.AUTHORITY)
        
        // Legacy/Standard keys
        intent.putExtra("sticker_pack_id", pack.identifier)
        intent.putExtra("sticker_pack_name", pack.name)
        intent.putExtra("sticker_pack_authority", StickerContentProvider.AUTHORITY)

        try {
            // Check if WhatsApp or WA Business is installed
            val pm = context.packageManager
            val isWA = isPackageInstalled("com.whatsapp", pm)
            val isWAB = isPackageInstalled("com.whatsapp.w4b", pm)

            if (isWA || isWAB) {
                // If both are installed, WhatsApp usually handles the chooser
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                android.widget.Toast.makeText(context, "WhatsApp is not installed", android.widget.Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("StickerPackManager", "Failed to launch WhatsApp", e)
        }
    }

    private fun isPackageInstalled(packageName: String, packageManager: android.content.pm.PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            false
        }
    }
}
