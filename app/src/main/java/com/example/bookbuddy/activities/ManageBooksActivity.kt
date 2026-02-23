package com.example.bookbuddy.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
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

class ManageBooksActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var bookAdapter: BookAdapter
    private lateinit var searchView: SearchView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateText: TextView
    private lateinit var btnBack: Button

    private val books = mutableListOf<Book>()
    private var isLibrarian = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_books)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            finish()
            return
        }

        initializeViews()
        setupToolbar()
        checkUserRole()
        setupRecyclerView()
        setupSearchView()
        setupClickListeners()
        loadBooks()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerView)
        searchView = findViewById(R.id.searchView)
        progressBar = findViewById(R.id.progressBar)
        emptyStateText = findViewById(R.id.emptyStateText)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.manage_books_title)
    }

    private fun checkUserRole() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                isLibrarian = user?.role == "librarian"

                if (!isLibrarian) {
                    Toast.makeText(this, R.string.access_denied, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
    }

    private fun setupRecyclerView() {
        bookAdapter = BookAdapter(books) { book ->
            if (isLibrarian) {
                showEditDeleteDialog(book)
            }
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
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadBooks() {
        showLoading(true)

        db.collection("books")
            .get()
            .addOnSuccessListener { snapshot ->
                showLoading(false)

                books.clear()
                snapshot.forEach { document ->
                    val book = document.toObject(Book::class.java)
                    book.id = document.id
                    books.add(book)
                }
                bookAdapter.updateList(books)

                if (books.isEmpty()) {
                    emptyStateText.visibility = View.VISIBLE
                    emptyStateText.text = getString(R.string.no_books_available)
                    recyclerView.visibility = View.GONE
                } else {
                    emptyStateText.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, R.string.failed_to_load_books, Toast.LENGTH_SHORT).show()
            }
    }

    private fun searchBooks(query: String) {
        showLoading(true)

        val results = books.filter { book ->
            book.title.contains(query, ignoreCase = true) ||
                    book.author.contains(query, ignoreCase = true) ||
                    book.genre.contains(query, ignoreCase = true)
        }

        showLoading(false)
        bookAdapter.updateList(results)

        if (results.isEmpty()) {
            emptyStateText.visibility = View.VISIBLE
            emptyStateText.text = getString(R.string.no_books_matching, query)
            recyclerView.visibility = View.GONE
        } else {
            emptyStateText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showEditDeleteDialog(book: Book) {
        val options = arrayOf("Edit Book", "Delete Book")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(book.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editBook(book)
                    1 -> deleteBook(book)
                }
            }
            .show()
    }

    private fun editBook(book: Book) {
        val intent = Intent(this, EditBookActivity::class.java)
        intent.putExtra("bookId", book.id)
        startActivity(intent)
    }

    private fun deleteBook(book: Book) {
        db.collection("borrowRecords")
            .whereEqualTo("bookId", book.id)
            .whereEqualTo("returnedAt", null)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    confirmDelete(book)
                } else {
                    Toast.makeText(
                        this,
                        R.string.cannot_delete_borrowed,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun confirmDelete(book: Book) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.delete_book_title)
            .setMessage(getString(R.string.delete_book_confirmation, book.title))
            .setPositiveButton("Delete") { _, _ ->
                performDelete(book)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDelete(book: Book) {
        showLoading(true)

        db.collection("books").document(book.id)
            .delete()
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, R.string.book_deleted_success, Toast.LENGTH_SHORT).show()
                loadBooks()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, getString(R.string.error_deleting_book, e.message), Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            progressBar.visibility = View.VISIBLE
            searchView.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            searchView.isEnabled = true
        }
    }
}