package com.example.bookbuddy.activities.fragments.member

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookbuddy.R
import com.example.bookbuddy.activities.shared.BookDetailActivity
import com.example.bookbuddy.adapters.BookAdapter
import com.example.bookbuddy.ai.BookAIHelper
import com.example.bookbuddy.databinding.FragmentBooksBinding
import com.example.bookbuddy.models.Book
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class BooksFragment : Fragment() {

    private var _binding: FragmentBooksBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var bookAdapter: BookAdapter
    private lateinit var bookAIHelper: BookAIHelper

    private val allBooks = mutableListOf<Book>()
    private val displayedBooks = mutableListOf<Book>()
    private var currentQuery = ""
    private var currentGenre = "All"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBooksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        bookAIHelper = BookAIHelper()

        setupRecyclerView()
        setupSearchListener()
        setupGenreChips()
        loadBooks()
    }

    private fun setupRecyclerView() {
        bookAdapter = BookAdapter(displayedBooks) { book ->
            val intent = Intent(requireContext(), BookDetailActivity::class.java)
            intent.putExtra("bookId", book.id)
            startActivity(intent)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = bookAdapter
    }

    private fun setupSearchListener() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s?.toString()?.trim() ?: ""
                binding.clearSearch.visibility = if (currentQuery.isNotEmpty()) View.VISIBLE else View.GONE

                if (currentQuery.length >= 3) {
                    performAISearch(currentQuery)
                } else if (currentQuery.isEmpty()) {
                    filterBooks()
                } else {
                    // For short queries, do basic filtering
                    filterBooks()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.clearSearch.setOnClickListener {
            binding.searchInput.text?.clear()
            currentQuery = ""
            filterBooks()
        }

        binding.aiSearchButton.setOnClickListener {
            if (currentQuery.length >= 3) {
                performAISearch(currentQuery)
            } else {
                Toast.makeText(requireContext(), "Enter at least 3 characters for AI search", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupGenreChips() {
        val genres = listOf("All", "Fiction", "Fantasy", "Sci-Fi", "Mystery", "Romance", "Biography", "History")

        binding.chipGroup.removeAllViews()
        genres.forEach { genre ->
            val chip = layoutInflater.inflate(R.layout.item_category_chip, binding.chipGroup, false) as Chip
            chip.text = genre
            chip.isChecked = genre == "All"

            chip.setOnClickListener {
                currentGenre = genre
                filterBooks()
            }

            binding.chipGroup.addView(chip)
        }
    }

    private fun loadBooks() {
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
                        // Skip invalid
                    }
                }
                filterBooks()
                showLoading(false)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(requireContext(), "Error loading books: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterBooks() {
        val filtered = allBooks.filter { book ->
            val matchesGenre = currentGenre == "All" || book.genre.equals(currentGenre, ignoreCase = true)
            val matchesSearch = currentQuery.isEmpty() ||
                    book.title.contains(currentQuery, ignoreCase = true) ||
                    book.author.contains(currentQuery, ignoreCase = true) ||
                    book.genre.contains(currentQuery, ignoreCase = true)

            matchesGenre && matchesSearch
        }

        displayedBooks.clear()
        displayedBooks.addAll(filtered)
        bookAdapter.updateList(displayedBooks)

        binding.resultsCount.text = "${displayedBooks.size} books found"
        binding.resultsCount.visibility = if (displayedBooks.isNotEmpty()) View.VISIBLE else View.GONE
        updateEmptyState()
    }

    private fun performAISearch(query: String) {
        showLoading(true)
        binding.aiSearchButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val results = bookAIHelper.aiSearch(query, allBooks)

                displayedBooks.clear()
                displayedBooks.addAll(results)
                bookAdapter.updateList(displayedBooks)

                binding.resultsCount.text = "${displayedBooks.size} AI-powered results"
                binding.resultsCount.visibility = if (displayedBooks.isNotEmpty()) View.VISIBLE else View.GONE
                updateEmptyState()

                if (results.isNotEmpty()) {
                    Toast.makeText(requireContext(), "✨ AI search found ${results.size} relevant books", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                filterBooks() // Fallback to regular filter
            } finally {
                showLoading(false)
                binding.aiSearchButton.isEnabled = true
            }
        }
    }

    private fun updateEmptyState() {
        if (displayedBooks.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE

            binding.emptyStateText.text = when {
                currentQuery.isNotEmpty() -> "No books found matching '$currentQuery'"
                currentGenre != "All" -> "No books found in $currentGenre genre"
                else -> "No books available in library"
            }
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}