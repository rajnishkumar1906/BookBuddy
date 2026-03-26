package com.example.bookbuddy.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookbuddy.R
import com.example.bookbuddy.activities.shared.BookDetailActivity
import com.example.bookbuddy.adapters.BookAdapter
import com.example.bookbuddy.ai.BookRecommendationHelper
import com.example.bookbuddy.models.Book
import com.example.bookbuddy.models.BookInteraction
import com.example.bookbuddy.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var popularRecyclerView: RecyclerView
    private lateinit var recommendedRecyclerView: RecyclerView
    private lateinit var bookAdapter: BookAdapter
    private lateinit var popularAdapter: BookAdapter
    private lateinit var recommendedAdapter: BookAdapter
    private lateinit var popularBooksLabel: TextView
    private lateinit var recommendedLabel: TextView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var emptyStateText: TextView
    private lateinit var emptyStateSubText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var welcomeText: TextView

    private var bookListener: ListenerRegistration? = null
    private val books = mutableListOf<Book>()
    private val popularBooks = mutableListOf<Book>()
    private val recommendedBooks = mutableListOf<Book>()
    private lateinit var recommendationHelper: BookRecommendationHelper
    private val userInteractions = mutableListOf<BookInteraction>()
    private var currentUserName = ""
    private var currentUserRole = "member"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        recommendationHelper = BookRecommendationHelper()

        initializeViews(view)
        setupRecyclerViews()
        getUserDetails()
        loadUserInteractions()
        loadBooks()

        return view
    }

    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        popularRecyclerView = view.findViewById(R.id.popularRecyclerView)
        recommendedRecyclerView = view.findViewById(R.id.recommendedRecyclerView)
        popularBooksLabel = view.findViewById(R.id.popularBooksLabel)
        recommendedLabel = view.findViewById(R.id.recommendedLabel)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        emptyStateSubText = view.findViewById(R.id.emptyStateSubText)
        progressBar = view.findViewById(R.id.progressBar)
        welcomeText = view.findViewById(R.id.welcomeText)
    }

    private fun setupRecyclerViews() {
        // Main books adapter (vertical)
        bookAdapter = BookAdapter(books) { book ->
            openBookDetail(book)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = bookAdapter

        // Popular books adapter (horizontal)
        popularAdapter = BookAdapter(popularBooks) { book ->
            openBookDetail(book)
        }
        popularRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        popularRecyclerView.adapter = popularAdapter

        // Recommended books adapter (horizontal)
        recommendedAdapter = BookAdapter(recommendedBooks) { book ->
            openBookDetail(book)
        }
        recommendedRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recommendedRecyclerView.adapter = recommendedAdapter
    }

    private fun openBookDetail(book: Book) {
        val intent = Intent(requireContext(), BookDetailActivity::class.java)
        intent.putExtra("bookId", book.id)
        intent.putExtra("source", "member")
        startActivity(intent)
    }

    private fun getUserDetails() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    currentUserName = user?.name ?: "Member"
                    currentUserRole = user?.role ?: "member"

                    welcomeText.text = "Welcome, $currentUserName!"
                    welcomeText.visibility = View.VISIBLE
                }
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
                    Toast.makeText(requireContext(), "Error loading books", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                books.clear()
                snapshot?.forEach { document ->
                    try {
                        val book = document.toObject(Book::class.java)
                        book.id = document.id
                        books.add(book)
                    } catch (e: Exception) {
                        // Skip invalid book
                    }
                }
                bookAdapter.updateList(books)

                // Update recommendation sections
                updatePopularBooks()
                updateRecommendedBooks()
                updateEmptyState()
            }
    }

    private fun updatePopularBooks() {
        if (books.isNotEmpty()) {
            val popular = recommendationHelper.getPopularBooks(books, 5)
            popularBooks.clear()
            popularBooks.addAll(popular)

            if (popularBooks.isNotEmpty()) {
                popularAdapter.updateList(popularBooks)
                popularBooksLabel.visibility = View.VISIBLE
                popularRecyclerView.visibility = View.VISIBLE
            } else {
                popularBooksLabel.visibility = View.GONE
                popularRecyclerView.visibility = View.GONE
            }
        }
    }

    private fun updateRecommendedBooks() {
        if (books.isNotEmpty()) {
            val recommended = if (userInteractions.isEmpty()) {
                recommendationHelper.getInitialSuggestions(books, 5)
            } else {
                recommendationHelper.getRecommendationsForUser(userInteractions, books, 5)
            }

            recommendedBooks.clear()
            recommendedBooks.addAll(recommended)

            if (recommendedBooks.isNotEmpty()) {
                recommendedAdapter.updateList(recommendedBooks)
                recommendedLabel.visibility = View.VISIBLE
                recommendedRecyclerView.visibility = View.VISIBLE
            } else {
                recommendedLabel.visibility = View.GONE
                recommendedRecyclerView.visibility = View.GONE
            }
        }
    }

    private fun updateEmptyState() {
        if (books.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE

            emptyStateText.text = if (currentUserRole == "librarian") {
                "No books in library yet."
            } else {
                "No books available yet."
            }

            emptyStateSubText.text = if (currentUserRole == "librarian") {
                "Go to Librarian Dashboard to add your first book!"
            } else {
                "Please check back later!"
            }
        } else {
            emptyStateLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        bookListener?.remove()
        super.onDestroyView()
    }
}