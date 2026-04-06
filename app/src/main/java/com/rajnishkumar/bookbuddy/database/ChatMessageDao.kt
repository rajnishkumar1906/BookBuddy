package com.rajnishkumar.bookbuddy.database

import androidx.room.*
import com.rajnishkumar.bookbuddy.models.ChatMessage

@Dao
interface ChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessage)

    @Query("SELECT * FROM chat_history WHERE bookId = :bookId ORDER BY timestamp ASC")
    suspend fun getChatHistory(bookId: String): List<ChatMessage>

    @Query("SELECT * FROM chat_history WHERE role = 'quiz' ORDER BY timestamp DESC")
    suspend fun getAllQuizMessages(): List<ChatMessage>

    @Query("DELETE FROM chat_history WHERE bookId = :bookId")
    suspend fun clearHistoryForBook(bookId: String)

    @Query("DELETE FROM chat_history")
    suspend fun deleteAll()

    @Query("DELETE FROM chat_history WHERE bookId NOT IN (SELECT DISTINCT bookId FROM chat_history ORDER BY timestamp DESC LIMIT 10)")
    suspend fun cleanupOldHistory()
}
