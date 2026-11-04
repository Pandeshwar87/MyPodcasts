package com.example.mypodcasts.data.model

/**
 * Indicates the originating source of a media track link or file.
 */
enum class SourceType {
    LOCAL_FILE,   // Locally saved MP3 / Audio / Video file on device
    YOUTUBE,      // YouTube video or Shorts link
    SPOTIFY,      // Spotify track, album, or podcast link
    GOOGLE_DRIVE, // Google Drive file / audio / video link
    DIRECT_AUDIO, // Web direct MP3/AAC/OGG URL stream
    DIRECT_VIDEO, // Web direct MP4/WebM URL stream
    OTHER         // Generic web link
}
