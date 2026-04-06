package com.rajnishkumar.bookbuddy.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rajnishkumar.bookbuddy.models.BookChunk

@Dao
interface BookChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chunks: List<BookChunk>)

    @Query("SELECT * FROM book_chunks WHERE bookId = :bookId")
    suspend fun getChunksForBook(bookId: String): List<BookChunk>

    @Query("DELETE FROM book_chunks WHERE bookId = :bookId")
    suspend fun deleteChunksForBook(bookId: String)

    @Query("DELETE FROM book_chunks")
    suspend fun deleteAll()
}
