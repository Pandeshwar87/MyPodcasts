package com.example.mypodcasts.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mypodcasts.data.model.MediaType
import com.example.mypodcasts.data.model.SourceType
import com.example.mypodcasts.ui.theme.*
import com.example.mypodcasts.util.ParsedUrlMetadata
import com.example.mypodcasts.util.UrlParser

@Composable
fun AddTrackDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        artistOrChannel: String?,
        mediaUrl: String,
        sourceType: SourceType,
        mediaType: MediaType,
        thumbnailUrl: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isLocalFileTab by remember { mutableStateOf(false) }

    // Link tab state
    var inputUrl by remember { mutableStateOf("") }
    var customTitle by remember { mutableStateOf("") }
    var artistOrChannel by remember { mutableStateOf("") }

    // Detected metadata from UrlParser
    var parsedMetadata by remember { mutableStateOf<ParsedUrlMetadata?>(null) }

    // Local file picker state
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }

    val audioFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            selectedAudioUri = uri
            val meta = UrlParser.parseUrl(uri.toString())
            parsedMetadata = meta
        }
    }

    // Auto-detect URL metadata when URL changes
    LaunchedEffect(inputUrl) {
        val trimmed = inputUrl.trim()
        if (trimmed.isNotBlank() && !isLocalFileTab) {
            val detected = UrlParser.parseUrl(trimmed)
            parsedMetadata = detected
        } else if (trimmed.isBlank()) {
            parsedMetadata = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add New Track",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Tab Selection (Online Link vs Local MP3 File)
                TabRow(
                    selectedTabIndex = if (isLocalFileTab) 1 else 0,
                    containerColor = DarkSurfaceVariant,
                    contentColor = PrimaryViolet,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = !isLocalFileTab,
                        onClick = { isLocalFileTab = false },
                        text = { Text("Web / App Link") },
                        icon = { Icon(Icons.Default.Link, contentDescription = null) }
                    )
                    Tab(
                        selected = isLocalFileTab,
                        onClick = { isLocalFileTab = true },
                        text = { Text("Local MP3") },
                        icon = { Icon(Icons.Default.AudioFile, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isLocalFileTab) {
                    // Online Link Input with Paste & Clear Buttons
                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { newValue ->
                            // Clean newlines/carriage returns if pasted
                            inputUrl = newValue.replace("\n", "").replace("\r", "")
                        },
                        label = { Text("URL Link (YouTube, Spotify, MP3 stream) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        ),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (inputUrl.isNotEmpty()) {
                                    IconButton(onClick = { inputUrl = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear URL",
                                            tint = TextSecondary
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        val clipText = clipboardManager.getText()?.text
                                        if (!clipText.isNullOrBlank()) {
                                            inputUrl = clipText.trim().replace("\n", "").replace("\r", "")
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste from Clipboard",
                                        tint = PrimaryViolet
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryViolet,
                            unfocusedBorderColor = CardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Auto-detected Badge Indicator
                    parsedMetadata?.let { meta ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Auto-detected: ", style = MaterialTheme.typography.labelMedium)
                            SourceBadge(sourceType = meta.sourceType)
                        }
                    }
                } else {
                    // Local MP3 Picker Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryViolet.copy(alpha = 0.15f))
                            .clickable { audioFilePickerLauncher.launch(arrayOf("audio/*")) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = "Pick Local Audio File",
                                tint = PrimaryViolet
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedAudioUri != null) "File Selected" else "Select Local MP3 File",
                                style = MaterialTheme.typography.titleMedium,
                                color = PrimaryViolet
                            )
                        }
                    }
                    if (selectedAudioUri != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = selectedAudioUri.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title Input
                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    label = { Text("Track Title") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryViolet,
                        unfocusedBorderColor = CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Artist / Channel Input
                OutlinedTextField(
                    value = artistOrChannel,
                    onValueChange = { artistOrChannel = it },
                    label = { Text("Artist / Channel / Podcast Host") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryViolet,
                        unfocusedBorderColor = CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val canSubmit = if (isLocalFileTab) selectedAudioUri != null else inputUrl.trim().isNotBlank()
            Button(
                onClick = {
                    if (canSubmit) {
                        val mediaUrl = if (isLocalFileTab) selectedAudioUri.toString() else inputUrl.trim()
                        val meta = if (!isLocalFileTab) UrlParser.parseUrl(mediaUrl) else (parsedMetadata ?: UrlParser.parseUrl(mediaUrl))
                        val title = if (customTitle.trim().isNotBlank()) customTitle.trim() else (meta.suggestedTitle ?: "Track")

                        onConfirm(
                            title,
                            artistOrChannel.trim().ifBlank { null },
                            mediaUrl,
                            meta.sourceType,
                            meta.mediaType,
                            meta.thumbnailUrl
                        )
                    }
                },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
            ) {
                Text("Add Track")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp)
    )
}
