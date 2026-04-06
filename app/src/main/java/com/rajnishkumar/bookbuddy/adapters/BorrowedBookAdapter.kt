package com.rajnishkumar.bookbuddy.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rajnishkumar.bookbuddy.R
import com.rajnishkumar.bookbuddy.models.BorrowRecord
import java.text.SimpleDateFormat
import java.util.*

class BorrowedBookAdapter(
    private val onReturnClick: (BorrowRecord) -> Unit,
    private val onViewDetailsClick: (BorrowRecord) -> Unit
) : RecyclerView.Adapter<BorrowedBookAdapter.BorrowedBookViewHolder>() {

    private var records = listOf<BorrowRecord>()

    fun setRecords(newRecords: List<BorrowRecord>) {
        this.records = newRecords
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BorrowedBookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_borrowed_book, parent, false)
        return BorrowedBookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BorrowedBookViewHolder, position: Int) {
        val record = records[position]
        holder.bind(record, onReturnClick, onViewDetailsClick)
    }

    override fun getItemCount() = records.size

    class BorrowedBookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCover: ImageView = itemView.findViewById(R.id.ivBorrowedBookCover)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvBorrowedBookTitle)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvBorrowedBookAuthor)
        private val tvDueDate: TextView = itemView.findViewById(R.id.tvDueDate)
        private val btnReturn: android.view.View = itemView.findViewById(R.id.btnReturnBook)
        private val btnView: android.view.View = itemView.findViewById(R.id.btnViewDetails)

        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        fun bind(record: BorrowRecord, onReturn: (BorrowRecord) -> Unit, onView: (BorrowRecord) -> Unit) {
            tvTitle.text = record.bookTitle
            tvAuthor.text = record.bookAuthor
            tvDueDate.text = "Due: ${dateFormat.format(Date(record.dueDate))}"

            Glide.with(itemView.context)
                .load(record.bookCoverUrl)
                .placeholder(R.drawable.ic_book_placeholder)
                .into(ivCover)

            btnReturn.setOnClickListener { onReturn(record) }
            btnView.setOnClickListener { onView(record) }
        }
    }
}
