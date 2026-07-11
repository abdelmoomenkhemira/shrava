package com.example.shrava.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shrava.data.ActivityRepository
import com.example.shrava.data.AppDatabase
import com.example.shrava.data.entity.ActivityEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ActivityRepository

    val activities: StateFlow<List<ActivityEntity>>

    init {
        val db = AppDatabase.getInstance(application)
        repository = ActivityRepository(db.activityDao(), db.locationPointDao())
        activities = repository.getAllActivities()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    fun deleteActivity(activityId: Long) {
        viewModelScope.launch {
            repository.deleteActivity(activityId)
        }
    }
}
