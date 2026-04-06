package com.rajnishkumar.bookbuddy.ui.dashboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.rajnishkumar.bookbuddy.R
import com.rajnishkumar.bookbuddy.ai.GeminiClient
import com.rajnishkumar.bookbuddy.ai.OpenLibraryClient
import com.rajnishkumar.bookbuddy.database.AppDatabase
import com.rajnishkumar.bookbuddy.models.Book
import com.rajnishkumar.bookbuddy.ui.book.BookDetailActivity
import com.rajnishkumar.bookbuddy.ui.book.SearchFragment
import com.rajnishkumar.bookbuddy.ui.sensor.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MemberDashboard : BaseActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fabVocalRobo: com.google.android.material.floatingactionbutton.FloatingActionButton
    
    private lateinit var ivDashProfilePic: ImageView
    private lateinit var tvDashName: TextView

    private var speechRecognizer: SpeechRecognizer? = null
    private var vocalDialog: AlertDialog? = null
    private val RECORD_AUDIO_REQUEST_CODE = 101

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var roomDb: AppDatabase
    private val openLibrary = OpenLibraryClient()
    private val geminiClient = GeminiClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_dashboard)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        roomDb = AppDatabase.getDatabase(this)

        bottomNav = findViewById(R.id.memberBottomNav)
        fabVocalRobo = findViewById(R.id.fabVocalRobo)
        ivDashProfilePic = findViewById(R.id.ivDashProfilePic)
        tvDashName = findViewById(R.id.tvDashName)

        setupVoiceComponents()
        loadTopBarInfo()
        setupFragmentResultListeners()

        fabVocalRobo.setOnClickListener { checkPermissionAndShowPopup() }

        if (savedInstanceState == null) loadFragment(MemberHomeFragment())

        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home -> MemberHomeFragment()
                R.id.nav_search -> SearchFragment()
                R.id.nav_scan -> ScannerFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> MemberHomeFragment()
            }
            loadFragment(fragment)
            true
        }
    }

    private fun setupFragmentResultListeners() {
        supportFragmentManager.setFragmentResultListener("scanner_request", this) { _, bundle ->
            val isbn = bundle.getString("SCAN_RESULT")
            if (isbn != null) {
                lookupBookByISBN(isbn)
            }
        }
    }

    private fun lookupBookByISBN(isbn: String) {
        val loadingDialog = AlertDialog.Builder(this)
            .setMessage("Checking Library for ISBN: $isbn...")
            .setCancelable(false)
            .show()
        
        val sanitizedIsbn = isbn.replace(Regex("[^a-zA-Z0-9]"), "")
        database.child("books").get().addOnSuccessListener { snapshot ->
            if (isFinishing || isDestroyed) return@addOnSuccessListener
            
            // Check if any book in our library matches this ISBN
            var foundId: String? = null
            for (child in snapshot.children) {
                if (child.child("isbn").getValue(String::class.java) == isbn) {
                    foundId = child.key
                    break
                }
            }

            loadingDialog.dismiss()
            if (foundId != null) {
                val intent = Intent(this, BookDetailActivity::class.java)
                intent.putExtra("BOOK_ID", foundId)
                startActivity(intent)
            } else {
                fetchExternalBookInfo(isbn)
            }
        }.addOnFailureListener {
            if (isFinishing || isDestroyed) return@addOnFailureListener
            loadingDialog.dismiss()
            Toast.makeText(this, "Library check failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchExternalBookInfo(isbn: String) {
        val loadingDialog = AlertDialog.Builder(this)
            .setMessage("Asking AI Librarian for details...")
            .setCancelable(false)
            .show()

        lifecycleScope.launch {
            try {
                // 1. Try free APIs first (Fast)
                var book = withContext(Dispatchers.IO) { openLibrary.getBookByISBN(isbn) }
                
                // 2. If API fails or gives empty data, ask Gemini to research it (Smart)
                if (book == null || book.title == "Unknown Title") {
                    val aiPrompt = "Find details for book with ISBN: $isbn. Return exactly: Title | Author | 100-word Description | 3 Genres. Plain text only."
                    val aiRaw = withContext(Dispatchers.IO) { geminiClient.generateDirectResponse(aiPrompt) }
                    
                    if (aiRaw.isNotEmpty() && aiRaw.contains("|")) {
                        val parts = aiRaw.split("|").map { it.trim() }
                        book = Book(
                            id = "ai_$isbn",
                            title = parts.getOrNull(0) ?: "Book Found",
                            author = parts.getOrNull(1) ?: "Unknown",
                            description = parts.getOrNull(2) ?: "No description found.",
                            genre = parts.getOrNull(3)?.split(",")?.map { it.trim() } ?: listOf("General"),
                            isbn = isbn
                        )
                    }
                }

                if (!isFinishing && !isDestroyed) {
                    loadingDialog.dismiss()
                    if (book != null) showExternalBookInfo(book)
                    else Toast.makeText(this@MemberDashboard, "Book info not found anywhere.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                if (!isFinishing && !isDestroyed) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@MemberDashboard, "AI Librarian is busy. Try again!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showExternalBookInfo(book: Book) {
        if (isFinishing || isDestroyed) return
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_external_book_info, null)
        dialogView.findViewById<TextView>(R.id.tvExtTitle).text = book.title
        dialogView.findViewById<TextView>(R.id.tvExtAuthor).text = "By ${book.author}"
        dialogView.findViewById<TextView>(R.id.tvExtDesc).text = book.description
        
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun loadTopBarInfo() {
        val uid = auth.currentUser?.uid ?: return
        database.child("users").child(uid).child("name").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { 
                if (!isFinishing && !isDestroyed) {
                    tvDashName.text = snapshot.getValue(String::class.java) ?: "User" 
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        lifecycleScope.launch {
            val localProfile = withContext(Dispatchers.IO) { roomDb.userProfileDao().getProfile(uid) }
            if (!isFinishing && !isDestroyed) {
                localProfile?.profilePicPath?.let { path ->
                    ivDashProfilePic.setPadding(0, 0, 0, 0)
                    ivDashProfilePic.imageTintList = null
                    Glide.with(this@MemberDashboard).load(Uri.parse(path)).placeholder(R.drawable.ic_profile).circleCrop().into(ivDashProfilePic)
                }
            }
        }
    }

    private fun setupVoiceComponents() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
    }

    private fun checkPermissionAndShowPopup() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
        } else showVocalPopup()
    }

    private fun showVocalPopup() {
        if (speechRecognizer == null || isFinishing || isDestroyed) return
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_vocal_listener, null)
        vocalDialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(true)
            .setOnDismissListener { speechRecognizer?.stopListening() }.create()
        vocalDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        vocalDialog?.show()
        startVoiceListening()
    }

    private fun startVoiceListening() {
        val recognizer = speechRecognizer ?: return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onResults(results: Bundle?) {
                vocalDialog?.dismiss()
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0)?.let { query ->
                    if (!isFinishing && !isDestroyed) {
                        startActivity(Intent(this@MemberDashboard, VocalSearchResultsActivity::class.java).putExtra("query", query))
                    }
                }
            }
            override fun onError(error: Int) { vocalDialog?.dismiss() }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer.startListening(intent)
    }

    override fun onDestroy() { 
        super.onDestroy()
        speechRecognizer?.destroy() 
        speechRecognizer = null
        vocalDialog?.dismiss()
    }
    
    private fun loadFragment(fragment: Fragment) { 
        if (!isFinishing && !isDestroyed) {
            supportFragmentManager.beginTransaction().replace(R.id.memberNavHost, fragment).addToBackStack(null).commit() 
        }
    }
}
