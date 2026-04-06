package com.rajnishkumar.bookbuddy.ui.dashboard

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import com.rajnishkumar.bookbuddy.R
import com.rajnishkumar.bookbuddy.adapters.BookPagingAdapter
import com.rajnishkumar.bookbuddy.ai.BulkUploadHelper
import com.rajnishkumar.bookbuddy.ai.OpenLibraryClient
import com.rajnishkumar.bookbuddy.models.Book
import com.rajnishkumar.bookbuddy.viewmodels.ManageBooksViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManageBooksFragment : Fragment() {

    private lateinit var etSearch: EditText
    private lateinit var tvCount: TextView
    private lateinit var btnEnrich: View
    private lateinit var rvManage: RecyclerView
    private lateinit var fabAdd: FloatingActionButton

    private lateinit var pagingAdapter: BookPagingAdapter
    private val viewModel: ManageBooksViewModel by viewModels()

    private val database = FirebaseDatabase.getInstance().reference
    private val openLibrary = OpenLibraryClient()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_manage_books, container, false)

        etSearch = view.findViewById(R.id.etSearchInventory)
        tvCount = view.findViewById(R.id.tvTotalBooksCount)
        btnEnrich = view.findViewById(R.id.btnAiEnrichAll)
        rvManage = view.findViewById(R.id.rvManageBooks)
        fabAdd = view.findViewById(R.id.fabAddBook)

        setupRecyclerView()
        observeViewModel()
        setupSearchListener()
        setupFragmentResultListeners()

        btnEnrich.setOnClickListener { batchAiEnrichment() }
        fabAdd.setOnClickListener { showAddOptions() }

        return view
    }

    private fun setupRecyclerView() {
        pagingAdapter = BookPagingAdapter { book -> showEditDialog(book) }
        rvManage.layoutManager = LinearLayoutManager(requireContext())
        rvManage.adapter = pagingAdapter
    }

    private fun observeViewModel() {
        // Collect Paging Data
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.booksFlow.collectLatest { pagingData ->
                pagingAdapter.submitData(pagingData)
            }
        }

        // Keep track of total count
        database.child("books").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isAdded) tvCount.text = "Total: ${snapshot.childrenCount} books"
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFragmentResultListeners() {
        setFragmentResultListener("scanner_request") { _, bundle ->
            val isbn = bundle.getString("SCAN_RESULT")
            if (isbn != null) {
                processISBNForAddition(isbn)
            }
        }
    }

    private fun showAddOptions() {
        val options = arrayOf("Scan ISBN Barcode 📸", "Manual Add ✍️")
        AlertDialog.Builder(requireContext())
            .setTitle("Add Book to Library")
            .setItems(options) { _, which ->
                if (which == 0) {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.librarianNavHost, ScannerFragment())
                        .addToBackStack(null)
                        .commit()
                } else {
                    showManualAddDialog()
                }
            }.show()
    }

    private fun showManualAddDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_book_manual, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.etManualTitle)
        val etAuthor = dialogView.findViewById<EditText>(R.id.etManualAuthor)
        val etIsbn = dialogView.findViewById<EditText>(R.id.etManualIsbn)
        val etGenre = dialogView.findViewById<EditText>(R.id.etManualGenre)

        AlertDialog.Builder(requireContext())
            .setTitle("Manual Book Entry")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = etTitle.text.toString().trim()
                val author = etAuthor.text.toString().trim()
                val isbn = etIsbn.text.toString().trim()
                val genreInput = etGenre.text.toString().trim()

                if (title.isNotEmpty()) {
                    val genreList = genreInput.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    val newBook = Book(
                        title = title,
                        author = author,
                        isbn = isbn,
                        genre = genreList
                    )
                    saveBookToFirebase(newBook)
                } else {
                    Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun processISBNForAddition(isbn: String) {
        database.child("books").orderByChild("isbn").equalTo(isbn).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    Toast.makeText(requireContext(), "Book already exists!", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    val basicBook = withContext(Dispatchers.IO) { openLibrary.getBookByISBN(isbn) }
                    if (basicBook != null) {
                        saveBookToFirebase(basicBook)
                    } else {
                        Toast.makeText(requireContext(), "ISBN not found. Add manually.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun saveBookToFirebase(book: Book) {
        viewLifecycleOwner.lifecycleScope.launch {
            val progressToast = Toast.makeText(requireContext(), "Saving book...", Toast.LENGTH_LONG)
            progressToast.show()
            
            val helper = BulkUploadHelper(requireContext())
            val result = withContext(Dispatchers.IO) { helper.saveBook(book) }

            if (isAdded) {
                if (result.isSuccess) {
                    val number = result.getOrNull()
                    Toast.makeText(requireContext(), "Book #$number added!", Toast.LENGTH_SHORT).show()
                    pagingAdapter.refresh()
                } else {
                    Toast.makeText(requireContext(), "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showEditDialog(book: Book) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_book, null)
        val etDesc = dialogView.findViewById<EditText>(R.id.etEditDesc)
        val etCopies = dialogView.findViewById<EditText>(R.id.etEditCopies)

        etDesc.setText(book.description)
        etCopies.setText(book.totalCopies.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("Manage: #${book.bookNumber} ${book.title}")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val newDesc = etDesc.text.toString().trim()
                val newCopies = etCopies.text.toString().toIntOrNull() ?: book.totalCopies
                updateBook(book.id, newDesc, newCopies)
            }
            .setNegativeButton("Delete") { _, _ -> confirmDelete(book) }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun updateBook(id: String, desc: String, copies: Int) {
        val updates = mapOf("description" to desc, "totalCopies" to copies, "availableCopies" to copies)
        database.child("books").child(id).updateChildren(updates).addOnSuccessListener {
            pagingAdapter.refresh()
        }
    }

    private fun confirmDelete(book: Book) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Delete")
            .setMessage("Delete Book #${book.bookNumber} - ${book.title}?")
            .setPositiveButton("Yes, Delete") { _, _ ->
                database.child("books").child(book.id).removeValue().addOnSuccessListener {
                    pagingAdapter.refresh()
                    Toast.makeText(requireContext(), "Book deleted.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun batchAiEnrichment() {
        Toast.makeText(requireContext(), "AI Enrichment starting...", Toast.LENGTH_SHORT).show()
    }
}
