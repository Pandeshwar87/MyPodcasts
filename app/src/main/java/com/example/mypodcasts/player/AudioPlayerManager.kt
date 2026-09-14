package com.example.mypodcasts.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.mypodcasts.data.model.SourceType
import com.example.mypodcasts.data.model.Track
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

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    init {
        initExoPlayer()
    }

    private fun initExoPlayer() {
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playerState.value = _playerState.value.copy(
                        isPlaying = isPlaying,
                        durationMs = duration.coerceAtLeast(0L)
                    )
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _playerState.value = _playerState.value.copy(
                            durationMs = duration.coerceAtLeast(0L),
                            error = null
                        )
                    } else if (playbackState == Player.STATE_ENDED) {
                        _playerState.value = _playerState.value.copy(
                            isPlaying = false
                        )
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    _playerState.value = _playerState.value.copy(
                        isPlaying = false,
                        error = error.localizedMessage ?: "Playback error"
                    )
                }
            })
        }
    }

    fun playTrack(track: Track) {
        val uri = Uri.parse(track.mediaUrl)

        when (track.sourceType) {
            SourceType.LOCAL_FILE, SourceType.DIRECT_AUDIO, SourceType.DIRECT_VIDEO -> {
                exoPlayer?.let { player ->
                    val mediaItem = MediaItem.fromUri(uri)
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()
                    _playerState.value = PlayerState(
                        currentTrack = track,
                        isPlaying = true
                    )
                }
            }
            SourceType.YOUTUBE, SourceType.SPOTIFY, SourceType.OTHER -> {
                // Launch native external app or web browser for YouTube/Spotify links
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

    fun stop() {
        exoPlayer?.stop()
        _playerState.value = PlayerState()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
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
        exoPlayer?.release()
        exoPlayer = null
    }
}
