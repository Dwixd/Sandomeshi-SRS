package com.example.deckmaking

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.draw.clip
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.deckmaking.data.WordDatabase
import com.example.deckmaking.data.WordRepository
import com.example.deckmaking.ui.theme.DeckMakingTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsManager = SettingsManager(this)
        enableEdgeToEdge()
        setContent {
            DeckMakingTheme {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(settingsManager)
                )
                val sharedViewModel: SharedViewModel = viewModel()
                MainScreen(settingsViewModel, sharedViewModel)
            }
        }
    }
}

@Composable
fun MainScreen(settingsViewModel: SettingsViewModel, sharedViewModel: SharedViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Hoisting ViewModels to Activity scope to prevent state loss during tab switching
    val context = LocalContext.current.applicationContext
    val repository = remember { WordRepository(WordDatabase.getDatabase(context).wordDao()) }
    
    val activity = LocalContext.current as ComponentActivity
    val inventoryViewModel: InventoryViewModel = viewModel(
        activity,
        factory = InventoryViewModel.Factory(repository)
    )
    val discoverViewModel: DiscoverViewModel = viewModel(activity)
    val createViewModel: CreateViewModel = viewModel(
        activity,
        factory = CreateViewModel.Factory(repository)
    )

    val items = listOf(
        BottomNavItem.Inventory,
        BottomNavItem.Discover,
        BottomNavItem.Create,
        BottomNavItem.Settings
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                NavigationBar(
                    modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(32.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets(0.dp)
                ) {
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = currentRoute == item.route,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        fun getRouteIndex(route: String?): Int {
            return when (route) {
                BottomNavItem.Inventory.route -> 0
                BottomNavItem.Discover.route -> 1
                BottomNavItem.Create.route -> 2
                BottomNavItem.Settings.route -> 3
                else -> 0
            }
        }

        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Discover.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                val initialIndex = getRouteIndex(initialState.destination.route)
                val targetIndex = getRouteIndex(targetState.destination.route)
                if (targetIndex > initialIndex) {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
                } else {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
                }
            },
            exitTransition = {
                val initialIndex = getRouteIndex(initialState.destination.route)
                val targetIndex = getRouteIndex(targetState.destination.route)
                if (targetIndex > initialIndex) {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
                } else {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
                }
            }
        ) {
            composable(BottomNavItem.Inventory.route) {
                InventoryScreen(
                    viewModel = inventoryViewModel,
                    contentPadding = innerPadding
                )
            }
            composable(BottomNavItem.Discover.route) {
                DiscoverScreen(
                    viewModel = discoverViewModel,
                    onNavigateToCreate = {
                        navController.navigate(BottomNavItem.Create.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    sharedViewModel = sharedViewModel,
                    contentPadding = innerPadding
                )
            }
            composable(BottomNavItem.Create.route) {
                DeckGeneratorScreen(
                    viewModel = createViewModel,
                    settingsViewModel = settingsViewModel,
                    sharedViewModel = sharedViewModel,
                    onNavigateToSettings = {
                        navController.navigate(BottomNavItem.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    contentPadding = innerPadding
                )
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = {
                        navController.popBackStack()
                    },
                    contentPadding = innerPadding
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckGeneratorScreen(
    viewModel: CreateViewModel,
    settingsViewModel: SettingsViewModel,
    sharedViewModel: SharedViewModel,
    onNavigateToSettings: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // UI State from ViewModel
    val selectedFileUri by viewModel.selectedFileUri.collectAsState()
    val extractedText by viewModel.extractedText.collectAsState()
    val selectedLevel by viewModel.selectedLevel.collectAsState()
    val selectedTypes by viewModel.selectedTypes.collectAsState()
    val generatedJsonResult by viewModel.generatedJsonResult.collectAsState()
    val itemCount by viewModel.itemCount.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val errorLog by viewModel.errorLog.collectAsState()

    // Settings

    val apiKey by settingsViewModel.apiKey.collectAsState()
    val selectedModel by settingsViewModel.selectedModel.collectAsState()
    val temperature by settingsViewModel.temperature.collectAsState()
    val deckLanguage by settingsViewModel.deckLanguage.collectAsState()

    val subtitleFromShared by sharedViewModel.subtitleTextToProcess.collectAsState()

    // Sync from SharedViewModel
    LaunchedEffect(subtitleFromShared) {
        if (subtitleFromShared.isNotEmpty()) {
            viewModel.setExtractedText(subtitleFromShared)
            viewModel.setSelectedFileUri(null)
            sharedViewModel.clearSubtitleText()
        }
    }

    val levels = listOf("N5", "N4", "N3", "N2", "N1")
    val itemTypes = listOf("Vocabulary", "Kanji", "Grammar")
    val itemTypesDisplay = mapOf("Vocabulary" to "Kosakata", "Kanji" to "Kanji", "Grammar" to "Tata Bahasa")

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        viewModel.setSelectedFileUri(uri)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val tsvContent = ExportHelper.parseJsonToTsv(generatedJsonResult)
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            outputStream.write(tsvContent.toByteArray())
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Export Successful!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Export Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // FIXED HEADER
            Text(
                text = "Buat Dek Anki",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .align(Alignment.Start)
            )

            // SCROLLABLE CONTENT
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = contentPadding.calculateBottomPadding() + 32.dp)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    filePickerLauncher.launch(arrayOf("*/*"))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("Pilih File Subtitle (.ass / .srt)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            selectedFileUri?.let { uri ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "File Terpilih:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uri.path ?: "Unknown path",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        scope.launch {
                            viewModel.setProcessing(true)
                            val raw = withContext(Dispatchers.IO) {
                                SubtitleParser.readTextFromUri(context, uri)
                            }

                            if (raw != null) {
                                val clean = withContext(Dispatchers.IO) {
                                    if (raw.contains("Dialogue:", ignoreCase = true)) {
                                        SubtitleParser.parseAss(raw)
                                    } else {
                                        SubtitleParser.parseSrt(raw)
                                    }
                                }
                                viewModel.setExtractedText(clean)
                            }
                            viewModel.setProcessing(false)
                        }
                    },
                    enabled = !isProcessing && !isGenerating,
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onSecondary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Memproses...")
                    } else {
                        Text("Ekstrak Teks Subtitle")
                    }
                }
            }

            if (extractedText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Preview Teks (200 karakter pertama):",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.align(Alignment.Start),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = extractedText.take(200).let { if (extractedText.length > 200) "$it..." else it },
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Pilih Level JLPT:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.align(Alignment.Start),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    levels.forEach { level ->
                        FilterChip(
                            selected = selectedLevel == level,
                            onClick = { viewModel.setSelectedLevel(level) },
                            label = { Text(level) },
                            shape = MaterialTheme.shapes.extraLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Pilih Tipe:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.align(Alignment.Start),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemTypes.forEach { type ->
                        FilterChip(
                            selected = selectedTypes.contains(type),
                            onClick = {
                                val newTypes = if (selectedTypes.contains(type)) {
                                    selectedTypes - type
                                } else {
                                    selectedTypes + type
                                }
                                viewModel.setSelectedTypes(newTypes)
                            },
                            label = { Text(itemTypesDisplay[type] ?: type) },
                            modifier = Modifier.padding(end = 8.dp),
                            shape = MaterialTheme.shapes.extraLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Pembuatan Dek akan membutuhkan banyak waktu, Anda bisa membiarkan aplikasi berjalan di latar belakang.",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                Button(
                    onClick = {
                        if (apiKey.isBlank()) {
                            Toast.makeText(context, "Silakan masukkan API Key di menu Settings", Toast.LENGTH_LONG).show()
                            onNavigateToSettings()
                            return@Button
                        }
                        
                        viewModel.generateFlashcards(
                            context = context,
                            apiKey = apiKey,
                            inputText = extractedText,
                            targetLevel = selectedLevel,
                            targetTypes = selectedTypes.toList(),
                            modelName = selectedModel,
                            temperature = temperature,
                            deckLanguage = deckLanguage
                        )
                    },
                    enabled = !isGenerating && !isProcessing && selectedTypes.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isGenerating) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Membangun Dek AI...")
                        }
                    } else {
                        Text("Buat Flashcard (Gemini AI)")
                    }
                }

                if (generatedJsonResult.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))

                    if (!generatedJsonResult.startsWith("Error:") && !generatedJsonResult.startsWith("Chunk failures:")) {
                        Button(
                            onClick = {
                                exportLauncher.launch("deck_${selectedLevel}_${System.currentTimeMillis()}.txt")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("Export to Anki (.txt)")
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = if (errorLog != null) "Detail Kesalahan:" else if (itemCount > 0) "Hasil JSON ($itemCount Item):" else "Hasil JSON:",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.align(Alignment.Start),
                        fontWeight = FontWeight.Bold,
                        color = if (errorLog != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        color = if (errorLog != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = generatedJsonResult,
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (errorLog != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

            }
        }
    }
}
}


