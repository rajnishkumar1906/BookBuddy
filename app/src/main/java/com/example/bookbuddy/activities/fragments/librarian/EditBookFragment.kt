package com.example.bookbuddy.fragments.librarian

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.bookbuddy.R
import com.example.bookbuddy.databinding.FragmentEditBookBinding
import com.example.bookbuddy.models.Book
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditBookFragment : Fragment() {

    private var _binding: FragmentEditBookBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var bookId: String = ""
    private var currentBook: Book? = null
    private var selectedImageUri: Uri? = null

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
            binding.coverUrlInput.setText("")
        }
    }

    companion object {
        fun newInstance(bookId: String): EditBookFragment {
            val fragment = EditBookFragment()
            val args = Bundle()
            args.putString("bookId", bookId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        bookId = arguments?.getString("bookId") ?: return

        setupGenreSpinner()
        setupClickListeners()
        loadBookDetails()
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

        binding.btnUpdate.setOnClickListener {
            updateBook()
        }

        binding.btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadBookDetails() {
        showLoading(true)

        db.collection("books").document(bookId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)

                if (!document.exists()) {
                    Toast.makeText(requireContext(), "Book not found", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                    return@addOnSuccessListener
                }

                currentBook = document.toObject(Book::class.java)
                currentBook?.id = document.id
                displayBookDetails()
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(requireContext(), "Error loading book", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
    }

    private fun displayBookDetails() {
        currentBook?.let { book ->
            binding.titleInput.setText(book.title)
            binding.authorInput.setText(book.author)
            binding.descriptionInput.setText(book.description)
            binding.summaryInput.setText(book.summary)
            binding.isbnInput.setText(book.isbn)
            binding.copiesInput.setText(book.totalCopies.toString())
            binding.availableCopiesInput.setText(book.availableCopies.toString())
            binding.languageInput.setText(book.language)
            binding.publisherInput.setText(book.publisher)
            binding.pageCountInput.setText(book.pageCount.toString())
            binding.coverUrlInput.setText(book.coverUrl)

            // Set genre spinner
            val genrePosition = genres.indexOfFirst { it.equals(book.genre, ignoreCase = true) }
            if (genrePosition >= 0) {
                binding.genreSpinner.setSelection(genrePosition)
            } else {
                binding.genreSpinner.setSelection(genres.size - 1) // Other
                binding.customGenreInput.setText(book.genre)
                binding.customGenreInput.visibility = View.VISIBLE
            }

            // Load cover image
            if (book.coverUrl.isNotEmpty()) {
                Glide.with(requireContext())
                    .load(book.coverUrl)
                    .placeholder(R.drawable.ic_book_placeholder)
                    .error(R.drawable.ic_book_error)
                    .into(binding.ivBookCover)
                binding.ivBookCover.visibility = View.VISIBLE
            }
        }
    }

    private fun updateBook() {
        val title = binding.titleInput.text.toString().trim()
        val author = binding.authorInput.text.toString().trim()
        val description = binding.descriptionInput.text.toString().trim()
        val summary = binding.summaryInput.text.toString().trim()
        val isbn = binding.isbnInput.text.toString().trim()
        val language = binding.languageInput.text.toString().trim()
        val publisher = binding.publisherInput.text.toString().trim()
        val coverUrl = binding.coverUrlInput.text.toString().trim()

        val totalCopies = binding.copiesInput.text.toString().toIntOrNull() ?: 1
        val availableCopies = binding.availableCopiesInput.text.toString().toIntOrNull() ?: totalCopies
        val pageCount = binding.pageCountInput.text.toString().toIntOrNull() ?: 0

        val genre = if (binding.genreSpinner.selectedItem.toString() == "Other") {
            binding.customGenreInput.text.toString().trim()
        } else {
            binding.genreSpinner.selectedItem.toString()
        }

        if (title.isEmpty()) {
            binding.titleInput.error = "Title is required"
            return
        }

        if (author.isEmpty()) {
            binding.authorInput.error = "Author is required"
            return
        }

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
            pageCount = pageCount
        )

        if (updatedBook == null) {
            Toast.makeText(requireContext(), "Error updating book", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        db.collection("books").document(bookId)
            .set(updatedBook)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(requireContext(), "✅ Book updated successfully!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(requireContext(), "Error updating: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnUpdate.isEnabled = !show
        binding.btnUpdate.alpha = if (show) 0.5f else 1.0f
        binding.btnCancel.isEnabled = !show
        binding.btnCancel.alpha = if (show) 0.5f else 1.0f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}