package com.rajnishkumar.bookbuddy.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rajnishkumar.bookbuddy.R
import com.rajnishkumar.bookbuddy.models.Book

class BookAdapter(private val onBookClick: (Book) -> Unit) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {
    private var books = listOf<Book>()

    fun setBooks(newBooks: List<Book>) {
        this.books = newBooks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        holder.bind(book, onBookClick)
    }

    override fun getItemCount() = books.size

    class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tvBookTitle)
        private val author: TextView = itemView.findViewById(R.id.tvBookAuthor)
        private val genre: TextView = itemView.findViewById(R.id.tvBookGenre)
        private val cover: ImageView = itemView.findViewById(R.id.ivBookCover)

        fun bind(book: Book, onClick: (Book) -> Unit) {
            title.text = book.title
            author.text = book.author
            // Display genre list as a comma-separated string
            genre.text = book.genre.joinToString(", ")
            
            Glide.with(itemView.context)
                .load(book.coverUrl)
                .placeholder(R.drawable.ic_book_placeholder)
                .into(cover)

            itemView.setOnClickListener { onClick(book) }
        }
    }
}
