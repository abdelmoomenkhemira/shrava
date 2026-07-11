package com.example.shrava.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shrava.ui.components.LiveStats
import com.example.shrava.ui.theme.AccentGreen
import com.example.shrava.ui.theme.AccentRed
import com.example.shrava.ui.theme.AccentOrange
import com.example.shrava.ui.theme.DarkBg
import com.example.shrava.ui.theme.TextMuted
import com.example.shrava.ui.viewmodel.TrackingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    activityType: String = "Run",
    onTrackingStopped: () -> Unit,
    onBack: () -> Unit,
    viewModel: TrackingViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(activityType) {
        viewModel.startTracking(activityType)
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.activityType.uppercase(),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    FilledIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = state.isSearchingForGps,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                GpsStatusBanner(
                    accuracy = state.gpsAccuracy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                LiveStats(
                    elapsedSeconds = state.elapsedSeconds,
                    distanceMeters = state.distanceMeters,
                    paceSecondsPerKm = state.currentPaceSecondsPerKm,
                    isGpsLocked = state.hasGpsFix,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                if (state.hasGpsFix && state.gpsAccuracy > 0f) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "GPS ${state.gpsAccuracy.toInt()}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.isPaused) {
                    FilledIconButton(
                        onClick = { viewModel.resumeTracking() },
                        modifier = Modifier.size(72.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = AccentGreen
                        )
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Resume",
                            modifier = Modifier.size(36.dp),
                            tint = Color.Black
                        )
                    }
                } else {
                    FilledIconButton(
                        onClick = { viewModel.pauseTracking() },
                        modifier = Modifier.size(72.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.15f)
                        ),
                        enabled = state.hasGpsFix
                    ) {
                        Icon(
                            Icons.Default.Pause,
                            contentDescription = "Pause",
                            modifier = Modifier.size(36.dp),
                            tint = if (state.hasGpsFix) Color.White else TextMuted
                        )
                    }
                }

                FilledIconButton(
                    onClick = {
                        viewModel.stopTracking()
                        onTrackingStopped()
                    },
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = AccentRed
                    ),
                    enabled = state.hasGpsFix
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(36.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun GpsStatusBanner(
    accuracy: Float,
    modifier: Modifier = Modifier
) {
    val accuracyText = if (accuracy > 0f) " · ${accuracy.toInt()}m" else ""

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AccentOrange.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(AccentOrange)
            )
            Text(
                text = "Searching for GPS signal$accuracyText",
                color = AccentOrange,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}
