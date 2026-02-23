package com.example.bookbuddy.utils

import com.example.bookbuddy.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = db.collection("users")

    // Get current user
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    // Get user by ID
    suspend fun getUserById(userId: String): User? {
        return try {
            val doc = usersCollection.document(userId).get().await()
            doc.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Update user
    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.id).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Add book to user's borrowed list
    suspend fun addBorrowedBook(userId: String, bookId: String): Result<Unit> {
        return try {
            val user = getUserById(userId) ?: return Result.failure(Exception("User not found"))
            user.borrowedBooks.add(bookId)
            usersCollection.document(userId).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Remove book from user's borrowed list
    suspend fun removeBorrowedBook(userId: String, bookId: String): Result<Unit> {
        return try {
            val user = getUserById(userId) ?: return Result.failure(Exception("User not found"))
            user.borrowedBooks.remove(bookId)
            usersCollection.document(userId).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Check if user is librarian
    suspend fun isLibrarian(userId: String): Boolean {
        val user = getUserById(userId) ?: return false
        return user.role == "librarian"
    }
}