package com.rajnishkumar.bookbuddy.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.rajnishkumar.bookbuddy.database.Converters

@Entity(tableName = "chat_history")
@TypeConverters(Converters::class)
data class ChatMessage(
    val bookId: String = "",
    val role: String = "", // "user", "assistant", or "quiz"
    var message: String = "", // Changed to var to support word-by-word streaming
    var quizJson: String? = null, 
    val timestamp: Long = System.currentTimeMillis(),
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)
