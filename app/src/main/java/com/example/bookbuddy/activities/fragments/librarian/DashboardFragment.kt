package com.example.bookbuddy.activities.fragments.librarian

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
import com.example.bookbuddy.databinding.FragmentDashboardBinding
import com.example.bookbuddy.fragments.librarian.ManageBooksFragment
import com.example.bookbuddy.models.Book
import com.example.bookbuddy.models.BorrowRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recentBooksAdapter: BookAdapter
    private val recentBooks = mutableListOf<Book>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupRecyclerView()
        loadLibrarianName()
        loadStats()
        loadRecentBooks()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        recentBooksAdapter = BookAdapter(recentBooks) { book ->
            val intent = Intent(requireContext(), BookDetailActivity::class.java)
            intent.putExtra("bookId", book.id)
            intent.putExtra("source", "librarian")
            startActivity(intent)
        }
        binding.recentBooksRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recentBooksRecyclerView.adapter = recentBooksAdapter
    }

    private fun setupClickListeners() {
        binding.btnAddBook.setOnClickListener {
            // Navigate to AddBookFragment using parent fragment manager
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddBookFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnManageBooks.setOnClickListener {
            // Navigate to ManageBooksFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ManageBooksFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnRefresh.setOnClickListener {
            loadStats()
            loadRecentBooks()
            Toast.makeText(requireContext(), "Dashboard refreshed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadLibrarianName() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val name = document.getString("name") ?: "Librarian"
                binding.welcomeText.text = "Welcome back, $name! 📚"
                binding.welcomeText.visibility = View.VISIBLE
            }
    }

    private fun loadStats() {
        // Load total books
        db.collection("books").get()
            .addOnSuccessListener { snapshot ->
                binding.totalBooksValue.text = snapshot.size().toString()

                // Calculate available books
                var available = 0
                snapshot.forEach { doc ->
                    val availableCopies = doc.getLong("availableCopies") ?: 0
                    available += availableCopies.toInt()
                }
                binding.availableBooksValue.text = available.toString()
            }

        // Load total members
        db.collection("users").whereEqualTo("role", "member").get()
            .addOnSuccessListener { snapshot ->
                binding.totalMembersValue.text = snapshot.size().toString()
            }

        // Load active borrowings
        db.collection("borrowRecords").whereEqualTo("returnedAt", null).get()
            .addOnSuccessListener { snapshot ->
                binding.activeBorrowingsValue.text = snapshot.size().toString()
            }

        // Load overdue books
        val now = System.currentTimeMillis()
        db.collection("borrowRecords")
            .whereEqualTo("returnedAt", null)
            .get()
            .addOnSuccessListener { snapshot ->
                var overdue = 0
                for (doc in snapshot) {
                    val record = doc.toObject(BorrowRecord::class.java)
                    if (record.dueDate.toDate().time < now) {
                        overdue++
                    }
                }
                binding.overdueValue.text = overdue.toString()
                if (overdue > 0) {
                    binding.overdueValue.setTextColor(requireContext().getColor(R.color.error))
                } else {
                    binding.overdueValue.setTextColor(requireContext().getColor(R.color.peacock_green))
                }
            }
    }

    private fun loadRecentBooks() {
        db.collection("books")
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { snapshot ->
                recentBooks.clear()
                snapshot.forEach { document ->
                    try {
                        val book = document.toObject(Book::class.java)
                        book.id = document.id
                        recentBooks.add(book)
                    } catch (e: Exception) {
                        // Skip invalid
                    }
                }
                recentBooksAdapter.updateList(recentBooks)

                if (recentBooks.isEmpty()) {
                    binding.recentBooksLabel.visibility = View.GONE
                    binding.recentBooksRecyclerView.visibility = View.GONE
                } else {
                    binding.recentBooksLabel.visibility = View.VISIBLE
                    binding.recentBooksRecyclerView.visibility = View.VISIBLE
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}