package com.rajnishkumar.bookbuddy.ui.dashboard

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.rajnishkumar.bookbuddy.R
import com.rajnishkumar.bookbuddy.database.AppDatabase
import com.rajnishkumar.bookbuddy.models.UserProfile
import com.rajnishkumar.bookbuddy.ui.auth.AuthActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibrarianProfileFragment : Fragment() {

    private lateinit var tvName: TextView
    private lateinit var ivProfilePic: ImageView
    private lateinit var tvTotalBooks: TextView
    private lateinit var tvActiveBorrows: TextView
    
    private lateinit var btnExport: View
    private lateinit var btnAnalytics: View
    private lateinit var btnManageMembers: View
    private lateinit var btnLogout: Button
    
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var roomDb: AppDatabase

    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            ivProfilePic.setPadding(0, 0, 0, 0)
            ivProfilePic.imageTintList = null 
            Glide.with(this).load(it).circleCrop().into(ivProfilePic)
            saveProfilePic(it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile_librarian, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        roomDb = AppDatabase.getDatabase(requireContext())

        tvName = view.findViewById(R.id.tvProfileName)
        ivProfilePic = view.findViewById(R.id.ivProfilePic)
        tvTotalBooks = view.findViewById(R.id.tvTotalBooksCount)
        tvActiveBorrows = view.findViewById(R.id.tvActiveBorrows)
        
        btnExport = view.findViewById(R.id.btnExportData)
        btnAnalytics = view.findViewById(R.id.btnViewAnalytics)
        btnManageMembers = view.findViewById(R.id.btnManageMembers)
        btnLogout = view.findViewById(R.id.btnLogoutProfile)

        loadLibrarianData()
        loadLibraryStats()

        view.findViewById<View>(R.id.cardProfilePic).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnExport.setOnClickListener { Toast.makeText(context, "Exporting CSV...", Toast.LENGTH_SHORT).show() }
        btnAnalytics.setOnClickListener { Toast.makeText(context, "Opening Analytics...", Toast.LENGTH_SHORT).show() }
        btnManageMembers.setOnClickListener { Toast.makeText(context, "Loading Members...", Toast.LENGTH_SHORT).show() }
        
        btnLogout.setOnClickListener { showLogoutConfirmationDialog() }

        return view
    }

    private fun loadLibrarianData() {
        val uid = auth.currentUser?.uid ?: return
        database.child("users").child(uid).child("name").get().addOnSuccessListener {
            if (isAdded) tvName.text = it.getValue(String::class.java) ?: "Librarian"
        }

        CoroutineScope(Dispatchers.Main).launch {
            val localProfile = withContext(Dispatchers.IO) { roomDb.userProfileDao().getProfile(uid) }
            localProfile?.profilePicPath?.let {
                ivProfilePic.setPadding(0, 0, 0, 0)
                ivProfilePic.imageTintList = null
                Glide.with(this@LibrarianProfileFragment).load(Uri.parse(it)).circleCrop().into(ivProfilePic)
            }
        }
    }

    private fun loadLibraryStats() {
        // Total Books
        database.child("books").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isAdded) tvTotalBooks.text = snapshot.childrenCount.toString()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // Global Active Borrows
        database.child("borrowRecords").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val active = snapshot.children.count { it.child("status").getValue(String::class.java) == "BORROWED" }
                if (isAdded) tvActiveBorrows.text = active.toString()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun saveProfilePic(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val existing = roomDb.userProfileDao().getProfile(uid)
            roomDb.userProfileDao().saveProfile(UserProfile(uid, uri.toString(), existing?.favoriteGenres ?: ""))
        }
    }

    private fun showLogoutConfirmationDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_logout, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<Button>(R.id.btnConfirmLogout).setOnClickListener {
            auth.signOut()
            startActivity(Intent(requireContext(), AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            requireActivity().finish()
        }
        dialog.show()
    }
}