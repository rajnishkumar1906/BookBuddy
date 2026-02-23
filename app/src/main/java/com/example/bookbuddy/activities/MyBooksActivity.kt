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
import com.example.bookbuddy.models.BorrowRecord
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyBooksActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var recyclerView: RecyclerView
    private lateinit var bookAdapter: BookAdapter
    private lateinit var emptyStateText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var backButton: Button
    private lateinit var tabLayout: TabLayout
    private lateinit var borrowedCountText: TextView
    private lateinit var returnedCountText: TextView

    private val currentlyBorrowed = mutableListOf<BorrowedBookItem>()
    private val borrowingHistory = mutableListOf<BorrowedBookItem>()
    private var currentTab = 0
    private var borrowListener: ListenerRegistration? = null

    data class BorrowedBookItem(
        val book: Book,
        val borrowRecord: BorrowRecord,
        val daysRemaining: Int = 0,
        val isOverdue: Boolean = false
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_books)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initializeViews()
        setupRecyclerView()
        setupTabLayout()
        setupClickListeners()
        loadUserBorrowedBooks()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerView)
        emptyStateText = findViewById(R.id.emptyStateText)
        progressBar = findViewById(R.id.progressBar)
        backButton = findViewById(R.id.backButton)
        tabLayout = findViewById(R.id.tabLayout)
        borrowedCountText = findViewById(R.id.borrowedCountText)
        returnedCountText = findViewById(R.id.returnedCountText)
    }

    private fun setupRecyclerView() {
        bookAdapter = BookAdapter(emptyList()) { book ->
            val intent = Intent(this, BookDetailActivity::class.java)
            intent.putExtra("bookId", book.id)
            intent.putExtra("source", "mybooks")
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = bookAdapter
    }

    private fun setupTabLayout() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                updateDisplayedBooks()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun loadUserBorrowedBooks() {
        val userId = auth.currentUser?.uid ?: return

        showLoading(true)

        borrowListener = db.collection("borrowRecords")
            .whereEqualTo("userId", userId)
            .orderBy("borrowedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    showLoading(false)
                    Toast.makeText(this@MyBooksActivity, "Error loading books", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    currentlyBorrowed.clear()
                    borrowingHistory.clear()

                    val bookIds = mutableListOf<String>()

                    snapshot.documents.forEach { document ->
                        val record = document.toObject(BorrowRecord::class.java)
                        if (record != null) {
                            record.id = document.id
                            bookIds.add(record.bookId)

                            val daysRemaining = calculateDaysRemaining(record.dueDate)
                            val isOverdue = daysRemaining < 0 && record.returnedAt == null

                            val placeholderBook = Book(record.bookId, record.bookTitle)

                            val item = BorrowedBookItem(
                                book = placeholderBook,
                                borrowRecord = record,
                                daysRemaining = daysRemaining,
                                isOverdue = isOverdue
                            )

                            if (record.returnedAt == null) {
                                currentlyBorrowed.add(item)
                            } else {
                                borrowingHistory.add(item)
                            }
                        }
                    }

                    if (bookIds.isNotEmpty()) {
                        loadBookDetails(bookIds.distinct())
                    } else {
                        updateDisplayedBooks()
                        updateCounts()
                        showLoading(false)
                    }
                }
            }
    }

    private fun loadBookDetails(bookIds: List<String>) {
        if (bookIds.isEmpty()) {
            updateDisplayedBooks()
            showLoading(false)
            return
        }

        val bookMap = mutableMapOf<String, Book>()
        var loadedCount = 0

        bookIds.forEach { bookId ->
            db.collection("books").document(bookId)
                .get()
                .addOnSuccessListener { document ->
                    val book = document.toObject(Book::class.java)
                    if (book != null) {
                        book.id = document.id
                        bookMap[bookId] = book
                    }
                    loadedCount++
                    if (loadedCount == bookIds.size) {
                        updateBorrowedItemsWithBooks(bookMap)
                        updateDisplayedBooks()
                        updateCounts()
                        showLoading(false)
                    }
                }
                .addOnFailureListener {
                    loadedCount++
                    if (loadedCount == bookIds.size) {
                        updateDisplayedBooks()
                        updateCounts()
                        showLoading(false)
                    }
                }
        }
    }

    private fun updateBorrowedItemsWithBooks(bookMap: Map<String, Book>) {
        currentlyBorrowed.forEachIndexed { index, item ->
            bookMap[item.book.id]?.let { book ->
                val updatedBook = item.book.copy(
                    author = book.author,
                    genre = book.genre,
                    coverUrl = book.coverUrl,
                    description = book.description,
                    availableCopies = book.availableCopies,
                    totalCopies = book.totalCopies
                )
                currentlyBorrowed[index] = item.copy(book = updatedBook)
            }
        }

        borrowingHistory.forEachIndexed { index, item ->
            bookMap[item.book.id]?.let { book ->
                val updatedBook = item.book.copy(
                    author = book.author,
                    genre = book.genre,
                    coverUrl = book.coverUrl,
                    description = book.description,
                    availableCopies = book.availableCopies,
                    totalCopies = book.totalCopies
                )
                borrowingHistory[index] = item.copy(book = updatedBook)
            }
        }
    }

    private fun updateDisplayedBooks() {
        val booksToShow = if (currentTab == 0) {
            currentlyBorrowed.map { it.book }
        } else {
            borrowingHistory.map { it.book }
        }

        bookAdapter.updateList(booksToShow)

        if (booksToShow.isEmpty()) {
            emptyStateText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE

            emptyStateText.text = if (currentTab == 0) {
                "You haven't borrowed any books yet\n\nBrowse books and borrow your first one!"
            } else {
                "No borrowing history yet\n\nBooks you return will appear here"
            }
        } else {
            emptyStateText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun updateCounts() {
        borrowedCountText.text = "${currentlyBorrowed.size} Currently Borrowed"
        returnedCountText.text = "${borrowingHistory.size} Returned"
    }

    private fun calculateDaysRemaining(dueDate: com.google.firebase.Timestamp): Int {
        return try {
            val currentTime = System.currentTimeMillis()
            val dueTime = dueDate.toDate().time
            val diffInMillis = dueTime - currentTime
            (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) {
            0
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        backButton.isEnabled = !show
    }

    override fun onDestroy() {
        borrowListener?.remove()
        super.onDestroy()
    }
}