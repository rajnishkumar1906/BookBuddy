package com.example.bookbuddy.activities.shared

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.bookbuddy.R
import com.example.bookbuddy.activities.auth.LoginActivity
import com.example.bookbuddy.databinding.ActivityBookDetailBinding
import com.example.bookbuddy.models.Book
import com.example.bookbuddy.models.BookInteraction
import com.example.bookbuddy.models.BorrowRecord
import com.example.bookbuddy.models.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class BookDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookDetailBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var book: Book

    private var isUserLibrarian = false
    private var hasUserBorrowed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val bookId = intent.getStringExtra("bookId") ?: return
        val source = intent.getStringExtra("source") ?: ""

        if (source == "mybooks") {
            hasUserBorrowed = true
        }

        loadBookDetails(bookId)
        checkUserRole()
        checkIfUserBorrowed(bookId)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.borrowButton.setOnClickListener { borrowBook() }
        binding.returnButton.setOnClickListener { returnBook() }
        binding.backButton.setOnClickListener { finish() }
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
                displayBookDetails()
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, "Error loading book", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayBookDetails() {
        binding.bookTitle.text = book.title
        binding.bookAuthor.text = "By ${book.author}"
        binding.bookGenre.text = "Genre: ${book.genre}"
        binding.bookDescription.text = book.description.ifEmpty { "No description available" }
        binding.bookSummary.text = book.summary.ifEmpty { "No summary available" }
        binding.bookAiSummary.text = book.aiSummary.ifEmpty { "AI summary not available" }

        binding.bookIsbn.text = "ISBN: ${book.isbn.ifEmpty { "N/A" }}"
        binding.bookPublisher.text = "Publisher: ${book.publisher.ifEmpty { "N/A" }}"
        binding.bookPages.text = "Pages: ${book.pageCount}"
        binding.bookLanguage.text = "Language: ${book.language}"

        val availabilityText = "Available: ${book.availableCopies}/${book.totalCopies}"
        binding.bookAvailability.text = availabilityText

        loadBookCover()

        if (book.availableCopies > 0) {
            binding.bookAvailability.setTextColor(getColor(R.color.success))
        } else {
            binding.bookAvailability.setTextColor(getColor(R.color.error))
        }

        updateButtonStates()
        recordInteraction("view")
    }

    private fun loadBookCover() {
        if (book.coverUrl.isNotEmpty()) {
            Glide.with(this)
                .load(book.coverUrl)
                .placeholder(R.drawable.ic_book_placeholder)
                .error(R.drawable.ic_book_error)
                .into(binding.bookCover)
            binding.bookCover.visibility = View.VISIBLE
            binding.noImageText.visibility = View.GONE
        } else {
            binding.bookCover.setImageResource(R.drawable.ic_book_placeholder)
            binding.bookCover.visibility = View.VISIBLE
            binding.noImageText.visibility = View.VISIBLE
            binding.noImageText.text = "No cover image available"
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
                updateButtonStates()
            }
    }

    private fun updateButtonStates() {
        if (isUserLibrarian) {
            binding.borrowButton.visibility = View.GONE
            binding.returnButton.visibility = View.GONE
        } else {
            if (hasUserBorrowed) {
                binding.borrowButton.visibility = View.GONE
                binding.returnButton.visibility = View.VISIBLE
                binding.returnButton.isEnabled = true
            } else {
                binding.borrowButton.visibility = View.VISIBLE
                binding.returnButton.visibility = View.GONE
                binding.borrowButton.isEnabled = book.availableCopies > 0
                binding.borrowButton.text = if (book.availableCopies > 0) "Borrow Book" else "Not Available"
            }
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

        val borrowRecord = BorrowRecord(
            bookId = book.id,
            bookTitle = book.title,
            userId = userId,
            userName = auth.currentUser?.displayName ?: "User",
            borrowedAt = Timestamp.now(),
            dueDate = Timestamp(Date(System.currentTimeMillis() + (14 * 24 * 60 * 60 * 1000))),
            status = "BORROWED"
        )

        db.runTransaction { transaction ->
            val bookRef = db.collection("books").document(book.id)
            transaction.update(bookRef, "availableCopies", book.availableCopies - 1)
            transaction.update(bookRef, "timesBorrowed", book.timesBorrowed + 1)

            val borrowRef = db.collection("borrowRecords").document()
            transaction.set(borrowRef, borrowRecord)
        }.addOnSuccessListener {
            db.collection("users").document(userId)
                .update("borrowedBooks", FieldValue.arrayUnion(book.id))
                .addOnSuccessListener {
                    showLoading(false)
                    recordInteraction("borrow")
                    Toast.makeText(this, "✅ Book borrowed successfully! Due in 14 days", Toast.LENGTH_LONG).show()

                    hasUserBorrowed = true
                    book.availableCopies--
                    updateButtonStates()
                    displayBookDetails()
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener { e ->
            showLoading(false)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun returnBook() {
        val userId = auth.currentUser?.uid ?: return

        showLoading(true)

        db.collection("borrowRecords")
            .whereEqualTo("userId", userId)
            .whereEqualTo("bookId", book.id)
            .whereEqualTo("returnedAt", null)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.isNotEmpty()) {
                    val record = snapshot.documents[0]
                    val recordId = record.id

                    db.runTransaction { transaction ->
                        val recordRef = db.collection("borrowRecords").document(recordId)
                        transaction.update(recordRef, "returnedAt", Timestamp.now())
                        transaction.update(recordRef, "status", "RETURNED")

                        val bookRef = db.collection("books").document(book.id)
                        transaction.update(bookRef, "availableCopies", book.availableCopies + 1)

                    }.addOnSuccessListener {
                        db.collection("users").document(userId)
                            .update("borrowedBooks", FieldValue.arrayRemove(book.id))
                            .addOnSuccessListener {
                                showLoading(false)
                                recordInteraction("return")
                                Toast.makeText(this, "✅ Book returned! Thank you!", Toast.LENGTH_SHORT).show()

                                hasUserBorrowed = false
                                book.availableCopies++
                                updateButtonStates()
                                displayBookDetails()
                            }
                            .addOnFailureListener { e ->
                                showLoading(false)
                                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }.addOnFailureListener { e ->
                        showLoading(false)
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    showLoading(false)
                    Toast.makeText(this, "No active borrow record found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.borrowButton.isEnabled = !show
        binding.returnButton.isEnabled = !show
    }
}