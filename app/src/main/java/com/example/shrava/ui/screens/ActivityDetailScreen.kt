package com.example.shrava.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shrava.ui.components.ElevationChart
import com.example.shrava.ui.components.LiveStats
import com.example.shrava.ui.components.SplitsTable
import com.example.shrava.ui.theme.AccentGreen
import com.example.shrava.ui.theme.AccentRed
import com.example.shrava.ui.theme.DarkBg
import com.example.shrava.ui.theme.DarkCard
import com.example.shrava.ui.theme.TextMuted
import com.example.shrava.ui.theme.TextSecondary
import com.example.shrava.ui.viewmodel.ActivityDetailViewModel
import com.example.shrava.util.LocationUtils
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    activityId: Long,
    onBack: () -> Unit,
    viewModel: ActivityDetailViewModel = viewModel()
) {
    val activity by viewModel.activity.collectAsState()
    val points by viewModel.points.collectAsState()
    val exportResult by viewModel.exportResult.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(activityId) {
        viewModel.loadActivity(activityId)
    }

    LaunchedEffect(exportResult) {
        exportResult?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearExportResult()
        }
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = { Text("ACTIVITY", fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = DarkCard
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.exportToGpx() }) {
                        Icon(Icons.Default.Share, contentDescription = "Export GPX", tint = TextSecondary)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        activity?.let { act ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Activity type header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = act.type.uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()) }
                    val dateStr = remember(act.startTime) { dateFormat.format(Date(act.startTime)) }
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Map
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setMultiTouchControls(false)
                            overlays.removeAll { it is Polyline }

                            if (points.size >= 2) {
                                val polyline = Polyline().apply {
                                    outlinePaint.color = android.graphics.Color.parseColor("#00E676")
                                    outlinePaint.strokeWidth = 8f
                                    setPoints(
                                        points.map { point ->
                                            org.osmdroid.util.GeoPoint(point.latitude, point.longitude)
                                        }
                                    )
                                }
                                overlays.add(polyline)

                                if (points.isNotEmpty()) {
                                    val lastPoint = points.last()
                                    controller?.setCenter(
                                        org.osmdroid.util.GeoPoint(lastPoint.latitude, lastPoint.longitude)
                                    )
                                    controller?.setZoom(15.0)
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                )

                Spacer(modifier = Modifier.height(20.dp))

                LiveStats(
                    elapsedSeconds = act.durationSeconds,
                    distanceMeters = act.distanceMeters,
                    paceSecondsPerKm = act.avgPaceSecondsPerKm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                ElevationChart(
                    points = points,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCard)
                )

                Spacer(modifier = Modifier.height(20.dp))

                SplitsTable(
                    splits = LocationUtils.calculateSplits(points),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                FilledTonalButton(
                    onClick = { viewModel.exportToGpx() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                        containerColor = DarkCard,
                        contentColor = AccentGreen
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = AccentGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Export as GPX")
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        } ?: run {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...", color = TextMuted)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = DarkCard,
            titleContentColor = Color.White,
            textContentColor = TextSecondary,
            title = { Text("Delete Activity") },
            text = { Text("Are you sure you want to delete this activity? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteActivity(activityId)
                    showDeleteDialog = false
                    onBack()
                }) {
                    Text("Delete", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
