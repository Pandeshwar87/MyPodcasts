package com.example.mypodcasts.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Track Entity stored in Room Database
 */
@Entity(
    tableName = "tracks",
    foreignKeys = [
        ForeignKey(
            entity = PlayList::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["playlistId"])]
)
data class Track(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val title: String,
    val artistOrChannel: String? = null,
    val mediaUrl: String,            // Local file/content URI or HTTP link
    val sourceType: SourceType,      // LOCAL_FILE, YOUTUBE, SPOTIFY, DIRECT_AUDIO, DIRECT_VIDEO, OTHER
    val mediaType: MediaType,        // AUDIO or VIDEO
    val thumbnailUrl: String? = null,
    val durationMs: Long? = null,
    val position: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)
