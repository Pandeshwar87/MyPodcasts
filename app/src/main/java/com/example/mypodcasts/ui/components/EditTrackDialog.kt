package com.example.mypodcasts.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mypodcasts.data.model.Track
import com.example.mypodcasts.ui.theme.*

@Composable
fun EditTrackDialog(
    track: Track,
    onDismiss: () -> Unit,
    onConfirm: (newTitle: String, newArtist: String?, newMediaUrl: String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var title by remember { mutableStateOf(track.title) }
    var artistOrChannel by remember { mutableStateOf(track.artistOrChannel ?: "") }
    var mediaUrl by remember { mutableStateOf(track.mediaUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Track Details",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Track Title *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    trailingIcon = {
                        if (title.isNotEmpty()) {
                            IconButton(onClick = { title = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Title",
                                    tint = TextSecondary
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

                Spacer(modifier = Modifier.height(12.dp))

                // Artist / Channel Input
                OutlinedTextField(
                    value = artistOrChannel,
                    onValueChange = { artistOrChannel = it },
                    label = { Text("Artist / Channel / Podcast Host") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryViolet,
                        unfocusedBorderColor = CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Media URL Link Input
                OutlinedTextField(
                    value = mediaUrl,
                    onValueChange = { newValue ->
                        mediaUrl = newValue.replace("\n", "").replace("\r", "")
                    },
                    label = { Text("URL Link / Media Path *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (mediaUrl.isNotEmpty()) {
                                IconButton(onClick = { mediaUrl = "" }) {
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
                                        mediaUrl = clipText.trim().replace("\n", "").replace("\r", "")
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
            }
        },
        confirmButton = {
            val canSubmit = title.trim().isNotBlank() && mediaUrl.trim().isNotBlank()
            Button(
                onClick = {
                    if (canSubmit) {
                        onConfirm(
                            title.trim(),
                            artistOrChannel.trim().ifBlank { null },
                            mediaUrl.trim()
                        )
                    }
                },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
            ) {
                Text("Save Changes")
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
