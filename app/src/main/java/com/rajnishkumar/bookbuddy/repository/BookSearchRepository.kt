package com.rajnishkumar.bookbuddy.repository

import android.content.Context
import android.util.Log
import com.rajnishkumar.bookbuddy.ai.AISearchHelper
import com.rajnishkumar.bookbuddy.ai.HuggingFaceClient
import com.rajnishkumar.bookbuddy.database.AppDatabase
import com.rajnishkumar.bookbuddy.models.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local-First Hybrid Search Repository
 */
class BookSearchRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val aiHelper = AISearchHelper()
    private val huggingFace = HuggingFaceClient()
    private val roomDb = AppDatabase.getDatabase(appContext)

    companion object {
        @Volatile
        private var INSTANCE: BookSearchRepository? = null

        fun getInstance(context: Context): BookSearchRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BookSearchRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    suspend fun searchBooks(query: String, limit: Int = 20): List<Book> = withContext(Dispatchers.IO) {
        val allLocalBooks = roomDb.bookDao().getAllBooks()
        
        // --- EMERGENCY FALLBACK ---
        // If local DB is empty, search results will be empty. 
        // Sync must complete first.
        if (allLocalBooks.isEmpty()) {
            Log.w("SearchRepo", "Local database is empty. Sync pending...")
            return@withContext emptyList()
        }

        if (query.isBlank()) return@withContext allLocalBooks.take(limit)

        val q = query.lowercase().trim()

        // 1. Keyword Search (Safe Fallback)
        val keywordResults = allLocalBooks.filter { book ->
            book.searchTitle.contains(q) || 
            book.searchAuthor.contains(q) || 
            book.genre.any { it.lowercase().contains(q) } ||
            book.description.lowercase().contains(q)
        }

        // 2. Local Semantic AI Matching
        val queryEmbedding = try { 
            huggingFace.getEmbedding(query) 
        } catch (e: Exception) { emptyList<Double>() }

        val semanticResults = if (queryEmbedding.isNotEmpty()) {
            allLocalBooks.map { book ->
                val sim = if (book.embedding.isNotEmpty()) {
                    aiHelper.calculateCosineSimilarity(queryEmbedding, book.embedding)
                } else 0.0
                book to sim
            }
            .filter { it.second > 0.05 } // Low threshold for better matching
            .sortedByDescending { it.second }
            .map { it.first }
        } else emptyList()

        // Combine: Semantic AI results first, then keyword fallback
        val combined = (semanticResults + keywordResults).distinctBy { it.id }.take(limit)
        
        Log.d("SearchRepo", "Search returned ${combined.size} results for: $query")
        combined
    }

    suspend fun getSearchMetadata(): List<Book> = withContext(Dispatchers.IO) {
        roomDb.bookDao().getAllBooks()
    }

    suspend fun getFullBookDetails(bookId: String): Book? = withContext(Dispatchers.IO) {
        roomDb.bookDao().getBookById(bookId)
    }
}
