package com.example.bookbuddy.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide  // 👈 ADD THIS IMPORT
import com.example.bookbuddy.R
import com.example.bookbuddy.models.Book
import com.example.bookbuddy.models.BookInteraction
import com.example.bookbuddy.models.BorrowRecord
import com.example.bookbuddy.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import java.util.Date

class BookDetailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var book: Book

    // UI Components
    private lateinit var bookCover: ImageView  // 👈 NEW
    private lateinit var noImageText: TextView // 👈 NEW
    private lateinit var bookTitle: TextView
    private lateinit var bookAuthor: TextView
    private lateinit var bookGenre: TextView
    private lateinit var bookDescription: TextView
    private lateinit var bookSummary: TextView
    private lateinit var bookAvailability: TextView
    private lateinit var borrowButton: Button
    private lateinit var returnButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var backButton: Button

    private var isUserLibrarian = false
    private var hasUserBorrowed = false
    private var currentBorrowRecordId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_detail)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        initializeViews()

        // Check if user is logged in
        if (auth.currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val bookId = intent.getStringExtra("bookId") ?: return
        val source = intent.getStringExtra("source") ?: ""

        // Check if this is from MyBooks (show return button)
        if (source == "mybooks") {
            hasUserBorrowed = true
        }

        loadBookDetails(bookId)
        checkUserRole()
        checkIfUserBorrowed(bookId)

        setupClickListeners()
    }

    private fun initializeViews() {
        bookCover = findViewById(R.id.bookCover)           // 👈 NEW
        noImageText = findViewById(R.id.noImageText)       // 👈 NEW
        bookTitle = findViewById(R.id.bookTitle)
        bookAuthor = findViewById(R.id.bookAuthor)
        bookGenre = findViewById(R.id.bookGenre)
        bookDescription = findViewById(R.id.bookDescription)
        bookSummary = findViewById(R.id.bookSummary)
        bookAvailability = findViewById(R.id.bookAvailability)
        borrowButton = findViewById(R.id.borrowButton)
        returnButton = findViewById(R.id.returnButton)
        progressBar = findViewById(R.id.progressBar)
        backButton = findViewById(R.id.backButton)
    }

    private fun setupClickListeners() {
        borrowButton.setOnClickListener {
            borrowBook()
        }

        returnButton.setOnClickListener {
            returnBook()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun loadBookDetails(bookId: String) {
        showLoading(true)

        db.collection("books").document(bookId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)

                if (!document.exists()) {
                    Toast.makeText(this, "Book not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                book = document.toObject(Book::class.java)!!
                book.id = document.id

                // Display book details
                displayBookDetails()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error loading book: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayBookDetails() {
        bookTitle.text = book.title
        bookAuthor.text = "By ${book.author}"
        bookGenre.text = "Genre: ${book.genre}"
        bookDescription.text = book.description.ifEmpty { "No description available" }
        bookSummary.text = book.summary.ifEmpty { generateSummary() }

        val availabilityText = "Available: ${book.availableCopies}/${book.totalCopies}"
        bookAvailability.text = availabilityText

        // 👇 NEW: Load book cover image
        loadBookCover()

        // Set color based on availability
        if (book.availableCopies > 0) {
            bookAvailability.setTextColor(getColor(R.color.purple_500))
        } else {
            bookAvailability.setTextColor(getColor(android.R.color.holo_red_dark))
        }

        // Update button states
        updateButtonStates()

        // Record view interaction
        recordInteraction("view")
    }

    // 👇 NEW: Function to load book cover image
    private fun loadBookCover() {
        if (book.coverUrl.isNotEmpty()) {
            // Try to load image from URL
            Glide.with(this)
                .load(book.coverUrl)
                .placeholder(R.drawable.ic_book_placeholder)  // While loading
                .error(R.drawable.ic_book_error)              // If error
                .into(bookCover)

            bookCover.visibility = View.VISIBLE
            noImageText.visibility = View.GONE
        } else {
            // No image URL - show placeholder
            bookCover.setImageResource(R.drawable.ic_book_placeholder)
            bookCover.visibility = View.VISIBLE
            noImageText.visibility = View.VISIBLE
            noImageText.text = "No cover image available"
        }
    }

    private fun generateSummary(): String {
        return when {
            book.description.length > 100 -> {
                val sentences = book.description.split("[.!?]".toRegex())
                if (sentences.size >= 2) {
                    "${sentences[0].trim()}. ${sentences[1].trim()}."
                } else {
                    book.description.take(150) + "..."
                }
            }
            else -> "A ${book.genre.lowercase()} book by ${book.author}."
        }
    }

    private fun checkUserRole() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                isUserLibrarian = user?.role == "librarian"
                updateButtonStates()
            }
    }

    private fun checkIfUserBorrowed(bookId: String) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("borrowRecords")
            .whereEqualTo("userId", userId)
            .whereEqualTo("bookId", bookId)
            .whereEqualTo("returnedAt", null)
            .get()
            .addOnSuccessListener { snapshot ->
                hasUserBorrowed = !snapshot.isEmpty
                if (hasUserBorrowed) {
                    currentBorrowRecordId = snapshot.documents.firstOrNull()?.id
                }
                updateButtonStates()
            }
    }

    private fun updateButtonStates() {
        if (hasUserBorrowed) {
            // User has borrowed this book - show Return button
            borrowButton.visibility = View.GONE
            returnButton.visibility = View.VISIBLE
            returnButton.isEnabled = true
        } else {
            // User hasn't borrowed - show Borrow button if available
            borrowButton.visibility = View.VISIBLE
            returnButton.visibility = View.GONE
            borrowButton.isEnabled = book.availableCopies > 0 && !isUserLibrarian
            borrowButton.text = if (book.availableCopies > 0) "Borrow Book" else "Not Available"
        }

        // Librarians see different options
        if (isUserLibrarian) {
            borrowButton.visibility = View.GONE
            // Could show Edit/Delete buttons for librarians
        }
    }

    private fun borrowBook() {
        val userId = auth.currentUser?.uid ?: return

        if (book.availableCopies <= 0) {
            Toast.makeText(this, "Book not available", Toast.LENGTH_SHORT).show()
            return
        }

        if (hasUserBorrowed) {
            Toast.makeText(this, "You already borrowed this book", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        // Create borrow record
        val borrowRecord = BorrowRecord(
            bookId = book.id,
            bookTitle = book.title,
            userId = userId,
            userName = auth.currentUser?.displayName ?: "User",
            borrowedAt = Timestamp.now(),
            dueDate = Timestamp(Date(System.currentTimeMillis() + (14 * 24 * 60 * 60 * 1000))), // 14 days
            status = "BORROWED"
        )

        // Run transaction to update both book and create borrow record
        db.runTransaction { transaction ->
            // Update book availability
            val bookRef = db.collection("books").document(book.id)
            transaction.update(bookRef, "availableCopies", book.availableCopies - 1)
            transaction.update(bookRef, "timesBorrowed", book.timesBorrowed + 1)

            // Add borrow record
            val borrowRef = db.collection("borrowRecords").document()
            transaction.set(borrowRef, borrowRecord)

        }.addOnSuccessListener {
            // Update user's borrowed books list
            db.collection("users").document(userId)
                .update("borrowedBooks", FieldValue.arrayUnion(book.id))
                .addOnSuccessListener {
                    showLoading(false)
                    recordInteraction("borrow")
                    Toast.makeText(this, "Book borrowed successfully! Due in 14 days", Toast.LENGTH_LONG).show()

                    // Update local state
                    hasUserBorrowed = true
                    book.availableCopies--
                    updateButtonStates()
                    displayBookDetails()
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(this, "Error updating user: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener { e ->
            showLoading(false)
            Toast.makeText(this, "Error borrowing book: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun returnBook() {
        val userId = auth.currentUser?.uid ?: return

        showLoading(true)

        // Find the active borrow record
        db.collection("borrowRecords")
            .whereEqualTo("userId", userId)
            .whereEqualTo("bookId", book.id)
            .whereEqualTo("returnedAt", null)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.isNotEmpty()) {
                    val record = snapshot.documents[0]
                    val recordId = record.id

                    // Run transaction to update both record and book
                    db.runTransaction { transaction ->
                        // Update borrow record
                        val recordRef = db.collection("borrowRecords").document(recordId)
                        transaction.update(recordRef, "returnedAt", Timestamp.now())
                        transaction.update(recordRef, "status", "RETURNED")

                        // Update book availability
                        val bookRef = db.collection("books").document(book.id)
                        transaction.update(bookRef, "availableCopies", book.availableCopies + 1)

                    }.addOnSuccessListener {
                        // Remove from user's borrowed books
                        db.collection("users").document(userId)
                            .update("borrowedBooks", FieldValue.arrayRemove(book.id))
                            .addOnSuccessListener {
                                showLoading(false)
                                recordInteraction("return")
                                Toast.makeText(this, "Book returned! Thank you!", Toast.LENGTH_SHORT).show()

                                // Update local state
                                hasUserBorrowed = false
                                book.availableCopies++
                                updateButtonStates()
                                displayBookDetails()
                            }
                            .addOnFailureListener { e ->
                                showLoading(false)
                                Toast.makeText(this, "Error updating user: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }.addOnFailureListener { e ->
                        showLoading(false)
                        Toast.makeText(this, "Error returning book: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    showLoading(false)
                    Toast.makeText(this, "No active borrow record found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error finding borrow record: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun recordInteraction(action: String) {
        val userId = auth.currentUser?.uid ?: return

        val interaction = BookInteraction(
            bookId = book.id,
            bookTitle = book.title,
            bookAuthor = book.author,
            bookGenre = book.genre,
            action = action,
            timestamp = System.currentTimeMillis()
        )

        db.collection("users").document(userId)
            .collection("interactions")
            .add(interaction)
            .addOnFailureListener { e ->
                // Just log, don't show to user
                e.printStackTrace()
            }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        borrowButton.isEnabled = !show
        returnButton.isEnabled = !show
    }
}