package com.syncparty.app.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syncparty.app.model.AttachmentType
import com.syncparty.app.model.ChatMessage
import com.syncparty.app.player.VoiceRecorderHelper
import com.syncparty.app.theme.*
import kotlinx.coroutines.delay
import java.io.InputStream

@Composable
fun ChatSheet(
    messages: List<ChatMessage>,
    onSendMessage: (text: String) -> Unit,
    onSendVoiceNote: (base64Audio: String, durationSec: Int) -> Unit = { _, _ -> },
    onSendAttachment: (name: String, type: AttachmentType, base64Data: String) -> Unit = { _, _, _ -> },
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val voiceRecorder = remember { VoiceRecorderHelper(context) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var currentlyPlayingMessageId by remember { mutableStateOf<String?>(null) }

    // Image/File Picker Launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                if (bytes != null) {
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val mimeType = context.contentResolver.getType(it) ?: ""
                    val isImage = mimeType.startsWith("image/")
                    onSendAttachment(
                        if (isImage) "Photo_${System.currentTimeMillis()}.jpg" else "File_${System.currentTimeMillis()}",
                        if (isImage) AttachmentType.IMAGE else AttachmentType.FILE,
                        base64
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            recordingSeconds = 0
            while (isRecordingVoice) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.65f)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
            .padding(16.dp)
    ) {
        // Chat Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Chat, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Room Chat & Media",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline,
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                if (message.isSystem) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentCyan
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = try {
                                Color(android.graphics.Color.parseColor(message.colorHex))
                            } catch (e: Exception) {
                                PrimaryPurple
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Text content
                        if (message.text.isNotBlank()) {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Voice Note rendering
                        if (message.attachmentType == AttachmentType.VOICE_NOTE && message.attachmentData != null) {
                            val isPlaying = currentlyPlayingMessageId == message.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PrimaryPurple.copy(alpha = 0.15f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (isPlaying) {
                                            voiceRecorder.stopPlayback()
                                            currentlyPlayingMessageId = null
                                        } else {
                                            currentlyPlayingMessageId = message.id
                                            voiceRecorder.playVoiceNote(message.attachmentData) {
                                                currentlyPlayingMessageId = null
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryPurple)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play voice note",
                                        tint = TextPrimaryDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Voice Message (${message.audioDurationSeconds}s)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isPlaying) "Playing audio..." else "Tap to listen",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Image attachment rendering
                        if (message.attachmentType == AttachmentType.IMAGE && message.attachmentData != null) {
                            val bitmap = remember(message.attachmentData) {
                                try {
                                    val bytes = Base64.decode(message.attachmentData, Base64.NO_WRAP)
                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (bitmap != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "Attached photo",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        }

                        // File attachment rendering
                        if (message.attachmentType == AttachmentType.FILE) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentOrange.copy(alpha = 0.15f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = AccentOrange)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message.attachmentName ?: "Attached File",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Live Voice Recording Bar indicator
        AnimatedVisibility(visible = isRecordingVoice) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DangerRed.copy(alpha = 0.2f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(DangerRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recording Voice Note... ${recordingSeconds}s",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = DangerRed
                    )
                }
                Text(
                    text = "Release to Send",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Input & Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Attachment Button (Images & Files)
            IconButton(
                onClick = { filePickerLauncher.launch("*/*") },
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = "Attach file/image",
                    tint = AccentCyan
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Text input field
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Type a message...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Hold-to-Record Voice Note OR Send Text Button
            if (inputText.isNotBlank()) {
                IconButton(
                    onClick = {
                        onSendMessage(inputText)
                        inputText = ""
                    },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryPurple)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = TextPrimaryDark)
                }
            } else {
                IconButton(
                    onClick = {
                        if (!isRecordingVoice) {
                            val started = voiceRecorder.startRecording()
                            if (started) isRecordingVoice = true
                        } else {
                            isRecordingVoice = false
                            val (base64Audio, duration) = voiceRecorder.stopRecording()
                            if (base64Audio != null) {
                                onSendVoiceNote(base64Audio, duration)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isRecordingVoice) DangerRed else PrimaryPurple)
                ) {
                    Icon(
                        imageVector = if (isRecordingVoice) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Voice Note",
                        tint = TextPrimaryDark
                    )
                }
            }
        }
    }
}
