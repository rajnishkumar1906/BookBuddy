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
import com.example.bookbuddy.models.Book
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

    private var bookListener: ListenerRegistration? = null
    private val books = mutableListOf<Book>()
    private var currentUserName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            navigateToLogin()
            return
        }

        initializeViews()
        setupToolbar()
        getUserDetails()
        setupRecyclerView()
        setupSearchView()
        setupClickListeners()
        loadBooks()
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
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.member_dashboard_title)
    }

    private fun setupRecyclerView() {
        bookAdapter = BookAdapter(books) { book ->
            val intent = Intent(this, BookDetailActivity::class.java)
            intent.putExtra("bookId", book.id)
            intent.putExtra("source", "member")
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = bookAdapter
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchBooks(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    loadBooks()
                } else {
                    searchBooks(newText)
                }
                return true
            }
        })
    }

    private fun setupClickListeners() {
        btnBrowseBooks.setOnClickListener {
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

                if (books.isEmpty()) {
                    emptyStateLayout.visibility = View.VISIBLE
                    emptyStateText.text = getString(R.string.member_empty_books)
                    recyclerView.visibility = View.GONE
                } else {
                    emptyStateLayout.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
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
                            book.genre.contains(query, ignoreCase = true)) {
                            searchResults.add(book)
                        }
                    } catch (e: Exception) {
                    }
                }

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