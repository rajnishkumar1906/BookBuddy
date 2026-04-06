package com.rajnishkumar.bookbuddy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.firebase.database.FirebaseDatabase
import com.rajnishkumar.bookbuddy.models.Book
import com.rajnishkumar.bookbuddy.paging.BookPagingSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.Flow

class ManageBooksViewModel : ViewModel() {

    private val database = FirebaseDatabase.getInstance().reference
    
    private val _searchQuery = MutableStateFlow<String?>(null)
    val searchQuery: StateFlow<String?> = _searchQuery

    @OptIn(ExperimentalCoroutinesApi::class)
    val booksFlow: Flow<PagingData<Book>> = _searchQuery.flatMapLatest { query ->
        Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 3
            ),
            pagingSourceFactory = { BookPagingSource(database, query) }
        ).flow.cachedIn(viewModelScope)
    }

    fun setSearchQuery(query: String?) {
        _searchQuery.value = query
    }
}
