package com.example.bookbuddy.activities.fragments.librarian

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bookbuddy.databinding.FragmentStatsBinding
import com.example.bookbuddy.models.BorrowRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*
import com.google.firebase.Timestamp

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        loadStats()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnRefresh.setOnClickListener {
            loadStats()
            Toast.makeText(requireContext(), "Statistics refreshed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadStats() {
        showLoading(true)

        // Load all required data
        loadCollectionStats()
        loadGenreStats()
        loadPopularBooks()
        loadMonthlyStats()
    }

    private fun loadCollectionStats() {
        // Total books
        db.collection("books").get()
            .addOnSuccessListener { snapshot ->
                binding.totalBooksValue.text = snapshot.size().toString()
            }

        // Total members
        db.collection("users").whereEqualTo("role", "member").get()
            .addOnSuccessListener { snapshot ->
                binding.totalMembersValue.text = snapshot.size().toString()
            }

        // Total librarians
        db.collection("users").whereEqualTo("role", "librarian").get()
            .addOnSuccessListener { snapshot ->
                binding.totalLibrariansValue.text = snapshot.size().toString()
            }
    }

    private fun loadGenreStats() {
        db.collection("books").get()
            .addOnSuccessListener { snapshot ->
                val genreCount = mutableMapOf<String, Int>()

                snapshot.forEach { doc ->
                    val genre = doc.getString("genre") ?: "Unknown"
                    genreCount[genre] = genreCount.getOrDefault(genre, 0) + 1
                }

                val topGenres = genreCount.entries
                    .sortedByDescending { it.value }
                    .take(5)
                    .joinToString("\n") { "${it.key}: ${it.value} books" }

                binding.topGenresValue.text = topGenres.ifEmpty { "No data" }
            }
    }

    private fun loadPopularBooks() {
        db.collection("books")
            .orderBy("timesBorrowed", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { snapshot ->
                val popularBooks = StringBuilder()
                snapshot.forEachIndexed { index, doc ->
                    val title = doc.getString("title") ?: "Unknown"
                    val times = doc.getLong("timesBorrowed") ?: 0
                    popularBooks.append("${index + 1}. $title ($times times)\n")
                }

                binding.popularBooksValue.text = popularBooks.toString().ifEmpty { "No borrowing data yet" }
            }
    }

    private fun loadMonthlyStats() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1

        // Books borrowed this month
        val startOfMonth = Timestamp(Date(year - 1900, month - 1, 1))
        val endOfMonth = Timestamp(Date(year - 1900, month, 1))

        db.collection("borrowRecords")
            .whereGreaterThanOrEqualTo("borrowedAt", startOfMonth)
            .whereLessThan("borrowedAt", endOfMonth)
            .get()
            .addOnSuccessListener { snapshot ->
                binding.monthlyBorrowsValue.text = snapshot.size().toString()
            }

        // Books returned this month
        db.collection("borrowRecords")
            .whereGreaterThanOrEqualTo("returnedAt", startOfMonth)
            .whereLessThan("returnedAt", endOfMonth)
            .get()
            .addOnSuccessListener { snapshot ->
                binding.monthlyReturnsValue.text = snapshot.size().toString()
            }

        // Average borrowing duration
        db.collection("borrowRecords")
            .whereNotEqualTo("returnedAt", null)
            .limit(100)
            .get()
            .addOnSuccessListener { snapshot ->
                var totalDays = 0L
                var count = 0

                snapshot.forEach { doc ->
                    val record = doc.toObject(BorrowRecord::class.java)
                    if (record.returnedAt != null) {
                        val borrowedTime = record.borrowedAt.toDate().time
                        val returnedTime = record.returnedAt!!.toDate().time
                        val days = (returnedTime - borrowedTime) / (1000 * 60 * 60 * 24)
                        totalDays += days
                        count++
                    }
                }

                val average = if (count > 0) totalDays / count else 0
                binding.avgDurationValue.text = "$average days"
            }

        showLoading(false)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}