package com.rajnishkumar.bookbuddy

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginFragment : Fragment() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvSignup: TextView
    private lateinit var tvStatus: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        etEmail = view.findViewById(R.id.etEmail)
        etPassword = view.findViewById(R.id.etPassword)
        btnLogin = view.findViewById(R.id.btnLogin)
        tvSignup = view.findViewById(R.id.tvSignup)
        tvStatus = view.findViewById(R.id.tvStatus)

        btnLogin.setOnClickListener { loginUser() }
        tvSignup.setOnClickListener {
            (activity as AuthActivity).loadFragment(SignupFragment())
        }

        return view
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            tvStatus.text = "Please enter email and password"
            return
        }

        tvStatus.text = "Logging in..."
        btnLogin.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    checkUserRole()
                } else {
                    tvStatus.text = "Login failed: ${task.exception?.message}"
                    btnLogin.isEnabled = true
                }
            }
    }

    private fun checkUserRole() {
        val userId = auth.currentUser?.uid ?: return

        database.reference.child("users").child(userId).get()
            .addOnSuccessListener { snapshot ->
                val role = snapshot.child("role").getValue(String::class.java) ?: "member"

                if (role == "librarian") {

                    startActivity(Intent(requireContext(), LibrarianDashboard::class.java))
                } else {
                    startActivity(Intent(requireContext(), MemberDashboard::class.java))
                }
                requireActivity().finish()
            }
            .addOnFailureListener {
                tvStatus.text = "Error checking role"
                btnLogin.isEnabled = true
            }
    }
}