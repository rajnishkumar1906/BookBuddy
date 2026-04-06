package com.rajnishkumar.bookbuddy.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.rajnishkumar.bookbuddy.ai.AISearchHelper
import com.rajnishkumar.bookbuddy.ai.HuggingFaceClient
import com.rajnishkumar.bookbuddy.models.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SearchViewModel : ViewModel() {

    private val huggingFaceClient = HuggingFaceClient()
    private val searchHelper = AISearchHelper()
    private val database = FirebaseDatabase.getInstance().reference

    private val _allBooks = MutableLiveData<List<Book>>()
    private val _searchResults = MutableLiveData<List<Book>>()
    val searchResults: LiveData<List<Book>> = _searchResults

    private val _isSearching = MutableLiveData<Boolean>(false)
    val isSearching: LiveData<Boolean> = _isSearching

    private val _isLibraryLoaded = MutableLiveData<Boolean>(false)
    val isLibraryLoaded: LiveData<Boolean> = _isLibraryLoaded

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private var hasFetchedLibrary = false

    fun fetchLibraryIfNeeded() {
        if (hasFetchedLibrary) return

        _isSearching.value = true

        viewModelScope.launch {
            try {
                // Fetch books and embeddings in parallel for speed
                val booksTask = database.child("books").get()
                val embeddingsTask = database.child("book_embeddings").get()
                
                val booksSnapshot = booksTask.await()
                val embeddingsSnapshot = embeddingsTask.await()

                // 1. Parse books
                val books = booksSnapshot.children.mapNotNull {
                    it.getValue(Book::class.java)?.apply {
                        id = it.key ?: ""
                    }
                }

                // 2. Parse embeddings and map them by book ID
                val embeddingMap = mutableMapOf<String, List<Double>>()
                embeddingsSnapshot.children.forEach { child ->
                    val bookId = child.key ?: return@forEach
                    val embeddingList = child.child("embedding").children.mapNotNull { it.getValue(Double::class.java) }
                    if (embeddingList.isNotEmpty()) {
                        embeddingMap[bookId] = embeddingList
                    }
                }

                // 3. Attach embeddings to books
                books.forEach { book ->
                    book.embedding = embeddingMap[book.id] ?: emptyList()
                }

                _allBooks.postValue(books)
                _isLibraryLoaded.postValue(true)
                hasFetchedLibrary = true
                Log.d("SearchViewModel", "Loaded ${books.size} books (with ${embeddingMap.size} embeddings)")

            } catch (e: Exception) {
                Log.e("SearchViewModel", "Failed to load library", e)
                _errorMessage.postValue("Failed to synchronize library.")
            } finally {
                _isSearching.postValue(false)
            }
        }
    }

    fun performAISearch(query: String) {
        val currentBooks = _allBooks.value ?: emptyList()

        if (query.isBlank() || currentBooks.isEmpty()) {
            _searchResults.value = emptyList()
            return
        }

        _isSearching.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val queryEmbedding = withContext(Dispatchers.IO) {
                    huggingFaceClient.getEmbedding(query)
                }

                if (queryEmbedding.isEmpty()) {
                    // Fallback to basic text search if AI embedding fails
                    val filtered = currentBooks.filter { 
                        it.title.contains(query, ignoreCase = true) || 
                        it.author.contains(query, ignoreCase = true) 
                    }
                    _searchResults.postValue(filtered)
                    return@launch
                }

                val topMatches = withContext(Dispatchers.Default) {
                    searchHelper.findTopMatches(
                        queryEmbedding = queryEmbedding,
                        allBooks = currentBooks,
                        topN = 15
                    )
                }

                // If semantic search found nothing, try basic keyword search as fallback
                if (topMatches.isEmpty()) {
                    val filtered = currentBooks.filter { 
                        it.title.contains(query, ignoreCase = true) || 
                        it.author.contains(query, ignoreCase = true) 
                    }
                    _searchResults.postValue(filtered)
                } else {
                    _searchResults.postValue(topMatches)
                }

            } catch (e: Exception) {
                Log.e("SearchViewModel", "Search failed", e)
                _searchResults.postValue(emptyList())
            } finally {
                _isSearching.postValue(false)
            }
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }
}