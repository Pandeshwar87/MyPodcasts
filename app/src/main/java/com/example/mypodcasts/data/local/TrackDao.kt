package com.example.mypodcasts.data.local

import androidx.room.*
import com.example.mypodcasts.data.model.Track
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks WHERE playlistId = :playlistId ORDER BY position ASC, addedAt ASC")
    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: Track): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<Track>)

    @Update
    suspend fun updateTrack(track: Track)

    @Update
    suspend fun updateTracks(tracks: List<Track>)

    @Delete
    suspend fun deleteTrack(track: Track)

    @Query("DELETE FROM tracks WHERE playlistId = :playlistId")
    suspend fun deleteAllTracksInPlaylist(playlistId: Long)
}
