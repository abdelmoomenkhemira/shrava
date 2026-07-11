package com.example.shrava.service

import com.example.shrava.data.entity.LocationPointEntity

data class TrackingState(
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val activityType: String = "Run",
    val elapsedSeconds: Long = 0L,
    val distanceMeters: Double = 0.0,
    val currentPaceSecondsPerKm: Double = 0.0,
    val routePoints: List<LocationPointEntity> = emptyList(),
    val startTime: Long = 0L,
    val hasGpsFix: Boolean = false,
    val gpsAccuracy: Float = 0f,
    val isSearchingForGps: Boolean = true
)
