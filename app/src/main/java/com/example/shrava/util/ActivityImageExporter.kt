package com.example.shrava.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Environment
import com.example.shrava.data.entity.ActivityEntity
import com.example.shrava.data.entity.LocationPointEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ActivityImageExporter {

    private const val WIDTH = 1080
    private const val HEIGHT = 1920
    private const val BG_COLOR = 0xFF0D0D0D.toInt()
    private const val GREEN = 0xFF00E676.toInt()
    private const val RED = 0xFFFF5252.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val GRAY = 0xFF8A8A8E.toInt()
    private const val DARK_CARD = 0xFF242424.toInt()
    private const val DARK_SURFACE = 0xFF1A1A1A.toInt()

    fun generate(context: Context, activity: ActivityEntity, points: List<LocationPointEntity>): File {
        val bitmap = android.graphics.Bitmap.createBitmap(WIDTH, HEIGHT, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        canvas.drawColor(BG_COLOR)

        // Gradient top
        val gradientPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, 400f,
                intArrayOf(0xFF1A1A1A.toInt(), BG_COLOR),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), 400f, gradientPaint)

        // Brand
        val brandPaint = Paint().apply {
            color = GREEN
            textSize = 48f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("SHRAVA", WIDTH / 2f, 100f, brandPaint)

        // Activity type
        val typePaint = Paint().apply {
            color = WHITE
            textSize = 72f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(activity.type.uppercase(), WIDTH / 2f, 200f, typePaint)

        // Date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date(activity.startTime))
        val datePaint = Paint().apply {
            color = GRAY
            textSize = 32f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(dateStr, WIDTH / 2f, 260f, datePaint)

        // Route
        if (points.size >= 2) {
            val geoPoints = points.map { org.osmdroid.util.GeoPoint(it.latitude, it.longitude) }
            val bbox = org.osmdroid.util.BoundingBox.fromGeoPoints(geoPoints)

            val routeTop = 320f
            val routeBottom = 1200f
            val routeLeft = 60f
            val routeRight = WIDTH - 60f
            val routeWidth = routeRight - routeLeft
            val routeHeight = routeBottom - routeTop
            val routeCenterY = (routeTop + routeBottom) / 2f

            val latRange = bbox.latNorth - bbox.latSouth
            val lonRange = bbox.lonEast - bbox.lonWest
            val padding = 40f

            val routePaint = Paint().apply {
                color = GREEN
                strokeWidth = 6f
                style = Paint.Style.STROKE
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            val routePath = Path()
            points.forEachIndexed { index, point ->
                val x = routeLeft + padding + ((point.longitude - bbox.lonWest) / lonRange).toFloat() * (routeWidth - 2 * padding)
                val y = routeTop + padding + (1f - (point.latitude - bbox.latSouth) / latRange).toFloat() * (routeHeight - 2 * padding)

                if (index == 0) {
                    routePath.moveTo(x, y)
                } else {
                    routePath.lineTo(x, y)
                }
            }

            canvas.drawPath(routePath, routePaint)

            // Start dot
            val firstPoint = points.first()
            val startX = routeLeft + padding + ((firstPoint.longitude - bbox.lonWest) / lonRange).toFloat() * (routeWidth - 2 * padding)
            val startY = routeTop + padding + (1f - (firstPoint.latitude - bbox.latSouth) / latRange).toFloat() * (routeHeight - 2 * padding)
            val dotPaint = Paint().apply { color = GREEN; isAntiAlias = true }
            canvas.drawCircle(startX, startY, 12f, dotPaint)

            // End dot
            val lastPoint = points.last()
            val endX = routeLeft + padding + ((lastPoint.longitude - bbox.lonWest) / lonRange).toFloat() * (routeWidth - 2 * padding)
            val endY = routeTop + padding + (1f - (lastPoint.latitude - bbox.latSouth) / latRange).toFloat() * (routeHeight - 2 * padding)
            dotPaint.color = RED
            canvas.drawCircle(endX, endY, 12f, dotPaint)
        }

        // Stats card background
        val cardPaint = Paint().apply { color = DARK_CARD }
        val cardRadius = 24f
        val cardRect = android.graphics.RectF(40f, 1280f, WIDTH - 40f, 1820f)
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardPaint)

        // Stats
        val statLabelPaint = Paint().apply {
            color = GRAY
            textSize = 28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val statValuePaint = Paint().apply {
            color = WHITE
            textSize = 64f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val statUnitPaint = Paint().apply {
            color = GRAY
            textSize = 32f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // Distance
        val distText = if (activity.distanceMeters >= 1000) {
            String.format("%.2f", activity.distanceMeters / 1000.0)
        } else {
            String.format("%.0f", activity.distanceMeters)
        }
        val distUnit = if (activity.distanceMeters >= 1000) "KM" else "M"
        canvas.drawText("DISTANCE", WIDTH / 2f, 1360f, statLabelPaint)
        canvas.drawText(distText, WIDTH / 2f, 1440f, statValuePaint)
        canvas.drawText(distUnit, WIDTH / 2f, 1480f, statUnitPaint)

        // Divider
        val dividerPaint = Paint().apply { color = 0xFF333333.toInt(); strokeWidth = 1f }
        canvas.drawLine(WIDTH / 3f, 1510f, WIDTH * 2f / 3f, 1510f, dividerPaint)

        // Duration
        val durationText = formatDuration(activity.durationSeconds)
        canvas.drawText("TIME", WIDTH / 2f, 1560f, statLabelPaint)
        canvas.drawText(durationText, WIDTH / 2f, 1640f, statValuePaint)

        // Divider 2
        canvas.drawLine(WIDTH / 3f, 1670f, WIDTH * 2f / 3f, 1670f, dividerPaint)

        // Pace
        val paceText = formatPace(activity.avgPaceSecondsPerKm)
        canvas.drawText("PACE", WIDTH / 2f, 1720f, statLabelPaint)
        canvas.drawText(paceText, WIDTH / 2f, 1790f, statValuePaint)
        canvas.drawText("/KM", WIDTH / 2f, 1810f, statUnitPaint)

        // Save
        val dir = File(context.cacheDir, "share")
        dir.mkdirs()
        val file = File(dir, "shrava_${activity.type}_${activity.startTime}.png")
        file.outputStream().use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        return file
    }

    private fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
    }

    private fun formatPace(secondsPerKm: Double): String {
        if (secondsPerKm <= 0) return "--:--"
        val minutes = (secondsPerKm / 60).toInt()
        val seconds = (secondsPerKm % 60).toInt()
        return String.format("%d:%02d", minutes, seconds)
    }
}
