package com.example.shrava.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.shrava.data.entity.LocationPointEntity
import com.example.shrava.ui.theme.AccentGreen
import com.example.shrava.ui.theme.DarkCard
import com.example.shrava.ui.theme.TextMuted

@Composable
fun ElevationChart(
    points: List<LocationPointEntity>,
    modifier: Modifier = Modifier
) {
    val altitudes = points.mapNotNull { it.altitude }
    if (altitudes.size < 2) return

    val minAlt = altitudes.min()
    val maxAlt = altitudes.max()
    val range = (maxAlt - minAlt).coerceAtLeast(1.0)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(vertical = 8.dp)
    ) {
        val width = size.width
        val height = size.height
        val padding = 8f

        // Grid lines
        for (i in 0..3) {
            val y = padding + (i.toFloat() / 3f) * (height - 2 * padding)
            drawLine(
                color = TextMuted.copy(alpha = 0.2f),
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1f
            )
        }

        // Area fill gradient
        val areaPath = Path()
        areaPath.moveTo(padding, height - padding)
        altitudes.forEachIndexed { index, altitude ->
            val x = padding + (index.toFloat() / (altitudes.size - 1)) * (width - 2 * padding)
            val normalizedAlt = ((altitude - minAlt) / range).toFloat()
            val y = height - padding - normalizedAlt * (height - 2 * padding)
            areaPath.lineTo(x, y)
        }
        areaPath.lineTo(width - padding, height - padding)
        areaPath.close()

        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    AccentGreen.copy(alpha = 0.3f),
                    AccentGreen.copy(alpha = 0.0f)
                )
            )
        )

        // Line
        val linePath = Path()
        altitudes.forEachIndexed { index, altitude ->
            val x = padding + (index.toFloat() / (altitudes.size - 1)) * (width - 2 * padding)
            val normalizedAlt = ((altitude - minAlt) / range).toFloat()
            val y = height - padding - normalizedAlt * (height - 2 * padding)

            if (index == 0) {
                linePath.moveTo(x, y)
            } else {
                linePath.lineTo(x, y)
            }
        }

        drawPath(
            path = linePath,
            color = AccentGreen,
            style = Stroke(width = 3f)
        )
    }
}
