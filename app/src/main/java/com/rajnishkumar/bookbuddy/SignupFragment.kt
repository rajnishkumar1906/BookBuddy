package com.rajnishkumar.bookbuddy

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignupFragment : Fragment() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var spinnerRole: Spinner
    private lateinit var etAdminCode: EditText
    private lateinit var btnSignup: Button
    private lateinit var tvLogin: TextView
    private lateinit var tvStatus: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private var selectedRole = "member"
    private val SECRET_CODE = "LIB2024"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_signup, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        etName = view.findViewById(R.id.etName)
        etEmail = view.findViewById(R.id.etEmail)
        etPassword = view.findViewById(R.id.etPassword)
        spinnerRole = view.findViewById(R.id.spinnerRole)
        etAdminCode = view.findViewById(R.id.etAdminCode)
        btnSignup = view.findViewById(R.id.btnSignup)
        tvLogin = view.findViewById(R.id.tvLogin)
        tvStatus = view.findViewById(R.id.tvStatus)

        setupRoleSpinner()
        btnSignup.setOnClickListener { signupUser() }
        tvLogin.setOnClickListener {
            (activity as AuthActivity).loadFragment(LoginFragment())
        }

        return view
    }

    private fun setupRoleSpinner() {
        val roles = arrayOf("Member", "Librarian")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapter

        spinnerRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedRole = if (position == 1) "librarian" else "member"
                etAdminCode.visibility = if (position == 1) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun signupUser() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val adminCode = etAdminCode.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            tvStatus.text = "Please fill all fields"
            return
        }

        if (selectedRole == "librarian" && adminCode != SECRET_CODE) {
            tvStatus.text = "Invalid secret code"
            return
        }

        tvStatus.text = "Creating account..."
        btnSignup.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    saveUserToDatabase(userId, name, email)
                } else {
                    tvStatus.text = "Signup failed: ${task.exception?.message}"
                    btnSignup.isEnabled = true
                }
            }
    }

    private fun saveUserToDatabase(userId: String, name: String, email: String) {
        val user = mapOf(
            "id" to userId,
            "name" to name,
            "email" to email,
            "role" to selectedRole
        )

        database.reference.child("users").child(userId).setValue(user)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Account created successfully!", Toast.LENGTH_LONG).show()

                if (selectedRole == "librarian") {
                    startActivity(Intent(requireContext(), LibrarianDashboard::class.java))
                } else {
                    startActivity(Intent(requireContext(), MemberDashboard::class.java))
                }
                requireActivity().finish()
            }
            .addOnFailureListener { e ->
                tvStatus.text = "Error: ${e.message}"
                btnSignup.isEnabled = true
            }
    }
}