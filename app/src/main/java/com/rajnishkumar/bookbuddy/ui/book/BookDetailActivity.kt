package com.rajnishkumar.bookbuddy.ui.book

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.rajnishkumar.bookbuddy.R
import com.rajnishkumar.bookbuddy.adapters.ChatAdapter
import com.rajnishkumar.bookbuddy.ai.GeminiClient
import com.rajnishkumar.bookbuddy.ai.RagService
import com.rajnishkumar.bookbuddy.common.Constants
import com.rajnishkumar.bookbuddy.database.AppDatabase
import com.rajnishkumar.bookbuddy.models.Book
import com.rajnishkumar.bookbuddy.models.BorrowRecord
import com.rajnishkumar.bookbuddy.models.ChatMessage
import com.rajnishkumar.bookbuddy.models.Quiz
import com.rajnishkumar.bookbuddy.repository.BookSearchRepository
import com.rajnishkumar.bookbuddy.ui.sensor.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookDetailActivity : BaseActivity() {

    private lateinit var ivCover: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvAuthor: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvPageIndicator: TextView
    private lateinit var btnPrevPage: Button
    private lateinit var btnNextPage: Button
    private lateinit var etAskAI: EditText
    private lateinit var btnAskAI: ImageButton
    private lateinit var btnBorrow: Button
    private lateinit var pbSummary: ProgressBar
    private lateinit var ratingBar: RatingBar
    private lateinit var btnBack: ImageButton

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var rvChatHistory: RecyclerView
    private lateinit var pbChat: ProgressBar
    private lateinit var chatAdapter: ChatAdapter

    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private val roomDb by lazy { AppDatabase.getDatabase(this) }
    private val bookRepo by lazy { BookSearchRepository.getInstance(this) }
    private val ragService by lazy { RagService(this) }
    private val geminiClient = GeminiClient()

    private var currentBook: Book? = null
    private var descriptionPages = listOf<String>()
    private var currentPage = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_detail)

        initViews()
        setupChatRecyclerView()

        val bookId = intent.getStringExtra("BOOK_ID") ?: run { finish(); return }
        if (intent.getBooleanExtra("OPEN_CHAT", false)) drawerLayout.openDrawer(GravityCompat.END)

        fetchBookDetails(bookId)
        loadLocalChatHistory(bookId)
        fetchFirebaseChatHistory(bookId)
        recordVisit(bookId)

        btnAskAI.setOnClickListener { handleUserQuestion() }
        findViewById<View>(R.id.btnOpenRobo).setOnClickListener { drawerLayout.openDrawer(GravityCompat.END) }
        findViewById<View>(R.id.btnClearChat).setOnClickListener { showClearChatConfirmation() }
        btnBorrow.setOnClickListener { borrowBook() }
        btnBack.setOnClickListener { finish() }

        btnPrevPage.setOnClickListener { if (currentPage > 0) updatePage(--currentPage) }
        btnNextPage.setOnClickListener { if (currentPage < descriptionPages.size - 1) updatePage(++currentPage) }

        ratingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser) updateRatingInFirebase(rating)
        }
    }

    private fun initViews() {
        ivCover = findViewById(R.id.ivDetailCover)
        tvTitle = findViewById(R.id.tvDetailTitle)
        tvAuthor = findViewById(R.id.tvBookAuthor)
        tvDescription = findViewById(R.id.tvDetailDescription)
        tvPageIndicator = findViewById(R.id.tvPageIndicator)
        btnPrevPage = findViewById(R.id.btnPrevPage)
        btnNextPage = findViewById(R.id.btnNextPage)
        etAskAI = findViewById(R.id.etAskAI)
        btnAskAI = findViewById(R.id.btnAskAI)
        btnBorrow = findViewById(R.id.btnBorrow)
        pbSummary = findViewById(R.id.pbSummary)
        ratingBar = findViewById(R.id.ratingBar)
        btnBack = findViewById(R.id.btnBack)
        drawerLayout = findViewById(R.id.drawerLayout)
        rvChatHistory = findViewById(R.id.rvChatHistory)
        pbChat = findViewById(R.id.pbChat)
    }

    private fun setupChatRecyclerView() {
        chatAdapter = ChatAdapter(
            onQuizChoice = { handlePracticeChoice(it) },
            onQuizSubmitted = { msg, quiz, score -> handleQuizSubmission(msg, quiz, score) }
        )
        rvChatHistory.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvChatHistory.adapter = chatAdapter
    }

    private fun handleUserQuestion() {
        val question = etAskAI.text.toString().trim()
        if (question.isEmpty()) return
        val book = currentBook ?: return
        val userId = auth.currentUser?.uid ?: return

        val userMsg = ChatMessage(bookId = book.id, role = "user", message = question)
        chatAdapter.addMessage(userMsg)
        saveChatMessage(book.id, userId, userMsg)

        pbChat.visibility = View.VISIBLE
        btnAskAI.isEnabled = false
        etAskAI.text.clear()

        lifecycleScope.launch {
            try {
                val response = ragService.processChat(question, book, getRecentHistory(book.id))
                if (!isFinishing && !isDestroyed) {
                    if (response.role == "assistant") {
                        simulateStreaming(book.id, userId, response.message)
                    } else {
                        val aiMsg = ChatMessage(bookId = book.id, role = response.role, message = response.message)
                        chatAdapter.addMessage(aiMsg)
                        saveChatMessage(book.id, userId, aiMsg)
                    }
                }
            } catch (e: Exception) {
                if (!isFinishing && !isDestroyed) chatAdapter.addMessage(ChatMessage(bookId = book.id, role = "assistant", message = "I'm a bit busy. Try again soon!"))
            } finally {
                if (!isFinishing && !isDestroyed) {
                    pbChat.visibility = View.GONE
                    btnAskAI.isEnabled = true
                }
            }
        }
    }

    private fun simulateStreaming(bookId: String, userId: String, fullText: String) {
        val aiMsg = ChatMessage(bookId = bookId, role = "assistant", message = "")
        chatAdapter.addMessage(aiMsg)
        
        lifecycleScope.launch {
            val words = fullText.split(" ")
            var currentText = ""
            words.forEachIndexed { index, word ->
                currentText += if (index == 0) word else " $word"
                chatAdapter.updateLastMessage(currentText)
                delay(60) // Typing speed
            }
            saveChatMessage(bookId, userId, aiMsg.apply { message = fullText })
        }
    }

    private fun updateRatingInFirebase(newRating: Float) {
        val book = currentBook ?: return
        if (book.id.startsWith("temp_") || book.id.startsWith("ai_")) {
            Toast.makeText(this, "Ratings only available for library books.", Toast.LENGTH_SHORT).show()
            return
        }

        val bookRef = database.child("books").child(book.id)
        bookRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(data: MutableData): Transaction.Result {
                val currentRating = data.child("averageRating").getValue(Float::class.java) ?: 0f
                val currentTotal = data.child("totalRatings").getValue(Int::class.java) ?: 0
                
                val newTotal = currentTotal + 1
                val newAvg = ((currentRating * currentTotal) + newRating) / newTotal
                
                data.child("averageRating").value = newAvg
                data.child("totalRatings").value = newTotal
                return Transaction.success(data)
            }
            override fun onComplete(e: DatabaseError?, committed: Boolean, snap: DataSnapshot?) {
                if (committed) Toast.makeText(this@BookDetailActivity, "Rating saved!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ... rest of the helper methods (fetchBookDetails, paginateDescription, updatePage, etc.) remain as previously improved
    private fun fetchBookDetails(bookId: String) {
        lifecycleScope.launch {
            val book = withContext(Dispatchers.IO) { bookRepo.getFullBookDetails(bookId) }
            if (isFinishing || isDestroyed) return@launch
            currentBook = book
            book?.let { b ->
                tvTitle.text = b.title
                tvAuthor.text = b.author
                ratingBar.rating = b.averageRating
                Glide.with(this@BookDetailActivity).load(b.coverUrl).placeholder(R.drawable.ic_splash_book).into(ivCover)
                paginateDescription(b.description)
                if (b.description.split("\\s+".toRegex()).size < 100) expandShortDescription(b)
            }
        }
    }

    private fun paginateDescription(content: String) {
        descriptionPages = content.chunked(500).take(5)
        currentPage = 0
        updatePage(0)
    }

    private fun updatePage(index: Int) {
        if (descriptionPages.isEmpty()) return
        tvDescription.text = descriptionPages[index]
        tvPageIndicator.text = "Page ${index + 1} of ${descriptionPages.size}"
        btnPrevPage.isEnabled = index > 0
        btnNextPage.isEnabled = index < descriptionPages.size - 1
    }

    private fun expandShortDescription(book: Book) {
        lifecycleScope.launch {
            try {
                val expanded = withContext(Dispatchers.IO) {
                    geminiClient.expandDescription(book.title, book.author, book.description, book.genre)
                }
                if (expanded.isNotEmpty() && expanded != book.description) {
                    book.description = expanded
                    paginateDescription(expanded)
                    database.child("books").child(book.id).child("description").setValue(expanded)
                }
            } catch (e: Exception) { Log.e("BookDetail", "Expansion failed", e) }
        }
    }

    private suspend fun getRecentHistory(bookId: String): String = withContext(Dispatchers.IO) {
        roomDb.chatMessageDao().getChatHistory(bookId).takeLast(6).joinToString("\n") { "${it.role}: ${it.message}" }
    }

    private fun handlePracticeChoice(isFullQuiz: Boolean) {
        val bookId = currentBook?.id ?: return
        val userId = auth.currentUser?.uid ?: return
        pbChat.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val book = currentBook ?: return@launch
                val context = "${book.title} by ${book.author}. ${book.description}"
                val quizJson = withContext(Dispatchers.IO) {
                    if (isFullQuiz) geminiClient.generateQuiz(book.title, context)
                    else geminiClient.generateSingleQuestion(book.title, context)
                }
                if (!isFinishing && !isDestroyed && quizJson.contains("questions")) {
                    val aiMsg = ChatMessage(bookId = bookId, role = "quiz", message = if (isFullQuiz) "Full Quiz" else "Quick Question", quizJson = quizJson)
                    chatAdapter.addMessage(aiMsg)
                    saveChatMessage(bookId, userId, aiMsg)
                }
            } catch (e: Exception) { Log.e("BookDetail", "Quiz failed", e) }
            finally { if (!isFinishing && !isDestroyed) pbChat.visibility = View.GONE }
        }
    }

    private fun handleQuizSubmission(chatMsg: ChatMessage, quiz: Quiz, score: Int) {
        val userId = auth.currentUser?.uid ?: return
        val bookId = currentBook?.id ?: return
        val statsRef = database.child("users").child(userId).child("stats")
        statsRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(data: MutableData): Transaction.Result {
                data.child("totalQuizScore").value = (data.child("totalQuizScore").getValue(Long::class.java) ?: 0L) + score
                data.child("totalQuizzesTaken").value = (data.child("totalQuizzesTaken").getValue(Long::class.java) ?: 0L) + 1
                return Transaction.success(data)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {}
        })
        saveChatMessage(bookId, userId, chatMsg)
        lifecycleScope.launch(Dispatchers.IO) { roomDb.chatMessageDao().insert(chatMsg) }
    }

    private fun borrowBook() {
        val book = currentBook ?: return
        val userId = auth.currentUser?.uid ?: return
        btnBorrow.isEnabled = false
        database.child("borrowRecords").orderByChild("userId").equalTo(userId).get().addOnSuccessListener { snapshot ->
            if (isFinishing || isDestroyed) return@addOnSuccessListener
            val borrowedCount = snapshot.children.count { it.child("status").getValue(String::class.java) == "BORROWED" }
            if (borrowedCount >= Constants.MAX_BORROW_LIMIT) {
                Toast.makeText(this, "Limit reached", Toast.LENGTH_SHORT).show()
                btnBorrow.isEnabled = true
                return@addOnSuccessListener
            }
            val ref = database.child("borrowRecords").push()
            val record = BorrowRecord(id = ref.key ?: "", bookId = book.id, userId = userId, bookTitle = book.title, bookAuthor = book.author, bookCoverUrl = book.coverUrl)
            ref.setValue(record).addOnSuccessListener {
                database.child("books").child(book.id).child("availableCopies").setValue(book.availableCopies - 1).addOnSuccessListener { finish() }
            }.addOnFailureListener { if (!isFinishing && !isDestroyed) btnBorrow.isEnabled = true }
        }
    }

    private fun recordVisit(bookId: String) {
        val userId = auth.currentUser?.uid ?: return
        database.child("visits").child(userId).child(bookId).setValue(System.currentTimeMillis())
    }

    private fun loadLocalChatHistory(bookId: String) {
        lifecycleScope.launch {
            val history = withContext(Dispatchers.IO) { roomDb.chatMessageDao().getChatHistory(bookId) }
            if (!isFinishing && !isDestroyed) chatAdapter.setMessages(history)
        }
    }

    private fun fetchFirebaseChatHistory(bookId: String) {
        val userId = auth.currentUser?.uid ?: return
        database.child("chats").child(bookId).child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing || isDestroyed) return
                val cloudMessages = snapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }
                if (cloudMessages.isNotEmpty()) {
                    chatAdapter.setMessages(cloudMessages)
                    lifecycleScope.launch(Dispatchers.IO) {
                        roomDb.chatMessageDao().clearHistoryForBook(bookId)
                        cloudMessages.forEach { roomDb.chatMessageDao().insert(it) }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showClearChatConfirmation() {
        AlertDialog.Builder(this).setTitle("Clear Chat").setMessage("Delete history?").setPositiveButton("Clear") { _, _ -> clearChatHistory() }.setNegativeButton("Cancel", null).show()
    }

    private fun clearChatHistory() {
        val bookId = currentBook?.id ?: return
        val userId = auth.currentUser?.uid ?: return
        database.child("chats").child(bookId).child(userId).removeValue()
        lifecycleScope.launch(Dispatchers.IO) {
            roomDb.chatMessageDao().clearHistoryForBook(bookId)
            withContext(Dispatchers.Main) { if (!isFinishing && !isDestroyed) chatAdapter.clearMessages() }
        }
    }

    private fun saveChatMessage(bookId: String, userId: String, message: ChatMessage) {
        database.child("chats").child(bookId).child(userId).push().setValue(message)
        lifecycleScope.launch(Dispatchers.IO) { roomDb.chatMessageDao().insert(message) }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch(Dispatchers.IO) { roomDb.chatMessageDao().cleanupOldHistory() }
    }
}
