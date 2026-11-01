package com.example.mypodcasts.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Playlist Entity stored in Room Database
 */
@Entity(tableName = "playlists")
data class PlayList(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val coverImageUrl: String? = null, // Local URI or remote image URL for avatar/cover
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
