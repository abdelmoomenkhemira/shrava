package com.example.shrava.util

import com.example.shrava.data.entity.ActivityEntity
import kotlin.math.pow

object RacePredictor {

    const val DISTANCE_5K = 5000.0
    const val DISTANCE_10K = 10000.0
    const val DISTANCE_HALF = 21097.5
    const val DISTANCE_MARATHON = 42195.0

    private const val RIEGEL_EXPONENT = 1.06

    data class RacePrediction(
        val distanceName: String,
        val distanceMeters: Double,
        val predictedSeconds: Long,
        val paceSecondsPerKm: Double
    )

    data class TrainingLevel(
        val name: String,
        val tip: String
    )

    fun predict(referenceRun: ActivityEntity): List<RacePrediction> {
        val d1 = referenceRun.distanceMeters
        val t1 = referenceRun.durationSeconds.toDouble()
        if (d1 <= 0 || t1 <= 0) return emptyList()

        return listOf(
            RacePrediction("5K", DISTANCE_5K, predictTime(t1, d1, DISTANCE_5K), 0.0),
            RacePrediction("10K", DISTANCE_10K, predictTime(t1, d1, DISTANCE_10K), 0.0),
            RacePrediction("Half Marathon", DISTANCE_HALF, predictTime(t1, d1, DISTANCE_HALF), 0.0),
            RacePrediction("Marathon", DISTANCE_MARATHON, predictTime(t1, d1, DISTANCE_MARATHON), 0.0)
        ).map {
            it.copy(paceSecondsPerKm = if (it.predictedSeconds > 0) it.predictedSeconds / (it.distanceMeters / 1000.0) else 0.0)
        }
    }

    private fun predictTime(t1: Double, d1: Double, d2: Double): Long {
        return (t1 * (d2 / d1).pow(RIEGEL_EXPONENT)).toLong()
    }

    fun getTrainingLevel(predicted5kSeconds: Long): TrainingLevel {
        return when {
            predicted5kSeconds < 20 * 60 -> TrainingLevel(
                "Advanced",
                "Focus on interval training and race-specific workouts. Consider adding hill repeats and tempo runs to sharpen your speed."
            )
            predicted5kSeconds < 25 * 60 -> TrainingLevel(
                "Intermediate",
                "Build your weekly mileage gradually and add tempo runs. Consistency is key — aim for 3-5 runs per week."
            )
            predicted5kSeconds < 30 * 60 -> TrainingLevel(
                "Beginner+",
                "Increase consistency by running 3-4 times per week. Mix easy runs with occasional faster efforts."
            )
            else -> TrainingLevel(
                "Beginner",
                "Build a base with easy runs and focus on time on feet. Walk/run intervals are a great way to start. Aim for 3 sessions per week."
            )
        }
    }

    fun findBestRun(runs: List<ActivityEntity>): ActivityEntity? {
        return runs
            .filter { it.type == "Run" && it.distanceMeters >= 1000 }
            .minByOrNull { it.avgPaceSecondsPerKm }
    }
}
