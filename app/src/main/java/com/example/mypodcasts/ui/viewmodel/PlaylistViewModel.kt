package com.example.mypodcasts.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mypodcasts.data.local.AppDatabase
import com.example.mypodcasts.data.model.MediaType
import com.example.mypodcasts.data.model.PlayList
import com.example.mypodcasts.data.model.PlayListWithTracks
import com.example.mypodcasts.data.model.SourceType
import com.example.mypodcasts.data.model.Track
import com.example.mypodcasts.data.repository.MediaRepository
import com.example.mypodcasts.player.AudioPlayerManager
import com.example.mypodcasts.player.PlayerState
import com.example.mypodcasts.util.FileUtil
import com.example.mypodcasts.util.ParsedUrlMetadata
import com.example.mypodcasts.util.UrlParser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MediaRepository
    val playerManager: AudioPlayerManager = AudioPlayerManager(application)
    val playerState: StateFlow<PlayerState> = playerManager.playerState

    val playlistsWithTracks: StateFlow<List<PlayListWithTracks>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MediaRepository(database.playlistDao(), database.trackDao())
        playlistsWithTracks = repository.allPlaylistsWithTracks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    /**
     * Create a new Playlist with an optional local image URI.
     */
    fun createPlaylist(title: String, description: String?, localImageUri: Uri?) {
        viewModelScope.launch {
            var avatarPath: String? = null
            if (localImageUri != null) {
                avatarPath = FileUtil.saveAvatarImageToInternalStorage(
                    getApplication(),
                    localImageUri
                )
            }
            val playlist = PlayList(
                title = title.trim(),
                description = description?.trim()?.ifBlank { null },
                coverImageUrl = avatarPath
            )
            repository.insertPlaylist(playlist)
        }
    }

    /**
     * Update an existing Playlist's details or avatar.
     */
    fun updatePlaylist(playlist: PlayList, newTitle: String, newDesc: String?, newAvatarUri: Uri?) {
        viewModelScope.launch {
            var avatarPath = playlist.coverImageUrl
            if (newAvatarUri != null) {
                avatarPath = FileUtil.saveAvatarImageToInternalStorage(
                    getApplication(),
                    newAvatarUri
                )
            }
            val updated = playlist.copy(
                title = newTitle.trim(),
                description = newDesc?.trim()?.ifBlank { null },
                coverImageUrl = avatarPath,
                updatedAt = System.currentTimeMillis()
            )
            repository.updatePlaylist(updated)
        }
    }

    /**
     * Delete a Playlist and all associated tracks (via foreign key CASCADE).
     */
    fun deletePlaylist(playlist: PlayList) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
        }
    }

    /**
     * Auto-detect metadata for a URL link or local file URI.
     */
    fun parseUrlInput(url: String): ParsedUrlMetadata {
        return UrlParser.parseUrl(url)
    }

    /**
     * Add a Track to a Playlist using pasted URL or selected local MP3 Uri.
     */
    fun addTrack(
        playlistId: Long,
        title: String,
        artistOrChannel: String?,
        mediaUrl: String,
        sourceType: SourceType,
        mediaType: MediaType,
        thumbnailUrl: String? = null
    ) {
        viewModelScope.launch {
            var finalTitle = title.trim()
            var finalArtist = artistOrChannel?.trim()?.ifBlank { null }
            var finalDuration: Long? = null

            // If it's a local audio file, extract ID3 metadata if available
            if (sourceType == SourceType.LOCAL_FILE && (mediaUrl.startsWith("content://") || mediaUrl.startsWith("file://"))) {
                try {
                    val metadata = FileUtil.getLocalAudioMetadata(
                        getApplication(),
                        Uri.parse(mediaUrl)
                    )
                    if (finalTitle.isBlank()) finalTitle = metadata.title
                    if (finalArtist == null) finalArtist = metadata.artist
                    finalDuration = metadata.durationMs
                } catch (_: Exception) {}
            }

            val track = Track(
                playlistId = playlistId,
                title = if (finalTitle.isNotBlank()) finalTitle else "Untitled Track",
                artistOrChannel = finalArtist,
                mediaUrl = mediaUrl.trim(),
                sourceType = sourceType,
                mediaType = mediaType,
                thumbnailUrl = thumbnailUrl,
                durationMs = finalDuration
            )
            repository.insertTrack(track)
        }
    }

    /**
     * Reorder playlists by assigning updated position indices and persisting to DB.
     */
    fun reorderPlaylists(playlists: List<PlayList>) {
        viewModelScope.launch {
            val reordered = playlists.mapIndexed { index, playlist ->
                playlist.copy(position = index)
            }
            repository.updatePlaylists(reordered)
        }
    }

    /**
     * Reorder tracks in a playlist by assigning updated position indices and persisting to DB.
     */
    fun reorderTracks(tracks: List<Track>) {
        viewModelScope.launch {
            val reordered = tracks.mapIndexed { index, track ->
                track.copy(position = index)
            }
            repository.updateTracks(reordered)
        }
    }

    /**
     * Delete a Track from a Playlist.
     */
    fun deleteTrack(track: Track) {
        viewModelScope.launch {
            repository.deleteTrack(track)
        }
    }

    /**
     * Play a track (local MP3 in ExoPlayer, or open YouTube/Spotify link).
     */
    fun playTrack(track: Track) {
        playerManager.playTrack(track)
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
