package com.example.shrava.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.shrava.ui.screens.ActivityDetailScreen
import com.example.shrava.ui.screens.HomeScreen
import com.example.shrava.ui.screens.MapDownloadScreen
import com.example.shrava.ui.screens.PermissionScreen
import com.example.shrava.ui.screens.TrackingScreen

@Composable
fun ShravaNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Permission.route
    ) {
        composable(Screen.Permission.route) {
            PermissionScreen(
                onPermissionsGranted = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Permission.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onStartActivity = { activityType ->
                    navController.navigate(Screen.Tracking.createRoute(activityType))
                },
                onActivityClick = { activityId ->
                    navController.navigate(Screen.ActivityDetail.createRoute(activityId))
                },
                onOpenMapDownload = {
                    navController.navigate(Screen.MapDownload.route)
                }
            )
        }

        composable(Screen.MapDownload.route) {
            MapDownloadScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Tracking.route,
            arguments = listOf(
                navArgument("activityType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val activityType = backStackEntry.arguments?.getString("activityType") ?: "Run"
            TrackingScreen(
                activityType = activityType,
                onTrackingStopped = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ActivityDetail.route,
            arguments = listOf(
                navArgument("activityId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getLong("activityId") ?: 0L
            ActivityDetailScreen(
                activityId = activityId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
