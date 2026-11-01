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
import com.example.mypodcasts.ui.screens.WebViewScreen
import com.example.mypodcasts.ui.theme.MyPodcastsTheme
import com.example.mypodcasts.ui.viewmodel.PlaylistViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
                                onBackClick = { navController.popBackStack() },
                                onOpenWebView = { url, title ->
                                    val encodedUrl = try {
                                        URLEncoder.encode(url, StandardCharsets.UTF_8.name())
                                    } catch (_: Exception) {
                                        url
                                    }
                                    val encodedTitle = try {
                                        URLEncoder.encode(title, StandardCharsets.UTF_8.name())
                                    } catch (_: Exception) {
                                        title
                                    }
                                    navController.navigate("webview?url=$encodedUrl&title=$encodedTitle")
                                }
                            )
                        }

                        composable(
                            route = "webview?url={url}&title={title}",
                            arguments = listOf(
                                navArgument("url") { type = NavType.StringType },
                                navArgument("title") {
                                    type = NavType.StringType
                                    defaultValue = "Video Player"
                                }
                            )
                        ) { backStackEntry ->
                            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
                            val title = backStackEntry.arguments?.getString("title") ?: "Video Player"

                            val decodedUrl = try {
                                URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.name())
                            } catch (_: Exception) {
                                encodedUrl
                            }
                            val decodedTitle = try {
                                URLDecoder.decode(title, StandardCharsets.UTF_8.name())
                            } catch (_: Exception) {
                                title
                            }

                            WebViewScreen(
                                initialUrl = decodedUrl,
                                title = decodedTitle,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }

                    // Mini Player Sticky Overlay at the bottom for local audio
                    if (playerState.currentTrack != null) {
                        MiniPlayerBar(
                            playerState = playerState,
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onRewind15 = { viewModel.rewind15Sec() },
                            onForward15 = { viewModel.forward15Sec() },
                            onSeekTo = { pos -> viewModel.seekTo(pos) },
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
