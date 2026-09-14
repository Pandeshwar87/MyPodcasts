package com.example.mypodcasts.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtil {

    /**
     * Copies a selected avatar image from Content URI to internal storage and returns the stored File URI string.
     */
    fun saveAvatarImageToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val avatarDir = File(context.filesDir, "playlist_avatars")
            if (!avatarDir.exists()) {
                avatarDir.mkdirs()
            }
            val fileName = "avatar_${System.currentTimeMillis()}.jpg"
            val destFile = File(avatarDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            sourceUri.toString() // Fallback to raw URI if copy fails
        }
    }

    /**
     * Extracts title, artist, and duration from a local audio file URI.
     */
    fun getLocalAudioMetadata(context: Context, uri: Uri): LocalAudioMetadata {
        var title: String? = null
        var artist: String? = null
        var durationMs: Long? = null

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMs = durationStr?.toLongOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        val fallbackName = uri.lastPathSegment?.substringAfterLast("/")?.substringAfterLast("%2F") ?: "Local Track"

        return LocalAudioMetadata(
            title = title?.takeIf { it.isNotBlank() } ?: fallbackName,
            artist = artist,
            durationMs = durationMs
        )
    }
}

data class LocalAudioMetadata(
    val title: String,
    val artist: String?,
    val durationMs: Long?
)
