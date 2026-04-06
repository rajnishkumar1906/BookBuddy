package com.rajnishkumar.bookbuddy.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.rajnishkumar.bookbuddy.database.Converters

@Entity(tableName = "book_chunks")
@TypeConverters(Converters::class)
data class BookChunk(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val bookId: String = "",           // Which book this chunk belongs to
    val text: String = "",             // Chunk of text (sentence/paragraph)
    val embedding: List<Double> = emptyList()
)
