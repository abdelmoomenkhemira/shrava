package com.example.shrava.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shrava.data.entity.ActivityEntity
import com.example.shrava.ui.theme.AccentGreen
import com.example.shrava.ui.theme.TextMuted
import com.example.shrava.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProgressionChart(
    runs: List<ActivityEntity>,
    modifier: Modifier = Modifier
) {
    if (runs.size < 2) return

    val sorted = runs.sortedBy { it.startTime }
    val paces = sorted.map { it.avgPaceSecondsPerKm }
    val minPace = paces.min() - 30
    val maxPace = paces.max() + 30
    val paceRange = (maxPace - minPace).coerceAtLeast(1.0)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(vertical = 8.dp)
    ) {
        val width = size.width
        val height = size.height
        val padding = 40f
        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding

        // Y-axis grid lines + labels
        val ySteps = 4
        for (i in 0..ySteps) {
            val y = padding + (i.toFloat() / ySteps) * chartHeight
            val paceValue = maxPace - (i.toFloat() / ySteps) * paceRange
            val minutes = (paceValue / 60).toInt()
            val seconds = (paceValue % 60).toInt()

            drawLine(
                color = TextMuted.copy(alpha = 0.2f),
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1f
            )

            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    "${minutes}:${String.format("%02d", seconds)}",
                    padding - 35f,
                    y + 5f,
                    android.graphics.Paint().apply {
                        color = TextSecondary.hashCode()
                        textSize = 24f
                    }
                )
            }
        }

        // Data points + line
        val pointPath = Path()
        val dateFormat = SimpleDateFormat("M/d", Locale.getDefault())

        sorted.forEachIndexed { index, run ->
            val x = padding + (index.toFloat() / (sorted.size - 1)) * chartWidth
            val normalizedPace = ((run.avgPaceSecondsPerKm - minPace) / paceRange).toFloat()
            val y = padding + (1f - normalizedPace) * chartHeight

            if (index == 0) {
                pointPath.moveTo(x, y)
            } else {
                pointPath.lineTo(x, y)
            }

            // Dot
            drawCircle(
                color = AccentGreen,
                radius = 5f,
                center = Offset(x, y)
            )

            // X-axis label (show every Nth)
            if (sorted.size <= 10 || index % (sorted.size / 5).coerceAtLeast(1) == 0 || index == sorted.size - 1) {
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        dateFormat.format(Date(run.startTime)),
                        x - 15f,
                        height - 5f,
                        android.graphics.Paint().apply {
                            color = TextSecondary.hashCode()
                            textSize = 20f
                        }
                    )
                }
            }
        }

        // Line
        drawPath(
            path = pointPath,
            color = AccentGreen,
            style = Stroke(width = 3f)
        )
    }
}
