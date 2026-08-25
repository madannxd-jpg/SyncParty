package com.syncparty.app.model

import java.util.UUID

enum class AttachmentType {
    NONE, IMAGE, FILE, VOICE_NOTE
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderName: String,
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val colorHex: String = "#8B5CF6",
    val isSystem: Boolean = false,
    val attachmentType: AttachmentType = AttachmentType.NONE,
    val attachmentName: String? = null,
    val attachmentData: String? = null, // Base64 image data or file uri
    val audioDurationSeconds: Int = 0
)
