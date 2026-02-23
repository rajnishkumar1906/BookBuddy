package com.example.bookbuddy.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookbuddy.R
import com.example.bookbuddy.adapters.BookAdapter
import com.example.bookbuddy.models.Book
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LibrarianDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // UI Components
    private lateinit var btnManualAdd: Button
    private lateinit var btnViewAllBooks: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStats: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var bookAdapter: BookAdapter
    private lateinit var btnRefresh: Button
    private lateinit var btnBack: Button

    private val books = mutableListOf<Book>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_librarian_dashboard)

            // Initialize Firebase with try-catch
            try {
                auth = FirebaseAuth.getInstance()
                db = FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                Toast.makeText(this, "Firebase initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // Initialize views with try-catch
            try {
                initializeViews()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load UI: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // Check librarian access
            checkLibrarianAccess()

            // Load data
            loadLibraryStats()
            loadRecentBooks()

            // Setup click listeners
            setupClickListeners()

        } catch (e: Exception) {
            Toast.makeText(this, "App error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        try {
            btnManualAdd = findViewById(R.id.btnManualAdd)
            btnViewAllBooks = findViewById(R.id.btnViewAllBooks)
            progressBar = findViewById(R.id.progressBar)
            tvStats = findViewById(R.id.tvStats)
            recyclerView = findViewById(R.id.recyclerView)
            btnRefresh = findViewById(R.id.btnRefresh)
            btnBack = findViewById(R.id.btnBack)

            bookAdapter = BookAdapter(books) { book ->
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

    private fun checkLibrarianAccess() {
        try {
            val userId = try {
                auth.currentUser?.uid
            } catch (e: Exception) {
                null
            }

            if (userId == null) {
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            showLoading(true)

            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    try {
                        showLoading(false)

                        val role = document.getString("role")
                        if (role != "librarian") {
                            Toast.makeText(this, "Access denied. Librarians only.", Toast.LENGTH_LONG).show()
                            finish()
                        }
                    } catch (e: Exception) {
                        showLoading(false)
                        Toast.makeText(this, "Error processing user data", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .addOnFailureListener { e ->
                    try {
                        showLoading(false)
                        Toast.makeText(this, "Error verifying access: ${e.message}", Toast.LENGTH_SHORT).show()
                        finish()
                    } catch (ex: Exception) {
                        // Ignore
                    }
                }
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(this, "Access check failed: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupClickListeners() {
        try {
            btnManualAdd.setOnClickListener {
                try {
                    startActivity(Intent(this, AddBookActivity::class.java))
                } catch (e: Exception) {
                    Toast.makeText(this, "Cannot open add book screen", Toast.LENGTH_SHORT).show()
                }
            }

            btnViewAllBooks.setOnClickListener {
                try {
                    startActivity(Intent(this, MainActivity::class.java))
                } catch (e: Exception) {
                    Toast.makeText(this, "Cannot open main screen", Toast.LENGTH_SHORT).show()
                }
            }

            btnRefresh.setOnClickListener {
                loadLibraryStats()
                loadRecentBooks()
            }

            btnBack.setOnClickListener {
                try {
                    finish()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting up buttons", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadLibraryStats() {
        try {
            showLoading(true)

            db.collection("books").get()
                .addOnSuccessListener { snapshot ->
                    try {
                        showLoading(false)

                        val totalBooks = snapshot.size()
                        var availableCopies = 0
                        var borrowedCount = 0

                        snapshot.forEach { doc ->
                            try {
                                val available = doc.getLong("availableCopies") ?: 0
                                val total = doc.getLong("totalCopies") ?: 0
                                availableCopies += available.toInt()
                                borrowedCount += (total.toInt() - available.toInt())
                            } catch (e: Exception) {
                                // Skip invalid document
                            }
                        }

                        tvStats.text = """
                            📊 LIBRARY STATISTICS
                            ────────────────────
                            Total Books: $totalBooks
                            Available Copies: $availableCopies
                            Currently Borrowed: $borrowedCount
                            ────────────────────
                        """.trimIndent()
                    } catch (e: Exception) {
                        showLoading(false)
                        Toast.makeText(this@LibrarianDashboardActivity, "Error processing stats", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    try {
                        showLoading(false)
                        Toast.makeText(this, "Error loading stats: ${e.message}", Toast.LENGTH_SHORT).show()
                        tvStats.text = "Failed to load statistics"
                    } catch (ex: Exception) {
                        // Ignore
                    }
                }
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(this, "Stats loading failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadRecentBooks() {
        try {
            db.collection("books")
                .orderBy("addedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener { snapshot ->
                    try {
                        books.clear()
                        snapshot.forEach { document ->
                            try {
                                val book = document.toObject(Book::class.java)
                                book.id = document.id
                                books.add(book)
                            } catch (e: Exception) {
                                // Skip invalid book entries
                            }
                        }
                        bookAdapter.updateList(books)
                    } catch (e: Exception) {
                        Toast.makeText(this@LibrarianDashboardActivity, "Error processing recent books", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    try {
                        Toast.makeText(this, "Error loading recent books: ${e.message}", Toast.LENGTH_SHORT).show()
                    } catch (ex: Exception) {
                        // Ignore
                    }
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load recent books: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(show: Boolean) {
        try {
            if (show) {
                progressBar.visibility = View.VISIBLE
                btnManualAdd.isEnabled = false
                btnManualAdd.alpha = 0.5f
                btnRefresh.isEnabled = false
                btnRefresh.alpha = 0.5f
                btnViewAllBooks.isEnabled = false
                btnViewAllBooks.alpha = 0.5f
                btnBack.isEnabled = false
                btnBack.alpha = 0.5f
            } else {
                progressBar.visibility = View.GONE
                btnManualAdd.isEnabled = true
                btnManualAdd.alpha = 1.0f
                btnRefresh.isEnabled = true
                btnRefresh.alpha = 1.0f
                btnViewAllBooks.isEnabled = true
                btnViewAllBooks.alpha = 1.0f
                btnBack.isEnabled = true
                btnBack.alpha = 1.0f
            }
        } catch (e: Exception) {
            // Ignore UI errors during loading
        }
    }
}