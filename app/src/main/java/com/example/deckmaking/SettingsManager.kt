package com.example.deckmaking

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_prefs")

class SettingsManager(context: Context) {
    private val dataStore = context.dataStore

    companion object {
        val API_KEY = stringPreferencesKey("api_key")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val DECK_LANGUAGE = stringPreferencesKey("deck_language")
    }

    val apiKeyFlow: Flow<String> = dataStore.data.map { it[API_KEY] ?: "" }
    val selectedModelFlow: Flow<String> = dataStore.data.map { it[SELECTED_MODEL] ?: "gemini-2.5-flash" }
    val temperatureFlow: Flow<Float> = dataStore.data.map { it[TEMPERATURE] ?: 0.2f }
    val deckLanguageFlow: Flow<String> = dataStore.data.map { it[DECK_LANGUAGE] ?: "Inggris" }


    suspend fun saveApiKey(key: String) {
        dataStore.edit { it[API_KEY] = key }
    }

    suspend fun saveSelectedModel(model: String) {
        dataStore.edit { it[SELECTED_MODEL] = model }
    }

    suspend fun saveTemperature(temp: Float) {
        dataStore.edit { it[TEMPERATURE] = temp }
    }

    suspend fun saveDeckLanguage(language: String) {
        dataStore.edit { it[DECK_LANGUAGE] = language }
    }
}

