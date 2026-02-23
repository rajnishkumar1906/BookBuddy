package com.example.bookbuddy.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.bookbuddy.R
import com.example.bookbuddy.models.Book

class BookAdapter(
    private var books: List<Book>,
    private val onItemClick: (Book) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.cardView)
        val title: TextView = itemView.findViewById(R.id.bookTitle)
        val author: TextView = itemView.findViewById(R.id.bookAuthor)
        val genre: TextView = itemView.findViewById(R.id.bookGenre)
        val availability: TextView = itemView.findViewById(R.id.bookAvailability)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        try {
            val book = books[position]

            // Set title
            holder.title.text = if (book.title.isNotEmpty()) book.title else "Untitled Book"

            // Set author
            holder.author.text = if (book.author.isNotEmpty()) "by ${book.author}" else "Unknown Author"

            // Set genre
            holder.genre.text = if (book.genre.isNotEmpty()) book.genre else "Uncategorized"

            // Set availability
            val availableText = "Available: ${book.availableCopies}/${book.totalCopies}"
            holder.availability.text = availableText

            // Set colors based on availability
            if (book.availableCopies > 0) {
                holder.availability.setTextColor(holder.itemView.context.getColor(R.color.peacock_green))
                holder.cardView.setCardBackgroundColor(holder.itemView.context.getColor(R.color.white))
            } else {
                holder.availability.setTextColor(holder.itemView.context.getColor(R.color.error))
                holder.cardView.setCardBackgroundColor(holder.itemView.context.getColor(R.color.peacock_green_soft))
            }

            // Set click listener
            holder.itemView.setOnClickListener {
                try {
                    onItemClick(book)
                } catch (e: Exception) {
                    Toast.makeText(holder.itemView.context, "Error opening book", Toast.LENGTH_SHORT).show()
                }
            }

        } catch (e: Exception) {
            // Show error state
            holder.title.text = "Error loading book"
            holder.author.text = ""
            holder.genre.text = ""
            holder.availability.text = "Please try again"
            holder.availability.setTextColor(holder.itemView.context.getColor(R.color.error))
            holder.cardView.setCardBackgroundColor(holder.itemView.context.getColor(R.color.peacock_green_soft))
            holder.itemView.setOnClickListener(null)
        }
    }

    override fun getItemCount(): Int = books.size

    fun updateList(newList: List<Book>) {
        books = newList
        notifyDataSetChanged()
    }

    fun getItem(position: Int): Book? {
        return if (position >= 0 && position < books.size) books[position] else null
    }

    fun isEmpty(): Boolean = books.isEmpty()

    fun clear() {
        books = emptyList()
        notifyDataSetChanged()
    }

    fun addItem(book: Book) {
        val newList = books.toMutableList()
        newList.add(book)
        books = newList
        notifyItemInserted(books.size - 1)
    }

    fun addAll(newBooks: List<Book>) {
        val newList = books.toMutableList()
        newList.addAll(newBooks)
        books = newList
        notifyDataSetChanged()
    }

    fun removeItem(bookId: String) {
        val index = books.indexOfFirst { it.id == bookId }
        if (index != -1) {
            val newList = books.toMutableList()
            newList.removeAt(index)
            books = newList
            notifyItemRemoved(index)
        }
    }

    fun updateItem(updatedBook: Book) {
        val index = books.indexOfFirst { it.id == updatedBook.id }
        if (index != -1) {
            val newList = books.toMutableList()
            newList[index] = updatedBook
            books = newList
            notifyItemChanged(index)
        }
    }

    fun getBooks(): List<Book> = books
}