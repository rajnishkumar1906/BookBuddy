package com.example.bookbuddy.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bookbuddy.R
import com.example.bookbuddy.models.Book
import com.example.bookbuddy.models.User
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddBookActivity : AppCompatActivity() {

    // UI Components
    private lateinit var titleInput: TextInputEditText
    private lateinit var authorInput: TextInputEditText
    private lateinit var genreSpinner: Spinner
    private lateinit var customGenreInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var summaryInput: TextInputEditText
    private lateinit var isbnInput: TextInputEditText
    private lateinit var copiesInput: TextInputEditText
    private lateinit var languageInput: TextInputEditText
    private lateinit var publisherInput: TextInputEditText
    private lateinit var pageCountInput: TextInputEditText
    private lateinit var coverUrlInput: TextInputEditText
    private lateinit var addButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var cancelButton: Button

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // User role
    private var isLibrarian = false

    // Predefined genres
    private val genres = arrayOf(
        "Fiction", "Non-Fiction", "Science Fiction", "Fantasy",
        "Mystery", "Thriller", "Romance", "Horror", "Biography",
        "History", "Self-Help", "Business", "Children", "Young Adult",
        "Poetry", "Drama", "Comedy", "Adventure", "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_add_book)

            // Initialize Firebase with try-catch
            try {
                auth = FirebaseAuth.getInstance()
                db = FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                Toast.makeText(this, "Firebase initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // Check if user is logged in
            try {
                if (auth.currentUser == null) {
                    Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    return
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Initialize views with try-catch
            try {
                initializeViews()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load UI: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // Verify librarian access
            verifyLibrarianAccess()

            // Setup genre spinner
            setupGenreSpinner()

            // Set click listeners
            setClickListeners()

        } catch (e: Exception) {
            Toast.makeText(this, "App error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        try {
            titleInput = findViewById(R.id.titleInput)
            authorInput = findViewById(R.id.authorInput)
            genreSpinner = findViewById(R.id.genreSpinner)
            customGenreInput = findViewById(R.id.customGenreInput)
            descriptionInput = findViewById(R.id.descriptionInput)
            summaryInput = findViewById(R.id.summaryInput)
            isbnInput = findViewById(R.id.isbnInput)
            copiesInput = findViewById(R.id.copiesInput)
            languageInput = findViewById(R.id.languageInput)
            publisherInput = findViewById(R.id.publisherInput)
            pageCountInput = findViewById(R.id.pageCountInput)
            coverUrlInput = findViewById(R.id.coverUrlInput)
            addButton = findViewById(R.id.addButton)
            progressBar = findViewById(R.id.progressBar)
            cancelButton = findViewById(R.id.cancelButton)

            // Set default values
            copiesInput.setText("1")
            languageInput.setText("English")

            // Initially hide custom genre input
            customGenreInput.visibility = View.GONE
        } catch (e: Exception) {
            throw Exception("View initialization failed: ${e.message}")
        }
    }

    private fun setupGenreSpinner() {
        try {
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genres)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            genreSpinner.adapter = adapter

            genreSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?,
                                            view: View?, position: Int, id: Long) {
                    try {
                        // Show custom input if "Other" is selected
                        customGenreInput.visibility = if (genres[position] == "Other") View.VISIBLE else View.GONE
                    } catch (e: Exception) {
                        // Ignore spinner errors
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    try {
                        customGenreInput.visibility = View.GONE
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to setup genre spinner: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun verifyLibrarianAccess() {
        try {
            showLoading(true)

            val userId = try {
                auth.currentUser?.uid ?: run {
                    Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this, "Error getting user ID", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    try {
                        showLoading(false)

                        val user = document.toObject(User::class.java)
                        isLibrarian = user?.role == "librarian"

                        if (!isLibrarian) {
                            Toast.makeText(
                                this,
                                "Access Denied: Only librarians can add books",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                    } catch (e: Exception) {
                        showLoading(false)
                        Toast.makeText(this, "Error processing user data", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .addOnFailureListener { e ->
                    try {
                        showLoading(false)
                        Toast.makeText(
                            this,
                            "Error verifying access: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } catch (ex: Exception) {
                        // Ignore
                    }
                }
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(this, "Verification error: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setClickListeners() {
        try {
            addButton.setOnClickListener {
                addBook()
            }

            cancelButton.setOnClickListener {
                try {
                    finish()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to setup buttons", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addBook() {
        try {
            // Get input values with null safety
            val title = try { titleInput.text.toString().trim() } catch (e: Exception) { "" }
            val author = try { authorInput.text.toString().trim() } catch (e: Exception) { "" }
            val description = try { descriptionInput.text.toString().trim() } catch (e: Exception) { "" }
            val summary = try { summaryInput.text.toString().trim() } catch (e: Exception) { "" }
            val isbn = try { isbnInput.text.toString().trim() } catch (e: Exception) { "" }
            val language = try { languageInput.text.toString().trim() } catch (e: Exception) { "" }
            val publisher = try { publisherInput.text.toString().trim() } catch (e: Exception) { "" }
            val coverUrl = try { coverUrlInput.text.toString().trim() } catch (e: Exception) { "" }

            val copies = try {
                copiesInput.text.toString().toIntOrNull() ?: 1
            } catch (e: Exception) {
                1
            }

            val pageCount = try {
                pageCountInput.text.toString().toIntOrNull() ?: 0
            } catch (e: Exception) {
                0
            }

            // Get genre from spinner
            val genre = try {
                if (genreSpinner.selectedItem.toString() == "Other") {
                    customGenreInput.text.toString().trim()
                } else {
                    genreSpinner.selectedItem.toString()
                }
            } catch (e: Exception) {
                ""
            }

            // Validate required fields
            try {
                when {
                    title.isEmpty() -> {
                        titleInput.error = "Title is required"
                        titleInput.requestFocus()
                        return
                    }
                    author.isEmpty() -> {
                        authorInput.error = "Author is required"
                        authorInput.requestFocus()
                        return
                    }
                    genre.isEmpty() -> {
                        if (customGenreInput.visibility == View.VISIBLE) {
                            customGenreInput.error = "Please enter a genre"
                            customGenreInput.requestFocus()
                        } else {
                            Toast.makeText(this, "Please select a genre", Toast.LENGTH_SHORT).show()
                        }
                        return
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Validation error", Toast.LENGTH_SHORT).show()
                return
            }

            // Show loading
            showLoading(true)

            // Create book object
            val book = try {
                Book(
                    title = title,
                    author = author,
                    genre = genre,
                    description = description,
                    summary = summary,
                    isbn = isbn,
                    coverUrl = coverUrl,
                    totalCopies = copies,
                    availableCopies = copies,
                    language = language.ifEmpty { "English" },
                    publisher = publisher,
                    pageCount = pageCount,
                    addedBy = auth.currentUser?.uid ?: "",
                    addedAt = System.currentTimeMillis(),
                    timesBorrowed = 0,
                    rating = 0f,
                    keywords = generateKeywords(title, author, genre, description)
                )
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this, "Error creating book: ${e.message}", Toast.LENGTH_SHORT).show()
                return
            }

            // Save to Firestore
            db.collection("books").add(book)
                .addOnSuccessListener { documentReference ->
                    try {
                        // Update book with its ID
                        book.id = documentReference.id
                        documentReference.set(book)

                        showLoading(false)
                        Toast.makeText(
                            this,
                            "✅ Book added successfully!",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()
                    } catch (e: Exception) {
                        showLoading(false)
                        Toast.makeText(this, "Error saving book: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    try {
                        showLoading(false)
                        Toast.makeText(
                            this,
                            "Error adding book: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (ex: Exception) {
                        // Ignore
                    }
                }
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(this, "Add book error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateKeywords(title: String, author: String, genre: String, description: String): List<String> {
        return try {
            val keywords = mutableListOf<String>()

            // Add title words (length > 3)
            title.split(" ").forEach {
                if (it.length > 3) keywords.add(it.lowercase())
            }

            // Add author name parts (length > 2)
            author.split(" ").forEach {
                if (it.length > 2) keywords.add(it.lowercase())
            }

            // Add genre
            keywords.add(genre.lowercase())

            // Add important words from description (first 5 words length > 4)
            description.split(" ").take(5).forEach { word ->
                try {
                    val cleanWord = word.lowercase().replace("[^a-zA-Z]".toRegex(), "")
                    if (cleanWord.length > 4) {
                        keywords.add(cleanWord)
                    }
                } catch (e: Exception) {
                    // Skip invalid words
                }
            }

            keywords.distinct()
        } catch (e: Exception) {
            emptyList() // Return empty list on error
        }
    }

    private fun showLoading(isLoading: Boolean) {
        try {
            if (isLoading) {
                progressBar.visibility = View.VISIBLE
                addButton.isEnabled = false
                addButton.alpha = 0.5f
                cancelButton.isEnabled = false
                cancelButton.alpha = 0.5f
                titleInput.isEnabled = false
                authorInput.isEnabled = false
                genreSpinner.isEnabled = false
                descriptionInput.isEnabled = false
            } else {
                progressBar.visibility = View.GONE
                addButton.isEnabled = true
                addButton.alpha = 1.0f
                cancelButton.isEnabled = true
                cancelButton.alpha = 1.0f
                titleInput.isEnabled = true
                authorInput.isEnabled = true
                genreSpinner.isEnabled = true
                descriptionInput.isEnabled = true
            }
        } catch (e: Exception) {
            // Ignore UI errors during loading
        }
    }
}