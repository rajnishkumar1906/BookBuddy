package com.example.bookbuddy.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookbuddy.R
import com.example.bookbuddy.adapters.BookAdapter
import com.example.bookbuddy.models.Book
import com.example.bookbuddy.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LibrarianDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var toolbar: Toolbar
    private lateinit var btnAddBook: Button
    private lateinit var btnManageBooks: Button
    private lateinit var btnViewBorrowings: Button
    private lateinit var btnStats: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStats: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var bookAdapter: BookAdapter
    private lateinit var welcomeText: TextView
    private lateinit var btnRefresh: Button

    private val recentBooks = mutableListOf<Book>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_librarian_dashboard)

        // Initialize Firebase
        try {
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Toast.makeText(this, "Firebase initialization failed", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Check if user is logged in
        if (auth.currentUser == null) {
            navigateToLogin()
            return
        }

        // Initialize views
        initializeViews()

        // Setup toolbar
        setupToolbar()

        // Verify librarian access
        verifyLibrarianAccess()

        // Setup click listeners
        setupClickListeners()

        // Load data
        loadLibraryStats()
        loadRecentBooks()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        btnAddBook = findViewById(R.id.btnAddBook)
        btnManageBooks = findViewById(R.id.btnManageBooks)
        btnViewBorrowings = findViewById(R.id.btnViewBorrowings)
        btnStats = findViewById(R.id.btnStats)
        btnRefresh = findViewById(R.id.btnRefresh)
        progressBar = findViewById(R.id.progressBar)
        tvStats = findViewById(R.id.tvStats)
        recyclerView = findViewById(R.id.recyclerView)
        welcomeText = findViewById(R.id.welcomeText)

        bookAdapter = BookAdapter(recentBooks) { book ->
            try {
                val intent = Intent(this, BookDetailActivity::class.java)
                intent.putExtra("bookId", book.id)
                intent.putExtra("source", "librarian")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot open book details", Toast.LENGTH_SHORT).show()
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = bookAdapter
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = "BookBuddy - Librarian Panel"
    }

    private fun verifyLibrarianAccess() {
        val userId = auth.currentUser?.uid ?: return

        showLoading(true)

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)

                if (document.exists()) {
                    val user = document.toObject(User::class.java)

                    // CRITICAL: If not librarian, redirect to member dashboard
                    if (user?.role != "librarian") {
                        Toast.makeText(this, "Redirecting to Member Dashboard...", Toast.LENGTH_SHORT).show()
                        navigateToMemberDashboard()
                        return@addOnSuccessListener
                    }

                    // Show welcome message for librarian
                    welcomeText.text = "Welcome back, Librarian ${user.name}!"
                    welcomeText.visibility = View.VISIBLE
                } else {
                    // If document doesn't exist, create it as member
                    createUserDocument(userId)
                }
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, "Error verifying access", Toast.LENGTH_SHORT).show()
                navigateToLogin()
            }
    }

    private fun createUserDocument(userId: String) {
        val user = User(
            id = userId,
            name = auth.currentUser?.displayName ?: "User",
            email = auth.currentUser?.email ?: "",
            role = "member"  // Default to member for safety
        )

        db.collection("users").document(userId)
            .set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Please sign in again", Toast.LENGTH_SHORT).show()
                logoutUser()
            }
    }

    private fun setupClickListeners() {
        btnAddBook.setOnClickListener {
            startActivity(Intent(this, AddBookActivity::class.java))
        }

        btnManageBooks.setOnClickListener {
            // You'll create this activity later
            Toast.makeText(this, "Manage Books - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        btnViewBorrowings.setOnClickListener {
            // You'll create this activity later
            Toast.makeText(this, "View Borrowings - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        btnStats.setOnClickListener {
            loadLibraryStats()
        }

        btnRefresh.setOnClickListener {
            loadLibraryStats()
            loadRecentBooks()
            Toast.makeText(this, "Dashboard Refreshed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadLibraryStats() {
        db.collection("books").get()
            .addOnSuccessListener { snapshot ->
                val totalBooks = snapshot.size()
                var availableCopies = 0
                var totalCopies = 0

                snapshot.forEach { doc ->
                    val available = doc.getLong("availableCopies")?.toInt() ?: 0
                    val total = doc.getLong("totalCopies")?.toInt() ?: 0
                    availableCopies += available
                    totalCopies += total
                }

                // Get active borrowings
                db.collection("borrowRecords")
                    .whereEqualTo("returnedAt", null)
                    .get()
                    .addOnSuccessListener { borrowSnapshot ->
                        val borrowedCount = borrowSnapshot.size()

                        tvStats.text = """
                            📊 LIBRARY STATISTICS
                            ════════════════════
                            📚 Total Books: $totalBooks
                            📖 Total Copies: $totalCopies
                            ✅ Available: $availableCopies
                            📌 Currently Borrowed: $borrowedCount
                            ════════════════════
                        """.trimIndent()
                    }
            }
            .addOnFailureListener {
                tvStats.text = "Failed to load statistics"
            }
    }

    private fun loadRecentBooks() {
        db.collection("books")
            .orderBy("addedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { snapshot ->
                recentBooks.clear()
                snapshot.forEach { document ->
                    try {
                        val book = document.toObject(Book::class.java)
                        book.id = document.id
                        recentBooks.add(book)
                    } catch (e: Exception) {
                        // Skip invalid book
                    }
                }
                bookAdapter.updateList(recentBooks)
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.librarian_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                loadLibraryStats()
                loadRecentBooks()
                Toast.makeText(this, "Refreshed", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_logout -> {
                logoutUser()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logoutUser() {
        auth.signOut()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToMemberDashboard() {
        val intent = Intent(this, MemberDashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnAddBook.isEnabled = !show
        btnManageBooks.isEnabled = !show
        btnViewBorrowings.isEnabled = !show
        btnStats.isEnabled = !show
        btnRefresh.isEnabled = !show
    }
}