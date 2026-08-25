package com.syncparty.app.webrtc

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.*
import java.util.concurrent.ConcurrentHashMap

class WebRtcManager private constructor(private val context: Context) {

    private val eglBase = EglBase.create()
    val eglContext: EglBase.Context get() = eglBase.eglBaseContext

    private val peerConnectionFactory: PeerConnectionFactory

    private var screenCapturer: ScreenCapturerAndroid? = null
    private var videoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null

    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    private val peerConnections = ConcurrentHashMap<String, PeerConnection>()

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
    )

    init {
        val videoEncoderFactory = DefaultVideoEncoderFactory(
            eglBase.eglBaseContext,
            /* enableIntelVp8Encoder = */ true,
            /* enableH264HighProfile = */ true
        )
        val videoDecoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(videoEncoderFactory)
            .setVideoDecoderFactory(videoDecoderFactory)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()

        createAudioTrack()
    }

    private fun createAudioTrack() {
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        }
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory.createAudioTrack("ARDAMSa0", audioSource)
        localAudioTrack?.setEnabled(true)
    }

    fun setAudioEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun startScreenCapture(resultCode: Int, resultData: Intent, width: Int = 1280, height: Int = 720, fps: Int = 30) {
        stopScreenCapture()
        val capturer = ScreenCapturerAndroid(resultData, object : MediaProjection.Callback() {
            override fun onStop() {
                Log.d(TAG, "MediaProjection onStop triggered")
                stopScreenCapture()
            }
        })
        screenCapturer = capturer
        val surfaceTextureHelper = SurfaceTextureHelper.create("ScreenCaptureThread", eglBase.eglBaseContext)
        val vSource = peerConnectionFactory.createVideoSource(capturer.isScreencast)
        videoSource = vSource
        capturer.initialize(surfaceTextureHelper, context, vSource.capturerObserver)
        capturer.startCapture(width, height, fps)

        localVideoTrack = peerConnectionFactory.createVideoTrack("ARDAMSv0", vSource)
        localVideoTrack?.setEnabled(true)

        // Attach local track to all active peer connections
        peerConnections.values.forEach { pc ->
            localVideoTrack?.let { track ->
                pc.addTrack(track, listOf("ARDAMS"))
            }
        }
    }

    fun stopScreenCapture() {
        try {
            screenCapturer?.stopCapture()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping screen capture", e)
        }
        screenCapturer?.dispose()
        screenCapturer = null

        localVideoTrack?.setEnabled(false)
        localVideoTrack?.dispose()
        localVideoTrack = null

        videoSource?.dispose()
        videoSource = null
    }

    fun getLocalVideoTrack(): VideoTrack? = localVideoTrack

    fun createPeerConnection(
        peerId: String,
        onIceCandidate: (IceCandidate) -> Unit,
        onRemoteTrackReceived: (VideoTrack) -> Unit
    ): PeerConnection? {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        val observer = object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE Connection State for $peerId: $state")
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let(onIceCandidate)
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                receiver?.track()?.let { track ->
                    if (track is VideoTrack) {
                        _remoteVideoTrack.value = track
                        onRemoteTrackReceived(track)
                    }
                }
            }
        }

        val pc = peerConnectionFactory.createPeerConnection(rtcConfig, observer) ?: return null

        // Add local audio and video tracks
        localAudioTrack?.let { pc.addTrack(it, listOf("ARDAMS")) }
        localVideoTrack?.let { pc.addTrack(it, listOf("ARDAMS")) }

        peerConnections[peerId] = pc
        return pc
    }

    fun closePeer(peerId: String) {
        peerConnections.remove(peerId)?.let { pc ->
            pc.close()
            pc.dispose()
        }
    }

    fun closeAll() {
        stopScreenCapture()
        peerConnections.forEach { (_, pc) ->
            pc.close()
            pc.dispose()
        }
        peerConnections.clear()
        _remoteVideoTrack.value = null
    }

    companion object {
        private const val TAG = "WebRtcManager"

        @Volatile
        private var INSTANCE: WebRtcManager? = null

        fun getInstance(context: Context): WebRtcManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WebRtcManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
