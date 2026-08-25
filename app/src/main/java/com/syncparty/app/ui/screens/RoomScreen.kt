package com.syncparty.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.syncparty.app.model.AttachmentType
import com.syncparty.app.model.Reaction
import com.syncparty.app.model.StreamMode
import com.syncparty.app.player.DirectVideoPlayer
import com.syncparty.app.player.SyncedWebBrowser
import com.syncparty.app.player.YouTubeWatchView
import com.syncparty.app.sync.SignalingClient
import com.syncparty.app.theme.*
import com.syncparty.app.ui.components.*
import com.syncparty.app.webrtc.WebRtcManager
import com.syncparty.app.webrtc.WebRtcVideoView
import kotlinx.coroutines.flow.collectLatest

@Composable
fun RoomScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    isScreenSharing: Boolean,
    onToggleScreenShare: () -> Unit,
    onLeaveRoom: () -> Unit,
    onEnterPip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val signalingClient = remember { SignalingClient.getInstance() }
    val roomState by signalingClient.roomState.collectAsState()
    val chatMessages by signalingClient.chatMessages.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val webRtcManager = remember { WebRtcManager.getInstance(context) }
    val remoteVideoTrack by webRtcManager.remoteVideoTrack.collectAsState()
    val localVideoTrack = remember(isScreenSharing) {
        if (isScreenSharing) webRtcManager.getLocalVideoTrack() else null
    }

    var isMuted by remember { mutableStateOf(false) }
    var isChatOpen by remember { mutableStateOf(false) }
    var isParticipantsOpen by remember { mutableStateOf(false) }
    var isModeSelectorOpen by remember { mutableStateOf(false) }

    val reactionsList = remember { mutableStateListOf<Reaction>() }

    LaunchedEffect(Unit) {
        signalingClient.reactions.collectLatest { reaction ->
            reactionsList.add(reaction)
        }
    }

    val isHost = roomState.hostId == signalingClient.currentUserId

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Media Stage
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isChatOpen) 320.dp else 0.dp),
            contentAlignment = Alignment.Center
        ) {
            when (roomState.activeMode) {
                StreamMode.SCREEN_SHARE -> {
                    val activeTrack = if (isScreenSharing) localVideoTrack else remoteVideoTrack
                    if (activeTrack != null) {
                        WebRtcVideoView(
                            videoTrack = activeTrack,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Empty State Placeholder
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(AccentCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ScreenShare,
                                    contentDescription = null,
                                    tint = AccentCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Screen Share Stream",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isHost) "Tap 'Share' in the bottom dock to stream your phone screen to friends." else "Waiting for host to share screen...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                StreamMode.YOUTUBE -> {
                    YouTubeWatchView(
                        videoId = roomState.activeMediaUrlOrId,
                        isPlaying = roomState.isPlaying,
                        seekPositionSeconds = (roomState.currentPositionMs / 1000).toFloat(),
                        isHost = isHost,
                        onPlaybackStateChanged = { playing, posSec ->
                            signalingClient.syncPlayback(playing, (posSec * 1000).toLong())
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                StreamMode.WEB_BROWSER -> {
                    SyncedWebBrowser(
                        initialUrl = roomState.activeMediaUrlOrId,
                        isHost = isHost,
                        onUrlChanged = { newUrl ->
                            if (isHost) {
                                signalingClient.updateStreamMode(StreamMode.WEB_BROWSER, newUrl)
                            }
                        },
                        onVideoSync = { action, time ->
                            if (isHost) {
                                val isPlaying = (action == "PLAY")
                                signalingClient.syncPlayback(isPlaying, (time * 1000).toLong())
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                StreamMode.DIRECT_VIDEO -> {
                    DirectVideoPlayer(
                        videoUrl = roomState.activeMediaUrlOrId,
                        isPlaying = roomState.isPlaying,
                        seekPositionMs = roomState.currentPositionMs,
                        isHost = isHost,
                        onPlaybackStateChanged = { playing, posMs ->
                            signalingClient.syncPlayback(playing, posMs)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Floating Reactions Overlays
        FloatingReactionsOverlay(reactions = reactionsList)

        // Top App Bar
        PartyAppBar(
            roomState = roomState,
            participantCount = roomState.participants.size,
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            onParticipantsClick = { isParticipantsOpen = true },
            onLeaveClick = {
                signalingClient.leaveRoom()
                onLeaveRoom()
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Bottom Controls Dock
        ControlsBar(
            isMuted = isMuted,
            isScreenSharing = isScreenSharing,
            isChatOpen = isChatOpen,
            onToggleMute = {
                isMuted = !isMuted
                webRtcManager.setAudioEnabled(!isMuted)
                signalingClient.toggleMute(isMuted)
            },
            onToggleScreenShare = onToggleScreenShare,
            onChangeModeClick = { isModeSelectorOpen = true },
            onToggleChat = { isChatOpen = !isChatOpen },
            onSendReaction = { emoji -> signalingClient.sendReaction(emoji) },
            onPipClick = onEnterPip,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Upgraded Chat Sheet with Voice Notes & Attachments
        if (isChatOpen) {
            ChatSheet(
                messages = chatMessages,
                onSendMessage = { text -> signalingClient.sendChatMessage(text) },
                onSendVoiceNote = { audio, duration -> signalingClient.sendVoiceNote(audio, duration) },
                onSendAttachment = { name, type, data -> signalingClient.sendAttachment(name, type, data) },
                onClose = { isChatOpen = false },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Participants Bottom Sheet
        if (isParticipantsOpen) {
            ParticipantsSheet(
                participants = roomState.participants,
                onClose = { isParticipantsOpen = false },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Mode Selector Dialog
        if (isModeSelectorOpen) {
            ModeSelectorDialog(
                currentMode = roomState.activeMode,
                onSelectMode = { mode, mediaUrlOrId ->
                    signalingClient.updateStreamMode(mode, mediaUrlOrId)
                },
                onDismiss = { isModeSelectorOpen = false }
            )
        }
    }
}
