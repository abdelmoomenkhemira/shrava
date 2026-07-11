package com.example.shrava.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shrava.ui.theme.AccentGreen
import com.example.shrava.ui.theme.TextMuted
import com.example.shrava.ui.theme.TextSecondary
import com.example.shrava.util.LocationUtils

@Composable
fun LiveStats(
    elapsedSeconds: Long,
    distanceMeters: Double,
    paceSecondsPerKm: Double,
    isGpsLocked: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isGpsLocked) {
            AnimatedStatItem(
                label = "DISTANCE",
                value = LocationUtils.metersToDisplayDistance(distanceMeters)
            )
            AnimatedStatItem(
                label = "TIME",
                value = LocationUtils.formatDuration(elapsedSeconds)
            )
            AnimatedStatItem(
                label = "PACE",
                value = "${LocationUtils.formatPace(paceSecondsPerKm)}/km"
            )
        } else {
            StatItem(label = "DISTANCE", value = "-- --")
            StatItem(label = "TIME", value = "--:--:--")
            StatItem(label = "PACE", value = "--:--")
        }
    }
}

@Composable
private fun AnimatedStatItem(label: String, value: String) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            (slideInVertically(tween(300)) { -it } + fadeIn(tween(300)))
                .togetherWith(slideOutVertically(tween(300)) { it } + fadeOut(tween(300)))
        },
        label = label
    ) { targetValue ->
        StatItem(label = label, value = targetValue)
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            letterSpacing = 2.sp
        )
        Text(
            text = value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
