package com.example.bookbuddy.models

data class Book(
    var id: String = "",
    var title: String = "",
    var author: String = "",
    var genre: String = "",
    var description: String = "",
    var summary: String = "",
    var isbn: String = "",
    var coverUrl: String = "",
    var availableCopies: Int = 1,
    var totalCopies: Int = 1,
    var timesBorrowed: Int = 0,
    var rating: Float = 0f,
    var keywords: List<String> = listOf(),
    var addedBy: String = "",
    var addedAt: Long = System.currentTimeMillis(),
    var language: String = "English",
    var pageCount: Int = 0,
    var publisher: String = ""
) {
    constructor() : this(
        id = "",
        title = "",
        author = "",
        genre = "",
        description = "",
        summary = "",
        isbn = "",
        coverUrl = "",
        availableCopies = 1,
        totalCopies = 1,
        timesBorrowed = 0,
        rating = 0f,
        keywords = emptyList(),
        addedBy = "",
        addedAt = System.currentTimeMillis(),
        language = "English",
        pageCount = 0,
        publisher = ""
    )

    constructor(id: String, title: String) : this(
        id = id,
        title = title,
        addedAt = System.currentTimeMillis()
    )

    fun isAvailable(): Boolean = availableCopies > 0

    fun getAvailabilityText(): String {
        return if (availableCopies > 0) {
            "Available: $availableCopies/$totalCopies"
        } else {
            "Not Available"
        }
    }

    fun getTitleWithAuthor(): String = "$title by $author"

    fun getShortDescription(): String {
        return if (description.length > 100) {
            description.substring(0, 100) + "..."
        } else {
            description
        }
    }

    fun matchesSearch(query: String): Boolean {
        val lowerQuery = query.lowercase()
        return title.lowercase().contains(lowerQuery) ||
                author.lowercase().contains(lowerQuery) ||
                genre.lowercase().contains(lowerQuery) ||
                keywords.any { it.lowercase().contains(lowerQuery) }
    }
}