package com.example.bookbuddy.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookbuddy.R
import com.example.bookbuddy.adapters.BookAdapter
import com.example.bookbuddy.ai.BookAIHelper
import com.example.bookbuddy.models.Book
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*

class AISearchActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var aiHelper: BookAIHelper

    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var backButton: Button

    private lateinit var bookAdapter: BookAdapter
    private val allBooks = mutableListOf<Book>()
    private val searchResults = mutableListOf<Book>()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_aisearch)

            // Initialize Firebase with try-catch
            try {
                auth = FirebaseAuth.getInstance()
                db = FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                Toast.makeText(this, "Firebase initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // Initialize AI Helper with try-catch
            try {
                aiHelper = BookAIHelper(this)
            } catch (e: Exception) {
                Toast.makeText(this, "AI Helper initialization failed: ${e.message}", Toast.LENGTH_SHORT).show()
                // Continue without AI features? Or finish?
            }

            // Initialize views with try-catch
            try {
                initializeViews()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load UI: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // Setup click listeners
            try {
                setupClickListeners()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to setup buttons", Toast.LENGTH_SHORT).show()
            }

            // Load all books
            loadAllBooks()

        } catch (e: Exception) {
            Toast.makeText(this, "App error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        try {
            searchInput = findViewById(R.id.searchInput)
            searchButton = findViewById(R.id.searchButton)
            progressBar = findViewById(R.id.progressBar)
            recyclerView = findViewById(R.id.recyclerView)
            emptyStateText = findViewById(R.id.emptyStateText)
            backButton = findViewById(R.id.backButton)

            bookAdapter = BookAdapter(searchResults) { book ->
                try {
                    val intent = Intent(this, BookDetailActivity::class.java)
                    intent.putExtra("bookId", book.id)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Cannot open book details", Toast.LENGTH_SHORT).show()
                }
            }

            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = bookAdapter
        } catch (e: Exception) {
            throw Exception("View initialization failed: ${e.message}")
        }
    }

    private fun setupClickListeners() {
        try {
            searchButton.setOnClickListener {
                performAISearch()
            }

            backButton.setOnClickListener {
                try {
                    finish()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting up click listeners", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAllBooks() {
        try {
            showLoading(true)

            db.collection("books")
                .get()
                .addOnSuccessListener { snapshot ->
                    try {
                        allBooks.clear()
                        snapshot.forEach { document ->
                            try {
                                val book = document.toObject(Book::class.java)
                                book.id = document.id
                                allBooks.add(book)
                            } catch (e: Exception) {
                                // Skip invalid book entries
                            }
                        }
                        showLoading(false)

                        if (allBooks.isEmpty()) {
                            emptyStateText.text = "No books available in library"
                            emptyStateText.visibility = View.VISIBLE
                        }
                    } catch (e: Exception) {
                        showLoading(false)
                        Toast.makeText(this@AISearchActivity, "Error processing books", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    try {
                        showLoading(false)
                        Toast.makeText(this, "Error loading books: ${e.message}", Toast.LENGTH_SHORT).show()
                        emptyStateText.text = "Failed to load books"
                        emptyStateText.visibility = View.VISIBLE
                    } catch (ex: Exception) {
                        // Ignore
                    }
                }
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(this, "Failed to load books: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performAISearch() {
        try {
            val query = try {
                searchInput.text.toString().trim()
            } catch (e: Exception) {
                ""
            }

            if (query.isEmpty()) {
                searchInput.error = "Please enter your search"
                return
            }

            showLoading(true)
            searchResults.clear()
            emptyStateText.visibility = View.GONE

            coroutineScope.launch {
                try {
                    // Process query with AI
                    val searchIntent = try {
                        aiHelper.processNaturalLanguageQuery(query)
                    } catch (e: Exception) {
                        // Fallback to basic search if AI fails
                        withContext(Dispatchers.Main) {
                            performBasicSearch(query)
                        }
                        return@launch
                    }

                    // Score books based on AI understanding
                    val scoredBooks = try {
                        scoreBooksBasedOnIntent(searchIntent)
                    } catch (e: Exception) {
                        emptyList()
                    }

                    withContext(Dispatchers.Main) {
                        try {
                            searchResults.addAll(scoredBooks)
                            bookAdapter.updateList(searchResults)

                            if (searchResults.isEmpty()) {
                                emptyStateText.text = "No books found matching:\n\"$query\"\n\nTry different words!"
                                emptyStateText.visibility = View.VISIBLE
                            } else {
                                emptyStateText.text = "Found ${searchResults.size} books matching your search"
                                emptyStateText.visibility = View.VISIBLE
                            }

                            showLoading(false)
                        } catch (e: Exception) {
                            showLoading(false)
                            Toast.makeText(this@AISearchActivity, "Error updating results", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Show what AI understood
                    try {
                        Toast.makeText(
                            this@AISearchActivity,
                            "🔍 Looking for: ${searchIntent.genres.joinToString()} ${searchIntent.topics.joinToString()}",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (e: Exception) {
                        // Ignore toast errors
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        try {
                            showLoading(false)
                            emptyStateText.text = "Search error: ${e.message}"
                            emptyStateText.visibility = View.VISIBLE
                            Toast.makeText(
                                this@AISearchActivity,
                                "Search error: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (ex: Exception) {
                            // Ignore
                        }
                    }
                }
            }
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(this, "Search failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performBasicSearch(query: String) {
        try {
            // Fallback basic search if AI fails
            val basicResults = allBooks.filter { book ->
                try {
                    book.title.contains(query, ignoreCase = true) ||
                            book.author.contains(query, ignoreCase = true) ||
                            book.genre.contains(query, ignoreCase = true) ||
                            book.description.contains(query, ignoreCase = true)
                } catch (e: Exception) {
                    false
                }
            }

            searchResults.clear()
            searchResults.addAll(basicResults)
            bookAdapter.updateList(searchResults)

            if (searchResults.isEmpty()) {
                emptyStateText.text = "No books found matching:\n\"$query\""
                emptyStateText.visibility = View.VISIBLE
            } else {
                emptyStateText.text = "Found ${searchResults.size} books matching your search"
                emptyStateText.visibility = View.VISIBLE
            }

            showLoading(false)

            Toast.makeText(
                this,
                "Using basic search (AI unavailable)",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(this, "Basic search failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scoreBooksBasedOnIntent(intent: BookAIHelper.SearchIntent): List<Book> {
        return try {
            val scored = mutableListOf<Pair<Book, Double>>()

            allBooks.forEach { book ->
                try {
                    var score = 0.0

                    // Match by genre
                    if (book.genre.lowercase() in intent.genres.map { it.lowercase() }) {
                        score += 15.0
                    }

                    // Match by author
                    if (book.author.lowercase() in intent.authors.map { it.lowercase() }) {
                        score += 12.0
                    }

                    // Match by topics in title
                    intent.topics.forEach { topic ->
                        if (book.title.lowercase().contains(topic.lowercase())) {
                            score += 8.0
                        }
                    }

                    // Match by topics in description
                    intent.topics.forEach { topic ->
                        if (book.description.lowercase().contains(topic.lowercase())) {
                            score += 5.0
                        }
                    }

                    // Match by book title from query
                    intent.bookTitles.forEach { title ->
                        if (book.title.lowercase().contains(title.lowercase())) {
                            score += 20.0
                        }
                    }

                    // Boost popular books
                    score += book.timesBorrowed * 0.2

                    if (score > 0) {
                        scored.add(book to score)
                    }
                } catch (e: Exception) {
                    // Skip scoring for this book if error
                }
            }

            scored.sortedByDescending { it.second }
                .take(20)
                .map { it.first }
        } catch (e: Exception) {
            emptyList() // Return empty list on error
        }
    }

    private fun showLoading(show: Boolean) {
        try {
            if (show) {
                progressBar.visibility = View.VISIBLE
                searchButton.isEnabled = false
                searchButton.alpha = 0.5f
                searchInput.isEnabled = false
                searchInput.alpha = 0.5f
            } else {
                progressBar.visibility = View.GONE
                searchButton.isEnabled = true
                searchButton.alpha = 1.0f
                searchInput.isEnabled = true
                searchInput.alpha = 1.0f
            }
        } catch (e: Exception) {
            // Ignore UI errors during loading
        }
    }

    override fun onDestroy() {
        try {
            coroutineScope.cancel()
        } catch (e: Exception) {
            // Ignore
        }
        super.onDestroy()
    }
}