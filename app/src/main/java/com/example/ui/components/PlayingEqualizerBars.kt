package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.RecorderRed

/**
 * Animated sound equalizer bars that pulse organically when audio is actively playing.
 * Adds tactile visual liveliness to cards and player bars.
 */
@Composable
fun PlayingEqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = RecorderRed,
    barWidth: Dp = 3.dp,
    maxHeight: Dp = 16.dp
) {
    val transition = rememberInfiniteTransition(label = "equalizer_transition")

    val bar1 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )

    val bar2 by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )

    val bar3 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    val bar4 by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )

    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val h1 = if (isPlaying) (maxHeight * bar1).coerceAtLeast(4.dp) else 4.dp
        val h2 = if (isPlaying) (maxHeight * bar2).coerceAtLeast(4.dp) else 6.dp
        val h3 = if (isPlaying) (maxHeight * bar3).coerceAtLeast(4.dp) else 5.dp
        val h4 = if (isPlaying) (maxHeight * bar4).coerceAtLeast(4.dp) else 4.dp

        Box(
            modifier = Modifier
                .width(barWidth)
                .height(h1)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(h2)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(h3)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(h4)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )
    }
}
