package com.example.bookbuddy.ai

import android.util.Log
import com.example.bookbuddy.models.Book
import kotlin.math.sqrt

class VectorSearch {

    private val TAG = "VectorSearch"

    fun findSimilarBooks(
        queryEmbedding: List<Double>,
        books: List<Book>,
        limit: Int = Constants.MAX_SEARCH_RESULTS,
        minSimilarity: Double = Constants.MIN_SIMILARITY_THRESHOLD
    ): List<Pair<Book, Double>> {

        // Validate query embedding dimensions
        if (queryEmbedding.size != Constants.EMBEDDING_DIMENSIONS) {
            Log.e(TAG, "❌ Query embedding has wrong dimensions: ${queryEmbedding.size}, expected: ${Constants.EMBEDDING_DIMENSIONS}")
            return emptyList()
        }

        val scoredBooks = mutableListOf<Pair<Book, Double>>()
        var validBooksCount = 0

        for (book in books) {
            // Check if book has embedding with correct dimensions
            if (book.embedding.isNotEmpty()) {
                if (book.embedding.size != Constants.EMBEDDING_DIMENSIONS) {
                    Log.w(TAG, "⚠️ Book '${book.title}' has wrong embedding dimensions: ${book.embedding.size}, expected: ${Constants.EMBEDDING_DIMENSIONS}")
                    continue
                }

                validBooksCount++
                val similarity = cosineSimilarity(queryEmbedding, book.embedding)
                if (similarity > minSimilarity) {
                    scoredBooks.add(book to similarity)
                }
            }
        }

        Log.d(TAG, "Found ${scoredBooks.size} similar books out of $validBooksCount valid books")
        return scoredBooks
            .sortedByDescending { it.second }
            .take(limit)
    }

    fun findSimilarToBook(
        targetBook: Book,
        allBooks: List<Book>,
        excludeIds: List<String> = emptyList(),
        limit: Int = 5
    ): List<Book> {
        if (targetBook.embedding.isEmpty()) {
            Log.w(TAG, "Target book has no embedding")
            return emptyList()
        }

        if (targetBook.embedding.size != Constants.EMBEDDING_DIMENSIONS) {
            Log.e(TAG, "❌ Target book embedding has wrong dimensions: ${targetBook.embedding.size}")
            return emptyList()
        }

        val scoredBooks = mutableListOf<Pair<Book, Double>>()

        for (book in allBooks) {
            if (book.id != targetBook.id &&
                book.id !in excludeIds &&
                book.embedding.isNotEmpty() &&
                book.embedding.size == Constants.EMBEDDING_DIMENSIONS) {

                val similarity = cosineSimilarity(targetBook.embedding, book.embedding)
                scoredBooks.add(book to similarity)
            }
        }

        return scoredBooks
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    private fun cosineSimilarity(vec1: List<Double>, vec2: List<Double>): Double {
        if (vec1.isEmpty() || vec2.isEmpty() || vec1.size != vec2.size) {
            return 0.0
        }

        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0

        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }

        return if (norm1 > 0 && norm2 > 0) {
            dotProduct / (sqrt(norm1) * sqrt(norm2))
        } else 0.0
    }

    fun createUserProfile(books: List<Book>): List<Double> {
        if (books.isEmpty()) {
            Log.d(TAG, "No books provided for user profile")
            return emptyList()
        }

        // Filter books with valid embeddings
        val validBooks = books.filter {
            it.embedding.isNotEmpty() && it.embedding.size == Constants.EMBEDDING_DIMENSIONS
        }

        if (validBooks.isEmpty()) {
            Log.w(TAG, "No valid books with embeddings found for user profile")
            return emptyList()
        }

        Log.d(TAG, "Creating user profile from ${validBooks.size} books")

        val size = Constants.EMBEDDING_DIMENSIONS
        val sum = MutableList(size) { 0.0 }

        for (book in validBooks) {
            for (i in 0 until size) {
                sum[i] += book.embedding[i]
            }
        }

        val profile = sum.map { it / validBooks.size }
        Log.d(TAG, "✅ User profile created successfully")
        return profile
    }
}