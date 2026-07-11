package com.example.shrava.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.cos
import kotlin.math.tan

class MapDownloadManager(private val context: Context) {

    private val tileCacheDir = File(context.cacheDir, "tiles")

    data class DownloadState(
        val isDownloading: Boolean = false,
        val progress: Float = 0f,
        val currentRegion: String = "",
        val downloadedSizeBytes: Long = 0L,
        val message: String = ""
    )

    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private fun latLngToTile(lat: Double, lng: Double, zoom: Int): Pair<Int, Int> {
        val n = (1 shl zoom).toDouble()
        val x = floor((lng + 180.0) / 360.0 * n).toInt()
        val latRad = Math.toRadians(lat)
        val y = floor((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / Math.PI) / 2.0 * n).toInt()
        return Pair(x, y)
    }

    suspend fun downloadRegion(
        bounds: BoundingBox,
        zoomMin: Int = 10,
        zoomMax: Int = 15
    ) = withContext(Dispatchers.IO) {
        _downloadState.value = DownloadState(
            isDownloading = true,
            currentRegion = "Preparing download...",
            progress = 0f
        )

        try {
            val tiles = mutableListOf<Triple<Int, Int, Int>>()

            for (zoom in zoomMin..zoomMax) {
                val (xMin, yMax) = latLngToTile(bounds.latNorth, bounds.lonWest, zoom)
                val (xMax, yMin) = latLngToTile(bounds.latSouth, bounds.lonEast, zoom)

                for (x in xMin..xMax) {
                    for (y in yMin..yMax) {
                        tiles.add(Triple(zoom, x, y))
                    }
                }
            }

            val totalTiles = tiles.size
            var downloaded = 0

            _downloadState.value = DownloadState(
                isDownloading = true,
                currentRegion = "Downloading $totalTiles tiles...",
                progress = 0f
            )

            for ((zoom, x, y) in tiles) {
                try {
                    val url = "https://tile.openstreetmap.org/$zoom/$x/$y.png"
                    val tileDir = File(tileCacheDir, "$zoom")
                    val tileFile = File(tileDir, "${x}_${y}.png")

                    if (!tileFile.exists()) {
                        tileDir.mkdirs()
                        val connection = URL(url).openConnection() as HttpURLConnection
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        connection.connect()

                        if (connection.responseCode == 200) {
                            connection.inputStream.use { input ->
                                tileFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        connection.disconnect()
                    }
                } catch (_: Exception) {}

                downloaded++
                val progress = downloaded.toFloat() / totalTiles
                _downloadState.value = DownloadState(
                    isDownloading = true,
                    currentRegion = "Downloading tiles... $downloaded/$totalTiles",
                    progress = progress,
                    downloadedSizeBytes = calculateCacheSize()
                )
            }

            _downloadState.value = DownloadState(
                isDownloading = false,
                currentRegion = "Download complete",
                progress = 1f,
                downloadedSizeBytes = calculateCacheSize(),
                message = "Download complete! Map is now available offline."
            )
        } catch (e: Exception) {
            _downloadState.value = DownloadState(
                isDownloading = false,
                message = "Download failed: ${e.message}"
            )
        }
    }

    fun calculateCacheSize(): Long {
        return if (tileCacheDir.exists()) {
            tileCacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0L
    }

    fun formatCacheSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }

    fun clearCache() {
        tileCacheDir.deleteRecursively()
        _downloadState.value = DownloadState(
            downloadedSizeBytes = 0L,
            message = "Cache cleared"
        )
    }
}
