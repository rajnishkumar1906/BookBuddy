package com.rajnishkumar.bookbuddy.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.rajnishkumar.bookbuddy.database.Converters

/**
 * Enhanced Book Model: Supports Local Vector Sync
 */
@Entity(tableName = "books")
@TypeConverters(Converters::class)
data class Book(
    @PrimaryKey
    var id: String = "",

    var bookNumber: Int = 0,

    var title: String = "",
    var author: String = "",
    
    var genre: List<String> = emptyList(),

    var description: String = "",
    var summary: String = "",
    var isbn: String = "",
    var coverUrl: String = "",

    var totalCopies: Int = 1,
    var availableCopies: Int = 1,

    // Embedding is now stored LOCALLY in user's phone, not on Firebase
    var embedding: List<Double> = emptyList(),

    var searchTitle: String = "",
    var searchAuthor: String = "",
    var searchKeywords: String = "",

    var addedAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(), // Track updates for sync
    var addedBy: String = "",

    var averageRating: Float = 0f,
    var totalRatings: Int = 0
)
