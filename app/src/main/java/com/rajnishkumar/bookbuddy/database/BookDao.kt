package com.rajnishkumar.bookbuddy.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rajnishkumar.bookbuddy.models.Book

@Dao
interface BookDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(books: List<Book>)

    @Query("SELECT * FROM books ORDER BY id ASC")
    suspend fun getAllBooks(): List<Book>

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: String): Book?

    @Query("SELECT * FROM books WHERE embedding = '' OR embedding IS NULL")
    suspend fun getBooksMissingEmbeddings(): List<Book>

    @Query("SELECT MAX(updatedAt) FROM books")
    suspend fun getLastSyncTime(): Long?

    @Query("DELETE FROM books")
    suspend fun deleteAll()
}
