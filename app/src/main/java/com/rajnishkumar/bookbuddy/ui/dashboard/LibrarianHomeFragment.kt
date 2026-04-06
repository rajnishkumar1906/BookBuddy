package com.rajnishkumar.bookbuddy.ui.dashboard

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.rajnishkumar.bookbuddy.R
import com.rajnishkumar.bookbuddy.ai.BulkUploadHelper
import com.rajnishkumar.bookbuddy.ai.OpenLibraryClient
import com.rajnishkumar.bookbuddy.models.Book
import com.rajnishkumar.bookbuddy.worker.BookUploadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class LibrarianHomeFragment : Fragment() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvName: TextView
    private lateinit var tvBooksCount: TextView
    private lateinit var tvMembersCount: TextView
    private lateinit var cardAddBook: CardView
    private lateinit var cardManageBooks: CardView
    private lateinit var cardStatistics: CardView
    // private lateinit var cardDeleteCollection: CardView // COMMENTED OUT

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val openLibraryClient = OpenLibraryClient()

    private var progressDialog: AlertDialog? = null

    private val csvPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { prepareAndStartWorker(it) }
    }

    private val scannerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val isbn = result.data?.getStringExtra("SCAN_RESULT") ?: return@registerForActivityResult
            fetchAndAddBookByISBN(isbn)
        }
    }

    private val createFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            val content = BulkUploadHelper(requireContext()).getSampleCSV()
            requireContext().contentResolver.openOutputStream(it)?.use { output ->
                output.write(content.toByteArray())
            }
            Toast.makeText(requireContext(), "Sample CSV saved!", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_librarian_home, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        tvWelcome = view.findViewById(R.id.tvWelcome)
        tvName = view.findViewById(R.id.tvName)
        tvBooksCount = view.findViewById(R.id.tvBooksCount)
        tvMembersCount = view.findViewById(R.id.tvMembersCount)
        cardAddBook = view.findViewById(R.id.cardAddBook)
        cardManageBooks = view.findViewById(R.id.cardManageBooks)
        cardStatistics = view.findViewById(R.id.cardStatistics)
        // cardDeleteCollection = view.findViewById(R.id.cardDeleteCollection) // COMMENTED OUT

        loadLibrarianInfo()
        loadStats()

        cardAddBook.setOnClickListener { showAddBookOptions() }
        cardManageBooks.setOnClickListener { showInventoryControlOptions() }
        cardStatistics.setOnClickListener {
            startActivity(Intent(requireContext(), DataAnalyticsActivity::class.java))
        }
        // cardDeleteCollection.setOnClickListener { showDeleteCollectionDialog() } // COMMENTED OUT

        // Maintenance tools also disabled
        // view.findViewById<View>(R.id.profileHeader).setOnLongClickListener { ... } 

        observeUploadProgress()

        return view
    }

    /* ALL DELETION LOGIC COMMENTED OUT FOR SAFETY
    private fun showMaintenanceDialog() {
        val options = arrayOf("Delete All Embeddings (Cloud) 🧊", "Clear Local Book Cache 🧹")
        AlertDialog.Builder(requireContext())
            .setTitle("Maintenance Tools")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> deleteCloudEmbeddings()
                    1 -> clearLocalCache()
                }
            }.show()
    }

    private fun deleteCloudEmbeddings() {
        AlertDialog.Builder(requireContext())
            .setTitle("DANGER ⚠️")
            .setMessage("Delete all embeddings from Firebase?")
            .setPositiveButton("YES") { _, _ ->
                database.reference.child("book_embeddings").removeValue()
            }.setNegativeButton("No", null).show()
    }

    private fun clearLocalCache() {
        lifecycleScope.launch(Dispatchers.IO) {
            val roomDb = com.rajnishkumar.bookbuddy.database.AppDatabase.getDatabase(requireContext())
            roomDb.bookDao().deleteAll()
            roomDb.bookChunkDao().deleteAll()
        }
    }

    private fun showDeleteCollectionDialog() { ... }
    private fun confirmFullDeletion(name: String, key: String) { ... }
    private fun deleteAllBooksWithReset() { ... }
    private fun deleteNormalCollection(key: String, name: String) { ... }
    */

    private fun prepareAndStartWorker(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tempFile = File(requireContext().cacheDir, "upload_temp_${System.currentTimeMillis()}.csv")
                requireContext().contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val inputData = workDataOf(BookUploadWorker.KEY_FILE_PATH to tempFile.absolutePath)
                val uploadRequest = OneTimeWorkRequestBuilder<BookUploadWorker>()
                    .setInputData(inputData)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()

                WorkManager.getInstance(requireContext()).enqueueUniqueWork(
                    "bulk_upload",
                    ExistingWorkPolicy.REPLACE,
                    uploadRequest
                )

                showProgressDialog()
            } catch (e: Exception) { }
        }
    }

    private fun observeUploadProgress() {
        WorkManager.getInstance(requireContext())
            .getWorkInfosForUniqueWorkLiveData("bulk_upload")
            .observe(viewLifecycleOwner, Observer { workInfos ->
                if (workInfos.isNullOrEmpty()) return@Observer
                val workInfo = workInfos[0]
                val data = workInfo.progress
                if (workInfo.state == WorkInfo.State.RUNNING) {
                    updateDialog(data.getInt("completed", 0), data.getInt("total", 0), data.getString("current") ?: "", data.getInt("percentage", 0))
                } else if (workInfo.state == WorkInfo.State.SUCCEEDED || workInfo.state == WorkInfo.State.FAILED) {
                    progressDialog?.dismiss()
                    progressDialog = null
                }
            })
    }

    private fun showProgressDialog() {
        if (progressDialog != null) return
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_progress, null)
        progressDialog = AlertDialog.Builder(requireContext()).setView(dialogView).setCancelable(true).setOnDismissListener { progressDialog = null }.create()
        progressDialog?.show()
    }

    private fun updateDialog(completed: Int, total: Int, current: String, percentage: Int) {
        if (progressDialog == null || !progressDialog!!.isShowing) return
        val tvStatus = progressDialog!!.findViewById<TextView>(R.id.tvProgressMessage)
        val tvStats = progressDialog!!.findViewById<TextView>(R.id.tvProgressStats)
        val progressBar = progressDialog!!.findViewById<ProgressBar>(R.id.progressBar)
        tvStatus?.text = "Uploading: $current"
        tvStats?.text = "Processed $completed of $total books"
        progressBar?.progress = percentage
    }

    private fun showInventoryControlOptions() {
        val options = arrayOf("Manage Inventory 📦", "Bulk Upload (CSV) 📄", "Get Sample CSV 📥")
        AlertDialog.Builder(requireContext())
            .setTitle("Inventory Control")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.librarianNavHost, ManageBooksFragment())
                            .addToBackStack(null)
                            .commit()
                    }
                    1 -> csvPickerLauncher.launch("*/*")
                    2 -> createFileLauncher.launch("book_buddy_sample.csv")
                }
            }.show()
    }

    private fun showAddBookOptions() {
        val options = arrayOf("Magic Scan 📸", "Manual Entry ✍️")
        AlertDialog.Builder(requireContext())
            .setTitle("Add New Book")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.librarianNavHost, ScannerFragment())
                            .addToBackStack(null)
                            .commit()
                    }
                    1 -> showManualEntryDialog()
                }
            }.show()
    }

    private fun showManualEntryDialog() {
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
                if (title.isNotEmpty()) {
                    val genreList = etGenre.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    confirmAndAddBook(Book(title = title, author = etAuthor.text.toString().trim(), isbn = etIsbn.text.toString().trim(), genre = genreList))
                }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun fetchAndAddBookByISBN(isbn: String) {
        val progressDialog = AlertDialog.Builder(requireContext()).setMessage("Fetching details...").setCancelable(false).show()
        viewLifecycleOwner.lifecycleScope.launch {
            val book = withContext(Dispatchers.IO) { openLibraryClient.getBookByISBN(isbn) }
            if (isAdded && !isDetached) {
                progressDialog.dismiss()
                if (book != null) confirmAndAddBook(book)
            }
        }
    }

    private fun confirmAndAddBook(book: Book) {
        AlertDialog.Builder(requireContext()).setTitle("Confirm Details").setMessage("Title: ${book.title}\n\nAdd book?")
            .setPositiveButton("YES") { _, _ -> saveBookStandardized(book) }.setNegativeButton("CANCEL", null).show()
    }

    private fun saveBookStandardized(book: Book) {
        viewLifecycleOwner.lifecycleScope.launch {
            val helper = BulkUploadHelper(requireContext())
            val result = withContext(Dispatchers.IO) { helper.saveBook(book) }
            if (isAdded && result.isSuccess) Toast.makeText(requireContext(), "Book added!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadLibrarianInfo() {
        val userId = auth.currentUser?.uid ?: return
        database.reference.child("users").child(userId).get().addOnSuccessListener { snapshot ->
            if (isAdded && !isDetached) {
                val name = snapshot.child("name").getValue(String::class.java) ?: "Librarian"
                tvName.text = "Hello, $name!"
            }
        }
    }

    private fun loadStats() {
        database.reference.child("books").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { if (isAdded) tvBooksCount.text = snapshot.childrenCount.toString() }
            override fun onCancelled(error: DatabaseError) {}
        })
        database.reference.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { if (isAdded) tvMembersCount.text = (if (snapshot.childrenCount > 0) snapshot.childrenCount - 1 else 0).toString() }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
