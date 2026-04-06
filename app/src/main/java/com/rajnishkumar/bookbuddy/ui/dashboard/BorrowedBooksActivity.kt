package com.rajnishkumar.bookbuddy.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.rajnishkumar.bookbuddy.R
import com.rajnishkumar.bookbuddy.adapters.BorrowedBookAdapter
import com.rajnishkumar.bookbuddy.models.BorrowRecord
import com.rajnishkumar.bookbuddy.ui.book.BookDetailActivity
import com.rajnishkumar.bookbuddy.ui.sensor.BaseActivity

class BorrowedBooksActivity : BaseActivity() {

    private lateinit var rvBorrowed: RecyclerView
    private lateinit var emptyStateContainer: View
    private lateinit var btnBack: ImageButton
    private lateinit var adapter: BorrowedBookAdapter

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_borrowed_books)

        initViews()
        setupRecyclerView()
        fetchBorrowedBooks()
    }

    private fun initViews() {
        rvBorrowed = findViewById(R.id.rvBorrowedBooks)
        emptyStateContainer = findViewById(R.id.emptyStateContainer)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = BorrowedBookAdapter(
            onReturnClick = { record -> returnBook(record) },
            onViewDetailsClick = { record ->
                val intent = Intent(this, BookDetailActivity::class.java)
                intent.putExtra("BOOK_ID", record.bookId)
                startActivity(intent)
            }
        )

        rvBorrowed.layoutManager = LinearLayoutManager(this)
        rvBorrowed.adapter = adapter
    }

    private fun fetchBorrowedBooks() {
        val userId = auth.currentUser?.uid ?: return

        // Show loading state if needed (optional)

        database.child("borrowRecords")
            .orderByChild("userId")
            .equalTo(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val borrowedRecords = mutableListOf<BorrowRecord>()

                for (recordSnap in snapshot.children) {
                    val record = recordSnap.getValue(BorrowRecord::class.java)
                    if (record?.status == "BORROWED") {
                        borrowedRecords.add(record)
                    }
                }

                if (borrowedRecords.isNotEmpty()) {
                    adapter.setRecords(borrowedRecords)
                    rvBorrowed.visibility = View.VISIBLE
                    emptyStateContainer.visibility = View.GONE
                } else {
                    rvBorrowed.visibility = View.GONE
                    emptyStateContainer.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { exception ->
                Log.e("BorrowedBooksActivity", "Failed to fetch borrowed books", exception)
                Toast.makeText(this, "Failed to load borrowed books", Toast.LENGTH_SHORT).show()
                rvBorrowed.visibility = View.GONE
                emptyStateContainer.visibility = View.VISIBLE
            }
    }

    private fun returnBook(record: BorrowRecord) {
        val updates = hashMapOf<String, Any>(
            "status" to "RETURNED",
            "returnDate" to System.currentTimeMillis()
        )

        database.child("borrowRecords").child(record.id)
            .updateChildren(updates)
            .addOnSuccessListener {
                // Increment available copies in books node
                database.child("books").child(record.bookId).child("availableCopies")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val currentAvailable = snapshot.getValue(Int::class.java) ?: 0
                        database.child("books").child(record.bookId)
                            .child("availableCopies")
                            .setValue(currentAvailable + 1)
                    }

                Toast.makeText(this, "Book returned successfully!", Toast.LENGTH_SHORT).show()

                // Refresh the list
                fetchBorrowedBooks()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to return book. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }
}