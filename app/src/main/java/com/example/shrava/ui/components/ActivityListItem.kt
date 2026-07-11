package com.example.shrava.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shrava.data.entity.ActivityEntity
import com.example.shrava.ui.theme.AccentGreen
import com.example.shrava.ui.theme.AccentBlue
import com.example.shrava.ui.theme.AccentOrange
import com.example.shrava.ui.theme.DarkCard
import com.example.shrava.ui.theme.TextSecondary
import com.example.shrava.util.LocationUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityListItem(
    activity: ActivityEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = when (activity.type) {
        "Run" -> AccentGreen
        "Ride" -> AccentBlue
        else -> AccentOrange
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val dateStr = remember(activity.startTime) { dateFormat.format(Date(activity.startTime)) }
    val distanceKm = remember(activity.distanceMeters) { LocationUtils.metersToDisplayDistance(activity.distanceMeters) }
    val durationStr = remember(activity.durationSeconds) { LocationUtils.formatDuration(activity.durationSeconds) }
    val paceStr = remember(activity.avgPaceSecondsPerKm) { LocationUtils.formatPace(activity.avgPaceSecondsPerKm) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(accentColor)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .background(DarkCard)
                .padding(16.dp)
        ) {
            Text(
                text = "${activity.type}  ·  $dateStr",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$distanceKm  ·  $durationStr  ·  $paceStr/km",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
