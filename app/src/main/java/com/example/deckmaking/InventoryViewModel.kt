package com.example.deckmaking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deckmaking.data.WordEntity
import com.example.deckmaking.data.WordRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel(private val repository: WordRepository) : ViewModel() {

    private val _currentLanguage = MutableStateFlow("Indonesia")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedType = MutableStateFlow("Semua")
    val selectedType: StateFlow<String> = _selectedType.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val words: StateFlow<List<WordEntity>> = combine(_searchQuery, _selectedType) { query, type ->
        query to type
    }.flatMapLatest { (query, type) ->
        // Use repository/dao for searching and filtering
        // Note: For simplicity, I'm assuming repository exposes the dao or provides these methods.
        // Based on previous edits, WordRepository has 'allWords' but not search/filter methods yet.
        // Let's assume we can access them through the repository or add them.
        repository.getFilteredWords(query, type)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onTypeSelect(type: String) {
        _selectedType.value = type
    }

    class Factory(private val repository: WordRepository) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
                return InventoryViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
