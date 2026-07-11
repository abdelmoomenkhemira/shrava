package com.example.shrava.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shrava.BuildConfig
import com.example.shrava.data.ActivityRepository
import com.example.shrava.data.AppDatabase
import com.example.shrava.data.entity.ActivityEntity
import com.example.shrava.util.LocationUtils
import com.example.shrava.util.RacePredictor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val role: String,
    val content: String
)

data class CoachState(
    val bestRun: ActivityEntity? = null,
    val predictions: List<RacePredictor.RacePrediction> = emptyList(),
    val level: RacePredictor.TrainingLevel? = null,
    val progressionRuns: List<ActivityEntity> = emptyList(),
    val hasRuns: Boolean = false
)

data class AiCoachState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isOnline: Boolean = true,
    val error: String? = null
)

class CoachViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ActivityRepository

    private val _coachState = MutableStateFlow(CoachState())
    val coachState: StateFlow<CoachState> = _coachState.asStateFlow()

    private val _aiState = MutableStateFlow(AiCoachState())
    val aiState: StateFlow<AiCoachState> = _aiState.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    init {
        val db = AppDatabase.getInstance(application)
        repository = ActivityRepository(db.activityDao(), db.locationPointDao())
        loadCoachData()
    }

    fun loadCoachData() {
        viewModelScope.launch {
            val allRuns = repository.getAllRuns()
            val bestRun = RacePredictor.findBestRun(allRuns)
            val predictions = bestRun?.let { RacePredictor.predict(it) } ?: emptyList()
            val level = predictions.firstOrNull { it.distanceName == "5K" }
                ?.let { RacePredictor.getTrainingLevel(it.predictedSeconds) }

            _coachState.value = CoachState(
                bestRun = bestRun,
                predictions = predictions,
                level = level,
                progressionRuns = allRuns.take(50),
                hasRuns = allRuns.isNotEmpty()
            )
        }
    }

    fun sendAiMessage(userMessage: String) {
        val currentMessages = _aiState.value.messages
        val updatedMessages = currentMessages + ChatMessage("user", userMessage)
        _aiState.value = _aiState.value.copy(
            messages = updatedMessages,
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                val response = callGroqApi(updatedMessages)
                _aiState.value = _aiState.value.copy(
                    messages = updatedMessages + ChatMessage("assistant", response),
                    isLoading = false
                )
            } catch (e: Exception) {
                _aiState.value = _aiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun clearAiError() {
        _aiState.value = _aiState.value.copy(error = null)
    }

    private suspend fun callGroqApi(messages: List<ChatMessage>): String {
        return withContext(Dispatchers.IO) {
            val systemPrompt = buildSystemPrompt()
            val messagesArray = JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", systemPrompt))
                messages.forEach { msg ->
                    put(JSONObject().put("role", msg.role).put("content", msg.content))
                }
            }

            val body = JSONObject().apply {
                put("messages", messagesArray)
                put("model", "qwen/qwen3-32b")
                put("temperature", 0.6)
                put("max_completion_tokens", 4096)
                put("top_p", 0.95)
            }

            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")

            if (!response.isSuccessful) {
                val errorJson = JSONObject(responseBody)
                val errorMsg = errorJson.optJSONObject("error")?.optString("message") ?: "API error"
                throw Exception(errorMsg)
            }

            val json = JSONObject(responseBody)
            json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }

    private fun buildSystemPrompt(): String {
        val state = _coachState.value
        val sb = StringBuilder()
        sb.appendLine("You are Shrava's AI running coach. You are knowledgeable, encouraging, and practical.")
        sb.appendLine("Give specific, actionable advice. Keep responses concise but helpful.")
        sb.appendLine()

        state.bestRun?.let { run ->
            sb.appendLine("USER'S BEST RUN:")
            sb.appendLine("- Distance: ${LocationUtils.metersToDisplayDistance(run.distanceMeters)}")
            sb.appendLine("- Duration: ${LocationUtils.formatDuration(run.durationSeconds)}")
            sb.appendLine("- Pace: ${LocationUtils.formatPace(run.avgPaceSecondsPerKm)}/km")
            sb.appendLine()
        }

        if (state.predictions.isNotEmpty()) {
            sb.appendLine("USER'S PREDICTED RACE TIMES:")
            state.predictions.forEach { pred ->
                sb.appendLine("- ${pred.distanceName}: ${LocationUtils.formatDuration(pred.predictedSeconds)} (${LocationUtils.formatPace(pred.paceSecondsPerKm)}/km)")
            }
            sb.appendLine()
        }

        state.level?.let { level ->
            sb.appendLine("USER'S LEVEL: ${level.name}")
            sb.appendLine()
        }

        val recentRuns = state.progressionRuns.take(5)
        if (recentRuns.isNotEmpty()) {
            sb.appendLine("RECENT ACTIVITIES:")
            recentRuns.forEach { run ->
                sb.appendLine("- ${LocationUtils.metersToDisplayDistance(run.distanceMeters)} in ${LocationUtils.formatDuration(run.durationSeconds)} (${LocationUtils.formatPace(run.avgPaceSecondsPerKm)}/km)")
            }
        }

        return sb.toString()
    }
}
