package com.example.bookbuddy.models

data class User(
    var id: String = "",
    var name: String = "",
    var email: String = "",
    var role: String = "member",  // 👈 "member" or "librarian"
    var borrowedBooks: MutableList<String> = mutableListOf(),
    var readingHistory: MutableList<BookInteraction> = mutableListOf(),
    var preferences: List<String> = listOf()
)