package com.rajnishkumar.bookbuddy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MemberDashboard : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvName: TextView
    private lateinit var tvBorrowedCount: TextView
    private lateinit var tvHistoryCount: TextView
    private lateinit var btnBrowseBooks: CardView
    private lateinit var btnMyBooks: CardView
    private lateinit var btnAISearch: CardView
    private lateinit var btnProfile: CardView
    private lateinit var btnLogout: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_dashboard)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        tvWelcome = findViewById(R.id.tvWelcome)
        tvName = findViewById(R.id.tvName)
        tvBorrowedCount = findViewById(R.id.tvBorrowedCount)
        tvHistoryCount = findViewById(R.id.tvHistoryCount)
        btnBrowseBooks = findViewById(R.id.btnBrowseBooks)
        btnMyBooks = findViewById(R.id.btnMyBooks)
        btnAISearch = findViewById(R.id.btnAISearch)
        btnProfile = findViewById(R.id.btnProfile)
        btnLogout = findViewById(R.id.btnLogout)

        loadUserInfo()
        loadStats()

        btnBrowseBooks.setOnClickListener {
            // Reusing AI Search as Browse for now
            startActivity(Intent(this, AISearchActivity::class.java))
        }

        btnMyBooks.setOnClickListener {
            Toast.makeText(this, "My Books - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        btnAISearch.setOnClickListener {
            startActivity(Intent(this, AISearchActivity::class.java))
        }

        btnProfile.setOnClickListener {
            Toast.makeText(this, "Profile - Coming Soon", Toast.LENGTH_SHORT).show()
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
                val name = snapshot.child("name").getValue(String::class.java) ?: "Member"
                tvName.text = "Welcome, $name!"
            }
            .addOnFailureListener {
                tvName.text = "Welcome, Member!"
            }
    }

    private fun loadStats() {
        val userId = auth.currentUser?.uid ?: return

        database.reference.child("borrowRecords")
            .orderByChild("userId")
            .equalTo(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                var borrowed = 0
                for (record in snapshot.children) {
                    val status = record.child("status").getValue(String::class.java)
                    if (status == "BORROWED") borrowed++
                }
                tvBorrowedCount.text = borrowed.toString()
            }
            .addOnFailureListener {
                tvBorrowedCount.text = "0"
            }

        database.reference.child("borrowRecords")
            .orderByChild("userId")
            .equalTo(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                var history = 0
                for (record in snapshot.children) {
                    val returnedAt = record.child("returnedAt").getValue(Long::class.java)
                    if (returnedAt != null) history++
                }
                tvHistoryCount.text = history.toString()
            }
            .addOnFailureListener {
                tvHistoryCount.text = "0"
            }
    }
}