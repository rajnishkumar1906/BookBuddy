package com.rajnishkumar.bookbuddy.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rajnishkumar.bookbuddy.R
import com.rajnishkumar.bookbuddy.models.Book

class BookPagingAdapter(private val onBookClick: (Book) -> Unit) :
    PagingDataAdapter<Book, BookPagingAdapter.BookViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = getItem(position)
        if (book != null) {
            holder.bind(book, onBookClick)
        }
    }

    class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tvBookTitle)
        private val author: TextView = itemView.findViewById(R.id.tvBookAuthor)
        private val genre: TextView = itemView.findViewById(R.id.tvBookGenre)
        private val cover: ImageView = itemView.findViewById(R.id.ivBookCover)

        fun bind(book: Book, onClick: (Book) -> Unit) {
            title.text = "#${book.bookNumber} ${book.title}"
            author.text = book.author
            // Updated to handle the List<String> genre format
            genre.text = book.genre.joinToString(", ")

            Glide.with(itemView.context)
                .load(book.coverUrl)
                .placeholder(R.drawable.ic_book_placeholder)
                .into(cover)

            itemView.setOnClickListener { onClick(book) }
        }
    }

    class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean =
            oldItem == newItem
    }
}
