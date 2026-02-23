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
    // Empty constructor needed for Firestore
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

    // Constructor for creating placeholder books (used in MyBooksActivity)
    constructor(id: String, title: String) : this(
        id = id,
        title = title,
        addedAt = System.currentTimeMillis()
    )

    // Constructor for creating new books with minimal info
    constructor(
        title: String,
        author: String,
        genre: String,
        description: String
    ) : this(
        title = title,
        author = author,
        genre = genre,
        description = description,
        addedAt = System.currentTimeMillis()
    )

    /**
     * Check if book is available for borrowing
     */
    fun isAvailable(): Boolean = availableCopies > 0

    /**
     * Get availability status text
     */
    fun getAvailabilityText(): String {
        return if (availableCopies > 0) {
            "Available: $availableCopies/$totalCopies"
        } else {
            "Not Available"
        }
    }

    /**
     * Get formatted title with author
     */
    fun getTitleWithAuthor(): String = "$title by $author"

    /**
     * Get short description (first 100 chars)
     */
    fun getShortDescription(): String {
        return if (description.length > 100) {
            description.substring(0, 100) + "..."
        } else {
            description
        }
    }

    /**
     * Check if book matches search query
     */
    fun matchesSearch(query: String): Boolean {
        val lowerQuery = query.lowercase()
        return title.lowercase().contains(lowerQuery) ||
                author.lowercase().contains(lowerQuery) ||
                genre.lowercase().contains(lowerQuery) ||
                keywords.any { it.lowercase().contains(lowerQuery) }
    }
}