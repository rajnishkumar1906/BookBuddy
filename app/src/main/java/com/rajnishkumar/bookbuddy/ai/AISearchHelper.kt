package com.rajnishkumar.bookbuddy.ai

import com.rajnishkumar.bookbuddy.models.Book
import com.rajnishkumar.bookbuddy.models.BookChunk
import kotlin.math.sqrt

class AISearchHelper {


    fun calculateCosineSimilarity(vectorA: List<Double>, vectorB: List<Double>): Double {
        if (vectorA.size != vectorB.size || vectorA.isEmpty()) {
            return 0.0
        }

        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in vectorA.indices) {
            val a = vectorA[i]
            val b = vectorB[i]
            dotProduct += a * b
            normA += a * a
            normB += b * b
        }

        return if (normA > 0.0 && normB > 0.0) {
            dotProduct / (sqrt(normA) * sqrt(normB))
        } else {
            0.0
        }
    }


    fun findTopMatches(
        queryEmbedding: List<Double>,
        allBooks: List<Book>,
        topN: Int = 10
    ): List<Book> {
        if (queryEmbedding.isEmpty() || allBooks.isEmpty()) {
            return emptyList()
        }

        return allBooks
            .mapNotNull { book ->
                if (book.embedding.isEmpty()) return@mapNotNull null
                val similarity = calculateCosineSimilarity(queryEmbedding, book.embedding)
                if (similarity > 0.0) book to similarity else null
            }
            .sortedByDescending { it.second }   // Sort by highest similarity first
            .take(topN)
            .map { it.first }
    }


    fun findRelevantChunks(
        queryEmbedding: List<Double>,
        chunks: List<BookChunk>,
        topK: Int = 3
    ): String {
        if (queryEmbedding.isEmpty() || chunks.isEmpty()) {
            return ""
        }

        return chunks
            .mapNotNull { chunk ->
                if (chunk.embedding.isEmpty()) return@mapNotNull null
                val similarity = calculateCosineSimilarity(queryEmbedding, chunk.embedding)
                if (similarity > 0.1) chunk to similarity else null  // Minimum threshold
            }
            .sortedByDescending { it.second }
            .take(topK)
            .joinToString(" ") { it.first.text }
    }

    /**
     * Optional: Normalize similarity score to 0-1 range for easier comparison
     */
    fun normalizeSimilarity(similarity: Double): Double {
        return (similarity + 1.0) / 2.0  // Converts from [-1,1] to [0,1]
    }
}