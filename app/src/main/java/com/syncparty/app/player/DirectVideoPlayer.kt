package com.syncparty.app.player

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun DirectVideoPlayer(
    videoUrl: String,
    isPlaying: Boolean,
    seekPositionMs: Long,
    isHost: Boolean,
    onPlaybackStateChanged: (isPlaying: Boolean, positionMs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(videoUrl) {
        val player = ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUrl)
            setMediaItem(mediaItem)
            prepare()
            seekTo(seekPositionMs)
            playWhenReady = isPlaying

            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    if (isHost) {
                        onPlaybackStateChanged(playing, currentPosition)
                    }
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    if (isHost && reason == Player.DISCONTINUITY_REASON_SEEK) {
                        onPlaybackStateChanged(playWhenReady, currentPosition)
                    }
                }
            })
        }
        exoPlayer = player

        onDispose {
            player.release()
        }
    }

    LaunchedEffect(isPlaying) {
        if (!isHost) {
            exoPlayer?.playWhenReady = isPlaying
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = isHost // Only host gets built-in scrubbing controllers
                    this.player = exoPlayer
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
