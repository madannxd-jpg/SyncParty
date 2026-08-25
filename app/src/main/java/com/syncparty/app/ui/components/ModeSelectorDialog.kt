package com.syncparty.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.syncparty.app.model.StreamMode
import com.syncparty.app.theme.*

@Composable
fun ModeSelectorDialog(
    currentMode: StreamMode,
    onSelectMode: (StreamMode, mediaUrlOrId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(currentMode) }
    var mediaInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                "Choose Streaming Source",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ModeItem(
                    title = "Screen Share",
                    desc = "Stream phone screen & games in real-time",
                    icon = Icons.AutoMirrored.Filled.ScreenShare,
                    iconTint = AccentCyan,
                    isSelected = selectedMode == StreamMode.SCREEN_SHARE,
                    onClick = { selectedMode = StreamMode.SCREEN_SHARE }
                )

                ModeItem(
                    title = "YouTube Watch Party",
                    desc = "Synchronize YouTube videos & playlists",
                    icon = Icons.Default.PlayCircle,
                    iconTint = DangerRed,
                    isSelected = selectedMode == StreamMode.YOUTUBE,
                    onClick = {
                        selectedMode = StreamMode.YOUTUBE
                        if (mediaInput.isBlank()) mediaInput = "dQw4w9WgXcQ"
                    }
                )

                ModeItem(
                    title = "Crunchyroll & Instagram Hub",
                    desc = "In-app login & host-synced anime & reels",
                    icon = Icons.Default.LockOpen,
                    iconTint = AccentOrange,
                    isSelected = selectedMode == StreamMode.WEB_BROWSER,
                    onClick = {
                        selectedMode = StreamMode.WEB_BROWSER
                        if (mediaInput.isBlank()) mediaInput = "https://www.crunchyroll.com"
                    }
                )

                ModeItem(
                    title = "Direct Video Stream",
                    desc = "Play direct MP4 / HLS (.m3u8) anime URLs",
                    icon = Icons.Default.Movie,
                    iconTint = AccentPink,
                    isSelected = selectedMode == StreamMode.DIRECT_VIDEO,
                    onClick = {
                        selectedMode = StreamMode.DIRECT_VIDEO
                        if (mediaInput.isBlank()) mediaInput = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                    }
                )

                if (selectedMode != StreamMode.SCREEN_SHARE) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = mediaInput,
                        onValueChange = { mediaInput = it },
                        label = {
                            Text(
                                when (selectedMode) {
                                    StreamMode.YOUTUBE -> "YouTube Video ID or URL"
                                    StreamMode.WEB_BROWSER -> "Website URL (e.g. Crunchyroll / Instagram)"
                                    StreamMode.DIRECT_VIDEO -> "Video Stream URL (.mp4, .m3u8)"
                                    else -> "Target URL"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var target = mediaInput.trim()
                    if (selectedMode == StreamMode.YOUTUBE) {
                        if (target.contains("v=")) {
                            target = target.substringAfter("v=").substringBefore("&")
                        } else if (target.contains("youtu.be/")) {
                            target = target.substringAfter("youtu.be/").substringBefore("?")
                        }
                        if (target.isBlank()) target = "dQw4w9WgXcQ"
                    }
                    onSelectMode(selectedMode, target)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text("Start Streaming", color = TextPrimaryDark)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun ModeItem(
    title: String,
    desc: String,
    icon: ImageVector,
    iconTint: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) PrimaryPurple.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
