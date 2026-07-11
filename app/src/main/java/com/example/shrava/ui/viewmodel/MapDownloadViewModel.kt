package com.example.shrava.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shrava.util.MapDownloadManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.BoundingBox

class MapDownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val downloadManager = MapDownloadManager(application)

    val downloadState: StateFlow<MapDownloadManager.DownloadState> = downloadManager.downloadState

    fun downloadRegion(bounds: BoundingBox, zoomMin: Int = 10, zoomMax: Int = 15) {
        viewModelScope.launch {
            downloadManager.downloadRegion(bounds, zoomMin, zoomMax)
        }
    }

    fun getCacheSize(): Long = downloadManager.calculateCacheSize()

    fun formatCacheSize(bytes: Long): String = downloadManager.formatCacheSize(bytes)

    fun clearCache() {
        downloadManager.clearCache()
    }
}
