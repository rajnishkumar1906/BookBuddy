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
import java.lang.Exception

class BookAdapter(
    private var books: List<Book>,
    private val onItemClick: (Book) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    // ViewHolder class with safe view binding
    class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView? = try { itemView.findViewById(R.id.cardView) } catch (e: Exception) { null }
        val title: TextView? = try { itemView.findViewById(R.id.bookTitle) } catch (e: Exception) { null }
        val author: TextView? = try { itemView.findViewById(R.id.bookAuthor) } catch (e: Exception) { null }
        val genre: TextView? = try { itemView.findViewById(R.id.bookGenre) } catch (e: Exception) { null }
        val availability: TextView? = try { itemView.findViewById(R.id.bookAvailability) } catch (e: Exception) { null }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        return try {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_book, parent, false)
            BookViewHolder(view)
        } catch (e: Exception) {
            // Return empty ViewHolder as fallback
            BookViewHolder(View(parent.context))
        }
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        try {
            // Safely get book at position
            val book = try {
                if (position >= 0 && position < books.size) books[position] else null
            } catch (e: Exception) {
                null
            }

            if (book == null) {
                setErrorState(holder)
                return
            }

            // Set title with null safety
            try {
                holder.title?.text = book.title.ifEmpty { "Untitled Book" }
            } catch (e: Exception) {
                holder.title?.text = "Untitled Book"
            }

            // Set author with null safety
            try {
                val authorText = if (book.author.isNotEmpty()) "by ${book.author}" else "Unknown Author"
                holder.author?.text = authorText
            } catch (e: Exception) {
                holder.author?.text = "Unknown Author"
            }

            // Set genre with null safety
            try {
                holder.genre?.text = book.genre.ifEmpty { "Uncategorized" }
            } catch (e: Exception) {
                holder.genre?.text = "Uncategorized"
            }

            // Set availability text and color
            try {
                val availableText = "Available: ${book.availableCopies}/${book.totalCopies}"
                holder.availability?.text = availableText

                // Change color based on availability
                if (book.availableCopies > 0) {
                    holder.availability?.setTextColor(holder.itemView.context.getColor(R.color.purple_500))
                } else {
                    holder.availability?.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
                }
            } catch (e: Exception) {
                holder.availability?.text = "Availability unknown"
                holder.availability?.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
            }

            // Set card view background if needed
            try {
                if (book.availableCopies <= 0) {
                    holder.cardView?.setCardBackgroundColor(holder.itemView.context.getColor(R.color.light_gray))
                } else {
                    holder.cardView?.setCardBackgroundColor(holder.itemView.context.getColor(android.R.color.white))
                }
            } catch (e: Exception) {
                // Ignore card color errors
            }

            // Set click listener with error handling
            holder.itemView.setOnClickListener {
                try {
                    onItemClick(book)
                } catch (e: Exception) {
                    try {
                        Toast.makeText(holder.itemView.context, "Error opening book", Toast.LENGTH_SHORT).show()
                    } catch (ex: Exception) {
                        // Ignore
                    }
                }
            }

        } catch (e: Exception) {
            // Global catch - show error state
            setErrorState(holder)
        }
    }

    private fun setErrorState(holder: BookViewHolder) {
        try {
            holder.title?.text = "Error loading book"
            holder.author?.text = ""
            holder.genre?.text = ""
            holder.availability?.text = "Please try again"
            holder.availability?.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))

            // Disable click listener for error state
            holder.itemView.setOnClickListener(null)
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun getItemCount(): Int {
        return try {
            books.size
        } catch (e: Exception) {
            0
        }
    }

    fun updateList(newList: List<Book>) {
        try {
            val oldSize = books.size
            books = newList
            val newSize = books.size

            if (oldSize != newSize) {
                notifyDataSetChanged()
            } else {
                // Only update changed items
                try {
                    notifyItemRangeChanged(0, newSize)
                } catch (e: Exception) {
                    notifyDataSetChanged()
                }
            }
        } catch (e: Exception) {
            // If update fails, at least update the reference
            books = newList
            try {
                notifyDataSetChanged()
            } catch (ex: Exception) {
                // Ignore
            }
        }
    }

    // Safe method to get item at position
    fun getItem(position: Int): Book? {
        return try {
            if (position >= 0 && position < books.size) books[position] else null
        } catch (e: Exception) {
            null
        }
    }

    // Check if list is empty
    fun isEmpty(): Boolean {
        return try {
            books.isEmpty()
        } catch (e: Exception) {
            true
        }
    }

    // Clear all items
    fun clear() {
        try {
            books = emptyList()
            notifyDataSetChanged()
        } catch (e: Exception) {
            books = emptyList()
        }
    }

    // Add single item
    fun addItem(book: Book) {
        try {
            val newList = books.toMutableList()
            newList.add(book)
            books = newList
            notifyItemInserted(books.size - 1)
        } catch (e: Exception) {
            try {
                books = books + book
                notifyDataSetChanged()
            } catch (ex: Exception) {
                // Ignore
            }
        }
    }

    // Add multiple items
    fun addAll(newBooks: List<Book>) {
        try {
            val startPosition = books.size
            val newList = books.toMutableList()
            newList.addAll(newBooks)
            books = newList
            notifyItemRangeInserted(startPosition, newBooks.size)
        } catch (e: Exception) {
            try {
                books = books + newBooks
                notifyDataSetChanged()
            } catch (ex: Exception) {
                // Ignore
            }
        }
    }

    // Remove item by ID
    fun removeItem(bookId: String) {
        try {
            val index = books.indexOfFirst { it.id == bookId }
            if (index != -1) {
                val newList = books.toMutableList()
                newList.removeAt(index)
                books = newList
                notifyItemRemoved(index)
            }
        } catch (e: Exception) {
            try {
                books = books.filter { it.id != bookId }
                notifyDataSetChanged()
            } catch (ex: Exception) {
                // Ignore
            }
        }
    }

    // Update existing item
    fun updateItem(updatedBook: Book) {
        try {
            val index = books.indexOfFirst { it.id == updatedBook.id }
            if (index != -1) {
                val newList = books.toMutableList()
                newList[index] = updatedBook
                books = newList
                notifyItemChanged(index)
            }
        } catch (e: Exception) {
            try {
                books = books.map { if (it.id == updatedBook.id) updatedBook else it }
                notifyDataSetChanged()
            } catch (ex: Exception) {
                // Ignore
            }
        }
    }

    // Get all books
    fun getBooks(): List<Book> {
        return try {
            books
        } catch (e: Exception) {
            emptyList()
        }
    }
}