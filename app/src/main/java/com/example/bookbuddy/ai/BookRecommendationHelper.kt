package com.example.bookbuddy.ai

import com.example.bookbuddy.models.Book
import com.example.bookbuddy.models.BookInteraction

class BookRecommendationHelper {

    fun getPopularBooks(allBooks: List<Book>, limit: Int = 5): List<Book> {
        return allBooks
            .sortedByDescending { it.timesBorrowed }
            .take(limit)
    }

    fun getRecentlyAdded(allBooks: List<Book>, limit: Int = 5): List<Book> {
        return allBooks
            .sortedByDescending { it.addedAt }
            .take(limit)
    }

    fun getRecommendationsForUser(
        userInteractions: List<BookInteraction>,
        allBooks: List<Book>,
        limit: Int = 5
    ): List<Book> {
        if (userInteractions.isEmpty()) {
            return emptyList()
        }

        val genreCount = mutableMapOf<String, Int>()
        val authorCount = mutableMapOf<String, Int>()
        val readBookIds = mutableSetOf<String>()

        userInteractions.forEach { interaction ->
            if (interaction.action == "borrow" || interaction.action == "view") {
                genreCount[interaction.bookGenre] = genreCount.getOrDefault(interaction.bookGenre, 0) + 1
                authorCount[interaction.bookAuthor] = authorCount.getOrDefault(interaction.bookAuthor, 0) + 1
                readBookIds.add(interaction.bookId)
            }
        }

        val topGenres = genreCount.entries.sortedByDescending { it.value }.take(2).map { it.key }
        val topAuthors = authorCount.entries.sortedByDescending { it.value }.take(1).map { it.key }

        val scoredBooks = allBooks
            .filter { it.id !in readBookIds && it.availableCopies > 0 }
            .map { book ->
                var score = 0
                if (book.genre in topGenres) score += 10
                if (book.author in topAuthors) score += 8
                score += book.timesBorrowed
                book to score
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }

        return scoredBooks
    }

    fun getBecauseYouRead(
        book: Book,
        allBooks: List<Book>,
        limit: Int = 3
    ): List<Book> {
        return allBooks
            .filter { it.id != book.id && it.availableCopies > 0 }
            .filter {
                it.genre == book.genre ||
                        it.author == book.author ||
                        it.keywords.any { keyword -> book.keywords.contains(keyword) }
            }
            .take(limit)
    }

    fun getInitialSuggestions(allBooks: List<Book>, limit: Int = 6): List<Book> {
        val popular = getPopularBooks(allBooks, limit / 2)
        val recent = getRecentlyAdded(allBooks, limit / 2)
        return (popular + recent).distinct().take(limit)
    }
}