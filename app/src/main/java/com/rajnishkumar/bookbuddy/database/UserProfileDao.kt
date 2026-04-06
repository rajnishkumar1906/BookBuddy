package com.rajnishkumar.bookbuddy.database

import androidx.room.*
import com.rajnishkumar.bookbuddy.models.UserProfile

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE uid = :uid")
    suspend fun getProfile(uid: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: UserProfile)
}
