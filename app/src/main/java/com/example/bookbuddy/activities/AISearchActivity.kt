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
import androidx.appcompat.widget.Toolbar
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
    private lateinit var toolbar: Toolbar

    private lateinit var bookAdapter: BookAdapter
    private val allBooks = mutableListOf<Book>()
    private val searchResults = mutableListOf<Book>()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aisearch)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        aiHelper = BookAIHelper(this)

        initializeViews()
        setupToolbar()
        setupClickListeners()
        loadAllBooks()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        searchInput = findViewById(R.id.searchInput)
        searchButton = findViewById(R.id.searchButton)
        progressBar = findViewById(R.id.progressBar)
        recyclerView = findViewById(R.id.recyclerView)
        emptyStateText = findViewById(R.id.emptyStateText)
        backButton = findViewById(R.id.backButton)

        bookAdapter = BookAdapter(searchResults) { book ->
            val intent = Intent(this, BookDetailActivity::class.java)
            intent.putExtra("bookId", book.id)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = bookAdapter
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.ai_search_title)
    }

    private fun setupClickListeners() {
        searchButton.setOnClickListener {
            performAISearch()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun loadAllBooks() {
        showLoading(true)

        db.collection("books")
            .get()
            .addOnSuccessListener { snapshot ->
                allBooks.clear()
                snapshot.forEach { document ->
                    try {
                        val book = document.toObject(Book::class.java)
                        book.id = document.id
                        allBooks.add(book)
                    } catch (e: Exception) {
                    }
                }
                showLoading(false)

                if (allBooks.isEmpty()) {
                    emptyStateText.text = "No books available in library"
                    emptyStateText.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, "Error loading books", Toast.LENGTH_SHORT).show()
            }
    }

    private fun performAISearch() {
        val query = searchInput.text.toString().trim()

        if (query.isEmpty()) {
            searchInput.error = "Please enter your search"
            return
        }

        showLoading(true)
        searchResults.clear()
        emptyStateText.visibility = View.GONE

        coroutineScope.launch {
            try {
                val searchIntent = aiHelper.processNaturalLanguageQuery(query)

                val scoredBooks = scoreBooksBasedOnIntent(searchIntent)

                withContext(Dispatchers.Main) {
                    searchResults.addAll(scoredBooks)
                    bookAdapter.updateList(searchResults)

                    if (searchResults.isEmpty()) {
                        emptyStateText.text = "No books found matching your description"
                        emptyStateText.visibility = View.VISIBLE
                    }

                    showLoading(false)
                }

                Toast.makeText(
                    this@AISearchActivity,
                    "🔍 Looking for: ${searchIntent.genres.joinToString()} ${searchIntent.topics.joinToString()}",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    performBasicSearch(query)
                }
            }
        }
    }

    private fun performBasicSearch(query: String) {
        val basicResults = allBooks.filter { book ->
            book.title.contains(query, ignoreCase = true) ||
                    book.author.contains(query, ignoreCase = true) ||
                    book.genre.contains(query, ignoreCase = true) ||
                    book.description.contains(query, ignoreCase = true)
        }

        searchResults.clear()
        searchResults.addAll(basicResults)
        bookAdapter.updateList(searchResults)

        if (searchResults.isEmpty()) {
            emptyStateText.text = "No books found matching your search"
            emptyStateText.visibility = View.VISIBLE
        }

        Toast.makeText(this, "Using basic search", Toast.LENGTH_SHORT).show()
    }

    private fun scoreBooksBasedOnIntent(intent: BookAIHelper.SearchIntent): List<Book> {
        val scored = mutableListOf<Pair<Book, Double>>()

        allBooks.forEach { book ->
            var score = 0.0

            if (book.genre.lowercase() in intent.genres.map { it.lowercase() }) {
                score += 15.0
            }

            if (book.author.lowercase() in intent.authors.map { it.lowercase() }) {
                score += 12.0
            }

            intent.topics.forEach { topic ->
                if (book.title.lowercase().contains(topic.lowercase())) {
                    score += 8.0
                }
                if (book.description.lowercase().contains(topic.lowercase())) {
                    score += 5.0
                }
            }

            intent.bookTitles.forEach { title ->
                if (book.title.lowercase().contains(title.lowercase())) {
                    score += 20.0
                }
            }

            score += book.timesBorrowed * 0.2

            if (score > 0) {
                scored.add(book to score)
            }
        }

        return scored.sortedByDescending { it.second }
            .take(20)
            .map { it.first }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            progressBar.visibility = View.VISIBLE
            searchButton.isEnabled = false
            searchButton.alpha = 0.5f
            searchInput.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            searchButton.isEnabled = true
            searchButton.alpha = 1.0f
            searchInput.isEnabled = true
        }
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        super.onDestroy()
    }
}