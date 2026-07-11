package com.example.shrava.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shrava.service.TrackingService
import com.example.shrava.service.TrackingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TrackingViewModel(application: Application) : AndroidViewModel(application) {

    private var service: TrackingService? = null
    private var bound = false

    private val _state = MutableStateFlow(TrackingState())
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val trackingBinder = binder as TrackingService.TrackingBinder
            service = trackingBinder.getService()
            bound = true
            viewModelScope.launch {
                service?.state?.collect { newState ->
                    _state.value = newState
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bound = false
        }
    }

    init {
        val intent = Intent(application, TrackingService::class.java)
        application.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun startTracking(activityType: String) {
        val intent = Intent(getApplication(), TrackingService::class.java).apply {
            action = TrackingService.ACTION_START
            putExtra(TrackingService.EXTRA_ACTIVITY_TYPE, activityType)
        }
        getApplication<Application>().startForegroundService(intent)
    }

    fun pauseTracking() {
        val intent = Intent(getApplication(), TrackingService::class.java).apply {
            action = TrackingService.ACTION_PAUSE
        }
        getApplication<Application>().startService(intent)
    }

    fun resumeTracking() {
        val intent = Intent(getApplication(), TrackingService::class.java).apply {
            action = TrackingService.ACTION_RESUME
        }
        getApplication<Application>().startService(intent)
    }

    fun stopTracking() {
        val intent = Intent(getApplication(), TrackingService::class.java).apply {
            action = TrackingService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
    }

    override fun onCleared() {
        super.onCleared()
        if (bound) {
            getApplication<Application>().unbindService(connection)
            bound = false
        }
    }
}
