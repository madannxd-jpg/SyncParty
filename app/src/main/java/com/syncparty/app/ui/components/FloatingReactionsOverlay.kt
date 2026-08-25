package com.syncparty.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syncparty.app.model.Reaction
import kotlinx.coroutines.delay

@Composable
fun FloatingReactionsOverlay(
    reactions: List<Reaction>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        reactions.takeLast(10).forEach { reaction ->
            SingleFloatingEmoji(reaction = reaction)
        }
    }
}

@Composable
private fun SingleFloatingEmoji(reaction: Reaction) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(reaction.id) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2400, easing = LinearEasing)
        )
    }

    if (animProgress.value < 1f) {
        val yOffset = (1f - animProgress.value) * 600f
        val alpha = if (animProgress.value > 0.7f) (1f - animProgress.value) / 0.3f else 1f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = (reaction.xOffset * 3).dp)
        ) {
            Text(
                text = reaction.emoji,
                fontSize = 32.sp,
                modifier = Modifier
                    .offset(y = yOffset.dp)
                    .alpha(alpha)
            )
        }
    }
}
