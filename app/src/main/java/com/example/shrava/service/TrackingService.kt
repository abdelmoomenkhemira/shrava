package com.example.shrava.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.shrava.MainActivity
import com.example.shrava.R
import com.example.shrava.ShravaApplication
import com.example.shrava.data.ActivityRepository
import com.example.shrava.data.AppDatabase
import com.example.shrava.data.entity.ActivityEntity
import com.example.shrava.data.entity.LocationPointEntity
import com.example.shrava.util.LocationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Collections
import kotlin.math.abs

class TrackingService : Service() {

    private val binder = TrackingBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    private var timerJob: Job? = null

    private val _state = MutableStateFlow(TrackingState())
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    private lateinit var repository: ActivityRepository

    private val filteredPoints: MutableList<LocationPointEntity> =
        Collections.synchronizedList(mutableListOf<LocationPointEntity>())
    private var lastElapsedSeconds = 0L
    private var firstFixReceived = false

    inner class TrackingBinder : Binder() {
        fun getService(): TrackingService = this@TrackingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(applicationContext)
        repository = ActivityRepository(db.activityDao(), db.locationPointDao())
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val activityType = intent.getStringExtra(EXTRA_ACTIVITY_TYPE) ?: "Run"
                startTracking(activityType)
            }
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking(activityType: String) {
        val notification = createNotification("Searching for GPS signal...")
        startForeground(NOTIFICATION_ID, notification)

        val now = System.currentTimeMillis()
        synchronized(filteredPoints) {
            filteredPoints.clear()
        }
        firstFixReceived = false
        lastElapsedSeconds = 0L

        _state.update {
            TrackingState(
                isTracking = true,
                isPaused = false,
                activityType = activityType,
                startTime = now,
                hasGpsFix = false,
                isSearchingForGps = true
            )
        }

        startLocationUpdates()
    }

    private fun pauseTracking() {
        _state.update { it.copy(isPaused = true) }
        stopLocationUpdates()
        timerJob?.cancel()
        updateNotification("Paused")
    }

    private fun resumeTracking() {
        _state.update { it.copy(isPaused = false) }
        startLocationUpdates()
        if (firstFixReceived) {
            startTimer()
        }
        updateNotification("Resuming ${_state.value.activityType.lowercase()}...")
    }

    private fun stopTracking() {
        stopLocationUpdates()
        timerJob?.cancel()

        val currentState = _state.value
        val pointsToSave: List<LocationPointEntity>
        synchronized(filteredPoints) {
            pointsToSave = filteredPoints.toList()
        }

        if (currentState.elapsedSeconds > 0 && pointsToSave.isNotEmpty()) {
            val simplifiedPoints = LocationUtils.douglasPeucker(pointsToSave, epsilon = 5.0)

            scope.launch {
                val activity = ActivityEntity(
                    type = currentState.activityType,
                    startTime = currentState.startTime,
                    durationSeconds = currentState.elapsedSeconds,
                    distanceMeters = currentState.distanceMeters,
                    avgPaceSecondsPerKm = LocationUtils.calculateAvgPace(
                        currentState.distanceMeters,
                        currentState.elapsedSeconds
                    )
                )
                val activityId = repository.insertActivity(activity)
                val pointsWithActivityId = simplifiedPoints.map {
                    it.copy(activityId = activityId)
                }
                repository.insertLocationPoints(pointsWithActivityId)
            }
        }

        _state.update {
            TrackingState(
                isTracking = false,
                isPaused = false,
                activityType = it.activityType
            )
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startLocationUpdates() {
        try {
            val minTimeMs = 2000L
            val minDistanceM = 2f

            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    handleNewLocation(location)
                }

                @Deprecated("Deprecated in API")
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minTimeMs,
                minDistanceM,
                locationListener!!,
                Looper.getMainLooper()
            )

            val gpsLastKnown = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (gpsLastKnown != null) {
                handleNewLocation(gpsLastKnown)
            }

            val networkLastKnown = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (networkLastKnown != null && gpsLastKnown == null) {
                handleNewLocation(networkLastKnown)
            }

            try {
                locationManager?.requestSingleUpdate(
                    LocationManager.GPS_PROVIDER,
                    object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            handleNewLocation(location)
                        }
                        @Deprecated("Deprecated in API")
                        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    },
                    Looper.getMainLooper()
                )
            } catch (_: SecurityException) {}

        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun handleNewLocation(location: Location) {
        if (_state.value.isPaused) return

        _state.update { it.copy(gpsAccuracy = location.accuracy) }

        if (location.accuracy > 30f) return

        if (firstFixReceived && location.accuracy > 15f) return

        val point = LocationPointEntity(
            activityId = 0,
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = if (location.hasAltitude()) location.altitude else null,
            timestamp = System.currentTimeMillis()
        )

        synchronized(filteredPoints) {
            if (filteredPoints.isNotEmpty()) {
                val lastPoint = filteredPoints.last()
                val timeDiffSec = (point.timestamp - lastPoint.timestamp) / 1000.0
                if (timeDiffSec > 0) {
                    val distance = LocationUtils.haversineDistance(
                        lastPoint.latitude, lastPoint.longitude,
                        point.latitude, point.longitude
                    )
                    val speedMps = distance / timeDiffSec
                    val maxSpeed = when (_state.value.activityType) {
                        "Walk" -> 4.0
                        "Ride" -> 25.0
                        else -> 10.0
                    }
                    if (speedMps > maxSpeed) return
                }
            }

            if (filteredPoints.size >= 2) {
                val p1 = filteredPoints[filteredPoints.size - 2]
                val p2 = filteredPoints.last()
                val bearing1 = LocationUtils.calculateBearing(p1, p2)
                val bearing2 = LocationUtils.calculateBearing(p2, point)
                val diff = abs(bearing1 - bearing2).let {
                    if (it > 180) 360 - it else it
                }
                if (diff > 100) return
            }

            if (filteredPoints.size >= 15) {
                val lastPoint = filteredPoints.last()
                val distance = LocationUtils.haversineDistance(
                    lastPoint.latitude, lastPoint.longitude,
                    point.latitude, point.longitude
                )
                if (distance < 2.0) return
            }

            filteredPoints.add(point)
        }

        if (!firstFixReceived) {
            firstFixReceived = true
            _state.update {
                it.copy(
                    hasGpsFix = true,
                    isSearchingForGps = false
                )
            }
            startTimer()
            updateNotification("GPS locked! Tracking ${_state.value.activityType.lowercase()}...")
        }

        val totalDistance: Double
        synchronized(filteredPoints) {
            totalDistance = LocationUtils.calculateTotalDistance(filteredPoints.toList())
        }
        val elapsed = _state.value.elapsedSeconds
        val pace = LocationUtils.calculateAvgPace(totalDistance, elapsed)

        _state.update {
            it.copy(
                distanceMeters = totalDistance,
                currentPaceSecondsPerKm = pace,
                routePoints = synchronized(filteredPoints) { filteredPoints.toList() }
            )
        }

        val distStr = LocationUtils.metersToDisplayDistance(totalDistance)
        updateNotification("${_state.value.activityType} — $distStr")
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                delay(1000)
                if (!_state.value.isPaused) {
                    lastElapsedSeconds++
                    _state.update { it.copy(elapsedSeconds = lastElapsedSeconds) }
                }
            }
        }
    }

    private fun stopLocationUpdates() {
        locationListener?.let {
            locationManager?.removeUpdates(it)
        }
        locationListener = null
    }

    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ShravaApplication.CHANNEL_ID)
            .setContentTitle("Shrava")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        timerJob?.cancel()
        scope.cancel()
    }

    companion object {
        const val ACTION_START = "com.example.shrava.START"
        const val ACTION_PAUSE = "com.example.shrava.PAUSE"
        const val ACTION_RESUME = "com.example.shrava.RESUME"
        const val ACTION_STOP = "com.example.shrava.STOP"
        const val EXTRA_ACTIVITY_TYPE = "activity_type"
        const val NOTIFICATION_ID = 1
    }
}
