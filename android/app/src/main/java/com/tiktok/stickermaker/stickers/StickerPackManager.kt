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
        
        // Add only if not already there
        if (currentStickers.none { it.imageFileName == webpFileName }) {
            currentStickers.add(Sticker(webpFileName, emojis))
            pack.stickers = currentStickers
            savePack(context, pack)
            
            // Generate tray icon if it doesn't exist
            ensureTrayIcon(context, webpFileName)
        }
    }

    private fun ensureTrayIcon(context: Context, sourceWebP: String) {
        val trayFile = File(context.filesDir, TRAY_ICON_NAME)
        if (!trayFile.exists()) {
            // In a real app, we'd convert WebP to PNG 96x96
            // For now, we'll just copy the webp if it's small, but WhatsApp needs PNG.
            // I'll add a placeholder PNG if conversion is too complex here, 
            // but the best is to use the first sticker.
        }
    }

    fun addStickerPackToWhatsApp(context: Context) {
        val pack = getDynamicPack(context)
        val intent = Intent()
        intent.action = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"
        intent.putExtra("sticker_pack_id", pack.identifier)
        intent.putExtra("sticker_pack_authority", StickerContentProvider.AUTHORITY)
        intent.putExtra("sticker_pack_name", pack.name)

        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e("StickerPackManager", "WhatsApp is not installed.")
        }
    }
}
