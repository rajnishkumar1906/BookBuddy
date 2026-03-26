package com.rajnishkumar.bookbuddy.ai

import com.rajnishkumar.bookbuddy.models.Book
import kotlin.math.sqrt

class AISearchHelper {

    /**
     * Calculates cosine similarity between two vectors.
     * Higher value means more similar (max 1.0).
     */
    fun calculateCosineSimilarity(vectorA: List<Double>, vectorB: List<Double>): Double {
        if (vectorA.size != vectorB.size || vectorA.isEmpty()) return 0.0

        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
            normA += vectorA[i] * vectorA[i]
            normB += vectorB[i] * vectorB[i]
        }

        return if (normA > 0 && normB > 0) {
            dotProduct / (sqrt(normA) * sqrt(normB))
        } else {
            0.0
        }
    }

    /**
     * Finds top matching books based on semantic similarity to the query embedding.
     */
    fun findTopMatches(queryEmbedding: List<Double>, allBooks: List<Book>, topN: Int = 10): List<Book> {
        return allBooks
            .filter { it.embedding.isNotEmpty() }
            .map { book ->
                val similarity = calculateCosineSimilarity(queryEmbedding, book.embedding)
                book to similarity
            }
            .sortedByDescending { it.second }
            .take(topN)
            .map { it.first }
    }
}