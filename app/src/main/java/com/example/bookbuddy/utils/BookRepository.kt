package com.example.bookbuddy.utils

import com.example.bookbuddy.models.Book
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class BookRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val booksCollection = db.collection("books")

    // Add a new book
    suspend fun addBook(book: Book): Result<String> {
        return try {
            val docRef = booksCollection.add(book).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update existing book
    suspend fun updateBook(bookId: String, book: Book): Result<Unit> {
        return try {
            booksCollection.document(bookId).set(book).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get all books with real-time updates
    fun getAllBooks(onResult: (List<Book>) -> Unit, onError: (Exception) -> Unit) {
        booksCollection.orderBy("title", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val books = snapshot?.toObjects(Book::class.java) ?: emptyList()
                onResult(books)
            }
    }

    // Search books by title/author/genre
    suspend fun searchBooks(query: String): List<Book> {
        val results = mutableListOf<Book>()

        try {
            // Search by title
            val titleSnapshot = booksCollection
                .whereGreaterThanOrEqualTo("title", query)
                .whereLessThanOrEqualTo("title", query + "\uf8ff")
                .get()
                .await()
            results.addAll(titleSnapshot.toObjects(Book::class.java))

            // Search by author
            val authorSnapshot = booksCollection
                .whereGreaterThanOrEqualTo("author", query)
                .whereLessThanOrEqualTo("author", query + "\uf8ff")
                .get()
                .await()
            results.addAll(authorSnapshot.toObjects(Book::class.java))

            // Search by genre
            val genreSnapshot = booksCollection
                .whereGreaterThanOrEqualTo("genre", query)
                .whereLessThanOrEqualTo("genre", query + "\uf8ff")
                .get()
                .await()
            results.addAll(genreSnapshot.toObjects(Book::class.java))

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return results.distinctBy { it.id }
    }

    // Get book by ID
    suspend fun getBookById(bookId: String): Book? {
        return try {
            val doc = booksCollection.document(bookId).get().await()
            doc.toObject(Book::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Borrow a book
    suspend fun borrowBook(bookId: String): Result<Boolean> {
        return try {
            val book = getBookById(bookId) ?: return Result.failure(Exception("Book not found"))

            if (book.availableCopies > 0) {
                // Update available copies
                booksCollection.document(bookId)
                    .update(
                        "availableCopies", book.availableCopies - 1,
                        "timesBorrowed", book.timesBorrowed + 1
                    )
                    .await()
                Result.success(true)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Return a book
    suspend fun returnBook(bookId: String): Result<Unit> {
        return try {
            val book = getBookById(bookId) ?: return Result.failure(Exception("Book not found"))

            booksCollection.document(bookId)
                .update("availableCopies", book.availableCopies + 1)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete a book (librarian only)
    suspend fun deleteBook(bookId: String): Result<Unit> {
        return try {
            booksCollection.document(bookId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}