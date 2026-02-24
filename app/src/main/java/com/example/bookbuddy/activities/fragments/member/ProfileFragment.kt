package com.example.bookbuddy.activities.fragments.member

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bookbuddy.R
import com.example.bookbuddy.activities.auth.LoginActivity
import com.example.bookbuddy.activities.shared.MyBooksActivity
import com.example.bookbuddy.databinding.FragmentProfileBinding
import com.example.bookbuddy.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var currentUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupClickListeners()
        loadUserProfile()
        loadUserStats()
    }

    private fun setupClickListeners() {
        binding.myBooksCard.setOnClickListener {
            startActivity(Intent(requireContext(), MyBooksActivity::class.java))
        }

        binding.settingsCard.setOnClickListener {
            Toast.makeText(requireContext(), "Settings coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.helpCard.setOnClickListener {
            Toast.makeText(requireContext(), "Help & Support coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.logoutButton.setOnClickListener {
            logoutUser()
        }
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                currentUser = document.toObject(User::class.java)

                binding.userName.text = currentUser?.name ?: "User"
                binding.userEmail.text = currentUser?.email ?: "No email"

                // Load profile image placeholder
                binding.profileImage.setImageResource(R.drawable.ic_profile_placeholder)
            }
    }

    private fun loadUserStats() {
        val userId = auth.currentUser?.uid ?: return

        // Load borrowed books count
        db.collection("borrowRecords")
            .whereEqualTo("userId", userId)
            .whereEqualTo("returnedAt", null)
            .get()
            .addOnSuccessListener { snapshot ->
                binding.borrowedCount.text = snapshot.size().toString()
            }

        // Load reading history count
        db.collection("users").document(userId)
            .collection("interactions")
            .whereEqualTo("action", "borrow")
            .get()
            .addOnSuccessListener { snapshot ->
                binding.historyCount.text = snapshot.size().toString()
            }

        // Load reading stats (total books read)
        db.collection("borrowRecords")
            .whereEqualTo("userId", userId)
            .whereNotEqualTo("returnedAt", null)
            .get()
            .addOnSuccessListener { snapshot ->
                binding.booksReadCount.text = snapshot.size().toString()
            }
    }

    private fun logoutUser() {
        auth.signOut()
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}