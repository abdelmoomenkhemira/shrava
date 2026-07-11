package com.example.shrava.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val startTime: Long,
    val durationSeconds: Long,
    val distanceMeters: Double,
    val avgPaceSecondsPerKm: Double
)
