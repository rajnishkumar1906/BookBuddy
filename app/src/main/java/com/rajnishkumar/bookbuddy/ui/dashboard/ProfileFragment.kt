package com.rajnishkumar.bookbuddy.ui.dashboard

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.rajnishkumar.bookbuddy.R
import com.rajnishkumar.bookbuddy.database.AppDatabase
import com.rajnishkumar.bookbuddy.models.UserProfile
import com.rajnishkumar.bookbuddy.ui.auth.AuthActivity
import com.rajnishkumar.bookbuddy.ui.book.BookDetailActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.drawable.toDrawable

class ProfileFragment : Fragment() {

    private var tvName: TextView? = null
    private var tvEmail: TextView? = null
    private var ivProfilePic: ImageView? = null
    private var cgGenres: ChipGroup? = null
    private var llRecentQuizzes: LinearLayout? = null
    private var tvNoQuizzes: View? = null

    private var tvBorrowedCount: TextView? = null
    private var tvQuizScore: TextView? = null
    private var tvQuizzesTaken: TextView? = null
    private var tvStreakCount: TextView? = null
    private var tvVisitedCount: TextView? = null // Added missing declaration

    private val favoriteGenres = mutableListOf<String>()
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var roomDb: AppDatabase

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            ivProfilePic?.let { view -> Glide.with(this).load(it).circleCrop().into(view) }
            saveProfileData(it.toString())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        roomDb = AppDatabase.getDatabase(requireContext())

        initializeViews(view)
        loadUserData()
        loadStats()
        loadRecentQuizzes()

        view.findViewById<View>(R.id.cardProfilePic).setOnClickListener { pickImageLauncher.launch("image/*") }
        view.findViewById<View>(R.id.btnAddGenre).setOnClickListener { showAddGenreDialog() }
        view.findViewById<View>(R.id.btnLogoutProfile).setOnClickListener { showLogoutConfirmationDialog() }
    }

    private fun initializeViews(view: View) {
        tvName = view.findViewById(R.id.tvProfileName)
        tvEmail = view.findViewById(R.id.tvProfileEmail)
        ivProfilePic = view.findViewById(R.id.ivProfilePic)
        cgGenres = view.findViewById(R.id.cgFavoriteGenres)
        llRecentQuizzes = view.findViewById(R.id.llRecentQuizzes)
        tvNoQuizzes = view.findViewById(R.id.tvNoQuizzes)
        tvBorrowedCount = view.findViewById(R.id.tvBorrowedCount)
        tvQuizScore = view.findViewById(R.id.tvQuizScore)
        tvQuizzesTaken = view.findViewById(R.id.tvQuizzesTaken)
        tvStreakCount = view.findViewById(R.id.tvStreakCount)
        tvVisitedCount = view.findViewById(R.id.tvVisitedCount) // Added missing initialization
    }

    private fun loadRecentQuizzes() {
        viewLifecycleOwner.lifecycleScope.launch {
            val quizMessages = withContext(Dispatchers.IO) {
                roomDb.chatMessageDao().getAllQuizMessages() 
            }

            if (quizMessages.isEmpty()) {
                tvNoQuizzes?.visibility = View.VISIBLE
                return@launch
            }

            tvNoQuizzes?.visibility = View.GONE
            llRecentQuizzes?.removeAllViews()

            val uniqueBookIds = quizMessages.map { it.bookId }.distinct().take(5)

            uniqueBookIds.forEach { bookId ->
                val book = withContext(Dispatchers.IO) { roomDb.bookDao().getBookById(bookId) }
                if (book != null) {
                    val item = LayoutInflater.from(requireContext()).inflate(R.layout.item_profile_stat_pill, llRecentQuizzes, false)
                    item.findViewById<TextView>(R.id.tvStatLabel).text = "Quiz: ${book.title}"
                    item.setOnClickListener {
                        val intent = Intent(requireContext(), BookDetailActivity::class.java)
                        intent.putExtra("BOOK_ID", book.id)
                        intent.putExtra("OPEN_CHAT", true)
                        startActivity(intent)
                    }
                    llRecentQuizzes?.addView(item)
                }
            }
        }
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        database.child("users").child(uid).get().addOnSuccessListener { snapshot ->
            if (!isAdded) return@addOnSuccessListener
            tvName?.text = snapshot.child("name").getValue(String::class.java) ?: "User"
            tvEmail?.text = snapshot.child("email").getValue(String::class.java) ?: ""
            val genres = snapshot.child("favoriteGenres").getValue(String::class.java) ?: ""
            favoriteGenres.clear()
            cgGenres?.removeAllViews()
            genres.split(",").filter { it.isNotBlank() }.forEach { addGenreChip(it) }
            
            val stats = snapshot.child("stats")
            tvQuizScore?.text = (stats.child("totalQuizScore").getValue(Long::class.java) ?: 0).toString()
            tvQuizzesTaken?.text = "${stats.child("totalQuizzesTaken").getValue(Long::class.java) ?: 0} Quizzes"
            tvStreakCount?.text = (stats.child("readingStreak").getValue(Long::class.java) ?: 0).toString()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val local = withContext(Dispatchers.IO) { roomDb.userProfileDao().getProfile(uid) }
            local?.profilePicPath?.let { path ->
                ivProfilePic?.let { Glide.with(this@ProfileFragment).load(Uri.parse(path)).circleCrop().into(it) }
            }
        }
    }

    private fun addGenreChip(genre: String) {
        favoriteGenres.add(genre)
        val chip = Chip(requireContext()).apply {
            text = genre
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                favoriteGenres.remove(genre)
                cgGenres?.removeView(this)
                saveProfileData(null)
            }
        }
        cgGenres?.addView(chip)
    }

    private fun showAddGenreDialog() {
        val input = EditText(requireContext()).apply { hint = "e.g. Mystery" }
        AlertDialog.Builder(requireContext()).setTitle("Add Genre").setView(input)
            .setPositiveButton("Add") { _, _ ->
                val g = input.text.toString().trim()
                if (g.isNotEmpty()) { addGenreChip(g); saveProfileData(null) }
            }.show()
    }

    private fun saveProfileData(newPicPath: String?) {
        val uid = auth.currentUser?.uid ?: return
        val genresString = favoriteGenres.distinct().joinToString(",")
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            database.child("users").child(uid).child("favoriteGenres").setValue(genresString)
            val existing = roomDb.userProfileDao().getProfile(uid)
            val finalPath = newPicPath ?: existing?.profilePicPath
            roomDb.userProfileDao().saveProfile(UserProfile(uid, finalPath, genresString))
        }
    }

    private fun loadStats() {
        val uid = auth.currentUser?.uid ?: return
        database.child("borrowRecords").orderByChild("userId").equalTo(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.children.count { it.child("status").getValue(String::class.java) == "BORROWED" }
                if (isAdded) tvBorrowedCount?.text = count.toString()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        database.child("visits").child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isAdded) tvVisitedCount?.text = snapshot.childrenCount.toString()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showLogoutConfirmationDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_logout, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialogView.findViewById<Button>(R.id.btnConfirmLogout).setOnClickListener {
            auth.signOut()
            startActivity(Intent(requireContext(), AuthActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
            requireActivity().finish()
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tvName = null
        tvEmail = null
        ivProfilePic = null
        cgGenres = null
        llRecentQuizzes = null
        tvNoQuizzes = null
        tvBorrowedCount = null
        tvQuizScore = null
        tvQuizzesTaken = null
        tvStreakCount = null
        tvVisitedCount = null // Added to cleanup
    }
}
