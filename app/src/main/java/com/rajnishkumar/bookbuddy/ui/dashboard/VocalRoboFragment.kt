package com.rajnishkumar.bookbuddy.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rajnishkumar.bookbuddy.R
import com.rajnishkumar.bookbuddy.ai.AISearchHelper
import com.rajnishkumar.bookbuddy.ai.GeminiClient
import com.rajnishkumar.bookbuddy.ai.HuggingFaceClient
import com.rajnishkumar.bookbuddy.models.Book
import com.rajnishkumar.bookbuddy.repository.BookSearchRepository
import com.rajnishkumar.bookbuddy.ui.canvas.AISearchVisualizerView
import kotlinx.coroutines.*
import java.util.*

class VocalRoboFragment : Fragment(), TextToSpeech.OnInitListener {

    private lateinit var tvSpokenText: TextView
    private lateinit var tvRoboStatus: TextView
    private lateinit var tvRoboResponse: TextView
    private lateinit var cardRoboMic: View
    private lateinit var cardVocalResults: View
    private lateinit var visualizer: AISearchVisualizerView
    private lateinit var ivRobo: ImageView

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    
    private val huggingFace = HuggingFaceClient()
    private val gemini = GeminiClient()
    private val searchHelper = AISearchHelper()
    private val bookRepo by lazy { BookSearchRepository.getInstance(requireContext()) }
    
    private var isListening = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_vocal_robo, container, false)

        tvSpokenText = view.findViewById(R.id.tvSpokenText)
        tvRoboStatus = view.findViewById(R.id.tvRoboStatus)
        tvRoboResponse = view.findViewById(R.id.tvRoboResponse)
        cardRoboMic = view.findViewById(R.id.cardRoboMic)
        cardVocalResults = view.findViewById(R.id.cardVocalResults)
        visualizer = view.findViewById(R.id.vocalVisualizer)
        ivRobo = view.findViewById(R.id.ivRoboIcon)

        setupVoiceComponents()

        cardRoboMic.setOnClickListener {
            if (!isListening) startListening() else stopListening()
        }

        return view
    }

    private fun setupVoiceComponents() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        tts = TextToSpeech(requireContext(), this)
    }

    private fun startListening() {
        isListening = true
        tvRoboStatus.text = "Listening..."
        visualizer.setSearching(true)
        val pulse = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse)
        cardRoboMic.startAnimation(pulse)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { stopListening() }
            override fun onError(error: Int) {
                stopListening()
                Toast.makeText(requireContext(), "Couldn't hear you. Try again!", Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0)?.let { text ->
                    tvSpokenText.text = "\"$text\""
                    performVocalSearch(text)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer.startListening(intent)
    }

    private fun stopListening() {
        isListening = false
        tvRoboStatus.text = "Tap to Talk"
        visualizer.setSearching(false)
        cardRoboMic.clearAnimation()
        speechRecognizer.stopListening()
    }

    private fun performVocalSearch(query: String) {
        tvRoboStatus.text = "Searching..."
        cardVocalResults.visibility = View.GONE
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Perform LOCAL Search first (Instant matching)
                val results = withContext(Dispatchers.IO) { bookRepo.searchBooks(query, limit = 4) }

                if (results.isEmpty()) {
                    speakAndShow("I'm looking into my library, but I couldn't find any books that match that description. Try describing the genre or topic differently!")
                    return@launch
                }

                // AI Spoken Response
                val resultsContext = results.joinToString("\n") { 
                    "Title: ${it.title}, Author: ${it.author}, Genres: ${it.genre.joinToString(", ")}" 
                }
                
                val prompt = """
                    You are Robo, the AI Librarian. Respond to the query: "$query".
                    Found these matches:
                    ${resultsContext}
                    
                    Task: Provide a warm, helpful spoken summary of these recommendations in 2 or 3 sentences. 
                    Be natural, talk like a human. No markdown symbols. Plain text only.
                """.trimIndent()

                val spokenResponse = withContext(Dispatchers.IO) { gemini.generateDirectResponse(prompt) }
                
                if (spokenResponse.isNotEmpty()) {
                    speakAndShow(spokenResponse)
                } else {
                    speakAndShow("I found some great matches for you! Have a look at the books listed below.")
                }

            } catch (e: Exception) {
                Log.e("VocalRobo", "Search failed", e)
                speakAndShow("I'm having a little trouble thinking right now. Could you please try asking again?")
            } finally {
                tvRoboStatus.text = "Tap to Talk"
            }
        }
    }

    private fun speakAndShow(text: String) {
        if (!isAdded) return
        cardVocalResults.visibility = View.VISIBLE
        tvRoboResponse.text = text
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "VocalRobo")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale.US
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        tts.stop()
        tts.shutdown()
    }
}
