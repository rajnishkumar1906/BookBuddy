package com.rajnishkumar.bookbuddy.ai

import android.content.Context
import android.util.Log
import com.rajnishkumar.bookbuddy.database.AppDatabase
import com.rajnishkumar.bookbuddy.models.Book
import com.rajnishkumar.bookbuddy.models.BookChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RagService(context: Context) {

    private val huggingFace = HuggingFaceClient()
    private val gemini = GeminiClient()
    private val searchHelper = AISearchHelper()
    private val roomDb = AppDatabase.getDatabase(context)
    private val tag = "RagService"

    data class RagResponse(
        val role: String, // "assistant" or "choice"
        val message: String
    )

    suspend fun processChat(
        query: String,
        book: Book,
        history: String
    ): RagResponse = withContext(Dispatchers.IO) {
        try {
            val intent = huggingFace.classifyQuery(query)
            Log.d(tag, "Intent: $intent")

            when (intent) {
                "GREETING" -> return@withContext RagResponse("assistant", "Hello! I'm your AI Librarian. How can I help you with \"${book.title}\" today?")
                "APPRECIATION" -> return@withContext RagResponse("assistant", "You're very welcome! I'm happy to help.")
                "REJECTION" -> return@withContext RagResponse("assistant", "No problem! Let me know if you have any other questions.")
                "PRACTICE" -> return@withContext RagResponse("choice", "Would you like a single question or a full quiz about \"${book.title}\"?")

                else -> {
                    val chunks = getOrPrepareChunks(book)
                    val queryEmb = huggingFace.getEmbedding(query)
                    val relevantContext = if (queryEmb.isNotEmpty() && chunks.isNotEmpty()) {
                        searchHelper.findRelevantChunks(queryEmb, chunks, topK = 5)
                    } else ""

                    val ragPrompt = """
                        You are the AI Librarian for: "${book.title}" by ${book.author}.
                        Genres: ${book.genre.joinToString(", ")}
                        Context: ${book.description}
                        Snippets: $relevantContext
                        Question: "$query"
                        
                        Instruction:
                        - Answer ONLY using context.
                        - Max 3 sentences.
                        - NO markdown/symbols.
                    """.trimIndent()

                    // Unifying to use the public generateAnswer method
                    val answer = gemini.generateAnswer(ragPrompt)
                    val finalMessage = if (answer.isNotBlank()) answer else "I'm looking at \"${book.title}\", but I'm not sure. Ask something else?"
                    return@withContext RagResponse("assistant", finalMessage)
                }
            }
        } catch (e: Exception) {
            return@withContext RagResponse("assistant", "I'm having trouble thinking. Try again later!")
        }
    }

    private suspend fun getOrPrepareChunks(book: Book): List<BookChunk> {
        val stored = roomDb.bookChunkDao().getChunksForBook(book.id)
        if (stored.isNotEmpty()) return stored
        val sentences = book.description.split(Regex("(?<=[.!?])\\s+")).filter { it.length > 20 }.take(15)
        val newChunks = sentences.map { sentence ->
            val emb = huggingFace.getEmbedding(sentence)
            BookChunk(bookId = book.id, text = sentence, embedding = emb)
        }.filter { it.embedding.isNotEmpty() }
        if (newChunks.isNotEmpty()) roomDb.bookChunkDao().insertAll(newChunks)
        return newChunks
    }
}
