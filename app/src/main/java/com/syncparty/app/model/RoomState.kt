package com.syncparty.app.model

data class RoomState(
    val roomId: String,
    val roomName: String = "SyncParty Room",
    val hostId: String,
    val activeMode: StreamMode = StreamMode.YOUTUBE,
    val activeMediaUrlOrId: String = "dQw4w9WgXcQ", // Default sample
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val isScreenShareActive: Boolean = false,
    val screenSharerId: String? = null,
    val participants: List<Participant> = emptyList()
)
