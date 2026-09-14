package com.example.mypodcasts.data.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 1-to-Many Relationship representation for Room
 */
data class PlayListWithTracks(
    @Embedded val playlist: PlayList,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlistId"
    )
    val tracks: List<Track>
)
