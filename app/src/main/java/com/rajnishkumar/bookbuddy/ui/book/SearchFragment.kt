package com.rajnishkumar.bookbuddy.ui.book

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.rajnishkumar.bookbuddy.R
import com.rajnishkumar.bookbuddy.adapters.BookAdapter
import com.rajnishkumar.bookbuddy.repository.BookSearchRepository
import com.rajnishkumar.bookbuddy.ui.canvas.AISearchVisualizerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchFragment : Fragment() {

    private lateinit var etSearchQuery: EditText
    private lateinit var btnSearch: View
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var searchProgressBar: View
    private lateinit var searchVisualizer: AISearchVisualizerView

    private val bookRepo by lazy { BookSearchRepository.getInstance(requireContext()) }
    private lateinit var adapter: BookAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        etSearchQuery = view.findViewById(R.id.etSearchQuery)
        btnSearch = view.findViewById(R.id.btnSearch)
        rvSearchResults = view.findViewById(R.id.rvSearchResults)
        searchProgressBar = view.findViewById(R.id.searchProgressBar)
        searchVisualizer = view.findViewById(R.id.searchVisualizer)

        adapter = BookAdapter { book ->
            val intent = Intent(requireContext(), BookDetailActivity::class.java)
            intent.putExtra("BOOK_ID", book.id)
            startActivity(intent)
        }

        rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        rvSearchResults.adapter = adapter

        setupSearchLogic(view)
        loadInitialBooks()

        return view
    }

    private fun loadInitialBooks() {
        viewLifecycleOwner.lifecycleScope.launch {
            val books = withContext(Dispatchers.IO) { bookRepo.getSearchMetadata() }
            if (isAdded) adapter.setBooks(books.take(20))
        }
    }

    private fun setupSearchLogic(view: View) {
        btnSearch.setOnClickListener { performSearch() }

        etSearchQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }

        val chipIds = listOf(R.id.chipSciFi, R.id.chipHistory, R.id.chipAI, R.id.chipMystery, R.id.chipSoftware)
        chipIds.forEach { id ->
            view.findViewById<Chip>(id)?.setOnClickListener {
                val suggestion = (it as Chip).text.toString().trim()
                etSearchQuery.setText(suggestion)
                performSearch(suggestion)
            }
        }
    }

    private fun performSearch(query: String? = null) {
        val searchQuery = query ?: etSearchQuery.text.toString().trim()
        if (searchQuery.isEmpty()) return

        searchProgressBar.visibility = View.VISIBLE
        searchVisualizer.setSearching(true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val results = withContext(Dispatchers.IO) { bookRepo.searchBooks(searchQuery, limit = 30) }
                if (isAdded) adapter.setBooks(results)
            } catch (e: Exception) {
                if (isAdded) Toast.makeText(requireContext(), "Search failed", Toast.LENGTH_SHORT).show()
            } finally {
                if (isAdded) {
                    searchProgressBar.visibility = View.GONE
                    searchVisualizer.setSearching(false)
                }
            }
        }
    }
}
