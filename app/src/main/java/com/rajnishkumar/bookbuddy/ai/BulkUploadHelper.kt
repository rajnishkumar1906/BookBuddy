package com.rajnishkumar.bookbuddy.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import com.opencsv.CSVReader
import com.rajnishkumar.bookbuddy.models.Book
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import kotlin.coroutines.resume

class BulkUploadHelper(private val context: Context) {

    private val database = FirebaseDatabase.getInstance().reference
    private val huggingFace = HuggingFaceClient()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "BulkUploadHelper"

    data class UploadProgress(
        val total: Int,
        val completed: Int,
        val currentBook: String,
        val status: String,
        val percentage: Int
    )

    data class UploadResult(
        val success: Int,
        val failed: Int
    )

    suspend fun uploadBooksFromCSV(
        uri: Uri,
        onProgress: (UploadProgress) -> Unit
    ): UploadResult = withContext(Dispatchers.IO) {

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw Exception("Cannot open file")

            val books = parseCSV(inputStream)
            inputStream.close()

            val total = books.size
            var completed = 0
            var failed = 0
            val userId = auth.currentUser?.uid ?: "unknown"

            for ((index, book) in books.withIndex()) {
                val percentage = ((index + 1) * 100 / total)

                onProgress(UploadProgress(total, completed, book.title, "📖 Processing: ${book.title}", percentage))

                try {
                    // --- AI: Embedding ---
                    val textForEmbedding = "${book.title} ${book.author} ${book.genre} ${book.description.take(500)}"
                    onProgress(UploadProgress(total, completed, book.title, "🧠 AI embedding...", percentage))
                    val embedding = huggingFace.getEmbedding(textForEmbedding)
                    if (embedding.isNotEmpty()) book.embedding = embedding

                    book.addedAt = System.currentTimeMillis()
                    book.addedBy = userId

                    // --- Save ---
                    val bookRef = database.child("books").push()
                    book.id = bookRef.key ?: ""

                    val saveResult = suspendCancellableCoroutine<Boolean> { cont ->
                        bookRef.setValue(book).addOnCompleteListener { task ->
                            if (cont.isActive) cont.resume(task.isSuccessful)
                        }
                    }

                    if (saveResult) completed++ else failed++

                } catch (e: Exception) {
                    failed++
                    Log.e(TAG, "Error: ${book.title}", e)
                }
            }
            UploadResult(completed, failed)
        } catch (e: Exception) {
            UploadResult(0, 0)
        }
    }

    private fun parseCSV(inputStream: java.io.InputStream): List<Book> {
        val books = mutableListOf<Book>()
        try {
            val reader = CSVReader(InputStreamReader(inputStream))
            reader.readNext() // header
            var line: Array<String>?
            while (reader.readNext().also { line = it } != null) {
                line?.let {
                    val title = it.getOrNull(1)?.trim() ?: ""
                    val author = it.getOrNull(2)?.trim() ?: ""
                    val genreString = it.getOrNull(3)?.trim() ?: ""
                    val description = it.getOrNull(4)?.trim() ?: ""
                    val coverUrl = it.getOrNull(6)?.trim() ?: ""
                    val isbn = it.getOrNull(0)?.trim() ?: ""

                    // Genre Parsing: "Bio, History" -> ["Bio", "History"]
                    val genres = genreString.split(",")
                        .map { g -> g.trim() }
                        .filter { g -> g.isNotEmpty() }
                        .distinct()

                    if (title.isNotEmpty() && author.isNotEmpty()) {
                        books.add(Book(
                            title = title,
                            author = author,
                            genre = genres.take(2).joinToString(", "), // Primary genres for display
                            genreList = genres, // All genres for filtering
                            description = description,
                            isbn = isbn,
                            coverUrl = coverUrl
                        ))
                    }
                }
            }
            reader.close()
        } catch (e: Exception) {}
        return books
    }
}