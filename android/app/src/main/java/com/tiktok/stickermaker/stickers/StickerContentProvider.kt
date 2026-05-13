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
        const val METADATA = "metadata"
        const val METADATA_ID = "metadata/*"
        const val STICKERS = "stickers/*"
        const val STICKERS_ASSET = "stickers_asset/*/*"
        const val STICKER_PACK_ICON_IN_DIR = "sticker_pack_icon/*/*"

        private const val METADATA_CODE = 1
        private const val METADATA_ID_CODE = 2
        private const val STICKERS_CODE = 3
        private const val STICKERS_ASSET_CODE = 4
        private const val STICKER_PACK_ICON_IN_DIR_CODE = 5

        private val MATCHER = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, METADATA, METADATA_CODE)
            addURI(AUTHORITY, METADATA_ID, METADATA_ID_CODE)
            addURI(AUTHORITY, STICKERS, STICKERS_CODE)
            addURI(AUTHORITY, STICKERS_ASSET, STICKERS_ASSET_CODE)
            addURI(AUTHORITY, STICKER_PACK_ICON_IN_DIR, STICKER_PACK_ICON_IN_DIR_CODE)
        }
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? {
        val code = MATCHER.match(uri)
        return when (code) {
            METADATA_CODE -> getPackCursor(uri)
            METADATA_ID_CODE -> getPackCursor(uri)
            STICKERS_CODE -> getStickersCursor(uri)
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    private fun getPackCursor(uri: Uri): Cursor {
        // WhatsApp expects these specific columns
        val columns = arrayOf(
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
            "image_data_version",
            "avoid_cache",
            "animated_sticker_pack"
        )
        val cursor = MatrixCursor(columns)
        
        // Return dummy dynamic pack info (We can hook this to StickerPackManager later)
        val pack = StickerPackManager.getDynamicPack(context!!)
        if (pack != null) {
            cursor.addRow(
                arrayOf(
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
                    pack.imageDataVersion,
                    if (pack.avoidCache) 1 else 0,
                    if (pack.animatedStickerPack) 1 else 0
                )
            )
        }
        return cursor
    }

    private fun getStickersCursor(uri: Uri): Cursor {
        val columns = arrayOf("sticker_file_name", "sticker_emoji")
        val cursor = MatrixCursor(columns)
        
        val pack = StickerPackManager.getDynamicPack(context!!)
        pack?.stickers?.forEach { sticker ->
            cursor.addRow(arrayOf(sticker.imageFileName, sticker.emojis.joinToString(",")))
        }
        return cursor
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        val matchCode = MATCHER.match(uri)
        val pathSegments = uri.pathSegments

        if (matchCode == STICKERS_ASSET_CODE || matchCode == STICKER_PACK_ICON_IN_DIR_CODE) {
            val identifier = pathSegments[1]
            val fileName = pathSegments[2]
            
            // Read the dynamic file from context.filesDir
            val file = File(context!!.filesDir, fileName)
            if (file.exists()) {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                return AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH)
            }
        }
        throw FileNotFoundException("File not found: $uri")
    }

    override fun getType(uri: Uri): String? {
        return when (MATCHER.match(uri)) {
            METADATA_CODE -> "vnd.android.cursor.dir/vnd.com.tiktok.stickermaker.stickercontentprovider.metadata"
            METADATA_ID_CODE -> "vnd.android.cursor.item/vnd.com.tiktok.stickermaker.stickercontentprovider.metadata"
            STICKERS_CODE -> "vnd.android.cursor.dir/vnd.com.tiktok.stickermaker.stickercontentprovider.stickers"
            STICKERS_ASSET_CODE -> "image/webp"
            STICKER_PACK_ICON_IN_DIR_CODE -> "image/png"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
}
