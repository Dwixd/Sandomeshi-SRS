package com.example.deckmaking

import org.json.JSONArray

object ExportHelper {

    /**
     * Converts the Gemini-generated JSON string into a TSV (Tab-Separated Values) format
     * suitable for Anki import.
     */
    fun parseJsonToTsv(jsonString: String): String {
        val tsvBuilder = StringBuilder()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                
                // Extract fields
                val item = jsonObject.optString("item", "").clean()
                val reading = jsonObject.optString("reading", "").clean()
                val meaningEn = jsonObject.optString("meaning_en", "").clean()
                val meaningId = jsonObject.optString("meaning_id", "").clean()
                val exampleSentence = jsonObject.optString("example_sentence", "").clean()
                val type = jsonObject.optString("type", "").clean()

                // Append as a TSV row: item [TAB] reading [TAB] meaning_en [TAB] meaning_id [TAB] example [TAB] type [NEWLINE]
                tsvBuilder.append(item).append("\t")
                    .append(reading).append("\t")
                    .append(meaningEn).append("\t")
                    .append(meaningId).append("\t")
                    .append(exampleSentence).append("\t")
                    .append(type).append("\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return tsvBuilder.toString()
    }

    /**
     * Replaces tabs and newlines with spaces to avoid breaking TSV structure.
     */
    private fun String.clean(): String {
        return this.replace("\t", " ")
            .replace("\n", " ")
            .replace("\r", " ")
            .trim()
    }
}
