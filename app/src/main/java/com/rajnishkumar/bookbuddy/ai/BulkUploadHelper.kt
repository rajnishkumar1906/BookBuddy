package com.rajnishkumar.bookbuddy.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.opencsv.CSVReader
import com.rajnishkumar.bookbuddy.models.Book
import com.rajnishkumar.bookbuddy.repository.BookSearchRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.tasks.await
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicInteger

data class UploadProgress(
    val total: Int,
    val completed: Int,
    val currentBook: String,
    val status: String,
    val percentage: Int
)

data class UploadResult(
    val success: Int,
    val failed: Int,
    val skipped: Int = 0
)

/**
 * Librarian Upload Helper:
 * 1. Uploads book text data to Firebase.
 * 2. NO EMBEDDINGS stored on Firebase (efficient).
 * 3. Updates 'sync_tracker' so user devices know new data is available.
 */
class BulkUploadHelper(private val context: Context) {

    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private val tag = "BulkUploadHelper"

    suspend fun saveBook(originalBook: Book): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val bookNumber = getNextAvailableBookNumber()
            saveBookToFirebase(originalBook, bookNumber)
            Result.success(bookNumber)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveBookToFirebase(originalBook: Book, bookNumber: Int) {
        val bookId = "book_$bookNumber"
        val timestamp = System.currentTimeMillis()

        val preparedBook = originalBook.copy(
            id = bookId,
            bookNumber = bookNumber,
            addedAt = timestamp,
            updatedAt = timestamp,
            addedBy = auth.currentUser?.uid ?: "admin",
            searchTitle = originalBook.title.lowercase().trim(),
            searchAuthor = originalBook.author.lowercase().trim(),
            searchKeywords = "${originalBook.title} ${originalBook.author} ${originalBook.genre.joinToString(" ")}".lowercase(),
            embedding = emptyList() // We do NOT store vectors on Firebase anymore
        )

        // 1. Save Book Data
        database.child("books").child(bookId).setValue(preparedBook).await()

        // 2. Update Sync Tracker (Global)
        // This tells every user's phone: "Something changed at this time"
        database.child("sync_tracker").child("last_update").setValue(ServerValue.TIMESTAMP).await()
    }

    suspend fun uploadBooksFromCSV(
        uri: Uri,
        onProgress: (UploadProgress) -> Unit
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("File error")
            val allParsedBooks = parseCSV(inputStream)
            inputStream.close()

            if (allParsedBooks.isEmpty()) return@withContext UploadResult(0, 0, 0)

            val existingSnap = database.child("books").get().await()
            val existingIsbns = existingSnap.children.mapNotNull { 
                it.child("isbn").getValue(String::class.java)?.trim() 
            }.filter { it.isNotEmpty() }.toSet()

            val booksToUpload = allParsedBooks.filter { 
                it.isbn.isBlank() || !existingIsbns.contains(it.isbn.trim()) 
            }
            val skippedCount = allParsedBooks.size - booksToUpload.size

            if (booksToUpload.isEmpty()) return@withContext UploadResult(0, 0, skippedCount)

            val total = booksToUpload.size
            val completed = AtomicInteger(0)
            val failed = AtomicInteger(0)
            
            var currentNum = getNextAvailableBookNumber()
            val semaphore = Semaphore(10) // Fast upload since no AI calls

            coroutineScope {
                booksToUpload.forEachIndexed { index, book ->
                    val myNumber = currentNum++ 
                    launch {
                        semaphore.withPermit {
                            try {
                                if (index % 10 == 0) {
                                    updateProgress(onProgress, total, completed.get(), book.title, "Uploading...", (index * 100) / total)
                                }
                                saveBookToFirebase(book, myNumber)
                                completed.incrementAndGet()
                            } catch (e: Exception) {
                                failed.incrementAndGet()
                            }
                        }
                    }
                }
            }

            UploadResult(completed.get(), failed.get(), skippedCount)
        } catch (e: Exception) {
            Log.e(tag, "Bulk upload failed", e)
            UploadResult(0, 0, 0)
        }
    }

    private suspend fun updateProgress(onProgress: (UploadProgress) -> Unit, total: Int, completed: Int, currentBook: String, status: String, percentage: Int) {
        withContext(Dispatchers.Main) { onProgress(UploadProgress(total, completed, currentBook, status, percentage)) }
    }

    private suspend fun getNextAvailableBookNumber(): Int {
        val maxSnapshot = database.child("books").orderByChild("bookNumber").limitToLast(1).get().await()
        return if (maxSnapshot.exists()) {
            val last = maxSnapshot.children.first().getValue(Book::class.java)
            (last?.bookNumber ?: 1000) + 1
        } else 1001
    }

    private fun parseCSV(inputStream: InputStream): List<Book> {
        val books = mutableListOf<Book>()
        val reader = CSVReader(InputStreamReader(inputStream))
        reader.readNext() 
        var line: Array<String>?
        while (reader.readNext().also { line = it } != null) {
            line?.let {
                val title = it.getOrNull(1)?.trim() ?: ""
                val author = it.getOrNull(2)?.trim() ?: ""
                if (title.isNotEmpty() && author.isNotEmpty()) {
                    books.add(Book(
                        title = title,
                        author = author,
                        genre = parseGenreList(it.getOrNull(3) ?: ""),
                        description = it.getOrNull(4) ?: "",
                        isbn = it.getOrNull(0) ?: "",
                        coverUrl = it.getOrNull(6) ?: ""
                    ))
                }
            }
        }
        return books
    }

    private fun parseGenreList(genreString: String): List<String> {
        return genreString.replace("[", "").replace("]", "").replace("'", "").split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun getSampleCSV(): String = "ISBN,Title,Author,Genre,Description,Rating,CoverUrl\n97801,Sample,Author,\"['Fiction']\",Desc,4.5,url"
}
