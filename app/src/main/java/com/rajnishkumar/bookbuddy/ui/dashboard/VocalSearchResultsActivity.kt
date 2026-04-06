package com.rajnishkumar.bookbuddy.ui.dashboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.rajnishkumar.bookbuddy.R
import com.rajnishkumar.bookbuddy.adapters.BookAdapter
import com.rajnishkumar.bookbuddy.ai.AISearchHelper
import com.rajnishkumar.bookbuddy.ai.GeminiClient
import com.rajnishkumar.bookbuddy.ai.HuggingFaceClient
import com.rajnishkumar.bookbuddy.models.Book
import com.rajnishkumar.bookbuddy.repository.BookSearchRepository
import com.rajnishkumar.bookbuddy.ui.book.BookDetailActivity
import com.rajnishkumar.bookbuddy.ui.canvas.AISearchVisualizerView
import com.rajnishkumar.bookbuddy.ui.sensor.BaseActivity
import kotlinx.coroutines.*
import java.util.*

class VocalSearchResultsActivity : BaseActivity(), TextToSpeech.OnInitListener {

    private lateinit var tvVocalQuery: TextView
    private lateinit var tvRoboSummary: TextView
    private lateinit var rvVocalResults: RecyclerView
    private lateinit var pbVocalSearch: ProgressBar
    private lateinit var visualizer: AISearchVisualizerView

    private lateinit var tts: TextToSpeech
    private var speechRecognizer: SpeechRecognizer? = null
    
    private val huggingFace = HuggingFaceClient()
    private val gemini = GeminiClient()
    private val searchHelper = AISearchHelper()
    private val bookRepo by lazy { BookSearchRepository.getInstance(this) }

    private val allBooks = mutableListOf<Book>()
    private lateinit var adapter: BookAdapter
    private var vocalQuery: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vocal_search_results)

        tvVocalQuery = findViewById(R.id.tvVocalQuery)
        tvRoboSummary = findViewById(R.id.tvRoboSummary)
        rvVocalResults = findViewById(R.id.rvVocalResults)
        pbVocalSearch = findViewById(R.id.pbVocalSearch)
        visualizer = findViewById(R.id.vocalSearchVisualizer)

        adapter = BookAdapter { book ->
            val intent = Intent(this, BookDetailActivity::class.java)
            intent.putExtra("BOOK_ID", book.id)
            startActivity(intent)
        }

        rvVocalResults.layoutManager = LinearLayoutManager(this)
        rvVocalResults.adapter = adapter
        rvVocalResults.isNestedScrollingEnabled = false

        tts = TextToSpeech(this, this)
        
        vocalQuery = intent.getStringExtra("query")
        
        if (vocalQuery != null) {
            tvVocalQuery.text = "You asked: \"$vocalQuery\""
            fetchAllBooksAndSearch(vocalQuery!!)
        } else {
            checkPermissionAndListen()
        }
    }

    private fun checkPermissionAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        } else {
            startListening()
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                tvVocalQuery.text = "Robo is listening..."
                visualizer.setSearching(true)
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    vocalQuery = matches[0]
                    tvVocalQuery.text = "You asked: \"$vocalQuery\""
                    fetchAllBooksAndSearch(vocalQuery!!)
                }
            }
            override fun onError(error: Int) {
                tvVocalQuery.text = "Robo couldn't hear you. Tap to try again."
                visualizer.setSearching(false)
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    private fun fetchAllBooksAndSearch(query: String) {
        pbVocalSearch.visibility = View.VISIBLE
        visualizer.setSearching(true)
        
        CoroutineScope(Dispatchers.Main).launch {
            // CRITICAL FIX: Use the optimized repository with resilient parsing
            val books = withContext(Dispatchers.IO) { bookRepo.getSearchMetadata() }
            allBooks.clear()
            allBooks.addAll(books)
            performVocalSearch(query)
        }
    }

    private fun performVocalSearch(query: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // 1. Semantic Search using embeddings
                val queryEmb = withContext(Dispatchers.IO) { huggingFace.getEmbedding(query) }
                
                // 2. Perform advanced search using repository logic (includes fallback)
                val results = withContext(Dispatchers.IO) { bookRepo.searchBooks(query, limit = 8) }

                if (results.isEmpty()) {
                    speakAndShow("I'm sorry, I couldn't find any books that match that description. Try asking for a different genre or topic!")
                    return@launch
                }

                adapter.setBooks(results)

                // 3. Human-like natural language summary
                val top4 = results.take(4)
                val resultsContext = top4.joinToString("\n") { 
                    "Title: ${it.title}, Author: ${it.author}, Genres: ${it.genre.joinToString(", ")}" 
                }
                
                val prompt = """
                    You are Robo, a helpful and friendly AI Librarian. 
                    User Query: "$query"
                    Matched Books:
                    $resultsContext
                    
                    Task: Provide a warm, natural conversational summary of these recommendations in exactly 2 or 3 sentences. 
                    Instructions: 
                    - Talk like a human librarian.
                    - Highlight why these books fit the user's request.
                    - No asterisks or markdown. Plain text only.
                """.trimIndent()

                val response = withContext(Dispatchers.IO) { gemini.generateDirectResponse(prompt) }
                speakAndShow(response.ifEmpty { "I found some great options like ${top4[0].title}. Check them out below!" })

            } catch (e: Exception) {
                Log.e("VocalSearch", "Search failed", e)
                speakAndShow("I'm having a little trouble with my memory right now. Can you ask me again?")
            } finally {
                pbVocalSearch.visibility = View.GONE
                visualizer.setSearching(false)
            }
        }
    }

    private fun speakAndShow(text: String) {
        tvRoboSummary.text = text
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "Robo")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale.US
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        tts.stop()
        tts.shutdown()
    }
}
