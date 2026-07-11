package com.example.shrava.util

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    companion object {
        private val USE_KILOMETERS = booleanPreferencesKey("use_kilometers")
        private val FIRST_LAUNCH_DONE = booleanPreferencesKey("first_launch_done")
    }

    val useKilometers: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[USE_KILOMETERS] ?: true
    }

    val isFirstLaunchDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[FIRST_LAUNCH_DONE] ?: false
    }

    suspend fun setUseKilometers(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[USE_KILOMETERS] = value
        }
    }

    suspend fun setFirstLaunchDone() {
        context.dataStore.edit { prefs ->
            prefs[FIRST_LAUNCH_DONE] = true
        }
    }
}
