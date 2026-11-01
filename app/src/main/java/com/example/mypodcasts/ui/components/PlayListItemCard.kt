package com.example.mypodcasts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.mypodcasts.data.model.PlayListWithTracks
import com.example.mypodcasts.ui.theme.*

@Composable
fun PlayListItemCard(
    playlistWithTracks: PlayListWithTracks,
    onClick: () -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    modifier: Modifier = Modifier
) {
    val playlist = playlistWithTracks.playlist
    val trackCount = playlistWithTracks.tracks.size

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Playlist Avatar Image or Icon Placeholder
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!playlist.coverImageUrl.isNull_or_blank()) {
                    AsyncImage(
                        model = playlist.coverImageUrl,
                        contentDescription = playlist.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = PrimaryViolet,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Playlist Title & Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.titleLarge
                )
                if (!playlist.description.isNull_or_blank()) {
                    Text(
                        text = playlist.description!!,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$trackCount track${if (trackCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = SecondaryViolet
                )
            }

            // Quick Reorder Up / Down Buttons
            if (onMoveUp != null && canMoveUp) {
                IconButton(
                    onClick = onMoveUp,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Move Up",
                        tint = SecondaryViolet,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (onMoveDown != null && canMoveDown) {
                IconButton(
                    onClick = onMoveDown,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Move Down",
                        tint = SecondaryViolet,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
