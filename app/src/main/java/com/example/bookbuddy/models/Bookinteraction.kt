package com.example.bookbuddy.models

data class BookInteraction(
    var bookId: String = "",
    var bookTitle: String = "",
    var bookAuthor: String = "",
    var bookGenre: String = "",
    var action: String = "", // "view", "borrow", "return", "rate"
    var rating: Float = 0f,
    var timestamp: Long = System.currentTimeMillis()
)