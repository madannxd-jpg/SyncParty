package com.syncparty.app.player

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.syncparty.app.theme.SurfaceDark

@Composable
fun YouTubeWatchView(
    videoId: String,
    isPlaying: Boolean,
    seekPositionSeconds: Float,
    isHost: Boolean,
    onPlaybackStateChanged: (isPlaying: Boolean, positionSeconds: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var youTubePlayerInstance by remember { mutableStateOf<YouTubePlayer?>(null) }
    var currentVideoId by remember { mutableStateOf(videoId) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                YouTubePlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    enableAutomaticInitialization = false

                    val listener = object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) {
                            youTubePlayerInstance = youTubePlayer
                            youTubePlayer.loadVideo(videoId, seekPositionSeconds)
                            if (!isPlaying) youTubePlayer.pause()
                        }

                        override fun onStateChange(
                            youTubePlayer: YouTubePlayer,
                            state: PlayerConstants.PlayerState
                        ) {
                            if (isHost) {
                                when (state) {
                                    PlayerConstants.PlayerState.PLAYING -> onPlaybackStateChanged(true, seekPositionSeconds)
                                    PlayerConstants.PlayerState.PAUSED -> onPlaybackStateChanged(false, seekPositionSeconds)
                                    else -> {}
                                }
                            }
                        }

                        override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                            if (isHost && isPlaying) {
                                onPlaybackStateChanged(true, second)
                            }
                        }
                    }

                    initialize(listener)
                }
            },
            update = { _ ->
                if (currentVideoId != videoId) {
                    currentVideoId = videoId
                    youTubePlayerInstance?.loadVideo(videoId, seekPositionSeconds)
                }
                if (!isHost) {
                    if (isPlaying) {
                        youTubePlayerInstance?.play()
                    } else {
                        youTubePlayerInstance?.pause()
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
