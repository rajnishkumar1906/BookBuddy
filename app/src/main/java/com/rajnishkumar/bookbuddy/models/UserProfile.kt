package com.rajnishkumar.bookbuddy.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val uid: String,
    val profilePicPath: String? = null,
    val favoriteGenres: String = "" // Stored as comma-separated string
)
