package com.example.deckmaking.data

import com.example.deckmaking.FlashcardItem
import com.example.deckmaking.GeminiService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class WordRepository(private val wordDao: WordDao) {

    val allWords: Flow<List<WordEntity>> = wordDao.getAllWords()

    suspend fun insertWords(words: List<WordEntity>) {
        wordDao.insertWords(words)
    }

    fun getFilteredWords(query: String, type: String): Flow<List<WordEntity>> {
        val mappedType = when (type) {
            "Kosakata" -> "Vocabulary"
            "Tata Bahasa" -> "Grammar"
            else -> type
        }
        return if (query.isEmpty()) {
            if (mappedType == "Semua" || mappedType == "All") {
                wordDao.getAllWords()
            } else {
                wordDao.getWordsByType(mappedType)
            }
        } else {
            wordDao.searchWords(query)
        }
    }

    suspend fun generateAndSaveNewCards(
        apiKey: String,
        inputText: String,
        targetLevel: String,
        targetTypes: List<String>,
        modelName: String,
        temperature: Float,
        deckLanguage: String,
        onProgress: (Int, Int) -> Unit
    ): Result<Int> {
        return try {
            val jsonResult = GeminiService.generateFlashcards(
                apiKey, inputText, targetLevel, targetTypes, modelName, temperature, deckLanguage, onProgress
            )

            if (jsonResult == null || jsonResult.startsWith("Chunk failures:") || jsonResult.startsWith("Error:")) {
                return Result.failure(Exception(jsonResult ?: "Unknown error from GeminiService"))
            }

            val itemType = object : TypeToken<List<FlashcardItem>>() {}.type
            val flashcards: List<FlashcardItem> = Gson().fromJson(jsonResult, itemType)

            val existingItems = wordDao.getAllWords().first().map { it.item }.toSet()
            
            val newEntities = flashcards
                .filter { it.item !in existingItems }
                .map { item ->
                    WordEntity(
                        type = item.type,
                        item = item.item,
                        reading = item.reading,
                        meaning_en = item.meaning_en,
                        meaning_id = item.meaning_id,
                        example_sentence = item.example_sentence
                    )
                }

            wordDao.insertWords(newEntities)
            Result.success(newEntities.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
