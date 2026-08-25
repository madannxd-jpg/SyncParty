package com.syncparty.app.model

data class Participant(
    val id: String,
    val name: String,
    val isHost: Boolean = false,
    val isMuted: Boolean = false,
    val isScreenSharing: Boolean = false,
    val isSpeaking: Boolean = false,
    val colorHex: String = "#8B5CF6"
)
