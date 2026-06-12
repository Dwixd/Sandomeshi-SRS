package com.example.deckmaking

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deckmaking.data.WordEntity
import com.example.deckmaking.data.WordRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateViewModel(private val repository: WordRepository) : ViewModel() {
    private val _selectedFileUri = MutableStateFlow<Uri?>(null)
    val selectedFileUri: StateFlow<Uri?> = _selectedFileUri.asStateFlow()

    private val _extractedText = MutableStateFlow("")
    val extractedText: StateFlow<String> = _extractedText.asStateFlow()

    private val _selectedLevel = MutableStateFlow("N5")
    val selectedLevel: StateFlow<String> = _selectedLevel.asStateFlow()

    private val _selectedTypes = MutableStateFlow(setOf("Vocabulary", "Kanji", "Grammar"))
    val selectedTypes: StateFlow<Set<String>> = _selectedTypes.asStateFlow()

    private val _generatedJsonResult = MutableStateFlow("")
    val generatedJsonResult: StateFlow<String> = _generatedJsonResult.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _errorLog = MutableStateFlow<String?>(null)
    val errorLog: StateFlow<String?> = _errorLog.asStateFlow()

    private val _itemCount = MutableStateFlow(0)
    val itemCount: StateFlow<Int> = _itemCount.asStateFlow()

    private val _progress = MutableStateFlow(0f)

    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "AI_GEN"

    fun setSelectedFileUri(uri: Uri?) {
        _selectedFileUri.value = uri
        if (uri != null) {
            _extractedText.value = ""
            _generatedJsonResult.value = ""
            _itemCount.value = 0
        }
    }

    fun setExtractedText(text: String) {
        _extractedText.value = text
    }

    fun setSelectedLevel(level: String) {
        _selectedLevel.value = level
    }

    fun setSelectedTypes(types: Set<String>) {
        _selectedTypes.value = types
    }

    fun setGeneratedJsonResult(result: String) {
        _generatedJsonResult.value = result
    }

    fun setProcessing(processing: Boolean) {
        _isProcessing.value = processing
    }

    fun setGenerating(generating: Boolean) {
        _isGenerating.value = generating
    }

    fun setErrorLog(error: String?) {
        _errorLog.value = error
    }

    fun setProgress(progress: Float) {

        _progress.value = progress
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Pembuatan Flashcard"
            val descriptionText = "Menampilkan progress pembuatan dek AI"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun updateNotification(context: Context, total: Int, current: Int, isFinished: Boolean = false, isError: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        createNotificationChannel(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val title = when {
            isError -> "Gagal Membuat Flashcard"
            isFinished -> "Selesai Membuat Flashcard"
            else -> "Membuat Flashcard..."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(!isFinished && !isError)
            .setAutoCancel(isFinished || isError)

        if (!isFinished && !isError) {
            builder.setProgress(total, current, false)
            builder.setContentText("Memproses chunk $current dari $total")
        } else if (isFinished) {
            builder.setProgress(0, 0, false)
            builder.setContentText("Berhasil mengekstraksi data.")
        } else {
            builder.setProgress(0, 0, false)
            builder.setContentText("Terjadi kesalahan saat menghubungi AI.")
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun generateFlashcards(
        context: Context,
        apiKey: String,
        inputText: String,
        targetLevel: String,
        targetTypes: List<String>,
        modelName: String,
        temperature: Float,
        deckLanguage: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            setGenerating(true)
            setProgress(0f)
            setErrorLog(null)
            _itemCount.value = 0
            updateNotification(context, 100, 0) // Initial progress state

            val result = GeminiService.generateFlashcards(
                apiKey = apiKey,
                inputText = inputText,
                targetLevel = targetLevel,
                targetTypes = targetTypes,
                modelName = modelName,
                temperature = temperature,
                deckLanguage = deckLanguage,
                onProgress = { current, total ->
                    val p = if (total > 0) current.toFloat() / total.toFloat() else 0f
                    setProgress(p)
                    updateNotification(context, total, current)
                }
            )

            if (result != null && !result.startsWith("Error:") && !result.startsWith("Chunk failures:")) {
                setGeneratedJsonResult(result)
                
                // --- BRIDGE TO ROOM DATABASE ---
                try {
                    val itemType = object : TypeToken<List<FlashcardItem>>() {}.type
                    val flashcards: List<FlashcardItem> = Gson().fromJson(result, itemType)
                    _itemCount.value = flashcards.size
                    
                    val entities = flashcards.map { item ->
                        WordEntity(
                            type = item.type,
                            item = item.item,
                            reading = item.reading,
                            meaning_en = item.meaning_en,
                            meaning_id = item.meaning_id,
                            example_sentence = item.example_sentence
                        )
                    }
                    
                    // Insert into Room (Repository uses WordDao internally)
                    repository.insertWords(entities)
                    
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        setErrorLog("Gagal menyimpan ke database: ${e.localizedMessage}")
                    }
                }
                // -------------------------------

                updateNotification(context, 0, 0, isFinished = true)
            } else {
                setGeneratedJsonResult(result ?: "Error: Gagal memproses data AI.")
                setErrorLog(result)
                updateNotification(context, 0, 0, isError = true)
            }
            setGenerating(false)
        }
    }

    class Factory(private val repository: WordRepository) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CreateViewModel::class.java)) {
                return CreateViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
