package com.rajnishkumar.bookbuddy.ai

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.rajnishkumar.bookbuddy.models.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class OpenLibraryClient {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Fetches book data from multiple free sources (OpenLibrary, Google Books)
     */
    suspend fun getBookByISBN(isbn: String): Book? = withContext(Dispatchers.IO) {
        // Try Google Books first as it often has better descriptions
        var book = fetchFromGoogleBooks(isbn)
        
        // If not found or description is poor, try OpenLibrary
        if (book == null || book.description.length < 100) {
            val olBook = fetchFromOpenLibrary(isbn)
            if (olBook != null) {
                if (book == null) {
                    book = olBook
                } else {
                    // Merge: take better cover or description
                    if (book.coverUrl.isEmpty()) book.coverUrl = olBook.coverUrl
                    if (olBook.description.length > book.description.length) {
                        book.description = olBook.description
                    }
                }
            }
        }
        
        return@withContext book
    }

    private fun fetchFromGoogleBooks(isbn: String): Book? {
        val url = "https://www.googleapis.com/books/v1/volumes?q=isbn:$isbn"
        val request = Request.Builder().url(url).build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val root = JsonParser.parseString(body).asJsonObject
                if (!root.has("items")) return null
                
                val item = root.getAsJsonArray("items")[0].asJsonObject.getAsJsonObject("volumeInfo")
                val title = item.get("title")?.asString ?: "Unknown Title"
                val authors = if (item.has("authors")) {
                    item.getAsJsonArray("authors").map { it.asString }.joinToString(", ")
                } else "Unknown Author"
                
                val description = item.get("description")?.asString ?: ""
                val categories = if (item.has("categories")) {
                    item.getAsJsonArray("categories").map { it.asString }
                } else emptyList()
                
                val coverUrl = item.getAsJsonObject("imageLinks")?.get("thumbnail")?.asString ?: ""

                return Book(
                    id = "temp_$isbn",
                    title = title,
                    author = authors,
                    description = description,
                    coverUrl = coverUrl.replace("http:", "https:"),
                    genre = if (categories.isNotEmpty()) categories else listOf("General"),
                    isbn = isbn
                )
            }
        } catch (e: Exception) {
            return null
        }
    }

    private fun fetchFromOpenLibrary(isbn: String): Book? {
        val url = "https://openlibrary.org/api/books?bibkeys=ISBN:$isbn&format=json&jscmd=data"
        val request = Request.Builder().url(url).build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val root = JsonParser.parseString(body).asJsonObject
                val key = "ISBN:$isbn"
                if (!root.has(key)) return null

                val data = root.getAsJsonObject(key)
                val title = data.get("title")?.asString ?: "Unknown Title"
                val author = if (data.has("authors")) {
                    data.getAsJsonArray("authors")[0].asJsonObject.get("name")?.asString ?: "Unknown Author"
                } else "Unknown Author"

                val subjects = if (data.has("subjects")) {
                    data.getAsJsonArray("subjects").mapNotNull { it.asJsonObject.get("name")?.asString }
                } else emptyList()

                val description = data.get("notes")?.asString ?: subjects.joinToString(", ")
                val coverUrl = data.getAsJsonObject("cover")?.get("large")?.asString ?: ""

                return Book(
                    id = "temp_$isbn",
                    title = title,
                    author = author,
                    description = description,
                    coverUrl = coverUrl,
                    genre = if (subjects.isNotEmpty()) subjects else listOf("General"),
                    isbn = isbn
                )
            }
        } catch (e: Exception) {
            return null
        }
    }
}
