package com.rajnishkumar.bookbuddy

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.FirebaseDatabase
import com.rajnishkumar.bookbuddy.ai.AISearchHelper
import com.rajnishkumar.bookbuddy.ai.HuggingFaceClient
import com.rajnishkumar.bookbuddy.models.Book
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AISearchActivity : AppCompatActivity() {

    private lateinit var etSearchQuery: android.widget.EditText
    private lateinit var btnSearch: android.widget.ImageButton
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var searchProgressBar: ProgressBar

    private val huggingFaceClient = HuggingFaceClient()
    private val searchHelper = AISearchHelper()
    private val database = FirebaseDatabase.getInstance().reference

    private var allBooks = mutableListOf<Book>()
    private lateinit var adapter: BookAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_search)

        etSearchQuery = findViewById(R.id.etSearchQuery)
        btnSearch = findViewById(R.id.btnSearch)
        rvSearchResults = findViewById(R.id.rvSearchResults)
        searchProgressBar = findViewById(R.id.searchProgressBar)

        adapter = BookAdapter { book ->
            val intent = Intent(this, BookDetailActivity::class.java)
            intent.putExtra("BOOK_ID", book.id)
            startActivity(intent)
        }

        rvSearchResults.layoutManager = LinearLayoutManager(this)
        rvSearchResults.adapter = adapter

        fetchAllBooks()

        btnSearch.setOnClickListener {
            val query = etSearchQuery.text.toString().trim()
            if (query.isNotEmpty()) {
                performAISearch(query)
            }
        }
    }

    private fun fetchAllBooks() {
        database.child("books").get().addOnSuccessListener { snapshot ->
            allBooks.clear()
            for (child in snapshot.children) {
                child.getValue(Book::class.java)?.let { allBooks.add(it) }
            }
        }
    }

    private fun performAISearch(query: String) {
        searchProgressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // 1. Get embedding for the query
                val queryEmbedding = withContext(Dispatchers.IO) {
                    huggingFaceClient.getEmbedding(query)
                }

                if (queryEmbedding.isEmpty()) {
                    Toast.makeText(this@AISearchActivity, "Could not process search", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // 2. Rank books locally
                val topMatches = withContext(Dispatchers.Default) {
                    searchHelper.findTopMatches(queryEmbedding, allBooks)
                }

                // 3. Update UI
                adapter.setBooks(topMatches)

            } catch (e: Exception) {
                Toast.makeText(this@AISearchActivity, "Search error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                searchProgressBar.visibility = View.GONE
            }
        }
    }

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
                genre.text = book.genre
                
                Glide.with(itemView.context)
                    .load(book.coverUrl)
                    .placeholder(R.drawable.ic_book_placeholder)
                    .into(cover)

                itemView.setOnClickListener { onClick(book) }
            }
        }
    }
}