package com.example.mypodcasts.data.repository

import com.example.mypodcasts.data.local.PlayListDao
import com.example.mypodcasts.data.local.TrackDao
import com.example.mypodcasts.data.model.PlayList
import com.example.mypodcasts.data.model.PlayListWithTracks
import com.example.mypodcasts.data.model.Track
import kotlinx.coroutines.flow.Flow

class MediaRepository(
    private val playlistDao: PlayListDao,
    private val trackDao: TrackDao
) {
    val allPlaylistsWithTracks: Flow<List<PlayListWithTracks>> =
        playlistDao.getAllPlaylistsWithTracks()

    fun getPlaylistWithTracks(playlistId: Long): Flow<PlayListWithTracks?> =
        playlistDao.getPlaylistWithTracks(playlistId)

    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>> =
        trackDao.getTracksForPlaylist(playlistId)

    suspend fun insertPlaylist(playlist: PlayList): Long =
        playlistDao.insertPlaylist(playlist)

    suspend fun updatePlaylist(playlist: PlayList) =
        playlistDao.updatePlaylist(playlist)

    suspend fun deletePlaylist(playlist: PlayList) =
        playlistDao.deletePlaylist(playlist)

    suspend fun insertTrack(track: Track): Long {
        val trackId = trackDao.insertTrack(track)
        // Touch playlist updatedAt timestamp
        playlistDao.getPlaylistById(track.playlistId)?.let { playlist ->
            playlistDao.updatePlaylist(playlist.copy(updatedAt = System.currentTimeMillis()))
        }
        return trackId
    }

    suspend fun updateTrack(track: Track) =
        trackDao.updateTrack(track)

    suspend fun updateTracks(tracks: List<Track>) =
        trackDao.updateTracks(tracks)

    suspend fun deleteTrack(track: Track) =
        trackDao.deleteTrack(track)
}
