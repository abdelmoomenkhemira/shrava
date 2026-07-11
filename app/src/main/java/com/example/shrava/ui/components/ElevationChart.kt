package com.example.shrava.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.shrava.data.entity.LocationPointEntity

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

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val width = size.width
        val height = size.height
        val padding = 4f

        drawRect(
            color = surfaceVariant,
            size = size
        )

        val path = Path()
        altitudes.forEachIndexed { index, altitude ->
            val x = padding + (index.toFloat() / (altitudes.size - 1)) * (width - 2 * padding)
            val normalizedAlt = ((altitude - minAlt) / range).toFloat()
            val y = height - padding - normalizedAlt * (height - 2 * padding)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3f)
        )
    }
}
