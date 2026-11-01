package com.example.mypodcasts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mypodcasts.data.model.SourceType
import com.example.mypodcasts.data.model.Track
import com.example.mypodcasts.ui.theme.*

@Composable
fun TrackItemCard(
    track: Track,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlayClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play Button on Far Left (Clean compact 26.dp circle)
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(PrimaryViolet.copy(alpha = 0.25f))
                    .clickable { onPlayClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Track",
                    tint = PrimaryViolet,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Increased Right Margin after Play Button
            Spacer(modifier = Modifier.width(16.dp))

            // Track Thumbnail or Source Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!track.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = track.thumbnailUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val icon = when (track.sourceType) {
                        SourceType.LOCAL_FILE -> Icons.Default.Folder
                        SourceType.YOUTUBE -> Icons.Default.Videocam
                        SourceType.SPOTIFY -> Icons.Default.Headphones
                        else -> Icons.Default.MusicNote
                    }
                    val iconTint = when (track.sourceType) {
                        SourceType.YOUTUBE -> AccentYouTubeRed
                        SourceType.SPOTIFY -> AccentSpotifyGreen
                        SourceType.LOCAL_FILE -> AccentEmerald
                        else -> PrimaryViolet
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Details in Middle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Source Tag Badge
                    SourceBadge(sourceType = track.sourceType)

                    if (!track.artistOrChannel.isNullOrBlank()) {
                        Text(
                            text = "• ${track.artistOrChannel}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            // Right Action Controls: Reorder Arrows & Delete Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
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

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Track",
                        tint = TextSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SourceBadge(sourceType: SourceType) {
    val (label, bgColor, textColor) = when (sourceType) {
        SourceType.LOCAL_FILE -> Triple("Local MP3", AccentEmerald.copy(alpha = 0.2f), AccentEmerald)
        SourceType.YOUTUBE -> Triple("YouTube", AccentYouTubeRed.copy(alpha = 0.2f), AccentYouTubeRed)
        SourceType.SPOTIFY -> Triple("Spotify", AccentSpotifyGreen.copy(alpha = 0.2f), AccentSpotifyGreen)
        SourceType.DIRECT_AUDIO -> Triple("Audio Stream", PrimaryViolet.copy(alpha = 0.2f), PrimaryViolet)
        SourceType.DIRECT_VIDEO -> Triple("Video Stream", AccentPink.copy(alpha = 0.2f), AccentPink)
        SourceType.OTHER -> Triple("Web Link", Color.Gray.copy(alpha = 0.2f), Color.LightGray)
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
