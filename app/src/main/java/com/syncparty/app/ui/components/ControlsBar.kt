package com.syncparty.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.automirrored.filled.StopScreenShare
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.syncparty.app.theme.*

@Composable
fun ControlsBar(
    isMuted: Boolean,
    isScreenSharing: Boolean,
    isChatOpen: Boolean,
    onToggleMute: () -> Unit,
    onToggleScreenShare: () -> Unit,
    onChangeModeClick: () -> Unit,
    onToggleChat: () -> Unit,
    onSendReaction: (String) -> Unit,
    onPipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showReactionPicker by remember { mutableStateOf(false) }
    val emojiList = listOf("🔥", "❤️", "😂", "🎉", "🍿", "👏", "😱")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Quick Reaction Picker Bar
        if (showReactionPicker) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                emojiList.forEach { emoji ->
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                onSendReaction(emoji)
                                showReactionPicker = false
                            }
                            .padding(4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Main Controls Dock
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Mute / Unmute Button
            DockIconButton(
                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label = if (isMuted) "Unmute" else "Mute",
                isActive = !isMuted,
                activeColor = AccentGreen,
                inactiveColor = DangerRed,
                onClick = onToggleMute
            )

            // Screen Share Toggle
            DockIconButton(
                icon = if (isScreenSharing) Icons.AutoMirrored.Filled.StopScreenShare else Icons.AutoMirrored.Filled.ScreenShare,
                label = "Share",
                isActive = isScreenSharing,
                activeColor = AccentCyan,
                inactiveColor = MaterialTheme.colorScheme.surfaceVariant,
                onClick = onToggleScreenShare
            )

            // Stream Mode Switcher (YouTube, Web Login, Crunchyroll, Video)
            DockIconButton(
                icon = Icons.Default.VideoLibrary,
                label = "Sources",
                isActive = true,
                activeColor = PrimaryPurple,
                inactiveColor = MaterialTheme.colorScheme.surfaceVariant,
                onClick = onChangeModeClick
            )

            // Live Chat Toggle
            DockIconButton(
                icon = Icons.Default.ChatBubble,
                label = "Chat",
                isActive = isChatOpen,
                activeColor = AccentPink,
                inactiveColor = MaterialTheme.colorScheme.surfaceVariant,
                onClick = onToggleChat
            )

            // Emoji Reactions Button
            IconButton(
                onClick = { showReactionPicker = !showReactionPicker },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(Icons.Default.AddReaction, contentDescription = "React", tint = AccentOrange)
            }

            // Picture-in-Picture Button
            IconButton(
                onClick = onPipClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(Icons.Default.PictureInPicture, contentDescription = "PiP", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun DockIconButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(if (isActive) activeColor else inactiveColor)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive && activeColor == AccentGreen) Color.Black else TextPrimaryDark,
            modifier = Modifier.size(22.dp)
        )
    }
}
