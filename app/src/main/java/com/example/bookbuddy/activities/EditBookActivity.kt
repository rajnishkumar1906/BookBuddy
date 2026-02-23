package com.example.bookbuddy.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.bookbuddy.R
import com.example.bookbuddy.models.Book
import com.example.bookbuddy.models.User
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.UUID

class EditBookActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference

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
    private lateinit var availableCopiesInput: TextInputEditText

    private lateinit var coverUrlInput: TextInputEditText
    private lateinit var btnUploadImage: Button
    private lateinit var btnTakePhoto: Button
    private lateinit var ivBookCover: ImageView
    private lateinit var tvImageStatus: TextView

    private lateinit var updateButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var cancelButton: Button

    private var bookId: String = ""
    private var currentBook: Book? = null
    private var selectedImageUri: Uri? = null
    private var uploadedImageUrl: String = ""
    private var isUploadingImage = false

    private val genres = arrayOf(
        "Fiction", "Non-Fiction", "Science Fiction", "Fantasy",
        "Mystery", "Thriller", "Romance", "Horror", "Biography",
        "History", "Self-Help", "Business", "Children", "Young Adult",
        "Poetry", "Drama", "Comedy", "Adventure", "Other"
    )

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            ivBookCover.setImageURI(it)
            ivBookCover.visibility = View.VISIBLE
            tvImageStatus.text = "✅ Image selected"
            tvImageStatus.visibility = View.VISIBLE
            coverUrlInput.setText("")
        }
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            selectedImageUri = null
            ivBookCover.setImageBitmap(it)
            ivBookCover.visibility = View.VISIBLE
            tvImageStatus.text = "✅ Photo taken"
            tvImageStatus.visibility = View.VISIBLE
            coverUrlInput.setText("")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_book)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        bookId = intent.getStringExtra("bookId") ?: run {
            Toast.makeText(this, "Book ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (auth.currentUser == null) {
            finish()
            return
        }

        initializeViews()
        setupGenreSpinner()
        setClickListeners()
        loadBookDetails()
    }

    private fun initializeViews() {
        titleInput = findViewById(R.id.titleInput)
        authorInput = findViewById(R.id.authorInput)
        genreSpinner = findViewById(R.id.genreSpinner)
        customGenreInput = findViewById(R.id.customGenreInput)
        descriptionInput = findViewById(R.id.descriptionInput)
        summaryInput = findViewById(R.id.summaryInput)
        isbnInput = findViewById(R.id.isbnInput)
        copiesInput = findViewById(R.id.copiesInput)
        availableCopiesInput = findViewById(R.id.availableCopiesInput)
        languageInput = findViewById(R.id.languageInput)
        publisherInput = findViewById(R.id.publisherInput)
        pageCountInput = findViewById(R.id.pageCountInput)

        coverUrlInput = findViewById(R.id.coverUrlInput)
        btnUploadImage = findViewById(R.id.btnUploadImage)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        ivBookCover = findViewById(R.id.ivBookCover)
        tvImageStatus = findViewById(R.id.tvImageStatus)

        updateButton = findViewById(R.id.updateButton)
        progressBar = findViewById(R.id.progressBar)
        cancelButton = findViewById(R.id.cancelButton)

        customGenreInput.visibility = View.GONE
    }

    private fun setupGenreSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genres)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        genreSpinner.adapter = adapter

        genreSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                customGenreInput.visibility = if (genres[position] == "Other") View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                customGenreInput.visibility = View.GONE
            }
        }
    }

    private fun setClickListeners() {
        btnUploadImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnTakePhoto.setOnClickListener {
            takePhotoLauncher.launch(null)
        }

        updateButton.setOnClickListener {
            updateBook()
        }

        cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun loadBookDetails() {
        showLoading(true)

        db.collection("books").document(bookId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)

                if (!document.exists()) {
                    Toast.makeText(this, "Book not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                currentBook = document.toObject(Book::class.java)
                currentBook?.id = document.id

                displayBookDetails()
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, "Error loading book", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayBookDetails() {
        currentBook?.let { book ->
            titleInput.setText(book.title)
            authorInput.setText(book.author)
            descriptionInput.setText(book.description)
            summaryInput.setText(book.summary)
            isbnInput.setText(book.isbn)
            copiesInput.setText(book.totalCopies.toString())
            availableCopiesInput.setText(book.availableCopies.toString())
            languageInput.setText(book.language)
            publisherInput.setText(book.publisher)
            pageCountInput.setText(book.pageCount.toString())
            coverUrlInput.setText(book.coverUrl)

            // Set genre spinner
            val genrePosition = genres.indexOfFirst { it.equals(book.genre, ignoreCase = true) }
            if (genrePosition >= 0) {
                genreSpinner.setSelection(genrePosition)
            } else {
                genreSpinner.setSelection(genres.size - 1) // Other
                customGenreInput.setText(book.genre)
                customGenreInput.visibility = View.VISIBLE
            }

            // Load cover image
            if (book.coverUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(book.coverUrl)
                    .placeholder(R.drawable.ic_book_placeholder)
                    .error(R.drawable.ic_book_error)
                    .into(ivBookCover)
                ivBookCover.visibility = View.VISIBLE
            }
        }
    }

    private fun updateBook() {
        val title = titleInput.text.toString().trim()
        val author = authorInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()
        val summary = summaryInput.text.toString().trim()
        val isbn = isbnInput.text.toString().trim()
        val language = languageInput.text.toString().trim()
        val publisher = publisherInput.text.toString().trim()
        val coverUrl = coverUrlInput.text.toString().trim()

        val totalCopies = copiesInput.text.toString().toIntOrNull() ?: 1
        val availableCopies = availableCopiesInput.text.toString().toIntOrNull() ?: totalCopies
        val pageCount = pageCountInput.text.toString().toIntOrNull() ?: 0

        val genre = if (genreSpinner.selectedItem.toString() == "Other") {
            customGenreInput.text.toString().trim()
        } else {
            genreSpinner.selectedItem.toString()
        }

        if (title.isEmpty()) {
            titleInput.error = "Title is required"
            titleInput.requestFocus()
            return
        }

        if (author.isEmpty()) {
            authorInput.error = "Author is required"
            authorInput.requestFocus()
            return
        }

        if (genre.isEmpty()) {
            Toast.makeText(this, "Genre is required", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUri != null) {
            uploadImageAndUpdateBook(title, author, genre, description, summary, isbn,
                totalCopies, availableCopies, language, publisher, pageCount, coverUrl)
        } else {
            saveUpdatedBook(title, author, genre, description, summary, isbn,
                totalCopies, availableCopies, language, publisher, pageCount, coverUrl)
        }
    }

    private fun uploadImageAndUpdateBook(
        title: String, author: String, genre: String, description: String,
        summary: String, isbn: String, totalCopies: Int, availableCopies: Int,
        language: String, publisher: String, pageCount: Int, coverUrl: String
    ) {
        showLoading(true)
        isUploadingImage = true

        val imageRef = storageRef.child("book_covers/${UUID.randomUUID()}.jpg")

        selectedImageUri?.let { uri ->
            imageRef.putFile(uri)
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    tvImageStatus.text = "Uploading: $progress%"
                }
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        uploadedImageUrl = downloadUri.toString()
                        saveUpdatedBook(title, author, genre, description, summary, isbn,
                            totalCopies, availableCopies, language, publisher, pageCount,
                            uploadedImageUrl)
                    }
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    isUploadingImage = false
                    Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun saveUpdatedBook(
        title: String, author: String, genre: String, description: String,
        summary: String, isbn: String, totalCopies: Int, availableCopies: Int,
        language: String, publisher: String, pageCount: Int, coverUrl: String
    ) {
        val updatedBook = currentBook?.copy(
            title = title,
            author = author,
            genre = genre,
            description = description,
            summary = summary,
            isbn = isbn,
            coverUrl = coverUrl,
            totalCopies = totalCopies,
            availableCopies = availableCopies,
            language = language,
            publisher = publisher,
            pageCount = pageCount,
            keywords = generateKeywords(title, author, genre, description)
        )

        if (updatedBook == null) {
            showLoading(false)
            Toast.makeText(this, "Error updating book", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("books").document(bookId)
            .set(updatedBook)
            .addOnSuccessListener {
                showLoading(false)
                isUploadingImage = false
                Toast.makeText(this, "✅ Book updated successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                isUploadingImage = false
                Toast.makeText(this, "Error updating: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun generateKeywords(title: String, author: String, genre: String, description: String): List<String> {
        val keywords = mutableListOf<String>()

        title.split(" ").forEach {
            if (it.length > 3) keywords.add(it.lowercase())
        }

        author.split(" ").forEach {
            if (it.length > 2) keywords.add(it.lowercase())
        }

        keywords.add(genre.lowercase())

        description.split(" ").take(5).forEach { word ->
            val cleanWord = word.lowercase().replace("[^a-zA-Z]".toRegex(), "")
            if (cleanWord.length > 4) {
                keywords.add(cleanWord)
            }
        }

        return keywords.distinct()
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            updateButton.isEnabled = false
            updateButton.alpha = 0.5f
            cancelButton.isEnabled = false
            cancelButton.alpha = 0.5f
        } else {
            progressBar.visibility = View.GONE
            updateButton.isEnabled = true
            updateButton.alpha = 1.0f
            cancelButton.isEnabled = true
            cancelButton.alpha = 1.0f
        }
    }
}