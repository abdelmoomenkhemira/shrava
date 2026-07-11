package com.example.shrava.util

import com.example.shrava.data.entity.LocationPointEntity
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object LocationUtils {

    private const val EARTH_RADIUS_METERS = 6_371_000.0
    private const val METERS_PER_KM = 1000.0

    fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2)
        val c = 2 * asin(sqrt(a))
        return EARTH_RADIUS_METERS * c
    }

    fun calculateBearing(from: LocationPointEntity, to: LocationPointEntity): Double {
        val dLng = Math.toRadians(to.longitude - from.longitude)
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val y = sin(dLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
        val bearing = Math.toDegrees(atan2(y, x))
        return (bearing + 360) % 360
    }

    fun calculateTotalDistance(points: List<LocationPointEntity>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 1 until points.size) {
            total += haversineDistance(
                points[i - 1].latitude, points[i - 1].longitude,
                points[i].latitude, points[i].longitude
            )
        }
        return total
    }

    fun calculateAvgPace(distanceMeters: Double, durationSeconds: Long): Double {
        if (distanceMeters <= 0) return 0.0
        return (durationSeconds / (distanceMeters / METERS_PER_KM))
    }

    fun calculateSplits(
        points: List<LocationPointEntity>,
        intervalKm: Double = 1.0
    ): List<Split> {
        if (points.size < 2) return emptyList()

        val splits = mutableListOf<Split>()
        var splitStartIndex = 0
        var splitDistance = 0.0
        var splitNumber = 1
        val intervalMeters = intervalKm * METERS_PER_KM

        for (i in 1 until points.size) {
            val segmentDist = haversineDistance(
                points[i - 1].latitude, points[i - 1].longitude,
                points[i].latitude, points[i].longitude
            )
            splitDistance += segmentDist

            if (splitDistance >= intervalMeters) {
                val duration = points[i].timestamp - points[splitStartIndex].timestamp
                val paceSecondsPerKm = if (splitDistance > 0) {
                    (duration / 1000.0) / (splitDistance / METERS_PER_KM)
                } else 0.0

                splits.add(
                    Split(
                        number = splitNumber,
                        distanceMeters = splitDistance,
                        durationSeconds = duration / 1000,
                        paceSecondsPerKm = paceSecondsPerKm
                    )
                )
                splitStartIndex = i
                splitDistance = 0.0
                splitNumber++
            }
        }

        return splits
    }

    fun calculateElevationGain(points: List<LocationPointEntity>, smoothingThreshold: Double = 3.0): Double {
        if (points.size < 2) return 0.0

        var totalGain = 0.0
        for (i in 1 until points.size) {
            val alt1 = points[i - 1].altitude ?: continue
            val alt2 = points[i].altitude ?: continue
            val diff = alt2 - alt1
            if (diff > smoothingThreshold) {
                totalGain += diff
            }
        }
        return totalGain
    }

    fun douglasPeucker(points: List<LocationPointEntity>, epsilon: Double = 5.0): List<LocationPointEntity> {
        if (points.size <= 2) return points

        var maxDist = 0.0
        var maxIndex = 0
        val first = points.first()
        val last = points.last()

        for (i in 1 until points.size - 1) {
            val dist = perpendicularDistance(points[i], first, last)
            if (dist > maxDist) {
                maxDist = dist
                maxIndex = i
            }
        }

        return if (maxDist > epsilon) {
            val left = douglasPeucker(points.subList(0, maxIndex + 1), epsilon)
            val right = douglasPeucker(points.subList(maxIndex, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(first, last)
        }
    }

    private fun perpendicularDistance(
        point: LocationPointEntity,
        lineStart: LocationPointEntity,
        lineEnd: LocationPointEntity
    ): Double {
        val dx = lineEnd.longitude - lineStart.longitude
        val dy = lineEnd.latitude - lineStart.latitude

        if (dx == 0.0 && dy == 0.0) {
            return haversineDistance(lineStart.latitude, lineStart.longitude, point.latitude, point.longitude)
        }

        val t = max(0.0, min(1.0,
            ((point.latitude - lineStart.latitude) * dy + (point.longitude - lineStart.longitude) * dx) /
            (dy * dy + dx * dx)
        ))

        val projLat = lineStart.latitude + t * dy
        val projLng = lineStart.longitude + t * dx

        return haversineDistance(point.latitude, point.longitude, projLat, projLng)
    }

    fun metersToDisplayDistance(meters: Double): String {
        return if (meters >= 1000) {
            String.format("%.2f km", meters / METERS_PER_KM)
        } else {
            String.format("%.0f m", meters)
        }
    }

    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }

    fun formatPace(secondsPerKm: Double): String {
        if (secondsPerKm <= 0) return "--:--"
        val minutes = (secondsPerKm / 60).toInt()
        val seconds = (secondsPerKm % 60).toInt()
        return String.format("%d:%02d", minutes, seconds)
    }
}

data class Split(
    val number: Int,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val paceSecondsPerKm: Double
)
