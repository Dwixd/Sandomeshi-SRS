package com.example.deckmaking.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)

    @Query("SELECT * FROM words WHERE item LIKE '%' || :searchQuery || '%' OR reading LIKE '%' || :searchQuery || '%' OR meaning_en LIKE '%' || :searchQuery || '%' OR meaning_id LIKE '%' || :searchQuery || '%'")
    fun searchWords(searchQuery: String): Flow<List<WordEntity>>

    @Query("SELECT * FROM words ORDER BY id DESC")
    fun getAllWords(): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE type = :type ORDER BY id DESC")
    fun getWordsByType(type: String): Flow<List<WordEntity>>
}
