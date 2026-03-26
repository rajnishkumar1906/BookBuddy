package com.rajnishkumar.bookbuddy

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.rajnishkumar.bookbuddy.ai.BulkUploadHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibrarianDashboard : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvName: TextView
    private lateinit var tvBooksCount: TextView
    private lateinit var tvMembersCount: TextView
    private lateinit var btnAddBook: CardView
    private lateinit var btnManageBooks: CardView
    private lateinit var btnBulkUpload: CardView
    private lateinit var btnStats: CardView
    private lateinit var btnLogout: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    // File picker for CSV upload
    private val csvPickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                importBooksFromCSV(it)
            } ?: run {
                Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show()
            }
        }

    // Create document for sample CSV download
    private val createFileLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            uri?.let {
                val content = BulkUploadHelper(this).getSampleCSV()
                contentResolver.openOutputStream(it)?.use { output ->
                    output.write(content.toByteArray())
                }
                Toast.makeText(this, "Sample CSV saved!", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_librarian_dashboard)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize views
        tvWelcome = findViewById(R.id.tvWelcome)
        tvName = findViewById(R.id.tvName)
        tvBooksCount = findViewById(R.id.tvBooksCount)
        tvMembersCount = findViewById(R.id.tvMembersCount)
        btnAddBook = findViewById(R.id.btnAddBook)
        btnManageBooks = findViewById(R.id.btnManageBooks)
        btnBulkUpload = findViewById(R.id.btnBulkUpload)
        btnStats = findViewById(R.id.btnStats)
        btnLogout = findViewById(R.id.btnLogout)

        loadUserInfo()
        loadStats()

        // Set click listeners
        btnAddBook.setOnClickListener {
            Toast.makeText(this, "Add Book - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        btnManageBooks.setOnClickListener {
            Toast.makeText(this, "Manage Books - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        btnBulkUpload.setOnClickListener {
            showBulkUploadDialog()
        }

        btnStats.setOnClickListener {
            Toast.makeText(this, "Statistics - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
    }

    private fun loadUserInfo() {
        val userId = auth.currentUser?.uid ?: return
        database.reference.child("users").child(userId).get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.child("name").getValue(String::class.java) ?: "Librarian"
                tvName.text = "Welcome, $name!"
            }
            .addOnFailureListener {
                tvName.text = "Welcome, Librarian!"
            }
    }

    private fun loadStats() {
        // Load total books count
        database.reference.child("books").get()
            .addOnSuccessListener { snapshot ->
                tvBooksCount.text = snapshot.childrenCount.toString()
            }
            .addOnFailureListener {
                tvBooksCount.text = "0"
            }

        // Load total members count
        database.reference.child("users").orderByChild("role").equalTo("member").get()
            .addOnSuccessListener { snapshot ->
                tvMembersCount.text = snapshot.childrenCount.toString()
            }
            .addOnFailureListener {
                tvMembersCount.text = "0"
            }
    }

    private fun showBulkUploadDialog() {
        val options = arrayOf("📁 Select CSV File", "📄 Download Sample CSV")
        AlertDialog.Builder(this)
            .setTitle("Bulk Upload Books")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> csvPickerLauncher.launch("*/*")
                    1 -> downloadSampleCSV()
                }
            }
            .show()
    }

    private fun downloadSampleCSV() {
        createFileLauncher.launch("bookbuddy_sample_books.csv")
    }

    private fun importBooksFromCSV(uri: Uri) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_progress, null)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvProgressMessage)
        val tvStats = dialogView.findViewById<TextView>(R.id.tvProgressStats)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)

        val progressDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        progressDialog.show()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val uploadHelper = BulkUploadHelper(this@LibrarianDashboard)

                val result = withContext(Dispatchers.IO) {
                    uploadHelper.uploadBooksFromCSV(uri) { progress ->
                        runOnUiThread {
                            tvMessage.text = progress.status
                            tvStats.text = "${progress.completed}/${progress.total} (${progress.percentage}%)"
                            progressBar.progress = progress.percentage
                        }
                    }
                }

                progressDialog.dismiss()

                val message = if (result.failed > 0) {
                    "✅ ${result.success} books added\n❌ ${result.failed} failed"
                } else {
                    "✅ Successfully added ${result.success} books!"
                }

                Toast.makeText(this@LibrarianDashboard, message, Toast.LENGTH_LONG).show()
                loadStats()

            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@LibrarianDashboard, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("CSV_DEBUG", "Upload error", e)
            }
        }
    }
}