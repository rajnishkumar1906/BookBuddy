package com.rajnishkumar.bookbuddy.ui.dashboard

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.FirebaseDatabase
import com.rajnishkumar.bookbuddy.R
import com.rajnishkumar.bookbuddy.models.Book
import com.rajnishkumar.bookbuddy.models.BorrowRecord
import com.rajnishkumar.bookbuddy.ui.canvas.GenreBubbleCanvasView
import com.rajnishkumar.bookbuddy.ui.canvas.GenreBarGraphView
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DataAnalyticsActivity : AppCompatActivity() {

    private lateinit var genreBubbleCanvas: GenreBubbleCanvasView
    private lateinit var genreBarGraph: GenreBarGraphView
    private lateinit var tvTotalBorrows: TextView
    private lateinit var tvActiveReaders: TextView
    private lateinit var tvTotalVisits: TextView
    private lateinit var tvPopularBook: TextView
    private lateinit var tvTopReaderName: TextView
    private lateinit var tvTopReaderScore: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_analytics)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        genreBubbleCanvas = findViewById(R.id.genreBubbleCanvas)
        genreBarGraph = findViewById(R.id.genreBarGraph)
        tvTotalBorrows = findViewById(R.id.tvTotalBorrows)
        tvActiveReaders = findViewById(R.id.tvActiveReaders)
        tvTotalVisits = findViewById(R.id.tvTotalVisits)
        tvPopularBook = findViewById(R.id.tvPopularBook)
        tvTopReaderName = findViewById(R.id.tvTopReaderName)
        tvTopReaderScore = findViewById(R.id.tvTopReaderScore)

        fetchAnalyticsData()
    }

    private fun fetchAnalyticsData() {
        val database = FirebaseDatabase.getInstance().reference

        lifecycleScope.launch {
            try {
                // Fetch All Data in Parallel
                val booksSnapshot = database.child("books").get().await()
                val books = booksSnapshot.children.mapNotNull { it.getValue(Book::class.java) }
                
                val borrowSnapshot = database.child("borrowRecords").get().await()
                val borrows = borrowSnapshot.children.mapNotNull { it.getValue(BorrowRecord::class.java) }

                val visitsSnapshot = database.child("visits").get().await()
                val usersSnapshot = database.child("users").get().await()

                processAndDisplayData(books, borrows, visitsSnapshot, usersSnapshot)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun processAndDisplayData(
        books: List<Book>, 
        borrows: List<BorrowRecord>, 
        visitsSnap: com.google.firebase.database.DataSnapshot,
        usersSnap: com.google.firebase.database.DataSnapshot
    ) {
        // 1. Genre Statistics (Now handles List<String>)
        val genreStats = mutableMapOf<String, Int>()
        books.forEach { book ->
            book.genre.forEach { g ->
                val genre = if (g.isBlank()) "Other" else g
                genreStats[genre] = genreStats.getOrDefault(genre, 0) + 1
            }
        }
        
        // Update both the Bubble Chart and the Bar Graph
        genreBubbleCanvas.setData(genreStats)
        genreBarGraph.setData(genreStats)

        // 2. Metrics
        tvTotalBorrows.text = borrows.size.toString()
        
        val activeUsers = borrows.map { it.userId }.distinct().size
        tvActiveReaders.text = activeUsers.toString()

        // 3. Visit Analytics
        var totalVisits = 0L
        val bookVisitCounts = mutableMapOf<String, Int>()
        visitsSnap.children.forEach { userVisits ->
            totalVisits += userVisits.childrenCount
            userVisits.children.forEach { bookVisit ->
                val bookId = bookVisit.key ?: return@forEach
                bookVisitCounts[bookId] = bookVisitCounts.getOrDefault(bookId, 0) + 1
            }
        }
        tvTotalVisits.text = totalVisits.toString()

        // 4. Most Popular Book (Based on Visits)
        val mostVisitedBookId = bookVisitCounts.maxByOrNull { it.value }?.key
        val popularBook = books.find { it.id == mostVisitedBookId }
        tvPopularBook.text = popularBook?.title ?: "N/A"

        // 5. Most Active User (Based on Borrows)
        val topUserEntry = borrows.groupBy { it.userId }.maxByOrNull { it.value.size }
        if (topUserEntry != null) {
            val topUserId = topUserEntry.key
            val userName = usersSnap.child(topUserId).child("name").getValue(String::class.java) ?: "Member"
            tvTopReaderName.text = userName
            tvTopReaderScore.text = "${topUserEntry.value.size} Borrows"
        }
    }
}
