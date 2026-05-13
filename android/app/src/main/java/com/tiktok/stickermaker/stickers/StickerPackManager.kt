package com.tiktok.stickermaker.stickers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log

object StickerPackManager {
    
    private var dynamicPack: StickerPack? = null

    /**
     * Retrieves the current dynamic sticker pack.
     * In a real app, this might read from Room DB or SharedPreferences.
     */
    fun getDynamicPack(context: Context): StickerPack? {
        if (dynamicPack == null) {
            // Initialize a default pack
            dynamicPack = StickerPack(
                identifier = "tiktok_stickers_1",
                name = "TikTok Hits",
                publisher = "TikTok Sticker Maker",
                trayImageFile = "tray_image.png",
                publisherEmail = "contact@example.com",
                publisherWebsite = "https://example.com",
                privacyPolicyWebsite = "https://example.com/privacy",
                licenseAgreementWebsite = "https://example.com/license",
                imageDataVersion = "1",
                avoidCache = false,
                animatedStickerPack = true,
                stickers = mutableListOf()
            )
        }
        return dynamicPack
    }

    /**
     * Adds a new WebP sticker to the pack.
     */
    fun addSticker(context: Context, webpFileName: String, emojis: List<String> = listOf("✨", "😂")) {
        val pack = getDynamicPack(context) ?: return
        val currentStickers = pack.stickers.toMutableList()
        currentStickers.add(Sticker(webpFileName, emojis))
        pack.stickers = currentStickers
    }

    /**
     * Fires the intent to open WhatsApp and add our sticker pack.
     */
    fun addStickerPackToWhatsApp(context: Context, identifier: String, stickerPackName: String) {
        val intent = Intent()
        intent.action = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"
        intent.putExtra("sticker_pack_id", identifier)
        intent.putExtra("sticker_pack_authority", StickerContentProvider.AUTHORITY)
        intent.putExtra("sticker_pack_name", stickerPackName)

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e("StickerPackManager", "WhatsApp is not installed.")
        }
    }
}
