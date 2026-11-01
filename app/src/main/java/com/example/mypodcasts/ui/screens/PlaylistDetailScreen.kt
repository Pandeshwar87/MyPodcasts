package com.example.mypodcasts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.mypodcasts.data.model.SourceType
import com.example.mypodcasts.data.model.Track
import com.example.mypodcasts.ui.components.AddTrackDialog
import com.example.mypodcasts.ui.components.EditPlaylistDialog
import com.example.mypodcasts.ui.components.TrackItemCard
import com.example.mypodcasts.ui.theme.*
import com.example.mypodcasts.ui.viewmodel.PlaylistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    viewModel: PlaylistViewModel,
    onBackClick: () -> Unit,
    onOpenWebView: (url: String, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val playlistsWithTracks by viewModel.playlistsWithTracks.collectAsState()
    val currentItem = remember(playlistsWithTracks, playlistId) {
        playlistsWithTracks.find { it.playlist.id == playlistId }
    }

    var showAddTrackDialog by remember { mutableStateOf(false) }
    var showEditPlaylistDialog by remember { mutableStateOf(false) }

    if (currentItem == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Text("Playlist not found", style = MaterialTheme.typography.titleLarge)
        }
        return
    }

    val playlist = currentItem.playlist
    val tracks = currentItem.sortedTracks

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlist.title, style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditPlaylistDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Playlist", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddTrackDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Track") },
                text = { Text("Add Track") },
                containerColor = PrimaryViolet,
                contentColor = TextPrimary
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Header Card with Avatar & Description
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { showEditPlaylistDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(14.dp))
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
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.title,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        if (!playlist.description.isNull_or_blank()) {
                            Text(
                                text = playlist.description!!,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${tracks.size} track${if (tracks.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SecondaryViolet
                        )
                    }

                    IconButton(onClick = { showEditPlaylistDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Playlist",
                            tint = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (tracks.isEmpty()) {
                // Empty tracks state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No tracks in this playlist",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'Add Track' to paste YouTube/Spotify links or pick local MP3 files!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    itemsIndexed(
                        items = tracks,
                        key = { _, track -> track.id }
                    ) { index, track ->
                        TrackItemCard(
                            track = track,
                            onPlayClick = {
                                if (track.sourceType == SourceType.LOCAL_FILE || track.sourceType == SourceType.DIRECT_AUDIO) {
                                    viewModel.playTrack(track)
                                } else {
                                    onOpenWebView(track.mediaUrl, track.title)
                                }
                            },
                            onDeleteClick = { viewModel.deleteTrack(track) },
                            onMoveUp = {
                                if (index > 0) {
                                    val mutableList = tracks.toMutableList()
                                    val temp = mutableList[index]
                                    mutableList[index] = mutableList[index - 1]
                                    mutableList[index - 1] = temp
                                    viewModel.reorderTracks(mutableList)
                                }
                            },
                            onMoveDown = {
                                if (index < tracks.size - 1) {
                                    val mutableList = tracks.toMutableList()
                                    val temp = mutableList[index]
                                    mutableList[index] = mutableList[index + 1]
                                    mutableList[index + 1] = temp
                                    viewModel.reorderTracks(mutableList)
                                }
                            },
                            canMoveUp = index > 0,
                            canMoveDown = index < tracks.size - 1
                        )
                    }
                }
            }
        }
    }

    if (showAddTrackDialog) {
        AddTrackDialog(
            onDismiss = { showAddTrackDialog = false },
            onConfirm = { title, artist, mediaUrl, sourceType, mediaType, thumbnail ->
                viewModel.addTrack(
                    playlistId = playlistId,
                    title = title,
                    artistOrChannel = artist,
                    mediaUrl = mediaUrl,
                    sourceType = sourceType,
                    mediaType = mediaType,
                    thumbnailUrl = thumbnail
                )
                showAddTrackDialog = false
            }
        )
    }

    if (showEditPlaylistDialog) {
        EditPlaylistDialog(
            playlist = playlist,
            onDismiss = { showEditPlaylistDialog = false },
            onConfirm = { newTitle, newDesc, newAvatarUri ->
                viewModel.updatePlaylist(playlist, newTitle, newDesc, newAvatarUri)
                showEditPlaylistDialog = false
            }
        )
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()

