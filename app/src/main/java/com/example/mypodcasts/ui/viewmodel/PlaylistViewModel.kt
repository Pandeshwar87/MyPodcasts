package com.example.mypodcasts.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.example.mypodcasts.server.LocalWebServer
import com.example.mypodcasts.util.FileUtil
import com.example.mypodcasts.util.NetworkUtil
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

    // Laptop Link (Option 3 Local Web Server) State
    var isServerRunning by mutableStateOf(false)
        private set
    var serverUrl by mutableStateOf<String?>(null)
        private set

    private var localWebServer: LocalWebServer? = null

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
     * Toggle Laptop Link Local Server ON / OFF.
     */
    fun toggleLaptopServer(context: Context) {
        if (isServerRunning) {
            stopLaptopServer()
        } else {
            startLaptopServer(context)
        }
    }

    fun startLaptopServer(context: Context) {
        if (localWebServer != null) return
        try {
            val server = LocalWebServer(
                port = 8080,
                getPlaylistsProvider = { playlistsWithTracks.value },
                onAddTrack = { playlistId, title, artist, mediaUrl ->
                    val meta = UrlParser.parseUrl(mediaUrl)
                    val finalTitle = if (title.isNotBlank()) title else (meta.suggestedTitle ?: "Track")
                    addTrack(
                        playlistId = playlistId,
                        title = finalTitle,
                        artistOrChannel = artist,
                        mediaUrl = mediaUrl,
                        sourceType = meta.sourceType,
                        mediaType = meta.mediaType,
                        thumbnailUrl = meta.thumbnailUrl
                    )
                }
            )
            server.start()
            localWebServer = server
            isServerRunning = true

            val ip = NetworkUtil.getLocalIpAddress(context)
            serverUrl = if (ip != null) "http://$ip:8080" else "Please connect phone to Wi-Fi"
        } catch (e: Exception) {
            e.printStackTrace()
            stopLaptopServer()
        }
    }

    fun stopLaptopServer() {
        try {
            localWebServer?.stop()
        } catch (_: Exception) {}
        localWebServer = null
        isServerRunning = false
        serverUrl = null
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
     * Update an existing Track's details (title, artist, mediaUrl).
     */
    fun updateTrack(track: Track, newTitle: String, newArtist: String?, newMediaUrl: String) {
        viewModelScope.launch {
            val trimmedUrl = newMediaUrl.trim()
            val meta = UrlParser.parseUrl(trimmedUrl)
            val updated = track.copy(
                title = newTitle.trim().ifBlank { "Untitled Track" },
                artistOrChannel = newArtist?.trim()?.ifBlank { null },
                mediaUrl = trimmedUrl,
                sourceType = meta.sourceType,
                mediaType = meta.mediaType,
                thumbnailUrl = meta.thumbnailUrl ?: track.thumbnailUrl
            )
            repository.updateTrack(updated)
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

    fun rewind15Sec() {
        playerManager.rewind15Sec()
    }

    fun forward15Sec() {
        playerManager.forward15Sec()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    override fun onCleared() {
        super.onCleared()
        stopLaptopServer()
        playerManager.release()
    }
}
