package com.syncparty.app.webrtc

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun WebRtcVideoView(
    videoTrack: VideoTrack?,
    modifier: Modifier = Modifier,
    scalingType: RendererCommon.ScalingType = RendererCommon.ScalingType.SCALE_ASPECT_FIT
) {
    val context = LocalContext.current
    val webRtcManager = remember { WebRtcManager.getInstance(context) }
    var currentRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    AndroidView(
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                init(webRtcManager.eglContext, null)
                setScalingType(scalingType)
                setEnableHardwareScaler(true)
                currentRenderer = this
                videoTrack?.addSink(this)
            }
        },
        update = { renderer ->
            renderer.setScalingType(scalingType)
        },
        modifier = modifier.fillMaxSize(),
        onRelease = { renderer ->
            videoTrack?.removeSink(renderer)
            renderer.release()
        }
    )

    DisposableEffect(videoTrack) {
        val renderer = currentRenderer
        if (renderer != null && videoTrack != null) {
            videoTrack.addSink(renderer)
        }
        onDispose {
            if (renderer != null && videoTrack != null) {
                videoTrack.removeSink(renderer)
            }
        }
    }
}
