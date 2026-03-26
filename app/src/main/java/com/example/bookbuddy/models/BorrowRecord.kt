package com.example.bookbuddy.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

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

    fun isOverdue(): Boolean {
        if (returnedAt != null) return false
        val now = System.currentTimeMillis()
        return dueDate.toDate().time < now
    }

    fun getDaysRemaining(): Int {
        val now = System.currentTimeMillis()
        val due = dueDate.toDate().time
        val diffInMillis = due - now
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    }

    fun getFormattedBorrowedDate(): String {
        return java.text.SimpleDateFormat(
            "MMM dd, yyyy",
            java.util.Locale.getDefault()
        ).format(borrowedAt.toDate())
    }

    fun getFormattedDueDate(): String {
        return java.text.SimpleDateFormat(
            "MMM dd, yyyy",
            java.util.Locale.getDefault()
        ).format(dueDate.toDate())
    }
}