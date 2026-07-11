package com.example.shrava.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Permission : Screen("permission")
    data object FirstLaunch : Screen("first_launch")
    data object MapDownload : Screen("map_download")
    data object Coach : Screen("coach")
    data object AiCoach : Screen("ai_coach")
    data object Tracking : Screen("tracking/{activityType}") {
        fun createRoute(activityType: String) = "tracking/$activityType"
    }
    data object ActivityDetail : Screen("activity_detail/{activityId}") {
        fun createRoute(activityId: Long) = "activity_detail/$activityId"
    }
}
