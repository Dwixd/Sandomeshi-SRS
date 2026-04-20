package com.example.deckmaking

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import com.example.deckmaking.ui.theme.DeckMakingTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeckMakingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DeckGeneratorScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckGeneratorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var extractedText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    // Selection state
    var selectedLevel by remember { mutableStateOf("N5") }
    val levels = listOf("N5", "N4", "N3", "N2", "N1")

    var selectedTypes by remember { mutableStateOf(setOf("Vocabulary", "Kanji", "Grammar")) }
    val itemTypes = listOf("Vocabulary", "Kanji", "Grammar")

    // State for Gemini AI process
    var isGenerating by remember { mutableStateOf(false) }
    var generatedJsonResult by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            extractedText = "" // Reset extracted text when a new file is picked
            generatedJsonResult = "" // Reset AI result
        }
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Anki Deck Generator",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                filePickerLauncher.launch(arrayOf("*/*"))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Pilih File Subtitle (.ass / .srt)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        selectedFileUri?.let { uri ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "File Terpilih:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
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
                        isProcessing = true
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
                            extractedText = clean
                        }
                        isProcessing = false
                    }
                },
                enabled = !isProcessing && !isGenerating,
                modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = extractedText.take(200).let { if (extractedText.length > 200) "$it..." else it },
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dynamic Selection Section
            Text(
                text = "Select JLPT Level:",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.align(Alignment.Start)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                levels.forEach { level ->
                    FilterChip(
                        selected = selectedLevel == level,
                        onClick = { selectedLevel = level },
                        label = { Text(level) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Select Types:",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.align(Alignment.Start)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemTypes.forEach { type ->
                    FilterChip(
                        selected = selectedTypes.contains(type),
                        onClick = {
                            selectedTypes = if (selectedTypes.contains(type)) {
                                selectedTypes - type
                            } else {
                                selectedTypes + type
                            }
                        },
                        label = { Text(type) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        isGenerating = true
                        val result = withContext(Dispatchers.IO) {
                            GeminiService.generateFlashcards(
                                apiKey = "AQ.Ab8RN6LGnOTuLvoDeT13Zu1sDx7Z0mrt56rtaMUyj6ReDGatHg",
                                inputText = extractedText,
                                targetLevel = selectedLevel,
                                targetTypes = selectedTypes.toList()
                            )
                        }
                        generatedJsonResult = result ?: "Error: Gagal memproses data AI."
                        isGenerating = false
                    }
                },
                enabled = !isGenerating && !isProcessing && selectedTypes.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onTertiary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Generating AI Deck...")
                } else {
                    Text("Generate Flashcards (Gemini AI)")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Pembuatan dek mungkin akan membutuhkan waktu yang lebih lama, mohon tunggu",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (generatedJsonResult.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        exportLauncher.launch("deck_${selectedLevel}_${System.currentTimeMillis()}.txt")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("Export to Anki (.txt)")
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "JSON Result:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 1.dp
                ) {
                    Text(
                        text = generatedJsonResult,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}