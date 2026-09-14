package com.example.mypodcasts.util

import com.example.mypodcasts.data.model.MediaType
import com.example.mypodcasts.data.model.SourceType
import java.net.URI

data class ParsedUrlMetadata(
    val sourceType: SourceType,
    val mediaType: MediaType,
    val suggestedTitle: String?,
    val thumbnailUrl: String? = null
)

object UrlParser {

    /**
     * Parses a URL string or file URI and auto-detects SourceType, MediaType,
     * thumbnail URL (e.g. YouTube video thumbnail), and suggested title.
     */
    fun parseUrl(url: String): ParsedUrlMetadata {
        val trimmed = url.trim()

        // 1. Check Local File / Content URI
        if (trimmed.startsWith("content://") || trimmed.startsWith("file://")) {
            val isVideo = trimmed.contains(".mp4", ignoreCase = true) ||
                    trimmed.contains(".mkv", ignoreCase = true) ||
                    trimmed.contains(".webm", ignoreCase = true)
            val mediaType = if (isVideo) MediaType.VIDEO else MediaType.AUDIO
            val nameFromUri = trimmed.substringAfterLast("/").substringAfterLast("%2F")
            return ParsedUrlMetadata(
                sourceType = SourceType.LOCAL_FILE,
                mediaType = mediaType,
                suggestedTitle = nameFromUri.ifBlank { "Local Track" }
            )
        }

        // 2. Check YouTube
        val ytVideoId = extractYouTubeVideoId(trimmed)
        if (ytVideoId != null) {
            val thumbnail = "https://img.youtube.com/vi/$ytVideoId/hqdefault.jpg"
            return ParsedUrlMetadata(
                sourceType = SourceType.YOUTUBE,
                mediaType = MediaType.VIDEO,
                suggestedTitle = "YouTube Video ($ytVideoId)",
                thumbnailUrl = thumbnail
            )
        }

        // 3. Check Spotify
        if (trimmed.contains("spotify.com", ignoreCase = true) || trimmed.startsWith("spotify:")) {
            val isEpisode = trimmed.contains("/episode/", ignoreCase = true)
            return ParsedUrlMetadata(
                sourceType = SourceType.SPOTIFY,
                mediaType = MediaType.AUDIO,
                suggestedTitle = if (isEpisode) "Spotify Podcast Episode" else "Spotify Track"
            )
        }

        // 4. Check Direct Audio Extension / Stream
        val cleanPath = try {
            URI(trimmed).path?.lowercase() ?: ""
        } catch (e: Exception) {
            trimmed.lowercase()
        }

        if (cleanPath.endsWith(".mp3") || cleanPath.endsWith(".m4a") ||
            cleanPath.endsWith(".aac") || cleanPath.endsWith(".ogg") ||
            cleanPath.endsWith(".wav") || cleanPath.endsWith(".flac")
        ) {
            val fileName = cleanPath.substringAfterLast("/")
            return ParsedUrlMetadata(
                sourceType = SourceType.DIRECT_AUDIO,
                mediaType = MediaType.AUDIO,
                suggestedTitle = fileName.ifBlank { "Direct Audio Stream" }
            )
        }

        // 5. Check Direct Video Extension
        if (cleanPath.endsWith(".mp4") || cleanPath.endsWith(".m3u8") ||
            cleanPath.endsWith(".webm") || cleanPath.endsWith(".mkv")
        ) {
            val fileName = cleanPath.substringAfterLast("/")
            return ParsedUrlMetadata(
                sourceType = SourceType.DIRECT_VIDEO,
                mediaType = MediaType.VIDEO,
                suggestedTitle = fileName.ifBlank { "Direct Video Stream" }
            )
        }

        // 6. Generic / Other Web Link
        val domain = try {
            URI(trimmed).host ?: "Web Link"
        } catch (e: Exception) {
            "Web Link"
        }

        return ParsedUrlMetadata(
            sourceType = SourceType.OTHER,
            mediaType = MediaType.AUDIO,
            suggestedTitle = "Link from $domain"
        )
    }

    /**
     * Extracts YouTube Video ID from standard YouTube links, short links, or Shorts links.
     */
    private fun extractYouTubeVideoId(url: String): String? {
        val patterns = listOf(
            Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/shorts/|youtube\\.com/embed/)([a-zA-Z0-9_-]{11})"),
            Regex("v=([a-zA-Z0-9_-]{11})")
        )

        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }
        return null
    }
}
