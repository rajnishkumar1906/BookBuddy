package com.example.bookbuddy.utils

import android.net.Uri
import com.example.bookbuddy.models.Book
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class CSVImporter(private val db: FirebaseFirestore) {

    /**
     * Expected CSV format:
     * title,author,genre,description,isbn,language,pages,copies,rating
     */
    suspend fun importFromCsv(inputStream: InputStream): ImportResult {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val lines = reader.readLines()

        if (lines.isEmpty()) {
            return ImportResult(0, 0, "Empty file")
        }

        val header = lines[0].split(",").map { it.trim().lowercase() }
        val requiredColumns = listOf("title", "author", "genre")

        // Validate required columns
        val missingColumns = requiredColumns.filterNot { col ->
            header.any { it == col }
        }

        if (missingColumns.isNotEmpty()) {
            return ImportResult(0, 0, "Missing columns: ${missingColumns.joinToString()}")
        }

        var successCount = 0
        var errorCount = 0
        val books = mutableListOf<Book>()

        for (i in 1 until lines.size) {
            try {
                val line = lines[i].trim()
                if (line.isEmpty()) continue

                val values = parseCsvLine(line)
                if (values.size >= header.size) {
                    val book = createBookFromRow(header, values)
                    books.add(book)
                    successCount++
                } else {
                    errorCount++
                }
            } catch (e: Exception) {
                errorCount++
            }
        }

        // Batch insert to Firestore
        if (books.isNotEmpty()) {
            val batch = db.batch()
            books.forEach { book ->
                val docRef = db.collection("books").document()
                batch.set(docRef, book)
            }
            batch.commit().await()
        }

        return ImportResult(successCount, errorCount, "Import completed")
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString().trim())
        return result
    }

    private fun createBookFromRow(header: List<String>, values: List<String>): Book {
        val book = Book()

        header.forEachIndexed { index, column ->
            when (column) {
                "title" -> book.title = values.getOrElse(index) { "" }
                "author" -> book.author = values.getOrElse(index) { "" }
                "genre" -> book.genre = values.getOrElse(index) { "" }
                "description" -> book.description = values.getOrElse(index) { "" }
                "isbn" -> book.isbn = values.getOrElse(index) { "" }
                "language" -> book.language = values.getOrElse(index) { "English" }
                "pages", "pagecount" -> {
                    book.pageCount = values.getOrElse(index) { "0" }.toIntOrNull() ?: 0
                }
                "copies", "availablecopies", "totalcopies" -> {
                    val copies = values.getOrElse(index) { "1" }.toIntOrNull() ?: 1
                    book.availableCopies = copies
                    book.totalCopies = copies
                }
                "rating" -> book.rating = values.getOrElse(index) { "0" }.toFloatOrNull() ?: 0f
                "publisher" -> book.publisher = values.getOrElse(index) { "" }
            }
        }

        // Set defaults
        if (book.description.isEmpty()) {
            book.description = "A ${book.genre} book by ${book.author}."
        }
        if (book.availableCopies == 0) {
            book.availableCopies = 1
            book.totalCopies = 1
        }

        return book
    }

    data class ImportResult(
        val successCount: Int,
        val errorCount: Int,
        val message: String
    )
}