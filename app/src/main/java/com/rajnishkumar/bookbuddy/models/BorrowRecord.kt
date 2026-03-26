package com.rajnishkumar.bookbuddy.models

data class BorrowRecord(
    var id: String = "",
    var bookId: String = "",
    var userId: String = "",
    var bookTitle: String = "",
    var bookAuthor: String = "",
    var bookCoverUrl: String = "",
    var borrowDate: Long = System.currentTimeMillis(),
    var dueDate: Long = System.currentTimeMillis() + (14 * 24 * 60 * 60 * 1000), // 14 days later
    var status: String = "BORROWED" // BORROWED, RETURNED
)