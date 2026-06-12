package com.example.deckmaking.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    indices = [Index(value = ["item"], unique = true)]
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,
    val item: String,
    val reading: String,
    val meaning_en: String,
    val meaning_id: String,
    val example_sentence: String
)
