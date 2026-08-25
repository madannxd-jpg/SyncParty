package com.syncparty.app.model

data class SignalingMessage(
    val type: String, // "JOIN", "ROOM_STATE", "OFFER", "ANSWER", "ICE", "SYNC_MEDIA", "CHAT", "REACTION", "LEAVE", "MUTE_STATE"
    val roomId: String,
    val senderId: String,
    val senderName: String = "",
    val targetId: String? = null,
    val sdp: String? = null,
    val candidate: IceCandidateModel? = null,
    val mediaSync: MediaSyncPayload? = null,
    val chatMessage: ChatMessage? = null,
    val reactionEmoji: String? = null,
    val isMuted: Boolean? = null,
    val isScreenSharing: Boolean? = null,
    val roomState: RoomState? = null
)

data class IceCandidateModel(
    val sdpMid: String,
    val sdpMLineIndex: Int,
    val candidate: String
)
