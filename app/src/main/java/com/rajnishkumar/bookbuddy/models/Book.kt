package com.rajnishkumar.bookbuddy.models

data class Book(
    var id: String = "",
    var title: String = "",
    var author: String = "",
    var genre: String = "",
    var genreList: List<String> = emptyList(),
    var description: String = "",
    var summary: String = "",
    var isbn: String = "",
    var coverUrl: String = "",
    var totalCopies: Int = 1,
    var availableCopies: Int = 1,
    var embedding: List<Double> = emptyList(),
    var addedAt: Long = System.currentTimeMillis(),
    var addedBy: String = "",
    
    // Rating fields
    var averageRating: Float = 0f,
    var totalRatings: Int = 0
)

data class Chunk(val text: String, val embedding: List<Double>)