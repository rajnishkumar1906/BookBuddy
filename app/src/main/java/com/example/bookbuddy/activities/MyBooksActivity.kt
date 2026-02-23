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

    // UI Components
    private lateinit var recyclerView: RecyclerView
    private lateinit var bookAdapter: BookAdapter
    private lateinit var emptyStateText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var backButton: Button
    private lateinit var tabLayout: TabLayout
    private lateinit var borrowedCountText: TextView
    private lateinit var returnedCountText: TextView

    // Data
    private val currentlyBorrowed = mutableListOf<BorrowedBookItem>()
    private val borrowingHistory = mutableListOf<BorrowedBookItem>()
    private var currentTab = 0 // 0 = Currently Borrowed, 1 = History
    private var borrowListener: ListenerRegistration? = null

    // Date formatter
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    data class BorrowedBookItem(
        val book: Book,
        val borrowRecord: BorrowRecord,
        val daysRemaining: Int = 0,
        val isOverdue: Boolean = false
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_my_books)

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

            // Check if user is logged in
            try {
                if (auth.currentUser == null) {
                    Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    return
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error checking login status", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Setup components with try-catch
            try {
                setupRecyclerView()
                setupTabLayout()
                setupClickListeners()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to setup UI: ${e.message}", Toast.LENGTH_SHORT).show()
            }

            // Load user's books
            loadUserBorrowedBooks()

        } catch (e: Exception) {
            Toast.makeText(this, "App error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        try {
            recyclerView = findViewById(R.id.recyclerView)
            emptyStateText = findViewById(R.id.emptyStateText)
            progressBar = findViewById(R.id.progressBar)
            backButton = findViewById(R.id.backButton)
            tabLayout = findViewById(R.id.tabLayout)
            borrowedCountText = findViewById(R.id.borrowedCountText)
            returnedCountText = findViewById(R.id.returnedCountText)
        } catch (e: Exception) {
            throw Exception("View initialization failed: ${e.message}")
        }
    }

    private fun setupRecyclerView() {
        try {
            bookAdapter = BookAdapter(emptyList()) { book ->
                try {
                    val intent = Intent(this, BookDetailActivity::class.java)
                    intent.putExtra("bookId", book.id)
                    intent.putExtra("bookTitle", book.title)
                    intent.putExtra("source", "mybooks")
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

    private fun setupTabLayout() {
        try {
            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    try {
                        currentTab = tab?.position ?: 0
                        updateDisplayedBooks()
                    } catch (e: Exception) {
                        Toast.makeText(this@MyBooksActivity, "Tab error", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to setup tabs: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        try {
            backButton.setOnClickListener {
                try {
                    finish()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to setup buttons", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserBorrowedBooks() {
        try {
            val userId = try {
                auth.currentUser?.uid ?: return
            } catch (e: Exception) {
                return
            }

            showLoading(true)

            // Listen for real-time updates to borrow records
            borrowListener = db.collection("borrowRecords")
                .whereEqualTo("userId", userId)
                .orderBy("borrowedAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    try {
                        if (error != null) {
                            showLoading(false)
                            Toast.makeText(this@MyBooksActivity, "Error loading books: ${error.message}", Toast.LENGTH_SHORT).show()
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            currentlyBorrowed.clear()
                            borrowingHistory.clear()

                            val bookIds = mutableListOf<String>()

                            snapshot.documents.forEach { document ->
                                try {
                                    val record = document.toObject(BorrowRecord::class.java)
                                    if (record != null) {
                                        record.id = document.id
                                        bookIds.add(record.bookId)

                                        // Calculate days remaining
                                        val daysRemaining = calculateDaysRemaining(record.dueDate)
                                        val isOverdue = daysRemaining < 0 && record.returnedAt == null

                                        // Create placeholder book
                                        val placeholderBook = try {
                                            Book(record.bookId, record.bookTitle)
                                        } catch (e: Exception) {
                                            Book() // Fallback to empty book
                                        }

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
                                } catch (e: Exception) {
                                    // Skip invalid records
                                }
                            }

                            // Load full book details
                            if (bookIds.isNotEmpty()) {
                                loadBookDetails(bookIds.distinct())
                            } else {
                                updateDisplayedBooks()
                                updateCounts()
                                showLoading(false)
                            }
                        }
                    } catch (e: Exception) {
                        showLoading(false)
                        Toast.makeText(this@MyBooksActivity, "Error processing borrowed books", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(this, "Failed to load borrowed books: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadBookDetails(bookIds: List<String>) {
        try {
            if (bookIds.isEmpty()) {
                updateDisplayedBooks()
                showLoading(false)
                return
            }

            // Create a map for quick lookup
            val bookMap = mutableMapOf<String, Book>()
            var loadedCount = 0

            bookIds.forEach { bookId ->
                try {
                    db.collection("books").document(bookId)
                        .get()
                        .addOnSuccessListener { document ->
                            try {
                                val book = document.toObject(Book::class.java)
                                if (book != null) {
                                    book.id = document.id
                                    bookMap[bookId] = book
                                }
                            } catch (e: Exception) {
                                // Skip invalid book
                            } finally {
                                loadedCount++
                                if (loadedCount == bookIds.size) {
                                    updateBorrowedItemsWithBooks(bookMap)
                                    updateDisplayedBooks()
                                    updateCounts()
                                    showLoading(false)
                                }
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
                } catch (e: Exception) {
                    loadedCount++
                    if (loadedCount == bookIds.size) {
                        updateDisplayedBooks()
                        updateCounts()
                        showLoading(false)
                    }
                }
            }
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(this, "Error loading book details: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateBorrowedItemsWithBooks(bookMap: Map<String, Book>) {
        try {
            currentlyBorrowed.forEach { item ->
                try {
                    bookMap[item.book.id]?.let { book ->
                        val updatedBook = item.book.copy(
                            author = book.author,
                            genre = book.genre,
                            coverUrl = book.coverUrl,
                            description = book.description,
                            availableCopies = book.availableCopies,
                            totalCopies = book.totalCopies
                        )
                        val index = currentlyBorrowed.indexOf(item)
                        if (index != -1) {
                            currentlyBorrowed[index] = item.copy(book = updatedBook)
                        }
                    }
                } catch (e: Exception) {
                    // Skip update for this item
                }
            }

            borrowingHistory.forEach { item ->
                try {
                    bookMap[item.book.id]?.let { book ->
                        val updatedBook = item.book.copy(
                            author = book.author,
                            genre = book.genre,
                            coverUrl = book.coverUrl,
                            description = book.description,
                            availableCopies = book.availableCopies,
                            totalCopies = book.totalCopies
                        )
                        val index = borrowingHistory.indexOf(item)
                        if (index != -1) {
                            borrowingHistory[index] = item.copy(book = updatedBook)
                        }
                    }
                } catch (e: Exception) {
                    // Skip update for this item
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error updating book details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateDisplayedBooks() {
        try {
            val booksToShow = if (currentTab == 0) {
                currentlyBorrowed.map { it.book }
            } else {
                borrowingHistory.map { it.book }
            }

            // Update adapter with the books
            bookAdapter.updateList(booksToShow)

            // Show/hide empty state
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
        } catch (e: Exception) {
            Toast.makeText(this, "Error updating display", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCounts() {
        try {
            borrowedCountText.text = "${currentlyBorrowed.size} Currently Borrowed"
            returnedCountText.text = "${borrowingHistory.size} Returned"
        } catch (e: Exception) {
            // Ignore count update errors
        }
    }

    private fun calculateDaysRemaining(dueDate: com.google.firebase.Timestamp): Int {
        return try {
            val currentTime = System.currentTimeMillis()
            val dueTime = dueDate.toDate().time
            val diffInMillis = dueTime - currentTime
            (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) {
            0 // Return 0 if calculation fails
        }
    }

    fun returnBook(borrowedItem: BorrowedBookItem) {
        try {
            val userId = try {
                auth.currentUser?.uid ?: return
            } catch (e: Exception) {
                Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                return
            }

            showLoading(true)

            // Run transaction to update both book and borrow record
            db.runTransaction { transaction ->
                try {
                    // Update borrow record
                    val recordRef = db.collection("borrowRecords").document(borrowedItem.borrowRecord.id)
                    transaction.update(recordRef, "returnedAt", com.google.firebase.Timestamp(Date()))
                    transaction.update(recordRef, "status", "RETURNED")

                    // Update book available copies
                    val bookRef = db.collection("books").document(borrowedItem.book.id)
                    val bookSnapshot = transaction.get(bookRef)
                    val currentAvailable = bookSnapshot.getLong("availableCopies") ?: 0
                    transaction.update(bookRef, "availableCopies", currentAvailable + 1)
                } catch (e: Exception) {
                    throw Exception("Transaction failed: ${e.message}")
                }
            }.addOnSuccessListener {
                try {
                    showLoading(false)
                    Toast.makeText(this, "Book returned successfully!", Toast.LENGTH_SHORT).show()

                    // Remove from currently borrowed list
                    val itemToMove = currentlyBorrowed.find { it.book.id == borrowedItem.book.id }
                    itemToMove?.let {
                        currentlyBorrowed.remove(it)
                        borrowingHistory.add(it)
                    }
                    updateDisplayedBooks()
                    updateCounts()
                } catch (e: Exception) {
                    showLoading(false)
                    Toast.makeText(this, "Error updating UI", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                try {
                    showLoading(false)
                    Toast.makeText(this, "Error returning book: ${e.message}", Toast.LENGTH_SHORT).show()
                } catch (ex: Exception) {
                    // Ignore
                }
            }
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(this, "Return error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun renewBook(borrowedItem: BorrowedBookItem) {
        try {
            val newDueDate = Date(System.currentTimeMillis() + (14 * 24 * 60 * 60 * 1000)) // 14 days from now

            showLoading(true)

            db.collection("borrowRecords").document(borrowedItem.borrowRecord.id)
                .update("dueDate", com.google.firebase.Timestamp(newDueDate))
                .addOnSuccessListener {
                    try {
                        showLoading(false)
                        Toast.makeText(this, "Book renewed! Due in 14 days", Toast.LENGTH_SHORT).show()
                        loadUserBorrowedBooks() // Reload to show updated due date
                    } catch (e: Exception) {
                        showLoading(false)
                    }
                }
                .addOnFailureListener { e ->
                    try {
                        showLoading(false)
                        Toast.makeText(this, "Error renewing: ${e.message}", Toast.LENGTH_SHORT).show()
                    } catch (ex: Exception) {
                        // Ignore
                    }
                }
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(this, "Renew error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(show: Boolean) {
        try {
            if (show) {
                progressBar.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                emptyStateText.visibility = View.GONE
                backButton.isEnabled = false
            } else {
                progressBar.visibility = View.GONE
                backButton.isEnabled = true
                updateDisplayedBooks()
            }
        } catch (e: Exception) {
            // Ignore loading UI errors
        }
    }

    override fun onDestroy() {
        try {
            borrowListener?.remove()
        } catch (e: Exception) {
            // Ignore listener cleanup errors
        }
        super.onDestroy()
    }
}