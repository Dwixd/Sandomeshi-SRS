package com.example.deckmaking

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay

/**
 * Data class representing a single flashcard item.
 */
data class FlashcardItem(
    val type: String,
    val item: String,
    val reading: String,
    val meaning: String,
    val example_sentence: String
)

object GeminiService {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Generates flashcards using Gemini AI with sequential chunking and JSON parsing.
     * Chunk size increased to 150 lines to provide more context.
     */
    suspend fun generateFlashcards(
        apiKey: String,
        inputText: String,
        targetLevel: String,
        targetTypes: List<String>,
        modelName: String,
        temperature: Float,
        deckLanguage: String,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): String? {
        // 1. Optimize input: remove duplicates, empty lines, and subtitle artifacts (arrows)
        val linesToProcess = inputText.lines()
            .map { it.replace(Regex("[→➡⇒▶]"), "").trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        // 2. Increased chunk size to 150 lines for better context
        val chunks = linesToProcess.chunked(150)
        val totalChunks = chunks.size
        val masterList = mutableListOf<FlashcardItem>()
        val errors = mutableListOf<String>()

        // Initial progress callback
        onProgress(0, totalChunks)

        val generativeModel = GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = generationConfig {
                this.temperature = temperature
                this.responseMimeType = "application/json"
            },
            requestOptions = RequestOptions(
                timeout = 180000L
            )
        )

        val typesString = targetTypes.joinToString(", ")

        for ((index, chunk) in chunks.withIndex()) {
            var retryCount = 0
            val maxRetries = 3
            var chunkSuccess = false

            val languageInstruction = if (deckLanguage == "Indonesia") {
                """
                IMPORTANT INSTRUCTION FOR INDONESIAN OUTPUT:
                - You must translate the Japanese words and sentences DIRECTLY into natural, colloquial Indonesian.
                - Do NOT translate from Japanese to English and then to Indonesian.
                - The 'meaning' field must contain the direct Indonesian translation.
                - Grammar explanations must be written in clear, native Indonesian.
                """.trimIndent()
            } else {
                """
                IMPORTANT INSTRUCTION FOR ENGLISH OUTPUT:
                - You must translate the Japanese words and sentences into natural English.
                - The 'meaning' field must contain the English translation.
                - Grammar explanations must be written in clear English.
                """.trimIndent()
            }

            while (retryCount < maxRetries && !chunkSuccess) {
                try {
                    val chunkText = chunk.joinToString("\n")
                    val prompt = """
                        $languageInstruction
                        
                        Act as a Japanese linguistic expert specializing in JLPT $targetLevel preparation.
                        Your task is to extract JLPT $targetLevel level $typesString from the following Japanese text:
                        
                        $chunkText
                        
                        Output the result as a JSON array of objects.
                        Each object MUST have these keys: "type", "item", "reading", "meaning", "example_sentence".
                        
                        STRICTLY return ONLY the JSON array.
                    """.trimIndent()

                    val response = generativeModel.generateContent(
                        content {
                            text(prompt)
                        }
                    )
                    
                    val rawText = response.text ?: throw Exception("Empty response body")
                    
                    // 3. Robust JSON Extraction
                    val jsonRegex = Regex("\\[[\\s\\S]*\\]")
                    val matchResult = jsonRegex.find(rawText)
                    val cleanJson = matchResult?.value ?: rawText
                    
                    // 4. Parse the chunk's JSON result
                    val itemType = object : TypeToken<List<FlashcardItem>>() {}.type
                    val parsedList: List<FlashcardItem> = gson.fromJson(cleanJson, itemType)
                    
                    if (parsedList.isNotEmpty()) {
                        masterList.addAll(parsedList)
                        Log.d("GeminiService", "Chunk ${index + 1} added ${parsedList.size} items.")
                    }
                    chunkSuccess = true
                } catch (e: Exception) {
                    retryCount++
                    val errorMessage = when (e) {
                        is PromptBlockedException -> "Blocked by safety settings"
                        is QuotaExceededException -> "Quota exceeded"
                        is ServerException -> "Server error: ${e.message}"
                        is SerializationException -> "Serialization error: ${e.message}"
                        else -> e.localizedMessage ?: "Unknown error"
                    }
                    
                    Log.w("GeminiService", "Chunk $index attempt $retryCount failed: $errorMessage")
                    
                    if (retryCount < maxRetries) {
                        delay(3000L * retryCount) // Exponential-ish backoff
                    } else {
                        errors.add("Chunk $index failed after $maxRetries attempts: $errorMessage")
                    }
                }
            }

            // Update progress after each chunk
            onProgress(index + 1, totalChunks)

            // 5. Rate limit delay (avoid 429)
            if (index < chunks.size - 1) {
                delay(4000L)
            }
        }


        // 6. Return results or detailed error log
        return when {
            masterList.isNotEmpty() -> gson.toJson(masterList)
            errors.isNotEmpty() -> "Chunk failures:\n" + errors.joinToString("\n")
            else -> "[]"
        }
    }
}

