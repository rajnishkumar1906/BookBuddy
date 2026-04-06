package com.rajnishkumar.bookbuddy.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.rajnishkumar.bookbuddy.ai.AISearchHelper
import com.rajnishkumar.bookbuddy.ai.HuggingFaceClient
import com.rajnishkumar.bookbuddy.models.Book
import com.rajnishkumar.bookbuddy.database.AppDatabase
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class MemberHomeViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val huggingFace = HuggingFaceClient()
    private val searchHelper = AISearchHelper()

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _readingProgress = MutableLiveData<Int>()
    val readingProgress: LiveData<Int> = _readingProgress

    private val _readingGoalText = MutableLiveData<String>()
    val readingGoalText: LiveData<String> = _readingGoalText

    private val _recentBooks = MutableLiveData<List<Book>>()
    val recentBooks: LiveData<List<Book>> = _recentBooks

    private val _recommendedBooks = MutableLiveData<List<Book>>()
    val recommendedBooks: LiveData<List<Book>> = _recommendedBooks

    private val _fantasyBooks = MutableLiveData<List<Book>>()
    val fantasyBooks: LiveData<List<Book>> = _fantasyBooks

    private val _adventureBooks = MutableLiveData<List<Book>>()
    val adventureBooks: LiveData<List<Book>> = _adventureBooks

    private val _mysteryBooks = MutableLiveData<List<Book>>()
    val mysteryBooks: LiveData<List<Book>> = _mysteryBooks

    private val _sciFiBooks = MutableLiveData<List<Book>>()
    val sciFiBooks: LiveData<List<Book>> = _sciFiBooks

    private val _historyBooks = MutableLiveData<List<Book>>()
    val historyBooks: LiveData<List<Book>> = _historyBooks

    private val _isNewUser = MutableLiveData<Boolean>(false)
    val isNewUser: LiveData<Boolean> = _isNewUser

    private var allBooksCache = listOf<Book>()
    private var hasLoaded = false

    fun loadDataIfNeeded(context: Context) {
        if (hasLoaded) return
        hasLoaded = true

        val uid = auth.currentUser?.uid ?: return
        val roomDb = AppDatabase.getDatabase(context)

        viewModelScope.launch {
            loadUserName(uid)
            loadReadingStats(uid)

            val localBooks = withContext(Dispatchers.IO) { roomDb.bookDao().getAllBooks() }
            if (localBooks.isNotEmpty()) {
                allBooksCache = localBooks
                updateUIWithCachedData()
            }

            supervisorScope {
                val visitsDeferred = async { 
                    try { database.child("visits").child(uid).orderByValue().limitToLast(10).get().await() } 
                    catch (e: Exception) { null } 
                }

                val visitsSnap = visitsDeferred.await()
                
                if (allBooksCache.isEmpty()) {
                    val booksSnap = try { database.child("books").limitToFirst(100).get().await() } catch (e: Exception) { null }
                    if (booksSnap != null) {
                        allBooksCache = booksSnap.children.mapNotNull { parseSafeBook(it) }
                        updateUIWithCachedData()
                    }
                }

                if (visitsSnap != null) {
                    val recentIds = visitsSnap.children.mapNotNull { it.key }.reversed()
                    val recentBooksList = recentIds.mapNotNull { id -> allBooksCache.find { it.id == id } }
                    _recentBooks.postValue(recentBooksList)
                    _isNewUser.postValue(recentIds.size <= 2)
                }
            }
        }
    }

    private fun updateUIWithCachedData() {
        _fantasyBooks.postValue(allBooksCache.filter { b -> b.genre.any { it.contains("Fantasy", ignoreCase = true) } }.shuffled().take(10))
        _adventureBooks.postValue(allBooksCache.filter { b -> b.genre.any { it.contains("Adventure", ignoreCase = true) } }.shuffled().take(10))
        _mysteryBooks.postValue(allBooksCache.filter { b -> b.genre.any { it.contains("Mystery", ignoreCase = true) || it.contains("Thriller", ignoreCase = true) } }.shuffled().take(10))
        _sciFiBooks.postValue(allBooksCache.filter { b -> b.genre.any { it.contains("Sci-Fi", ignoreCase = true) || it.contains("Science Fiction", ignoreCase = true) } }.shuffled().take(10))
        _historyBooks.postValue(allBooksCache.filter { b -> b.genre.any { it.contains("History", ignoreCase = true) || it.contains("Historical", ignoreCase = true) } }.shuffled().take(10))
        
        val popular = allBooksCache.sortedByDescending { it.averageRating }.take(15)
        _recommendedBooks.postValue(popular.shuffled().take(10))
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
            val rawGenre = snapshot.child("genre").value?.toString() ?: ""
            Book(
                id = snapshot.key ?: "",
                title = snapshot.child("title").getValue(String::class.java) ?: "Unknown",
                author = snapshot.child("author").getValue(String::class.java) ?: "Unknown",
                genre = parseGenreString(rawGenre),
                coverUrl = snapshot.child("coverUrl").getValue(String::class.java) ?: ""
            )
        }
    }

    private fun parseGenreString(input: String): List<String> {
        if (input.isBlank()) return emptyList()
        return input.replace("[", "").replace("]", "").replace("'", "").split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun loadUserName(uid: String) {
        database.child("users").child(uid).child("name").get().addOnSuccessListener {
            _userName.value = it.getValue(String::class.java) ?: "Member"
        }
    }

    private fun loadReadingStats(uid: String) {
        database.child("borrowRecords").orderByChild("userId").equalTo(uid).get().addOnSuccessListener { snapshot ->
            val borrowedCount = snapshot.children.count { it.child("status").getValue(String::class.java) == "BORROWED" }
            val progress = (borrowedCount.toFloat() / 5 * 100).toInt().coerceAtMost(100)
            _readingProgress.value = progress
            _readingGoalText.value = "$progress% goal reached ($borrowedCount/5)"
        }
    }
}
