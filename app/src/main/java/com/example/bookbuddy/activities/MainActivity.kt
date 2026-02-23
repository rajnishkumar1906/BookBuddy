package com.example.bookbuddy.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookbuddy.R
import com.example.bookbuddy.adapters.BookAdapter
import com.example.bookbuddy.models.Book
import com.example.bookbuddy.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var bookAdapter: BookAdapter
    private lateinit var searchView: SearchView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var emptyStateText: TextView
    private lateinit var emptyStateSubText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var welcomeText: TextView

    private var bookListener: ListenerRegistration? = null
    private val books = mutableListOf<Book>()
    private var currentUserRole = "member"
    private var currentUserName = ""

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_main)

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

            // Setup Toolbar
            try {
                setupToolbar()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setup toolbar: ${e.message}")
            }

            // Check if user is logged in
            try {
                if (auth.currentUser == null) {
                    navigateToLogin()
                    return
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error checking login status", Toast.LENGTH_SHORT).show()
                navigateToLogin()
                return
            }

            // Get current user role and name (FORCE FROM SERVER)
            getUserDetails()

            // Setup views with try-catch
            try {
                setupRecyclerView()
                setupSearchView()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to setup views: ${e.message}", Toast.LENGTH_SHORT).show()
            }

            // Load books
            loadBooks()

        } catch (e: Exception) {
            Toast.makeText(this, "App error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        try {
            toolbar = findViewById(R.id.toolbar)
            recyclerView = findViewById(R.id.recyclerView)
            searchView = findViewById(R.id.searchView)
            emptyStateLayout = findViewById(R.id.emptyStateLayout)
            emptyStateText = findViewById(R.id.emptyStateText)
            emptyStateSubText = findViewById(R.id.emptyStateSubText)
            progressBar = findViewById(R.id.progressBar)
            welcomeText = findViewById(R.id.welcomeText)
        } catch (e: Exception) {
            throw Exception("View initialization failed: ${e.message}")
        }
    }

    private fun setupToolbar() {
        try {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(true)
            supportActionBar?.title = "BOOKBUDDY"
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up toolbar: ${e.message}")
        }
    }

    private fun setupRecyclerView() {
        try {
            bookAdapter = BookAdapter(books) { book ->
                try {
                    val intent = Intent(this, BookDetailActivity::class.java)
                    intent.putExtra("bookId", book.id)
                    intent.putExtra("bookTitle", book.title)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Cannot open book details", Toast.LENGTH_SHORT).show()
                }
            }
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = bookAdapter
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to setup book list: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSearchView() {
        try {
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    try {
                        query?.let { searchBooks(it) }
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Search error", Toast.LENGTH_SHORT).show()
                    }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    try {
                        if (newText.isNullOrEmpty()) {
                            loadBooks()
                        } else {
                            searchBooks(newText)
                        }
                    } catch (e: Exception) {
                        // Ignore search errors during typing
                    }
                    return true
                }
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to setup search", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getUserDetails() {
        try {
            val userId = try {
                auth.currentUser?.uid ?: run {
                    Log.e(TAG, "No user ID found")
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting user ID: ${e.message}")
                return
            }

            Log.d(TAG, "Loading user details for ID: $userId")

            // FORCE REFRESH FROM SERVER - DON'T USE CACHE
            db.collection("users").document(userId)
                .get(Source.SERVER)
                .addOnSuccessListener { document ->
                    try {
                        if (document.exists()) {
                            val user = document.toObject(User::class.java)
                            currentUserRole = user?.role ?: "member"
                            currentUserName = user?.name ?: "User"

                            Log.d(TAG, "User role loaded from server: $currentUserRole")
                            Log.d(TAG, "User name: $currentUserName")

                            // Show welcome message
                            welcomeText.visibility = View.VISIBLE
                            welcomeText.text = "Welcome, $currentUserName!"

                            // Show toast with role for debugging
                            Toast.makeText(
                                this@MainActivity,
                                "Logged in as: $currentUserRole",
                                Toast.LENGTH_LONG
                            ).show()

                            // Force menu to refresh
                            invalidateOptionsMenu()

                            // Update empty state text based on role
                            updateEmptyStateText()
                        } else {
                            Log.e(TAG, "User document does not exist")
                            // Create user document if it doesn't exist
                            createUserDocument(userId)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing user document: ${e.message}")
                        currentUserRole = "member"
                        invalidateOptionsMenu()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to load user details: ${e.message}")
                    try {
                        currentUserRole = "member"
                        invalidateOptionsMenu()
                        Toast.makeText(this, "Failed to load user details", Toast.LENGTH_SHORT).show()
                    } catch (ex: Exception) {
                        // Ignore
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getUserDetails: ${e.message}")
            Toast.makeText(this, "Error loading user details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createUserDocument(userId: String) {
        try {
            val user = User(
                id = userId,
                name = auth.currentUser?.displayName ?: "User",
                email = auth.currentUser?.email ?: "",
                role = "member"
            )

            db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener {
                    Log.d(TAG, "User document created successfully")
                    currentUserRole = "member"
                    currentUserName = user.name
                    welcomeText.text = "Welcome, $currentUserName!"
                    invalidateOptionsMenu()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to create user document: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user document: ${e.message}")
        }
    }

    private fun updateEmptyStateText() {
        try {
            if (books.isEmpty()) {
                emptyStateText.text = if (currentUserRole == "librarian") {
                    "No books in library yet."
                } else {
                    "No books available yet."
                }

                emptyStateSubText.text = if (currentUserRole == "librarian") {
                    "Click + to add your first book!"
                } else {
                    "Please check back later!"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating empty state text: ${e.message}")
        }
    }

    private fun loadBooks() {
        try {
            showLoading(true)

            bookListener = db.collection("books")
                .addSnapshotListener { snapshot, error ->
                    try {
                        showLoading(false)

                        if (error != null) {
                            Toast.makeText(this, "Error loading books: ${error.message}", Toast.LENGTH_SHORT).show()
                            return@addSnapshotListener
                        }

                        books.clear()
                        snapshot?.forEach { document ->
                            try {
                                val book = document.toObject(Book::class.java)
                                book.id = document.id
                                books.add(book)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing book: ${e.message}")
                                // Skip invalid book entries
                            }
                        }
                        bookAdapter.updateList(books)

                        // Show empty state if no books
                        if (books.isEmpty()) {
                            showEmptyState(true)
                            updateEmptyStateText()
                        } else {
                            showEmptyState(false)
                        }
                    } catch (e: Exception) {
                        showLoading(false)
                        Log.e(TAG, "Error updating book list: ${e.message}")
                        Toast.makeText(this@MainActivity, "Error updating book list", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: Exception) {
            showLoading(false)
            Log.e(TAG, "Failed to load books: ${e.message}")
            Toast.makeText(this, "Failed to load books: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun searchBooks(query: String) {
        try {
            showLoading(true)

            val searchResults = mutableListOf<Book>()

            db.collection("books")
                .get()
                .addOnSuccessListener { snapshot ->
                    try {
                        showLoading(false)

                        searchResults.clear()
                        snapshot.forEach { document ->
                            try {
                                val book = document.toObject(Book::class.java)
                                book.id = document.id

                                if (book.title.contains(query, ignoreCase = true) ||
                                    book.author.contains(query, ignoreCase = true) ||
                                    book.genre.contains(query, ignoreCase = true)) {
                                    searchResults.add(book)
                                }
                            } catch (e: Exception) {
                                // Skip invalid books in search
                            }
                        }

                        if (searchResults.isEmpty()) {
                            showEmptyState(true)
                            emptyStateText.text = "No books found matching '$query'"
                            emptyStateSubText.text = "Try different keywords"
                        } else {
                            showEmptyState(false)
                        }

                        bookAdapter.updateList(searchResults)
                    } catch (e: Exception) {
                        showLoading(false)
                        Log.e(TAG, "Error processing search: ${e.message}")
                        Toast.makeText(this@MainActivity, "Error processing search", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    try {
                        showLoading(false)
                        Log.e(TAG, "Search failed: ${e.message}")
                        Toast.makeText(this, "Search failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    } catch (ex: Exception) {
                        // Ignore
                    }
                }
        } catch (e: Exception) {
            showLoading(false)
            Log.e(TAG, "Search error: ${e.message}")
            Toast.makeText(this, "Search error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEmptyState(show: Boolean) {
        try {
            if (show) {
                emptyStateLayout.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyStateLayout.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing empty state: ${e.message}")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return try {
            menuInflater.inflate(R.menu.main_menu, menu)

            val addBookItem = menu.findItem(R.id.action_add_book)
            val dashboardItem = menu.findItem(R.id.action_librarian_dashboard)

            Log.d(TAG, "Creating menu for role: $currentUserRole")

            if (currentUserRole == "librarian") {
                addBookItem?.isVisible = true
                dashboardItem?.isVisible = true
                Log.d(TAG, "Showing librarian menu (Add Book + Dashboard)")
            } else {
                addBookItem?.isVisible = false
                dashboardItem?.isVisible = false
                Log.d(TAG, "Showing member menu (no Add Book)")
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load menu: ${e.message}")
            Toast.makeText(this, "Failed to load menu", Toast.LENGTH_SHORT).show()
            super.onCreateOptionsMenu(menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return try {
            when (item.itemId) {
                R.id.action_librarian_dashboard -> {
                    startActivity(Intent(this, LibrarianDashboardActivity::class.java))
                    true
                }
                R.id.action_add_book -> {
                    startActivity(Intent(this, AddBookActivity::class.java))
                    true
                }
                R.id.action_my_books -> {
                    startActivity(Intent(this, MyBooksActivity::class.java))
                    true
                }
                R.id.action_ai_search -> {
                    startActivity(Intent(this, AISearchActivity::class.java))
                    true
                }
                R.id.action_logout -> {
                    logoutUser()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error: ${e.message}")
            Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun logoutUser() {
        try {
            auth.signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        } catch (e: Exception) {
            Log.e(TAG, "Logout error: ${e.message}")
            Toast.makeText(this, "Logout error", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        try {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error: ${e.message}")
            Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showLoading(show: Boolean) {
        try {
            if (show) {
                progressBar.visibility = View.VISIBLE
                searchView.isEnabled = false
            } else {
                progressBar.visibility = View.GONE
                searchView.isEnabled = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing loading: ${e.message}")
        }
    }

    override fun onDestroy() {
        try {
            bookListener?.remove()
        } catch (e: Exception) {
            Log.e(TAG, "Error removing listener: ${e.message}")
        }
        super.onDestroy()
    }
}