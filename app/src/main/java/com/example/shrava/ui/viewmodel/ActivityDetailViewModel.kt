package com.example.shrava.ui.viewmodel

import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shrava.data.ActivityRepository
import com.example.shrava.data.AppDatabase
import com.example.shrava.data.entity.ActivityEntity
import com.example.shrava.data.entity.LocationPointEntity
import com.example.shrava.util.GpxExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ActivityDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ActivityRepository

    private val _activity = MutableStateFlow<ActivityEntity?>(null)
    val activity: StateFlow<ActivityEntity?> = _activity.asStateFlow()

    private val _points = MutableStateFlow<List<LocationPointEntity>>(emptyList())
    val points: StateFlow<List<LocationPointEntity>> = _points.asStateFlow()

    private val _exportResult = MutableStateFlow<String?>(null)
    val exportResult: StateFlow<String?> = _exportResult.asStateFlow()

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

    fun exportToGpx() {
        viewModelScope.launch {
            val act = _activity.value ?: return@launch
            val pts = _points.value
            try {
                val gpxContent = GpxExporter.generateGpx(act, pts)
                val fileName = "shrava_${act.type}_${act.startTime}.gpx"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = getApplication<Application>().contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/gpx+xml")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let {
                        resolver.openOutputStream(it)?.use { stream ->
                            stream.write(gpxContent.toByteArray())
                        }
                    }
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    )
                    java.io.File(downloadsDir, fileName).writeText(gpxContent)
                }
                _exportResult.value = "Exported: $fileName"
            } catch (e: Exception) {
                _exportResult.value = "Export failed: ${e.message}"
            }
        }
    }

    fun clearExportResult() {
        _exportResult.value = null
    }

    fun deleteActivity(activityId: Long) {
        viewModelScope.launch {
            repository.deleteActivity(activityId)
        }
    }
}
