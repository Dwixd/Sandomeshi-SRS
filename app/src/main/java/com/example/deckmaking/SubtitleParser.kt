package com.example.deckmaking

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

object SubtitleParser {

    /**
     * Reads raw text from a URI using ContentResolver.
     */
    fun readTextFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Parses ASS subtitle format.
     * Extracts text from "Dialogue:" lines, removes tags like { \pos(x,y) },
     * and replaces \N or \n with spaces.
     */
    fun parseAss(rawText: String): String {
        val lines = rawText.lines()
        val result = StringBuilder()

        val tagRegex = Regex("\\{[^}]*\\}")
        val lineBreakRegex = Regex("\\\\[nN]")

        for (line in lines) {
            if (line.startsWith("Dialogue:", ignoreCase = true)) {
                // ASS Dialogue format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
                // Text is the 10th element (index 9) after splitting by comma 9 times
                val parts = line.split(",", limit = 10)
                if (parts.size >= 10) {
                    var text = parts[9]
                    // Remove styling tags {...}
                    text = text.replace(tagRegex, "")
                    // Replace \N or \n with space
                    text = text.replace(lineBreakRegex, " ")
                    result.append(text.trim()).append(" ")
                }
            }
        }
        return result.toString().trim()
    }

    /**
     * Parses SRT subtitle format.
     * Removes timestamps, index numbers, and HTML tags.
     */
    fun parseSrt(rawText: String): String {
        val timestampRegex = Regex("""\d{2}:\d{2}:\d{2},\d{3} --> \d{2}:\d{2}:\d{2},\d{3}""")
        val indexRegex = Regex("""^\d+$""")
        val htmlTagRegex = Regex("<[^>]*>")

        return rawText.lines()
            .map { it.trim() }
            .filter { line ->
                line.isNotEmpty() &&
                !line.matches(timestampRegex) &&
                !line.matches(indexRegex)
            }
            .joinToString(" ") { it.replace(htmlTagRegex, "") }
            .trim()
    }
}
