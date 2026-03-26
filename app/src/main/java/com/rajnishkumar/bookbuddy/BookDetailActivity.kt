package com.rajnishkumar.bookbuddy

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.rajnishkumar.bookbuddy.ai.AISearchHelper
import com.rajnishkumar.bookbuddy.ai.GeminiClient
import com.rajnishkumar.bookbuddy.ai.HuggingFaceClient
import com.rajnishkumar.bookbuddy.models.Book
import com.rajnishkumar.bookbuddy.models.BorrowRecord
import com.rajnishkumar.bookbuddy.models.Chunk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookDetailActivity : AppCompatActivity() {

    private lateinit var ivCover: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvAuthor: TextView
    private lateinit var tvGenre: TextView
    private lateinit var tvSummary: TextView
    private lateinit var tvDescription: TextView
    private lateinit var etAskAI: EditText
    private lateinit var btnAskAI: ImageButton
    private lateinit var tvAIAnswer: TextView
    private lateinit var btnBorrow: Button
    private lateinit var btnGenerateSummary: Button
    private lateinit var pbSummary: ProgressBar
    private lateinit var ratingBar: RatingBar
    
    private lateinit var switchRAG: MaterialSwitch
    private lateinit var layoutAIChat: LinearLayout

    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private val huggingFace = HuggingFaceClient()
    private val geminiClient = GeminiClient()
    private val searchHelper = AISearchHelper()
    
    private var currentBook: Book? = null
    private var chatHistory = StringBuilder()
    private var bookChunks = mutableListOf<Chunk>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_detail)

        ivCover = findViewById(R.id.ivDetailCover)
        tvTitle = findViewById(R.id.tvDetailTitle)
        tvAuthor = findViewById(R.id.tvBookAuthor)
        tvGenre = findViewById(R.id.tvBookGenre)
        tvSummary = findViewById(R.id.tvDetailSummary)
        tvDescription = findViewById(R.id.tvDetailDescription)
        etAskAI = findViewById(R.id.etAskAI)
        btnAskAI = findViewById(R.id.btnAskAI)
        tvAIAnswer = findViewById(R.id.tvAIAnswer)
        btnBorrow = findViewById(R.id.btnBorrow)
        btnGenerateSummary = findViewById(R.id.btnGenerateSummary)
        pbSummary = findViewById(R.id.pbSummary)
        ratingBar = findViewById(R.id.ratingBar)
        
        switchRAG = findViewById(R.id.switchRAG)
        layoutAIChat = findViewById(R.id.layoutAIChat)

        val bookId = intent.getStringExtra("BOOK_ID") ?: return
        fetchBookDetails(bookId)

        btnGenerateSummary.setOnClickListener { generateAndSaveSummary() }
        
        btnAskAI.setOnClickListener { 
            val question = etAskAI.text.toString().trim()
            if (question.isNotEmpty()) askAIWithRAG(question) 
        }

        switchRAG.setOnCheckedChangeListener { _, isChecked ->
            layoutAIChat.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        btnBorrow.setOnClickListener { borrowBook() }

        ratingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser) updateRatingInFirebase(rating)
        }
    }

    private fun fetchBookDetails(bookId: String) {
        database.child("books").child(bookId).get().addOnSuccessListener { snapshot ->
            currentBook = snapshot.getValue(Book::class.java)
            currentBook?.let { book ->
                tvTitle.text = book.title
                tvAuthor.text = book.author
                tvGenre.text = book.genre
                tvDescription.text = book.description
                ratingBar.rating = book.averageRating
                
                checkAndEnhanceDescription(book)

                if (book.summary.isNotEmpty()) {
                    tvSummary.visibility = View.VISIBLE
                    tvSummary.text = book.summary
                    btnGenerateSummary.visibility = View.GONE
                } else {
                    tvSummary.visibility = View.GONE
                    btnGenerateSummary.visibility = View.VISIBLE
                }

                if (book.availableCopies <= 0) {
                    btnBorrow.isEnabled = false
                    btnBorrow.text = "Out of Stock"
                }

                Glide.with(this).load(book.coverUrl).placeholder(R.drawable.ic_book_placeholder).into(ivCover)
            }
        }
    }

    private fun borrowBook() {
        val book = currentBook ?: return
        val userId = auth.currentUser?.uid ?: return

        if (book.availableCopies <= 0) {
            Toast.makeText(this, "Book is not available", Toast.LENGTH_SHORT).show()
            return
        }

        btnBorrow.isEnabled = false
        val recordRef = database.child("borrowRecords").push()
        val recordId = recordRef.key ?: ""
        
        val record = BorrowRecord(
            id = recordId,
            bookId = book.id,
            userId = userId,
            bookTitle = book.title,
            bookAuthor = book.author,
            bookCoverUrl = book.coverUrl
        )

        recordRef.setValue(record).addOnSuccessListener {
            // Update book availability
            database.child("books").child(book.id).child("availableCopies").setValue(book.availableCopies - 1)
                .addOnSuccessListener {
                    Toast.makeText(this, "Book borrowed successfully!", Toast.LENGTH_LONG).show()
                    finish()
                }
        }.addOnFailureListener {
            btnBorrow.isEnabled = true
            Toast.makeText(this, "Borrowing failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndEnhanceDescription(book: Book) {
        val wordCount = book.description.split(Regex("\\s+")).size
        if (wordCount < 100) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val expanded = withContext(Dispatchers.IO) {
                        geminiClient.expandDescription(book.title, book.author, book.description)
                    }
                    if (expanded.isNotEmpty() && !expanded.contains("Error")) {
                        book.description = expanded
                        tvDescription.text = expanded
                        database.child("books").child(book.id).child("description").setValue(expanded)
                        bookChunks.clear()
                    }
                } catch (e: Exception) {
                    Log.e("BookDetail", "Auto-enhancement failed", e)
                }
            }
        }
    }

    private fun generateAndSaveSummary() {
        val book = currentBook ?: return
        btnGenerateSummary.visibility = View.GONE
        pbSummary.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.Main).launch {
            try {
                var summary = withContext(Dispatchers.IO) { huggingFace.summarizeText(book.description) }
                if (summary.contains("error", true) || summary.isEmpty() || summary == "Summary unavailable.") {
                    summary = withContext(Dispatchers.IO) { geminiClient.summarizeBook(book.description) }
                }
                if (summary.isNotEmpty() && summary != "Summary unavailable.") {
                    book.summary = summary
                    tvSummary.text = summary
                    tvSummary.visibility = View.VISIBLE
                    database.child("books").child(book.id).child("summary").setValue(summary)
                } else {
                    btnGenerateSummary.visibility = View.VISIBLE
                }
            } finally { pbSummary.visibility = View.GONE }
        }
    }

    private fun askAIWithRAG(question: String) {
        tvAIAnswer.visibility = View.VISIBLE
        tvAIAnswer.text = "Thinking..."
        btnAskAI.isEnabled = false

        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (bookChunks.isEmpty()) {
                    prepareChunksOnDemand()
                }

                val questionEmbedding = withContext(Dispatchers.IO) { huggingFace.getEmbedding(question) }
                val relevantContext = findRelevantChunks(questionEmbedding)

                var answer = withContext(Dispatchers.IO) {
                    huggingFace.chatWithBook(relevantContext, chatHistory.toString(), question)
                }
                
                if (answer.contains("Error", true) || answer.isEmpty() || answer == "AI is currently busy.") {
                    answer = withContext(Dispatchers.IO) {
                        geminiClient.chatWithBook(relevantContext, chatHistory.toString(), question)
                    }
                }

                chatHistory.append("User: $question\nAssistant: $answer\n")
                tvAIAnswer.text = "AI Answer: $answer"
                etAskAI.text.clear()
            } catch (e: Exception) {
                tvAIAnswer.text = "Error: Could not process request"
            } finally { btnAskAI.isEnabled = true }
        }
    }

    private suspend fun prepareChunksOnDemand() = withContext(Dispatchers.IO) {
        val description = currentBook?.description ?: return@withContext
        val sentences = description.split(Regex("(?<=[.!?])\\s+"))
        for (sentence in sentences) {
            if (sentence.length > 10) {
                val emb = huggingFace.getEmbedding(sentence)
                if (emb.isNotEmpty()) bookChunks.add(Chunk(sentence, emb))
            }
        }
    }

    private fun findRelevantChunks(queryEmb: List<Double>): String {
        if (bookChunks.isEmpty()) return currentBook?.description ?: ""
        
        return bookChunks
            .map { it.text to searchHelper.calculateCosineSimilarity(queryEmb, it.embedding) }
            .sortedByDescending { it.second }
            .take(3)
            .joinToString(" ") { it.first }
    }

    private fun updateRatingInFirebase(newRating: Float) {
        val book = currentBook ?: return
        val newTotal = book.totalRatings + 1
        val newAvg = ((book.averageRating * book.totalRatings) + newRating) / newTotal
        
        val updates = mapOf(
            "averageRating" to newAvg,
            "totalRatings" to newTotal
        )
        
        database.child("books").child(book.id).updateChildren(updates).addOnSuccessListener {
            book.averageRating = newAvg
            book.totalRatings = newTotal
            Toast.makeText(this, "Thanks for rating!", Toast.LENGTH_SHORT).show()
        }
    }
}