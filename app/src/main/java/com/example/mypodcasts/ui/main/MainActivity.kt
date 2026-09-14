package com.example.mypodcasts.ui.main

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mypodcasts.ui.components.MiniPlayerBar
import com.example.mypodcasts.ui.screens.PlaylistDetailScreen
import com.example.mypodcasts.ui.screens.PlaylistListScreen
import com.example.mypodcasts.ui.theme.MyPodcastsTheme
import com.example.mypodcasts.ui.viewmodel.PlaylistViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PlaylistViewModel by viewModels()

    // Permission launcher for accessing media on device
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestMediaPermissions()

        setContent {
            MyPodcastsTheme {
                val navController = rememberNavController()
                val playerState by viewModel.playerState.collectAsState()

                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = "playlist_list"
                    ) {
                        composable("playlist_list") {
                            PlaylistListScreen(
                                viewModel = viewModel,
                                onPlaylistClick = { id ->
                                    navController.navigate("playlist_detail/$id")
                                }
                            )
                        }

                        composable(
                            route = "playlist_detail/{playlistId}",
                            arguments = listOf(
                                navArgument("playlistId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
                            PlaylistDetailScreen(
                                playlistId = playlistId,
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }

                    // Mini Player Sticky Overlay at the bottom
                    if (playerState.currentTrack != null) {
                        MiniPlayerBar(
                            playerState = playerState,
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onClose = { viewModel.playerManager.stop() },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }

    private fun requestMediaPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }
}
