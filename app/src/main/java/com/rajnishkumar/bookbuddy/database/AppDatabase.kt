package com.rajnishkumar.bookbuddy.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rajnishkumar.bookbuddy.models.Book
import com.rajnishkumar.bookbuddy.models.BookChunk
import com.rajnishkumar.bookbuddy.models.ChatMessage
import com.rajnishkumar.bookbuddy.models.UserProfile

@Database(
    entities = [
        Book::class,
        BookChunk::class,
        ChatMessage::class,
        UserProfile::class
    ],
    version = 8, // Increased to 8 for Local Vector Sync (updatedAt field)
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun bookChunkDao(): BookChunkDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bookbuddy_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
