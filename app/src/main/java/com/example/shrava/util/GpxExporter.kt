package com.example.shrava.util

import com.example.shrava.data.entity.ActivityEntity
import com.example.shrava.data.entity.LocationPointEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object GpxExporter {

    fun generateGpx(activity: ActivityEntity, points: List<LocationPointEntity>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<gpx version="1.1" creator="Shrava" xmlns="http://www.topografix.com/GPX/1/1">""")

        sb.appendLine("  <metadata>")
        val startDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(activity.startTime))
        sb.appendLine("    <name>${activity.type} - $startDate</name>")
        sb.appendLine("  </metadata>")

        sb.appendLine("  <trk>")
        sb.appendLine("    <name>${activity.type}</name>")
        sb.appendLine("    <type>${activity.type}</type>")
        sb.appendLine("    <trkseg>")

        for (point in points) {
            sb.append("      <trkpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">")
            point.altitude?.let { sb.append("<ele>${it}</ele>") }
            sb.append("<time>${dateFormat.format(Date(point.timestamp))}</time>")
            sb.appendLine("</trkpt>")
        }

        sb.appendLine("    </trkseg>")
        sb.appendLine("  </trk>")
        sb.appendLine("</gpx>")

        return sb.toString()
    }
}
