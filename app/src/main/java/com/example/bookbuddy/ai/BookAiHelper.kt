package com.example.bookbuddy.ai

import com.example.bookbuddy.models.Book
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookAIHelper {

    private val huggingFace = HuggingFaceClient()  // 👈 Uses the model
    private val gemini = GeminiClient()
    private val vectorSearch = VectorSearch()      // 👈 Uses the dimensions

    fun processNewBook(book: Book, onComplete: (Book) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val textForEmbedding = buildString {
                    append(book.title)
                    append(" ")
                    append(book.genre)
                    append(" ")
                    append(book.description.take(500))
                }

                // 👈 Get 384-dim embedding from Hugging Face
                val embedding = huggingFace.getEmbedding(textForEmbedding)

                val summary = if (book.description.length > 20) {
                    gemini.generateSummary(book.description)
                } else {
                    "A ${book.genre.lowercase()} book by ${book.author}."
                }

                withContext(Dispatchers.Main) {
                    book.embedding = embedding      // 👈 Store the embedding
                    book.aiSummary = summary
                    onComplete(book)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete(book)
                }
            }
        }
    }

    suspend fun aiSearch(query: String, books: List<Book>): List<Book> = withContext(Dispatchers.IO) {
        if (books.isEmpty()) return@withContext emptyList()

        try {
            // 👈 Get query embedding (also 384-dim)
            val queryEmbedding = huggingFace.getEmbedding(query)
            if (queryEmbedding.isEmpty()) return@withContext emptyList()

            // 👈 Find similar books using cosine similarity
            val results = vectorSearch.findSimilarBooks(queryEmbedding, books)
            results.map { it.first }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getRecommendations(
        userBooks: List<Book>,
        allBooks: List<Book>,
        limit: Int = 5
    ): List<Book> = withContext(Dispatchers.IO) {
        if (userBooks.isEmpty() || allBooks.isEmpty()) return@withContext emptyList()

        try {
            // 👈 Create user profile by averaging embeddings (all 384-dim)
            val userProfile = vectorSearch.createUserProfile(userBooks)
            if (userProfile.isEmpty()) return@withContext emptyList()

            val excludeIds = userBooks.map { it.id }
            val candidateBooks = allBooks.filter { it.id !in excludeIds }

            val results = vectorSearch.findSimilarBooks(userProfile, candidateBooks, limit = limit)
            results.map { it.first }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getBecauseYouRead(
        book: Book,
        allBooks: List<Book>,
        limit: Int = 3
    ): List<Book> = withContext(Dispatchers.IO) {
        try {
            vectorSearch.findSimilarToBook(book, allBooks, limit = limit)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}