package com.rajnishkumar.bookbuddy.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rajnishkumar.bookbuddy.R
import com.rajnishkumar.bookbuddy.models.Book

class CompactBookAdapter(private val onBookClick: (Book) -> Unit) : RecyclerView.Adapter<CompactBookAdapter.BookViewHolder>() {
    private var books = listOf<Book>()

    @SuppressLint("NotifyDataSetChanged")
    fun setBooks(newBooks: List<Book>) {
        this.books = newBooks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book_compact, parent, false)
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
        private val cover: ImageView = itemView.findViewById(R.id.ivBookCover)

        fun bind(book: Book, onClick: (Book) -> Unit) {
            title.text = book.title
            author.text = book.author
            
            Glide.with(itemView.context)
                .load(book.coverUrl)
                .placeholder(R.drawable.ic_books)
                .into(cover)

            itemView.setOnClickListener { onClick(book) }
        }
    }
}