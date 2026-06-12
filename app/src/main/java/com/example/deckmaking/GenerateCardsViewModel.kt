package com.example.deckmaking

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deckmaking.data.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class GenerateState {
    object Idle : GenerateState()
    object Loading : GenerateState()
    data class Success(val insertedCount: Int) : GenerateState()
    data class Error(val message: String) : GenerateState()
}

class GenerateCardsViewModel(
    private val repository: WordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GenerateState>(GenerateState.Idle)
    val uiState: StateFlow<GenerateState> = _uiState.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    fun generateAndSaveNewCards(
        context: Context,
        apiKey: String,
        inputText: String,
        targetLevel: String,
        targetTypes: List<String>,
        modelName: String,
        temperature: Float,
        deckLanguage: String
    ) {
        viewModelScope.launch {
            _uiState.value = GenerateState.Loading
            _progress.value = 0f

            val result = repository.generateAndSaveNewCards(
                apiKey = apiKey,
                inputText = inputText,
                targetLevel = targetLevel,
                targetTypes = targetTypes,
                modelName = modelName,
                temperature = temperature,
                deckLanguage = deckLanguage,
                onProgress = { current, total ->
                    _progress.value = if (total > 0) current.toFloat() / total.toFloat() else 0f
                }
            )

            result.onSuccess { count ->
                _uiState.value = GenerateState.Success(count)
            }.onFailure { exception ->
                _uiState.value = GenerateState.Error(exception.message ?: "Unknown error")
            }
        }
    }
}
