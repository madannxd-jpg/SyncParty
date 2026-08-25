package com.syncparty.app.sync

import android.util.Log
import com.google.gson.Gson
import com.syncparty.app.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import java.util.UUID
import java.util.concurrent.TimeUnit

class SignalingClient private constructor() {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var currentUserId: String = UUID.randomUUID().toString().take(8)
    var currentUserName: String = "User_${currentUserId.take(4)}"
    var currentUserColor: String = listOf("#8B5CF6", "#06B6D4", "#EC4899", "#10B981", "#F59E0B").random()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _roomState = MutableStateFlow(
        RoomState(
            roomId = "DEMO-01",
            roomName = "SyncParty Room",
            hostId = currentUserId,
            participants = listOf(
                Participant(
                    id = currentUserId,
                    name = currentUserName,
                    isHost = true,
                    colorHex = currentUserColor
                )
            )
        )
    )
    val roomState: StateFlow<RoomState> = _roomState

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                senderId = "system",
                senderName = "SyncParty Bot",
                text = "Welcome to SyncParty! You can stream your screen, co-watch YouTube, or log into Crunchyroll & Instagram together.",
                isSystem = true
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _reactions = MutableSharedFlow<Reaction>()
    val reactions: SharedFlow<Reaction> = _reactions

    var onOfferReceived: ((senderId: String, sdp: String) -> Unit)? = null
    var onAnswerReceived: ((senderId: String, sdp: String) -> Unit)? = null
    var onIceCandidateReceived: ((senderId: String, candidate: IceCandidateModel) -> Unit)? = null

    fun connect(serverUrl: String = "wss://echo.websocket.events") {
        try {
            val request = Request.Builder().url(serverUrl).build()
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket connected to $serverUrl")
                    _isConnected.value = true
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    handleIncomingMessage(text)
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket closed: $reason")
                    _isConnected.value = false
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.w(TAG, "WebSocket failure, continuing with local in-memory room mode: ${t.message}")
                    _isConnected.value = true
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Connect error", e)
            _isConnected.value = true
        }
    }

    private fun handleIncomingMessage(json: String) {
        try {
            val msg = gson.fromJson(json, SignalingMessage::class.java)
            if (msg.senderId == currentUserId) return

            when (msg.type) {
                "OFFER" -> msg.sdp?.let { sdp -> onOfferReceived?.invoke(msg.senderId, sdp) }
                "ANSWER" -> msg.sdp?.let { sdp -> onAnswerReceived?.invoke(msg.senderId, sdp) }
                "ICE" -> msg.candidate?.let { cand -> onIceCandidateReceived?.invoke(msg.senderId, cand) }
                "ROOM_STATE" -> msg.roomState?.let { state -> _roomState.value = state }
                "SYNC_MEDIA" -> msg.mediaSync?.let { sync ->
                    _roomState.value = _roomState.value.copy(
                        activeMode = sync.mode,
                        activeMediaUrlOrId = sync.mediaUrlOrId,
                        isPlaying = sync.isPlaying,
                        currentPositionMs = sync.currentPositionMs
                    )
                }
                "CHAT" -> msg.chatMessage?.let { chat ->
                    _chatMessages.value = _chatMessages.value + chat
                }
                "REACTION" -> msg.reactionEmoji?.let { emoji ->
                    coroutineScope.launch {
                        _reactions.emit(Reaction(emoji = emoji, senderName = msg.senderName))
                    }
                }
                "MUTE_STATE" -> {
                    val updated = _roomState.value.participants.map { p ->
                        if (p.id == msg.senderId) p.copy(isMuted = msg.isMuted ?: p.isMuted) else p
                    }
                    _roomState.value = _roomState.value.copy(participants = updated)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message", e)
        }
    }

    fun createRoom(roomName: String = "Watch Party"): String {
        val newRoomId = (100000..999999).random().toString()
        val me = Participant(
            id = currentUserId,
            name = currentUserName,
            isHost = true,
            colorHex = currentUserColor
        )
        _roomState.value = RoomState(
            roomId = newRoomId,
            roomName = roomName,
            hostId = currentUserId,
            participants = listOf(me)
        )
        sendSystemMessage("Created room $newRoomId as host.")
        return newRoomId
    }

    fun joinRoom(roomId: String, userName: String = currentUserName) {
        currentUserName = userName
        val me = Participant(
            id = currentUserId,
            name = userName,
            isHost = false,
            colorHex = currentUserColor
        )
        val currentParticipants = _roomState.value.participants.filterNot { it.id == currentUserId }
        _roomState.value = _roomState.value.copy(
            roomId = roomId,
            participants = currentParticipants + me
        )
        val joinMsg = SignalingMessage(
            type = "JOIN",
            roomId = roomId,
            senderId = currentUserId,
            senderName = currentUserName
        )
        sendMessage(joinMsg)
        sendSystemMessage("$currentUserName joined the room.")
    }

    fun updateStreamMode(mode: StreamMode, mediaUrlOrId: String) {
        val updated = _roomState.value.copy(
            activeMode = mode,
            activeMediaUrlOrId = mediaUrlOrId,
            isScreenShareActive = (mode == StreamMode.SCREEN_SHARE),
            screenSharerId = if (mode == StreamMode.SCREEN_SHARE) currentUserId else null
        )
        _roomState.value = updated
        val sync = MediaSyncPayload(
            mode = mode,
            mediaUrlOrId = mediaUrlOrId,
            isPlaying = updated.isPlaying,
            currentPositionMs = updated.currentPositionMs
        )
        sendMessage(
            SignalingMessage(
                type = "SYNC_MEDIA",
                roomId = _roomState.value.roomId,
                senderId = currentUserId,
                senderName = currentUserName,
                mediaSync = sync
            )
        )
        sendSystemMessage("Switched stream mode to ${mode.displayName}")
    }

    fun syncPlayback(isPlaying: Boolean, positionMs: Long) {
        val updated = _roomState.value.copy(
            isPlaying = isPlaying,
            currentPositionMs = positionMs
        )
        _roomState.value = updated
        val sync = MediaSyncPayload(
            mode = _roomState.value.activeMode,
            mediaUrlOrId = _roomState.value.activeMediaUrlOrId,
            isPlaying = isPlaying,
            currentPositionMs = positionMs
        )
        sendMessage(
            SignalingMessage(
                type = "SYNC_MEDIA",
                roomId = _roomState.value.roomId,
                senderId = currentUserId,
                senderName = currentUserName,
                mediaSync = sync
            )
        )
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val chat = ChatMessage(
            senderId = currentUserId,
            senderName = currentUserName,
            text = text.trim(),
            colorHex = currentUserColor
        )
        _chatMessages.value = _chatMessages.value + chat
        sendMessage(
            SignalingMessage(
                type = "CHAT",
                roomId = _roomState.value.roomId,
                senderId = currentUserId,
                senderName = currentUserName,
                chatMessage = chat
            )
        )
    }

    fun sendVoiceNote(base64Audio: String, durationSec: Int) {
        val chat = ChatMessage(
            senderId = currentUserId,
            senderName = currentUserName,
            text = "",
            colorHex = currentUserColor,
            attachmentType = AttachmentType.VOICE_NOTE,
            attachmentData = base64Audio,
            audioDurationSeconds = durationSec
        )
        _chatMessages.value = _chatMessages.value + chat
        sendMessage(
            SignalingMessage(
                type = "CHAT",
                roomId = _roomState.value.roomId,
                senderId = currentUserId,
                senderName = currentUserName,
                chatMessage = chat
            )
        )
    }

    fun sendAttachment(name: String, type: AttachmentType, base64Data: String) {
        val chat = ChatMessage(
            senderId = currentUserId,
            senderName = currentUserName,
            text = "",
            colorHex = currentUserColor,
            attachmentType = type,
            attachmentName = name,
            attachmentData = base64Data
        )
        _chatMessages.value = _chatMessages.value + chat
        sendMessage(
            SignalingMessage(
                type = "CHAT",
                roomId = _roomState.value.roomId,
                senderId = currentUserId,
                senderName = currentUserName,
                chatMessage = chat
            )
        )
    }

    fun sendReaction(emoji: String) {
        coroutineScope.launch {
            _reactions.emit(Reaction(emoji = emoji, senderName = currentUserName))
        }
        sendMessage(
            SignalingMessage(
                type = "REACTION",
                roomId = _roomState.value.roomId,
                senderId = currentUserId,
                senderName = currentUserName,
                reactionEmoji = emoji
            )
        )
    }

    fun toggleMute(isMuted: Boolean) {
        val updated = _roomState.value.participants.map {
            if (it.id == currentUserId) it.copy(isMuted = isMuted) else it
        }
        _roomState.value = _roomState.value.copy(participants = updated)
        sendMessage(
            SignalingMessage(
                type = "MUTE_STATE",
                roomId = _roomState.value.roomId,
                senderId = currentUserId,
                isMuted = isMuted
            )
        )
    }

    private fun sendSystemMessage(text: String) {
        val msg = ChatMessage(
            senderId = "system",
            senderName = "System",
            text = text,
            isSystem = true
        )
        _chatMessages.value = _chatMessages.value + msg
    }

    private fun sendMessage(msg: SignalingMessage) {
        val json = gson.toJson(msg)
        webSocket?.send(json)
    }

    fun leaveRoom() {
        sendMessage(
            SignalingMessage(
                type = "LEAVE",
                roomId = _roomState.value.roomId,
                senderId = currentUserId,
                senderName = currentUserName
            )
        )
        sendSystemMessage("$currentUserName left the room.")
    }

    companion object {
        private const val TAG = "SignalingClient"

        @Volatile
        private var INSTANCE: SignalingClient? = null

        fun getInstance(): SignalingClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SignalingClient().also { INSTANCE = it }
            }
        }
    }
}
