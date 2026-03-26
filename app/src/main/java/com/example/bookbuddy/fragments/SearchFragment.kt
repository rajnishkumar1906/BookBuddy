package com.example.bookbuddy.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
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
import com.example.bookbuddy.models.Book
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SearchFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var searchInput: EditText
    private lateinit var clearSearch: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var bookAdapter: BookAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var emptyStateText: TextView
    private lateinit var emptyStateSubText: TextView
    private lateinit var resultsCount: TextView

    private val allBooks = mutableListOf<Book>()
    private val searchResults = mutableListOf<Book>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initializeViews(view)
        setupSearchListener()
        loadAllBooks()

        return view
    }

    private fun initializeViews(view: View) {
        searchInput = view.findViewById(R.id.searchInput)
        clearSearch = view.findViewById(R.id.clearSearch)
        recyclerView = view.findViewById(R.id.recyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        emptyStateSubText = view.findViewById(R.id.emptyStateSubText)
        resultsCount = view.findViewById(R.id.resultsCount)

        bookAdapter = BookAdapter(searchResults) { book ->
            val intent = Intent(requireContext(), BookDetailActivity::class.java)
            intent.putExtra("bookId", book.id)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = bookAdapter

        clearSearch.setOnClickListener {
            searchInput.text.clear()
            clearSearch.visibility = View.GONE
            showAllBooks()
        }
    }

    private fun setupSearchListener() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                clearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE

                if (query.isEmpty()) {
                    showAllBooks()
                } else {
                    searchBooks(query)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadAllBooks() {
        showLoading(true)

        db.collection("books")
            .get()
            .addOnSuccessListener { snapshot ->
                allBooks.clear()
                snapshot.forEach { document ->
                    try {
                        val book = document.toObject(Book::class.java)
                        book.id = document.id
                        allBooks.add(book)
                    } catch (e: Exception) {
                        // Skip invalid book
                    }
                }
                showAllBooks()
                showLoading(false)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(requireContext(), "Error loading books: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAllBooks() {
        searchResults.clear()
        searchResults.addAll(allBooks)
        bookAdapter.updateList(searchResults)
        updateResultsCount()
        updateEmptyState()
    }

    private fun searchBooks(query: String) {
        val results = allBooks.filter { book ->
            book.title.contains(query, ignoreCase = true) ||
                    book.author.contains(query, ignoreCase = true) ||
                    book.genre.contains(query, ignoreCase = true) ||
                    book.description.contains(query, ignoreCase = true)
        }

        searchResults.clear()
        searchResults.addAll(results)
        bookAdapter.updateList(searchResults)
        updateResultsCount()
        updateEmptyState()
    }

    private fun updateResultsCount() {
        if (searchResults.isNotEmpty()) {
            resultsCount.text = "${searchResults.size} books found"
            resultsCount.visibility = View.VISIBLE
        } else {
            resultsCount.visibility = View.GONE
        }
    }

    private fun updateEmptyState() {
        if (searchResults.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE

            if (allBooks.isEmpty()) {
                emptyStateText.text = "No books in library"
                emptyStateSubText.text = "Check back later"
            } else if (searchInput.text.toString().isNotEmpty()) {
                emptyStateText.text = "No books found"
                emptyStateSubText.text = "Try different keywords"
            } else {
                emptyStateLayout.visibility = View.GONE
            }
        } else {
            emptyStateLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        searchInput.isEnabled = !show
    }
}