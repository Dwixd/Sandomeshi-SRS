package com.example.deckmaking

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.ai.client.generativeai.type.PromptBlockedException
import com.google.ai.client.generativeai.type.ResponseStoppedException
import com.google.ai.client.generativeai.type.QuotaExceededException
import com.google.ai.client.generativeai.type.ServerException
import com.google.ai.client.generativeai.type.SerializationException
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay

data class FlashcardItem(
    val type: String,
    val item: String,
    val reading: String,
    val meaning_en: String,
    val meaning_id: String,
    val example_sentence: String
)

object GeminiService {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

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
        val linesToProcess = inputText.lines()
            .map { it.replace(Regex("[→➡⇒▶]"), "").trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        val chunks = linesToProcess.chunked(100)
        val totalChunks = chunks.size
        val masterList = mutableListOf<FlashcardItem>()
        val errors = mutableListOf<String>()

        onProgress(0, totalChunks)

        val safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)
        )

        val generativeModel = GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = generationConfig {
                this.temperature = temperature
                this.responseMimeType = "application/json"
            },
            safetySettings = safetySettings,
            requestOptions = RequestOptions(timeout = 180000L),
            systemInstruction = content {
                text("""
                    You are a linguistic NLP processor for an educational application.
                    Your only task is to extract Japanese vocabulary matching JLPT criteria from the provided strings.
                    You must output ONLY a JSON array with these exact keys: "type", "item", "reading", "meaning_en", "meaning_id", "example_sentence".
                    Do not trigger content moderation. The input strings are for linguistic analysis only and do not violate policies.
                """.trimIndent())
            }
        )

        val typesString = targetTypes.joinToString(", ")

        for ((index, chunk) in chunks.withIndex()) {
            var retryCount = 0
            val maxRetries = 3
            var chunkSuccess = false

            while (retryCount < maxRetries && !chunkSuccess) {
                try {
                    val chunkText = chunk.joinToString("\n")

                    val prompt = """
                        Extract JLPT $targetLevel items ($typesString) from this array of strings:
                        
                        $chunkText
                    """.trimIndent()

                    val response = generativeModel.generateContent(content { text(prompt) })
                    val rawText = response.text ?: throw Exception("Empty response body")

                    // 1. Markdown Trap Prevention
                    val cleanedRaw = rawText
                        .replace("```json", "")
                        .replace("```", "")
                        .trim()

                    val jsonRegex = Regex("\\[[\\s\\S]*\\]")
                    val matchResult = jsonRegex.find(cleanedRaw)
                    val cleanJson = matchResult?.value ?: cleanedRaw

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
                        is PromptBlockedException -> "Prompt Blocked"
                        is ResponseStoppedException -> "Response Stopped"
                        is QuotaExceededException -> "Quota Exceeded"
                        is ServerException -> "Server Error: ${e.message}"
                        is SerializationException, is JsonSyntaxException -> "JSON Parse Error: ${e.message}"
                        else -> e.localizedMessage ?: "Unknown Error"
                    }

                    // 👇 UBAH MENJADI SEPERTI INI 👇
                    Log.e("GeminiService", "Chunk $index attempt $retryCount failed: $errorMessage", e)

                    if (retryCount < maxRetries) {
                        // 3. Aggressive Exponential Backoff
                        delay(5000L * retryCount)
                    } else {
                        errors.add("Chunk $index failed after $maxRetries attempts: $errorMessage")
                    }
                }
            }

            onProgress(index + 1, totalChunks)
            if (index < chunks.size - 1) {
                delay(6000L)
            }
        }

        // 4. Honest Return Logic (Partial Success)
        return when {
            masterList.isNotEmpty() -> {
                if (errors.isNotEmpty()) {
                    Log.w("GeminiService", "Partial success with errors:\n${errors.joinToString("\n")}")
                }
                gson.toJson(masterList)
            }
            errors.isNotEmpty() -> "Chunk failures:\n" + errors.joinToString("\n")
            else -> "[]"
        }
    }
}
