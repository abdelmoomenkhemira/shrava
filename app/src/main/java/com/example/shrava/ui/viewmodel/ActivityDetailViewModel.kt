package com.example.shrava.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shrava.data.ActivityRepository
import com.example.shrava.data.AppDatabase
import com.example.shrava.data.entity.ActivityEntity
import com.example.shrava.data.entity.LocationPointEntity
import com.example.shrava.util.ActivityImageExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ActivityDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ActivityRepository

    private val _activity = MutableStateFlow<ActivityEntity?>(null)
    val activity: StateFlow<ActivityEntity?> = _activity.asStateFlow()

    private val _points = MutableStateFlow<List<LocationPointEntity>>(emptyList())
    val points: StateFlow<List<LocationPointEntity>> = _points.asStateFlow()

    private val _shareUri = MutableStateFlow<Uri?>(null)
    val shareUri: StateFlow<Uri?> = _shareUri.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        repository = ActivityRepository(db.activityDao(), db.locationPointDao())
    }

    fun loadActivity(activityId: Long) {
        viewModelScope.launch {
            _activity.value = repository.getActivityById(activityId)
            _points.value = repository.getLocationPoints(activityId)
        }
    }

    fun shareActivityImage() {
        viewModelScope.launch {
            val act = _activity.value ?: return@launch
            val pts = _points.value
            if (pts.size < 2) {
                _toastMessage.value = "Not enough GPS points to generate image"
                return@launch
            }
            try {
                val context = getApplication<Application>()
                val file = ActivityImageExporter.generate(context, act, pts)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                _shareUri.value = uri
            } catch (e: Exception) {
                _toastMessage.value = "Failed to generate image: ${e.message}"
            }
        }
    }

    fun clearShareUri() {
        _shareUri.value = null
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun deleteActivity(activityId: Long) {
        viewModelScope.launch {
            repository.deleteActivity(activityId)
        }
    }
}
