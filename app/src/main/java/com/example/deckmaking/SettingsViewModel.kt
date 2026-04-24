package com.example.deckmaking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsManager: SettingsManager) : ViewModel() {

    val apiKey: StateFlow<String> = settingsManager.apiKeyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val selectedModel: StateFlow<String> = settingsManager.selectedModelFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "gemini-1.5-flash")

    val temperature: StateFlow<Float> = settingsManager.temperatureFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.2f)

    val deckLanguage: StateFlow<String> = settingsManager.deckLanguageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Inggris")

    fun saveApiKey(key: String) {
        viewModelScope.launch { settingsManager.saveApiKey(key) }
    }

    fun saveSelectedModel(model: String) {
        viewModelScope.launch { settingsManager.saveSelectedModel(model) }
    }

    fun saveTemperature(temp: Float) {
        viewModelScope.launch { settingsManager.saveTemperature(temp) }
    }

    fun saveDeckLanguage(language: String) {
        viewModelScope.launch { settingsManager.saveDeckLanguage(language) }
    }

    class Factory(private val settingsManager: SettingsManager) : ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(settingsManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
