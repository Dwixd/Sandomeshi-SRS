package com.example.deckmaking

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.RequestOptions

object GeminiService {

    /**
     * Generates flashcards using Gemini AI with dynamic level and types.
     */
    suspend fun generateFlashcards(
        apiKey: String,
        inputText: String,
        targetLevel: String,
        targetTypes: List<String>
    ): String? {
        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey,
            requestOptions = RequestOptions(
                timeout = 180000L
            )
        )

        val typesString = targetTypes.joinToString(", ")
        val prompt = """
            Act as a Japanese linguistic expert specializing in JLPT $targetLevel preparation.
            Your task is to extract JLPT $targetLevel level $typesString from the following Japanese text:
            
            $inputText
            
            Return the output STRICTLY as a JSON array. Do NOT include any markdown formatting like ```json or any other text.
            The JSON schema must be exactly like this:
            [
              {
                "type": "one of the requested types: $typesString",
                "item": "The word or grammar point",
                "reading": "The hiragana/reading",
                "meaning": "The meaning in Indonesian",
                "example_sentence": "A sentence using the item, taken or adapted from the input text"
              }
            ]
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(
                content {
                    text(prompt)
                }
            )
            // Filter out potential markdown blocks if AI ignores instructions
            response.text?.replace("```json", "")?.replace("```", "")?.trim()
        } catch (e: Exception) {
            // Mencatat error di Logcat dan mengembalikannya ke layar UI
            Log.e("GeminiError", "API Call Failed", e)
            "Error API: ${e.localizedMessage}"
        }
    }
}