package com.example.deckmaking

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedViewModel : ViewModel() {
    private val _subtitleTextToProcess = MutableStateFlow("")
    val subtitleTextToProcess = _subtitleTextToProcess.asStateFlow()

    fun setSubtitleText(text: String) {
        _subtitleTextToProcess.value = text
    }

    fun clearSubtitleText() {
        _subtitleTextToProcess.value = ""
    }
}
