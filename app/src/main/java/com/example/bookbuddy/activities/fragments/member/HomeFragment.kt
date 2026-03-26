package com.example.bookbuddy.activities.fragments.member

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookbuddy.activities.shared.BookDetailActivity
import com.example.bookbuddy.adapters.BookAdapter
import com.example.bookbuddy.ai.BookAIHelper
import com.example.bookbuddy.databinding.FragmentHomeBinding
import com.example.bookbuddy.models.Book
import com.example.bookbuddy.models.BookInteraction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var bookAdapter: BookAdapter
    private lateinit var popularAdapter: BookAdapter
    private lateinit var recommendedAdapter: BookAdapter
    private lateinit var bookAIHelper: BookAIHelper

    private var bookListener: ListenerRegistration? = null
    private val allBooks = mutableListOf<Book>()
    private val popularBooks = mutableListOf<Book>()
    private val recommendedBooks = mutableListOf<Book>()
    private val userInteractions = mutableListOf<BookInteraction>()
    private var currentUserName = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        bookAIHelper = BookAIHelper()

        setupRecyclerViews()
        loadUserData()
        loadUserInteractions()
        loadBooks()
    }

    private fun setupRecyclerViews() {
        // Main books adapter (vertical)
        bookAdapter = BookAdapter(allBooks) { book ->
            openBookDetail(book)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = bookAdapter

        // Popular books adapter (horizontal)
        popularAdapter = BookAdapter(popularBooks) { book ->
            openBookDetail(book)
        }
        binding.popularRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.popularRecyclerView.adapter = popularAdapter

        // Recommended books adapter (horizontal)
        recommendedAdapter = BookAdapter(recommendedBooks) { book ->
            openBookDetail(book)
        }
        binding.recommendedRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recommendedRecyclerView.adapter = recommendedAdapter
    }

    private fun openBookDetail(book: Book) {
        val intent = Intent(requireContext(), BookDetailActivity::class.java)
        intent.putExtra("bookId", book.id)
        intent.putExtra("source", "member")
        startActivity(intent)
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                currentUserName = document.getString("name") ?: "Member"
                binding.welcomeText.text = "Welcome back, $currentUserName! 👋"
                binding.welcomeText.visibility = View.VISIBLE
            }
    }

    private fun loadUserInteractions() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("interactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { snapshot ->
                userInteractions.clear()
                snapshot.forEach { document ->
                    try {
                        val interaction = document.toObject(BookInteraction::class.java)
                        userInteractions.add(interaction)
                    } catch (e: Exception) {
                        // Skip invalid
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

                allBooks.clear()
                snapshot?.forEach { document ->
                    try {
                        val book = document.toObject(Book::class.java)
                        book.id = document.id
                        allBooks.add(book)
                    } catch (e: Exception) {
                        // Skip invalid
                    }
                }

                bookAdapter.updateList(allBooks)
                updatePopularBooks()
                updateRecommendedBooks()
                updateEmptyState()
            }
    }

    private fun updatePopularBooks() {
        val popular = allBooks
            .sortedByDescending { it.timesBorrowed }
            .take(5)

        popularBooks.clear()
        popularBooks.addAll(popular)

        if (popularBooks.isNotEmpty()) {
            popularAdapter.updateList(popularBooks)
            binding.popularBooksLabel.visibility = View.VISIBLE
            binding.popularRecyclerView.visibility = View.VISIBLE
        } else {
            binding.popularBooksLabel.visibility = View.GONE
            binding.popularRecyclerView.visibility = View.GONE
        }
    }

    private suspend fun getAIRecommendations(): List<Book> {
        if (userInteractions.isEmpty() || allBooks.isEmpty()) {
            return emptyList()
        }

        // Get books user has interacted with
        val userBookIds = userInteractions.map { it.bookId }.distinct()
        val userBooks = allBooks.filter { it.id in userBookIds }

        return bookAIHelper.getRecommendations(userBooks, allBooks, limit = 5)
    }

    private fun updateRecommendedBooks() {
        // Show loading for recommendations
        binding.recommendedLabel.visibility = View.GONE
        binding.recommendedRecyclerView.visibility = View.GONE

        // Run AI recommendations in background using viewLifecycleOwner
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val recommendations = getAIRecommendations()

                recommendedBooks.clear()
                recommendedBooks.addAll(recommendations)

                if (recommendedBooks.isNotEmpty()) {
                    recommendedAdapter.updateList(recommendedBooks)
                    binding.recommendedLabel.visibility = View.VISIBLE
                    binding.recommendedRecyclerView.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle error gracefully
            }
        }
    }

    private fun updateEmptyState() {
        if (allBooks.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
            binding.emptyStateText.text = "No books available in library yet"
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
        bookListener?.remove()
        _binding = null
    }
}