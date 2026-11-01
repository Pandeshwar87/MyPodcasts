package com.example.mypodcasts.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mypodcasts.data.model.PlayList
import com.example.mypodcasts.ui.components.AddPlaylistDialog
import com.example.mypodcasts.ui.components.EditPlaylistDialog
import com.example.mypodcasts.ui.components.PlayListItemCard
import com.example.mypodcasts.ui.theme.*
import com.example.mypodcasts.ui.viewmodel.PlaylistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistListScreen(
    viewModel: PlaylistViewModel,
    onPlaylistClick: (playlistId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val playlistsWithTracks by viewModel.playlistsWithTracks.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPlaylist by remember { mutableStateOf<PlayList?>(null) }

    val filteredPlaylists = remember(playlistsWithTracks, searchQuery) {
        if (searchQuery.isBlank()) {
            playlistsWithTracks
        } else {
            playlistsWithTracks.filter { item ->
                item.playlist.title.contains(searchQuery, ignoreCase = true) ||
                        (item.playlist.description?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = null,
                            tint = PrimaryViolet,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "My Playlists",
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "New Playlist") },
                text = { Text("New Playlist") },
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search playlists...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryViolet,
                    unfocusedBorderColor = CardBorder,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredPlaylists.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No Playlists Yet" else "No matching playlists",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "Tap '+' to create your first playlist!" else "Try a different search term.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                // Playlists List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    itemsIndexed(
                        items = filteredPlaylists,
                        key = { _, item -> item.playlist.id }
                    ) { index, item ->
                        PlayListItemCard(
                            playlistWithTracks = item,
                            onClick = { onPlaylistClick(item.playlist.id) },
                            onEditClick = { editingPlaylist = item.playlist },
                            onDeleteClick = { viewModel.deletePlaylist(item.playlist) },
                            onMoveUp = {
                                if (index > 0) {
                                    val mutableList = playlistsWithTracks.map { it.playlist }.toMutableList()
                                    val temp = mutableList[index]
                                    mutableList[index] = mutableList[index - 1]
                                    mutableList[index - 1] = temp
                                    viewModel.reorderPlaylists(mutableList)
                                }
                            },
                            onMoveDown = {
                                if (index < filteredPlaylists.size - 1) {
                                    val mutableList = playlistsWithTracks.map { it.playlist }.toMutableList()
                                    val temp = mutableList[index]
                                    mutableList[index] = mutableList[index + 1]
                                    mutableList[index + 1] = temp
                                    viewModel.reorderPlaylists(mutableList)
                                }
                            },
                            canMoveUp = index > 0,
                            canMoveDown = index < filteredPlaylists.size - 1
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPlaylistDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, desc, avatarUri ->
                viewModel.createPlaylist(title, desc, avatarUri)
                showAddDialog = false
            }
        )
    }

    editingPlaylist?.let { playlist ->
        EditPlaylistDialog(
            playlist = playlist,
            onDismiss = { editingPlaylist = null },
            onConfirm = { newTitle, newDesc, newAvatarUri ->
                viewModel.updatePlaylist(playlist, newTitle, newDesc, newAvatarUri)
                editingPlaylist = null
            }
        )
    }
}
