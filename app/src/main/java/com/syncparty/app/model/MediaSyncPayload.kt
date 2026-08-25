package com.syncparty.app.model

data class MediaSyncPayload(
    val mode: StreamMode,
    val mediaUrlOrId: String,
    val isPlaying: Boolean,
    val currentPositionMs: Long,
    val playbackSpeed: Float = 1.0f,
    val timestampSent: Long = System.currentTimeMillis()
)
