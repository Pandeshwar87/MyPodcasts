@file:OptIn(UnstableApi::class)

package com.example.mypodcasts.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.example.mypodcasts.data.model.SourceType
import com.example.mypodcasts.data.model.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val error: String? = null
)

class AudioPlayerManager(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    var mediaSession: MediaSession? = null
        private set

    private var positionUpdateJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    init {
        initExoPlayer()
        BackgroundAudioService.playerManagerRef = this
    }

    private fun initExoPlayer() {
        exoPlayer = ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(15000L)
            .setSeekForwardIncrementMs(15000L)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _playerState.value = _playerState.value.copy(
                            isPlaying = isPlaying,
                            durationMs = duration.coerceAtLeast(0L),
                            positionMs = currentPosition.coerceAtLeast(0L)
                        )

                        val track = _playerState.value.currentTrack
                        if (track != null) {
                            BackgroundAudioService.updateNotification(
                                context = context,
                                title = track.title,
                                artist = track.artistOrChannel,
                                isPlaying = isPlaying
                            )
                        }

                        if (isPlaying) {
                            startPositionUpdates()
                        } else {
                            stopPositionUpdates()
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            _playerState.value = _playerState.value.copy(
                                durationMs = duration.coerceAtLeast(0L),
                                positionMs = currentPosition.coerceAtLeast(0L),
                                error = null
                            )
                        } else if (playbackState == Player.STATE_ENDED) {
                            _playerState.value = _playerState.value.copy(
                                isPlaying = false
                            )
                            stopPositionUpdates()
                            BackgroundAudioService.stopService(context)
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        _playerState.value = _playerState.value.copy(
                            isPlaying = false,
                            error = error.localizedMessage ?: "Playback error"
                        )
                        stopPositionUpdates()
                        BackgroundAudioService.stopService(context)
                    }
                })
            }

        exoPlayer?.let { player ->
            try {
                mediaSession = MediaSession.Builder(context, player)
                    .setId("MyPodcastsMediaSession")
                    .build()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateJob = coroutineScope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        _playerState.value = _playerState.value.copy(
                            positionMs = player.currentPosition.coerceAtLeast(0L),
                            durationMs = player.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    fun playTrack(track: Track) {
        val uri = Uri.parse(track.mediaUrl)

        when (track.sourceType) {
            SourceType.LOCAL_FILE, SourceType.DIRECT_AUDIO, SourceType.DIRECT_VIDEO -> {
                exoPlayer?.let { player ->
                    if (uri.scheme == "content") {
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (_: Exception) {}
                    }

                    val mediaMetadata = MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artistOrChannel ?: "MyPodcasts")
                        .build()

                    val mediaItem = MediaItem.Builder()
                        .setUri(uri)
                        .setMediaMetadata(mediaMetadata)
                        .build()

                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()
                    _playerState.value = PlayerState(
                        currentTrack = track,
                        isPlaying = true
                    )
                    BackgroundAudioService.startService(
                        context = context,
                        title = track.title,
                        artist = track.artistOrChannel,
                        isPlaying = true
                    )
                }
            }
            SourceType.YOUTUBE, SourceType.SPOTIFY, SourceType.OTHER -> {
                launchExternalMedia(track.mediaUrl)
                _playerState.value = PlayerState(
                    currentTrack = track,
                    isPlaying = false
                )
            }
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    fun rewind15Sec() {
        exoPlayer?.let { player ->
            val newPos = (player.currentPosition - 15000L).coerceAtLeast(0L)
            player.seekTo(newPos)
            _playerState.value = _playerState.value.copy(positionMs = newPos)
        }
    }

    fun forward15Sec() {
        exoPlayer?.let { player ->
            val maxDuration = player.duration.coerceAtLeast(0L)
            val newPos = (player.currentPosition + 15000L).coerceAtMost(maxDuration)
            player.seekTo(newPos)
            _playerState.value = _playerState.value.copy(positionMs = newPos)
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.let { player ->
            val targetPos = positionMs.coerceIn(0L, player.duration.coerceAtLeast(0L))
            player.seekTo(targetPos)
            _playerState.value = _playerState.value.copy(positionMs = targetPos)
        }
    }

    fun stop() {
        stopPositionUpdates()
        exoPlayer?.stop()
        _playerState.value = PlayerState()
        BackgroundAudioService.stopService(context)
    }

    private fun launchExternalMedia(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        stop()
        coroutineScope.cancel()
        mediaSession?.release()
        mediaSession = null
        exoPlayer?.release()
        exoPlayer = null
    }
}
