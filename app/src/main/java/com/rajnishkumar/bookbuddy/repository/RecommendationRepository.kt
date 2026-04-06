package com.rajnishkumar.bookbuddy.repository

import com.google.firebase.database.FirebaseDatabase
import com.rajnishkumar.bookbuddy.models.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RecommendationRepository {

    private val database = FirebaseDatabase.getInstance().reference

    suspend fun getPersonalizedRecommendations(
        userId: String,
        allBooks: List<Book>,
        limit: Int = 10
    ): List<Book> = withContext(Dispatchers.IO) {

        val userSnapshot = database.child("users").child(userId).get().await()

        // Get favorite genres from Profile
        val favoriteGenres = userSnapshot.child("favoriteGenres")
            .getValue(String::class.java)
            ?.split(",")
            ?.map { it.trim().lowercase() }
            ?.filter { it.isNotEmpty() } ?: emptyList()

        // Get visited book IDs
        val visitsSnapshot = database.child("visits").child(userId).get().await()
        val visitedBookIds = visitsSnapshot.children.mapNotNull { it.key }

        val scored = allBooks.map { book ->
            var score = 0.0

            // Genre match bonus - Unified to use the new book.genre List
            val bookGenres = book.genre.map { it.lowercase() }
            val genreMatches = bookGenres.count { genre ->
                favoriteGenres.any { fav -> genre.contains(fav) || fav.contains(genre) }
            }
            score += genreMatches * 30

            // Visit history bonus
            if (visitedBookIds.contains(book.id)) score += 45

            // Rating bonus
            score += book.averageRating * 10

            book to score
        }

        scored.sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    // Fallback: Popular books
    fun getPopularBooks(allBooks: List<Book>, limit: Int = 8): List<Book> {
        return allBooks
            .sortedByDescending { it.averageRating * (it.totalRatings + 1) }
            .take(limit)
    }
}
