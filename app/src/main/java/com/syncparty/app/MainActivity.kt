package com.syncparty.app

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.syncparty.app.auth.GoogleAuthManager
import com.syncparty.app.data.local.RoomHistoryItem
import com.syncparty.app.data.local.SyncPartyDbHelper
import com.syncparty.app.data.local.UserProfile
import com.syncparty.app.data.local.UserSessionManager
import com.syncparty.app.model.StreamMode
import com.syncparty.app.service.ScreenShareService
import com.syncparty.app.sync.SignalingClient
import com.syncparty.app.theme.SyncPartyTheme
import com.syncparty.app.theme.ThemeMode
import com.syncparty.app.ui.screens.AuthScreen
import com.syncparty.app.ui.screens.HomeScreen
import com.syncparty.app.ui.screens.RoomScreen
import com.syncparty.app.webrtc.WebRtcManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var signalingClient: SignalingClient
    private lateinit var webRtcManager: WebRtcManager
    private lateinit var sessionManager: UserSessionManager
    private lateinit var dbHelper: SyncPartyDbHelper
    private lateinit var authManager: GoogleAuthManager

    private var isScreenSharing by mutableStateOf(false)
    private var currentScreen by mutableStateOf("auth")
    private var isDarkTheme by mutableStateOf(true)
    private var currentUserProfile by mutableStateOf<UserProfile?>(null)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (!recordAudioGranted) {
            Toast.makeText(this, "Microphone permission is required for voice chat & notes", Toast.LENGTH_SHORT).show()
        }
    }

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            isScreenSharing = true
            ScreenShareService.startService(this, result.resultCode, result.data!!)
            signalingClient.updateStreamMode(StreamMode.SCREEN_SHARE, "local_screen")
            Toast.makeText(this, "Screen sharing started!", Toast.LENGTH_SHORT).show()
        } else {
            isScreenSharing = false
            Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        signalingClient = SignalingClient.getInstance()
        webRtcManager = WebRtcManager.getInstance(this)
        sessionManager = UserSessionManager.getInstance(this)
        dbHelper = SyncPartyDbHelper.getInstance(this)
        authManager = GoogleAuthManager(this)

        signalingClient.connect()
        checkPermissions()

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val themeMode = if (isDarkTheme) ThemeMode.DARK else ThemeMode.LIGHT

            LaunchedEffect(Unit) {
                val profile = sessionManager.getUserProfile()
                if (profile != null) {
                    currentUserProfile = profile
                    signalingClient.currentUserId = profile.userId
                    signalingClient.currentUserName = profile.displayName
                    currentScreen = "home"
                } else {
                    currentScreen = "auth"
                }
            }

            SyncPartyTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (currentScreen) {
                        "auth" -> {
                            AuthScreen(
                                onLoginSuccess = { user ->
                                    currentUserProfile = user
                                    signalingClient.currentUserId = user.userId
                                    signalingClient.currentUserName = user.displayName
                                    currentScreen = "home"
                                }
                            )
                        }

                        "home" -> {
                            currentUserProfile?.let { user ->
                                HomeScreen(
                                    userProfile = user,
                                    isDarkTheme = isDarkTheme,
                                    onToggleTheme = { isDarkTheme = !isDarkTheme },
                                    onSignOut = {
                                        coroutineScope.launch {
                                            authManager.signOut()
                                            currentUserProfile = null
                                            currentScreen = "auth"
                                        }
                                    },
                                    onCreateRoom = { roomName ->
                                        val roomId = signalingClient.createRoom(roomName)
                                        dbHelper.saveRoomHistory(
                                            RoomHistoryItem(
                                                roomId = roomId,
                                                roomName = roomName,
                                                hostName = user.displayName,
                                                lastJoined = System.currentTimeMillis(),
                                                activeMode = StreamMode.YOUTUBE
                                            )
                                        )
                                        currentScreen = "room"
                                    },
                                    onJoinRoom = { roomId, userName ->
                                        signalingClient.joinRoom(roomId, userName)
                                        dbHelper.saveRoomHistory(
                                            RoomHistoryItem(
                                                roomId = roomId,
                                                roomName = "Party $roomId",
                                                hostName = "Host",
                                                lastJoined = System.currentTimeMillis(),
                                                activeMode = StreamMode.YOUTUBE
                                            )
                                        )
                                        currentScreen = "room"
                                    }
                                )
                            }
                        }

                        "room" -> {
                            RoomScreen(
                                isDarkTheme = isDarkTheme,
                                onToggleTheme = { isDarkTheme = !isDarkTheme },
                                isScreenSharing = isScreenSharing,
                                onToggleScreenShare = {
                                    if (isScreenSharing) {
                                        stopScreenShare()
                                    } else {
                                        startScreenShare()
                                    }
                                },
                                onLeaveRoom = {
                                    stopScreenShare()
                                    currentScreen = "home"
                                },
                                onEnterPip = {
                                    enterPipMode()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun startScreenShare() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        screenCaptureLauncher.launch(captureIntent)
    }

    private fun stopScreenShare() {
        if (isScreenSharing) {
            isScreenSharing = false
            ScreenShareService.stopService(this)
            Toast.makeText(this, "Screen sharing stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                Toast.makeText(this, "Picture-in-Picture not supported on this device", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (currentScreen == "room") {
            enterPipMode()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScreenShare()
        webRtcManager.closeAll()
    }
}
