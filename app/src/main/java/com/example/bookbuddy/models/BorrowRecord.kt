package com.example.bookbuddy.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * BorrowRecord Model
 *
 * Tracks each book borrowing transaction in the library
 * Used by: BookDetailActivity, MyBooksActivity
 *
 * @property id Unique document ID in Firestore
 * @property bookId ID of the borrowed book
 * @property bookTitle Title of the borrowed book
 * @property userId ID of the user who borrowed
 * @property userName Name of the user who borrowed
 * @property borrowedAt When the book was borrowed
 * @property dueDate When the book should be returned
 * @property returnedAt When the book was actually returned (null if not returned)
 * @property status Current status: BORROWED, RETURNED, OVERDUE
 */
data class BorrowRecord(
    @DocumentId
    var id: String = "",
    var bookId: String = "",
    var bookTitle: String = "",
    var userId: String = "",
    var userName: String = "",
    var borrowedAt: Timestamp = Timestamp.now(),
    var dueDate: Timestamp = Timestamp.now(),
    var returnedAt: Timestamp? = null,
    var status: String = "BORROWED" // BORROWED, RETURNED, OVERDUE
) {
    // Empty constructor required for Firestore
    constructor() : this(
        id = "",
        bookId = "",
        bookTitle = "",
        userId = "",
        userName = "",
        borrowedAt = Timestamp.now(),
        dueDate = Timestamp.now(),
        returnedAt = null,
        status = "BORROWED"
    )

    /**
     * Helper function to check if book is overdue
     */
    fun isOverdue(): Boolean {
        if (returnedAt != null) return false
        val now = System.currentTimeMillis()
        return dueDate.toDate().time < now
    }

    /**
     * Helper function to get days remaining until due
     * Returns negative if overdue
     */
    fun getDaysRemaining(): Int {
        val now = System.currentTimeMillis()
        val due = dueDate.toDate().time
        val diffInMillis = due - now
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    }

    /**
     * Helper function to get formatted borrowed date
     */
    fun getFormattedBorrowedDate(): String {
        return java.text.SimpleDateFormat(
            "MMM dd, yyyy",
            java.util.Locale.getDefault()
        ).format(borrowedAt.toDate())
    }

    /**
     * Helper function to get formatted due date
     */
    fun getFormattedDueDate(): String {
        return java.text.SimpleDateFormat(
            "MMM dd, yyyy",
            java.util.Locale.getDefault()
        ).format(dueDate.toDate())
    }
}