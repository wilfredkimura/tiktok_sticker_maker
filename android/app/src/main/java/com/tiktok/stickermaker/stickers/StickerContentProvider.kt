package com.tiktok.stickermaker.stickers

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import java.io.FileNotFoundException

class StickerContentProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.tiktok.stickermaker.stickercontentprovider"
        
        // WhatsApp standard paths
        private const val METADATA = "metadata"
        private const val STICKERS = "stickers"
        private const val STICKERS_ASSET = "stickers_asset"
        private const val STICKER_PACK_TRAY_ICON = "sticker_pack_icon"

        private const val METADATA_CODE = 1
        private const val STICKERS_CODE = 2
        private const val STICKERS_ASSET_CODE = 3
        private const val STICKER_PACK_TRAY_ICON_CODE = 4

        private val MATCHER = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, METADATA, METADATA_CODE)
            addURI(AUTHORITY, "$STICKERS/*", STICKERS_CODE)
            addURI(AUTHORITY, "$STICKERS_ASSET/*/*", STICKERS_ASSET_CODE)
            addURI(AUTHORITY, "$STICKER_PACK_TRAY_ICON/*/*", STICKER_PACK_TRAY_ICON_CODE)
        }
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? {
        return when (MATCHER.match(uri)) {
            METADATA_CODE -> getPackCursor()
            STICKERS_CODE -> getStickersCursor(uri.lastPathSegment!!)
            else -> null
        }
    }

    private fun getPackCursor(): Cursor {
        val cursor = MatrixCursor(arrayOf(
            "sticker_pack_identifier",
            "sticker_pack_name",
            "sticker_pack_publisher",
            "sticker_pack_icon",
            "android_play_store_link",
            "ios_app_store_link",
            "sticker_pack_publisher_email",
            "sticker_pack_publisher_website",
            "sticker_pack_privacy_policy_website",
            "sticker_pack_license_agreement_website",
            "animated_sticker_pack"
        ))
        
        val pack = StickerPackManager.getDynamicPack(context!!)
        cursor.addRow(arrayOf(
            pack.identifier,
            pack.name,
            pack.publisher,
            pack.trayImageFile,
            "", // Play store link
            "", // iOS store link
            pack.publisherEmail,
            pack.publisherWebsite,
            pack.privacyPolicyWebsite,
            pack.licenseAgreementWebsite,
            if (pack.animatedStickerPack) 1 else 0
        ))
        return cursor
    }

    private fun getStickersCursor(identifier: String): Cursor {
        val cursor = MatrixCursor(arrayOf("sticker_file_name", "sticker_emoji"))
        val pack = StickerPackManager.getDynamicPack(context!!)
        if (pack.identifier == identifier) {
            pack.stickers.forEach { sticker ->
                cursor.addRow(arrayOf(sticker.imageFileName, sticker.emojis.joinToString(",")))
            }
        }
        return cursor
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        val matchCode = MATCHER.match(uri)
        if (matchCode == STICKERS_ASSET_CODE || matchCode == STICKER_PACK_TRAY_ICON_CODE) {
            val fileName = uri.lastPathSegment!!
            val file = if (matchCode == STICKER_PACK_TRAY_ICON_CODE) {
                File(context!!.filesDir, fileName)
            } else {
                File(com.tiktok.stickermaker.utils.StickerStorage.getStickersDirectory(context!!), fileName)
            }
            
            if (file.exists()) {
                return AssetFileDescriptor(
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY),
                    0, AssetFileDescriptor.UNKNOWN_LENGTH
                )
            }
        }
        return null
    }

    override fun getType(uri: Uri): String? {
        return when (MATCHER.match(uri)) {
            METADATA_CODE -> "vnd.android.cursor.dir/vnd.$AUTHORITY.metadata"
            STICKERS_CODE -> "vnd.android.cursor.dir/vnd.$AUTHORITY.stickers"
            STICKERS_ASSET_CODE -> "image/webp"
            STICKER_PACK_TRAY_ICON_CODE -> "image/png"
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
}
