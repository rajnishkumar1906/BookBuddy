package com.example.bookbuddy.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
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
import com.example.bookbuddy.ai.BookRecommendationHelper
import com.example.bookbuddy.models.Book
import com.example.bookbuddy.models.BookInteraction
import com.example.bookbuddy.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MemberDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var bookAdapter: BookAdapter
    private lateinit var searchView: SearchView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var emptyStateText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var welcomeText: TextView
    private lateinit var btnBrowseBooks: Button
    private lateinit var btnMyBooks: Button
    private lateinit var btnAISearch: Button

    // New UI elements for recommendations
    private lateinit var popularBooksLabel: TextView
    private lateinit var recommendedLabel: TextView
    private lateinit var popularRecyclerView: RecyclerView
    private lateinit var recommendedRecyclerView: RecyclerView
    private lateinit var mainContentLayout: LinearLayout

    private var bookListener: ListenerRegistration? = null
    private val books = mutableListOf<Book>()
    private val popularBooks = mutableListOf<Book>()
    private val recommendedBooks = mutableListOf<Book>()
    private var currentUserName = ""
    private lateinit var recommendationHelper: BookRecommendationHelper
    private val userInteractions = mutableListOf<BookInteraction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        recommendationHelper = BookRecommendationHelper()

        if (auth.currentUser == null) {
            navigateToLogin()
            return
        }

        initializeViews()
        setupToolbar()
        getUserDetails()
        setupRecyclerViews()
        setupSearchView()
        setupClickListeners()
        loadBooks()
        loadUserInteractions()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerView)
        searchView = findViewById(R.id.searchView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        emptyStateText = findViewById(R.id.emptyStateText)
        progressBar = findViewById(R.id.progressBar)
        welcomeText = findViewById(R.id.welcomeText)
        btnBrowseBooks = findViewById(R.id.btnBrowseBooks)
        btnMyBooks = findViewById(R.id.btnMyBooks)
        btnAISearch = findViewById(R.id.btnAISearch)

        // Initialize new views
        popularBooksLabel = findViewById(R.id.popularBooksLabel)
        recommendedLabel = findViewById(R.id.recommendedLabel)
        popularRecyclerView = findViewById(R.id.popularRecyclerView)
        recommendedRecyclerView = findViewById(R.id.recommendedRecyclerView)
        mainContentLayout = findViewById(R.id.mainContentLayout)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.member_dashboard_title)
    }

    private fun setupRecyclerViews() {
        // Main books adapter
        bookAdapter = BookAdapter(books) { book ->
            val intent = Intent(this, BookDetailActivity::class.java)
            intent.putExtra("bookId", book.id)
            intent.putExtra("source", "member")
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = bookAdapter

        // Popular books adapter
        val popularAdapter = BookAdapter(popularBooks) { book ->
            val intent = Intent(this, BookDetailActivity::class.java)
            intent.putExtra("bookId", book.id)
            intent.putExtra("source", "member")
            startActivity(intent)
        }
        popularRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        popularRecyclerView.adapter = popularAdapter

        // Recommended books adapter
        val recommendedAdapter = BookAdapter(recommendedBooks) { book ->
            val intent = Intent(this, BookDetailActivity::class.java)
            intent.putExtra("bookId", book.id)
            intent.putExtra("source", "member")
            startActivity(intent)
        }
        recommendedRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recommendedRecyclerView.adapter = recommendedAdapter
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchBooks(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    showMainContent()
                    loadBooks()
                } else {
                    searchBooks(newText)
                }
                return true
            }
        })

        searchView.setOnCloseListener {
            showMainContent()
            loadBooks()
            false
        }
    }

    private fun setupClickListeners() {
        btnBrowseBooks.setOnClickListener {
            showMainContent()
            loadBooks()
            Toast.makeText(this, R.string.member_browse_books, Toast.LENGTH_SHORT).show()
        }

        btnMyBooks.setOnClickListener {
            startActivity(Intent(this, MyBooksActivity::class.java))
        }

        btnAISearch.setOnClickListener {
            startActivity(Intent(this, AISearchActivity::class.java))
        }
    }

    private fun getUserDetails() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    currentUserName = user?.name ?: "Member"

                    if (user?.role == "librarian") {
                        Toast.makeText(this, R.string.librarian_access_redirect, Toast.LENGTH_SHORT).show()
                        navigateToLibrarianDashboard()
                        return@addOnSuccessListener
                    }

                    val welcomeMessage = getString(R.string.member_welcome, currentUserName)
                    welcomeText.text = welcomeMessage
                    welcomeText.visibility = View.VISIBLE
                } else {
                    createUserDocument(userId)
                }
            }
            .addOnFailureListener {
                welcomeText.text = getString(R.string.member_welcome_default)
                welcomeText.visibility = View.VISIBLE
            }
    }

    private fun createUserDocument(userId: String) {
        val user = User(
            id = userId,
            name = auth.currentUser?.displayName ?: "User",
            email = auth.currentUser?.email ?: "",
            role = "member"
        )

        db.collection("users").document(userId)
            .set(user)
            .addOnSuccessListener {
                currentUserName = user.name
                welcomeText.text = getString(R.string.member_welcome, currentUserName)
                welcomeText.visibility = View.VISIBLE
            }
    }

    private fun loadUserInteractions() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("interactions")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { snapshot ->
                userInteractions.clear()
                snapshot.forEach { document ->
                    try {
                        val interaction = document.toObject(BookInteraction::class.java)
                        userInteractions.add(interaction)
                    } catch (e: Exception) {
                        // Skip invalid interactions
                    }
                }
            }
    }

    private fun loadBooks() {
        showLoading(true)

        bookListener = db.collection("books")
            .addSnapshotListener { snapshot, error ->
                showLoading(false)

                if (error != null) {
                    Toast.makeText(this, R.string.member_error_loading, Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                books.clear()
                snapshot?.forEach { document ->
                    try {
                        val book = document.toObject(Book::class.java)
                        book.id = document.id
                        books.add(book)
                    } catch (e: Exception) {
                    }
                }
                bookAdapter.updateList(books)

                // Update recommendation sections
                updatePopularBooks()
                updateRecommendedBooks()

                if (books.isEmpty()) {
                    emptyStateLayout.visibility = View.VISIBLE
                    emptyStateText.text = getString(R.string.member_empty_books)
                    recyclerView.visibility = View.GONE
                    popularBooksLabel.visibility = View.GONE
                    popularRecyclerView.visibility = View.GONE
                    recommendedLabel.visibility = View.GONE
                    recommendedRecyclerView.visibility = View.GONE
                } else {
                    emptyStateLayout.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
    }

    private fun updatePopularBooks() {
        if (books.isNotEmpty()) {
            val popular = recommendationHelper.getPopularBooks(books, 5)
            popularBooks.clear()
            popularBooks.addAll(popular)

            if (popularBooks.isNotEmpty()) {
                (popularRecyclerView.adapter as BookAdapter).updateList(popularBooks)
                popularBooksLabel.visibility = View.VISIBLE
                popularRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun updateRecommendedBooks() {
        if (books.isNotEmpty()) {
            val recommended = if (userInteractions.isEmpty()) {
                // New user - show initial suggestions
                recommendationHelper.getInitialSuggestions(books, 5)
            } else {
                // Existing user - personalized recommendations
                recommendationHelper.getRecommendationsForUser(userInteractions, books, 5)
            }

            recommendedBooks.clear()
            recommendedBooks.addAll(recommended)

            if (recommendedBooks.isNotEmpty()) {
                (recommendedRecyclerView.adapter as BookAdapter).updateList(recommendedBooks)
                recommendedLabel.visibility = View.VISIBLE
                recommendedRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun searchBooks(query: String) {
        showLoading(true)

        val searchResults = mutableListOf<Book>()

        db.collection("books")
            .get()
            .addOnSuccessListener { snapshot ->
                showLoading(false)

                snapshot.forEach { document ->
                    try {
                        val book = document.toObject(Book::class.java)
                        book.id = document.id

                        if (book.title.contains(query, ignoreCase = true) ||
                            book.author.contains(query, ignoreCase = true) ||
                            book.genre.contains(query, ignoreCase = true) ||
                            book.keywords.any { it.contains(query, ignoreCase = true) }) {
                            searchResults.add(book)
                        }
                    } catch (e: Exception) {
                    }
                }

                // Hide recommendation sections during search
                popularBooksLabel.visibility = View.GONE
                popularRecyclerView.visibility = View.GONE
                recommendedLabel.visibility = View.GONE
                recommendedRecyclerView.visibility = View.GONE

                bookAdapter.updateList(searchResults)

                if (searchResults.isEmpty()) {
                    emptyStateLayout.visibility = View.VISIBLE
                    emptyStateText.text = getString(R.string.member_empty_search, query)
                    recyclerView.visibility = View.GONE
                } else {
                    emptyStateLayout.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, R.string.member_error_loading, Toast.LENGTH_SHORT).show()
            }
    }

    private fun showMainContent() {
        popularBooksLabel.visibility = if (popularBooks.isNotEmpty()) View.VISIBLE else View.GONE
        popularRecyclerView.visibility = if (popularBooks.isNotEmpty()) View.VISIBLE else View.GONE
        recommendedLabel.visibility = if (recommendedBooks.isNotEmpty()) View.VISIBLE else View.GONE
        recommendedRecyclerView.visibility = if (recommendedBooks.isNotEmpty()) View.VISIBLE else View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.member_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logoutUser()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logoutUser() {
        auth.signOut()
        Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show()
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToLibrarianDashboard() {
        val intent = Intent(this, LibrarianDashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        searchView.isEnabled = !show
    }

    override fun onDestroy() {
        bookListener?.remove()
        super.onDestroy()
    }
}