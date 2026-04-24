package com.example.deckmaking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onNavigateToCreate: () -> Unit,
    sharedViewModel: SharedViewModel,
    contentPadding: PaddingValues,
    viewModel: DiscoverViewModel = viewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val animeList by viewModel.filteredAnimeList.collectAsState()
    val fileState by viewModel.fileState.collectAsState()

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }
    var selectedAnime by remember { mutableStateOf<AnimeEntry?>(null) }
    var isDownloading by remember { mutableStateOf(false) }

    val types = listOf("Semua", "TV", "Movie")
    val typesMap = mapOf("Semua" to "All", "TV" to "TV", "Movie" to "Movie")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // FIXED HEADER
            Text(
                text = "Repositori Subtitle",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )

            // SCROLLABLE CONTENT
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari anime...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Bersihkan")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    types.forEach { type ->
                        FilterChip(
                            selected = selectedType == (typesMap[type] ?: type),
                            onClick = { viewModel.onTypeSelected(typesMap[type] ?: type) },
                            label = { Text(type) },
                            shape = MaterialTheme.shapes.extraLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Anime List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(
                        bottom = contentPadding.calculateBottomPadding() + 32.dp
                    )
                ) {
                    items(animeList) { anime ->
                        AnimeListItem(anime) {
                            selectedAnime = anime
                            viewModel.fetchFiles(anime)
                            showSheet = true
                        }
                    }
                }
            }
        }
    }

    if (showSheet && selectedAnime != null) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            var selectedFormat by remember { mutableStateOf("All") }
            var sheetSearchQuery by remember { mutableStateOf("") }
            val formats = listOf("All", ".ass", ".srt")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Pilih File: ${selectedAnime?.title}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Sheet Local Search Bar
                OutlinedTextField(
                    value = sheetSearchQuery,
                    onValueChange = { sheetSearchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari episode atau grup...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (sheetSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { sheetSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Bersihkan")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Format Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val formatsDisplay = mapOf("All" to "Semua", ".ass" to ".ass", ".srt" to ".srt")
                    formats.forEach { format ->
                        FilterChip(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format },
                            label = { Text(formatsDisplay[format] ?: format) },
                            shape = MaterialTheme.shapes.extraLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (val state = fileState) {
                    is FileState.Loading -> {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is FileState.Success -> {
                        val filteredFiles = remember(state.files, selectedFormat, sheetSearchQuery) {
                            state.files.filter { file ->
                                val matchesFormat = selectedFormat == "All" || file.endsWith(selectedFormat, ignoreCase = true)
                                val matchesSearch = sheetSearchQuery.isEmpty() || file.contains(sheetSearchQuery, ignoreCase = true)
                                matchesFormat && matchesSearch
                            }
                        }

                        if (filteredFiles.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Tidak ada file ditemukan untuk format ini", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredFiles) { filename ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                scope.launch {
                                                    isDownloading = true
                                                    val text = viewModel.downloadFileText(selectedAnime!!, filename)
                                                    if (text != null) {
                                                        sharedViewModel.setSubtitleText(text)
                                                        showSheet = false
                                                        onNavigateToCreate()
                                                    }
                                                    isDownloading = false
                                                }
                                            },
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Text(
                                            text = filename,
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is FileState.Error -> {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                    else -> {}
                }
            }
        }
    }

    if (isDownloading) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = { },
            title = { Text("Mengunduh Subtitle") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        )
    }
}

@Composable
fun AnimeListItem(anime: AnimeEntry, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        ListItem(
            modifier = Modifier.padding(horizontal = 8.dp),
            headlineContent = {
                Text(
                    text = anime.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            trailingContent = {
                TypeBadge(type = anime.type)
            },
            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
        )
    }
}

@Composable
fun TypeBadge(type: String) {
    val containerColor = if (type == "TV") {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    
    val contentColor = if (type == "TV") {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = type,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
