package com.rajnishkumar.bookbuddy.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.rajnishkumar.bookbuddy.models.Book
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BookPagingSource(
    private val database: DatabaseReference,
    private val searchQuery: String? = null
) : PagingSource<String, Book>() {

    override fun getRefreshKey(state: PagingState<String, Book>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Book> {
        return try {
            val query = if (searchQuery.isNullOrEmpty()) {
                // Default pagination: Order by key (ID)
                val base = database.child("books").orderByKey()
                if (params.key != null) base.startAfter(params.key).limitToFirst(params.loadSize)
                else base.limitToFirst(params.loadSize)
            } else {
                // Search mode: Order by searchTitle for prefix matching
                val q = searchQuery.lowercase().trim()
                database.child("books")
                    .orderByChild("searchTitle")
                    .startAt(q)
                    .endAt(q + "\uf8ff")
                    .limitToFirst(params.loadSize)
            }

            val snapshot = query.getSnapshot()
            val books = snapshot.children.mapNotNull { parseSafeBook(it) }
            val lastKey = snapshot.children.lastOrNull()?.key

            LoadResult.Page(
                data = books,
                prevKey = null,
                nextKey = if (books.size < params.loadSize) null else lastKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private fun parseSafeBook(snapshot: DataSnapshot): Book? {
        return try {
            val book = snapshot.getValue(Book::class.java) ?: return null
            book.id = snapshot.key ?: ""
            if (book.genre.isEmpty()) {
                val rawGenre = snapshot.child("genre").value
                if (rawGenre is String) book.genre = parseGenreString(rawGenre)
            }
            book
        } catch (e: Exception) {
            val id = snapshot.key ?: ""
            val title = snapshot.child("title").getValue(String::class.java) ?: "Unknown"
            val author = snapshot.child("author").getValue(String::class.java) ?: "Unknown"
            val rawGenre = snapshot.child("genre").value?.toString() ?: ""
            Book(
                id = id,
                title = title,
                author = author,
                genre = parseGenreString(rawGenre),
                coverUrl = snapshot.child("coverUrl").getValue(String::class.java) ?: "",
                bookNumber = snapshot.child("bookNumber").getValue(Int::class.java) ?: 0
            )
        }
    }

    private fun parseGenreString(input: String): List<String> {
        if (input.isBlank()) return emptyList()
        return if (input.contains("[")) {
            input.replace("[", "").replace("]", "").replace("'", "").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            input.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }

    private suspend fun com.google.firebase.database.Query.getSnapshot(): DataSnapshot = suspendCancellableCoroutine { continuation ->
        addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                continuation.resume(snapshot)
            }
            override fun onCancelled(error: DatabaseError) {
                // Fixed: Changed from java.util.Result to kotlin.Result (implicitly)
                continuation.resumeWith(Result.failure(error.toException()))
            }
        })
    }
}
