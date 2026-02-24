package com.example.bookbuddy.activities.fragments.librarian

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.bookbuddy.ai.BookAIHelper
import com.example.bookbuddy.databinding.FragmentAddBookBinding
import com.example.bookbuddy.models.Book
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddBookFragment : Fragment() {

    private var _binding: FragmentAddBookBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var bookAIHelper: BookAIHelper

    private var selectedImageUri: Uri? = null
    private var isUploading = false
    private val TAG = "AddBookFragment"

    private val genres = arrayOf(
        "Fiction", "Non-Fiction", "Science Fiction", "Fantasy",
        "Mystery", "Thriller", "Romance", "Horror", "Biography",
        "History", "Self-Help", "Business", "Children", "Young Adult",
        "Poetry", "Drama", "Comedy", "Adventure", "Other"
    )

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.ivBookCover.setImageURI(it)
            binding.ivBookCover.visibility = View.VISIBLE
            binding.tvImageStatus.text = "✅ Image selected"
            binding.tvImageStatus.visibility = View.VISIBLE
            binding.coverUrlInput.setText("")
        }
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            selectedImageUri = null
            binding.ivBookCover.setImageBitmap(it)
            binding.ivBookCover.visibility = View.VISIBLE
            binding.tvImageStatus.text = "✅ Photo taken"
            binding.tvImageStatus.visibility = View.VISIBLE
            binding.coverUrlInput.setText("")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        bookAIHelper = BookAIHelper()

        setupGenreSpinner()
        setupClickListeners()

        binding.copiesInput.setText("1")
        binding.languageInput.setText("English")
    }

    private fun setupGenreSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, genres)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.genreSpinner.adapter = adapter

        binding.genreSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.customGenreInput.visibility = if (genres[position] == "Other") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                binding.customGenreInput.visibility = View.GONE
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnUploadImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnTakePhoto.setOnClickListener {
            takePhotoLauncher.launch(null)
        }

        binding.btnAddBook.setOnClickListener {
            addBook()
        }

        binding.btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun addBook() {
        val title = binding.titleInput.text.toString().trim()
        val author = binding.authorInput.text.toString().trim()
        val description = binding.descriptionInput.text.toString().trim()
        val summary = binding.summaryInput.text.toString().trim()
        val isbn = binding.isbnInput.text.toString().trim()
        val language = binding.languageInput.text.toString().trim()
        val publisher = binding.publisherInput.text.toString().trim()
        val coverUrl = binding.coverUrlInput.text.toString().trim()

        val copies = binding.copiesInput.text.toString().toIntOrNull() ?: 1
        val pageCount = binding.pageCountInput.text.toString().toIntOrNull() ?: 0

        val genre = if (binding.genreSpinner.selectedItem.toString() == "Other") {
            binding.customGenreInput.text.toString().trim()
        } else {
            binding.genreSpinner.selectedItem.toString()
        }

        // Validate inputs
        if (title.isEmpty()) {
            binding.titleInput.error = "Title is required"
            return
        }

        if (author.isEmpty()) {
            binding.authorInput.error = "Author is required"
            return
        }

        if (genre.isEmpty()) {
            Toast.makeText(requireContext(), "Genre is required", Toast.LENGTH_SHORT).show()
            return
        }

        // Create book object (without embedding/aiSummary)
        val book = Book(
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
            addedAt = System.currentTimeMillis()
        )

        // Save to Firestore
        saveBookToFirestore(book)
    }

    private fun saveBookToFirestore(book: Book) {
        showLoading(true)

        // Update UI to show AI processing will happen
        binding.tvImageStatus.text = "📚 Saving book..."
        binding.tvImageStatus.visibility = View.VISIBLE

        db.collection("books").add(book)
            .addOnSuccessListener { documentReference ->
                book.id = documentReference.id

                // First save the basic book info
                documentReference.set(book)
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ Book saved to Firestore: ${book.title}")

                        // Show success message
                        Toast.makeText(requireContext(),
                            "✅ Book added! Generating AI summary...",
                            Toast.LENGTH_SHORT).show()

                        binding.tvImageStatus.text = "🤖 Generating AI features..."

                        // Now process AI in background WITHOUT closing fragment yet
                        processBookWithAI(book, documentReference)
                    }
                    .addOnFailureListener { e ->
                        showLoading(false)
                        Log.e(TAG, "❌ Error saving book: ${e.message}")
                        Toast.makeText(requireContext(),
                            "Error saving book: ${e.message}",
                            Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e(TAG, "❌ Error adding book: ${e.message}")
                Toast.makeText(requireContext(),
                    "Error adding book: ${e.message}",
                    Toast.LENGTH_LONG).show()
            }
    }

    private fun processBookWithAI(book: Book, documentRef: DocumentReference) {
        lifecycleScope.launch {
            try {
                // Show AI processing status
                withContext(Dispatchers.Main) {
                    binding.tvImageStatus.text = "🧠 Generating embedding and summary..."
                }

                // Process AI in background
                bookAIHelper.processNewBook(book) { processedBook ->
                    // Update Firestore with AI data
                    documentRef.set(processedBook)
                        .addOnSuccessListener {
                            Log.d(TAG, "✅ AI features added for: ${processedBook.title}")
                            Log.d(TAG, "   Embedding size: ${processedBook.embedding.size}")
                            Log.d(TAG, "   AI Summary: ${processedBook.aiSummary.take(50)}...")

                            // Now close the fragment
                            showLoading(false)
                            Toast.makeText(requireContext(),
                                "✅ Book ready with AI features!",
                                Toast.LENGTH_LONG).show()
                            parentFragmentManager.popBackStack()
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "❌ Failed to update AI data: ${e.message}")
                            showLoading(false)
                            Toast.makeText(requireContext(),
                                "⚠️ Book saved but AI features failed",
                                Toast.LENGTH_LONG).show()
                            parentFragmentManager.popBackStack()
                        }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ AI processing error: ${e.message}", e)
                showLoading(false)
                Toast.makeText(requireContext(),
                    "⚠️ Book saved but AI processing failed",
                    Toast.LENGTH_LONG).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        isUploading = show
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnAddBook.isEnabled = !show
        binding.btnAddBook.alpha = if (show) 0.5f else 1.0f
        binding.btnUploadImage.isEnabled = !show
        binding.btnTakePhoto.isEnabled = !show

        // Show/hide status text
        if (show) {
            binding.tvImageStatus.visibility = View.VISIBLE
        } else {
            binding.tvImageStatus.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}