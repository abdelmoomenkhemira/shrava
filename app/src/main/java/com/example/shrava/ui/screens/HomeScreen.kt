package com.example.shrava.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shrava.data.entity.ActivityEntity
import com.example.shrava.ui.components.ActivityListItem
import com.example.shrava.ui.components.ActivityTypePicker
import com.example.shrava.ui.components.BottomNavBar
import com.example.shrava.ui.components.BottomNavItem
import com.example.shrava.ui.theme.AccentGreen
import com.example.shrava.ui.theme.DarkBg
import com.example.shrava.ui.theme.DarkSurface
import com.example.shrava.ui.theme.TextMuted
import com.example.shrava.ui.theme.TextSecondary
import com.example.shrava.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onStartActivity: (String) -> Unit,
    onActivityClick: (Long) -> Unit,
    onOpenMapDownload: () -> Unit,
    onOpenCoach: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val activities by viewModel.activities.collectAsState()
    var showTypePicker by remember { mutableStateOf(false) }
    var activityToDelete by remember { mutableStateOf<ActivityEntity?>(null) }
    var selectedNav by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SHRAVA",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                IconButton(
                    onClick = onOpenMapDownload,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = DarkSurface
                    )
                ) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = "Map Downloads",
                        tint = TextSecondary
                    )
                }
            }
        },
        bottomBar = {
            BottomNavBar(
                selectedItem = BottomNavItem.entries[selectedNav],
                onItemClick = { item ->
                    val index = BottomNavItem.entries.indexOf(item)
                    selectedNav = index
                    when (item) {
                        BottomNavItem.Coach -> onOpenCoach()
                        else -> {}
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showTypePicker = true },
                containerColor = AccentGreen,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Start Activity",
                    tint = Color.Black
                )
            }
        }
    ) { padding ->
        if (activities.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "NO ACTIVITIES",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextMuted,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap + to start tracking",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                items(activities) { activity ->
                    ActivityListItem(
                        activity = activity,
                        onClick = { onActivityClick(activity.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showTypePicker) {
        ActivityTypePicker(
            onTypeSelected = { type ->
                showTypePicker = false
                onStartActivity(type)
            },
            onDismiss = { showTypePicker = false }
        )
    }

    activityToDelete?.let { activity ->
        AlertDialog(
            onDismissRequest = { activityToDelete = null },
            containerColor = DarkSurface,
            titleContentColor = Color.White,
            textContentColor = TextSecondary,
            title = { Text("Delete Activity") },
            text = { Text("Are you sure you want to delete this activity?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteActivity(activity.id)
                    activityToDelete = null
                }) {
                    Text("Delete", color = AccentGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { activityToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
