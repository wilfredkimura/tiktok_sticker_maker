package com.tiktok.stickermaker.stickers

data class Sticker(
    val imageFileName: String,
    val emojis: List<String>
)

data class StickerPack(
    val identifier: String,
    val name: String,
    val publisher: String,
    val trayImageFile: String,
    val publisherEmail: String,
    val publisherWebsite: String,
    val privacyPolicyWebsite: String,
    val licenseAgreementWebsite: String,
    val imageDataVersion: String,
    val avoidCache: Boolean,
    val animatedStickerPack: Boolean,
    var stickers: List<Sticker> = emptyList()
)
