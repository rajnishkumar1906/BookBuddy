package com.example.bookbuddy.fragments.librarian

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookbuddy.R
import com.example.bookbuddy.activities.shared.BookDetailActivity
import com.example.bookbuddy.adapters.BookAdapter
import com.example.bookbuddy.databinding.FragmentManageBooksBinding
import com.example.bookbuddy.models.Book
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ManageBooksFragment : Fragment() {

    private var _binding: FragmentManageBooksBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var bookAdapter: BookAdapter

    private val books = mutableListOf<Book>()
    private var isLibrarian = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageBooksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        checkUserRole()
        setupRecyclerView()
        setupSearchView()
        setupClickListeners()
        loadBooks()
    }

    private fun setupRecyclerView() {
        bookAdapter = BookAdapter(books) { book ->
            if (isLibrarian) {
                showEditDeleteDialog(book)
            }
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = bookAdapter
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchBooks(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    loadBooks()
                } else {
                    searchBooks(newText)
                }
                return true
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun checkUserRole() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val role = document.getString("role")
                isLibrarian = role == "librarian"

                if (!isLibrarian) {
                    Toast.makeText(requireContext(), "Access denied", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
    }

    private fun loadBooks() {
        showLoading(true)

        db.collection("books")
            .get()
            .addOnSuccessListener { snapshot ->
                books.clear()
                snapshot.forEach { document ->
                    val book = document.toObject(Book::class.java)
                    book.id = document.id
                    books.add(book)
                }
                bookAdapter.updateList(books)
                showLoading(false)
                updateEmptyState()
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(requireContext(), "Failed to load books", Toast.LENGTH_SHORT).show()
            }
    }

    private fun searchBooks(query: String) {
        val results = books.filter { book ->
            book.title.contains(query, ignoreCase = true) ||
                    book.author.contains(query, ignoreCase = true) ||
                    book.genre.contains(query, ignoreCase = true)
        }
        bookAdapter.updateList(results)
        updateEmptyState(results.isEmpty())
    }

    private fun showEditDeleteDialog(book: Book) {
        val options = arrayOf("View Details", "Edit Book", "Delete Book")

        AlertDialog.Builder(requireContext())
            .setTitle(book.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewBook(book)
                    1 -> editBook(book)
                    2 -> deleteBook(book)
                }
            }
            .show()
    }

    private fun viewBook(book: Book) {
        val intent = Intent(requireContext(), BookDetailActivity::class.java)
        intent.putExtra("bookId", book.id)
        intent.putExtra("source", "librarian")
        startActivity(intent)
    }

    private fun editBook(book: Book) {
        // Navigate to EditBookFragment
        val fragment = EditBookFragment.newInstance(book.id)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun deleteBook(book: Book) {
        // Check if book is currently borrowed
        db.collection("borrowRecords")
            .whereEqualTo("bookId", book.id)
            .whereEqualTo("returnedAt", null)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    confirmDelete(book)
                } else {
                    Toast.makeText(requireContext(), "Cannot delete: Book is currently borrowed", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun confirmDelete(book: Book) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Book")
            .setMessage("Are you sure you want to delete '${book.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                performDelete(book)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDelete(book: Book) {
        showLoading(true)

        db.collection("books").document(book.id)
            .delete()
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(requireContext(), "Book deleted successfully", Toast.LENGTH_SHORT).show()
                loadBooks()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(requireContext(), "Error deleting: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateEmptyState(isEmpty: Boolean = books.isEmpty()) {
        if (isEmpty) {
            binding.emptyStateText.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyStateText.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.searchView.isEnabled = !show
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}