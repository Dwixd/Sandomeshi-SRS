package com.example.deckmaking

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AnimeEntry(val title: String, val type: String)

sealed class FileState {
    object Idle : FileState()
    object Loading : FileState()
    data class Success(val files: List<String>) : FileState()
    data class Error(val message: String) : FileState()
}

class DiscoverViewModel : ViewModel() {

    // Removed hardcoded mock list and replaced with a StateFlow for fetched data
    private val _allAnime = MutableStateFlow<List<AnimeEntry>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedType = MutableStateFlow("All")
    val selectedType: StateFlow<String> = _selectedType.asStateFlow()

    private val _fileState = MutableStateFlow<FileState>(FileState.Idle)
    val fileState: StateFlow<FileState> = _fileState.asStateFlow()

    // Filtering logic now combines the dynamic backend data with search and filter states
    val filteredAnimeList: StateFlow<List<AnimeEntry>> = combine(
        _allAnime,
        _searchQuery,
        _selectedType
    ) { animeList, query, type ->
        animeList.filter { anime ->
            val matchesQuery = anime.title.contains(query, ignoreCase = true)
            val matchesType = type == "All" || anime.type == type
            matchesQuery && matchesType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        fetchAllAnime()
    }

    private fun fetchAllAnime() {
        viewModelScope.launch {
            try {
                // Fetching real list from VPS backend
                val result = SubtitleClient.service.getAllAnime()
                _allAnime.value = result
            } catch (e: Exception) {
                Log.e("DiscoverViewModel", "Failed to fetch anime list from backend", e)
                // Basic error handling - list remains empty or could be handled via a UI state
            }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onTypeSelected(type: String) {
        _selectedType.value = type
    }

    fun fetchFiles(anime: AnimeEntry) {
        viewModelScope.launch {
            _fileState.value = FileState.Loading
            try {
                // Fix: Always use the exact type from AnimeEntry ("TV" or "Movie")
                val typeForApi = anime.type 
                val files = SubtitleClient.service.getAnimeFiles(typeForApi, anime.title)
                _fileState.value = FileState.Success(files)
            } catch (e: Exception) {
                _fileState.value = FileState.Error(e.localizedMessage ?: "Failed to fetch files")
            }
        }
    }

    suspend fun downloadFileText(anime: AnimeEntry, filename: String): String? {
        return try {
            val folder = if (anime.type == "TV") "anime_tv" else "anime_movie"
            val response = SubtitleClient.service.downloadSubtitle(folder, anime.title, filename)
            val rawText = response.string()

            // Apply parsing logic based on file extension
            when {
                filename.endsWith(".ass", ignoreCase = true) -> SubtitleParser.parseAss(rawText)
                filename.endsWith(".srt", ignoreCase = true) -> SubtitleParser.parseSrt(rawText)
                else -> rawText // Fallback to raw if unknown format
            }
        } catch (e: Exception) {
            null
        }
    }
}
