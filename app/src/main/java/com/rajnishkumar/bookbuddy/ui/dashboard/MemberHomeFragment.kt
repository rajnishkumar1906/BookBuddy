package com.rajnishkumar.bookbuddy.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rajnishkumar.bookbuddy.R
import com.rajnishkumar.bookbuddy.adapters.CompactBookAdapter
import com.rajnishkumar.bookbuddy.models.Book
import com.rajnishkumar.bookbuddy.ui.book.BookDetailActivity
import com.rajnishkumar.bookbuddy.viewmodels.MemberHomeViewModel

class MemberHomeFragment : Fragment() {

    private val viewModel: MemberHomeViewModel by viewModels()

    // Views
    private var tvName: TextView? = null
    private var pbGoal: ProgressBar? = null
    private var tvGoalStatus: TextView? = null
    private var rvRecentBooks: RecyclerView? = null
    private var rvRecommendedBooks: RecyclerView? = null
    private var rvFantasyBooks: RecyclerView? = null
    private var rvMysteryBooks: RecyclerView? = null
    private var rvSciFiBooks: RecyclerView? = null
    private var rvAdventureBooks: RecyclerView? = null
    private var rvHistoryBooks: RecyclerView? = null
    private var cardNewUserWelcome: View? = null

    private val recentAdapter = CompactBookAdapter { openBookDetail(it) }
    private val recommendedAdapter = CompactBookAdapter { openBookDetail(it) }
    private val fantasyAdapter = CompactBookAdapter { openBookDetail(it) }
    private val mysteryAdapter = CompactBookAdapter { openBookDetail(it) }
    private val sciFiAdapter = CompactBookAdapter { openBookDetail(it) }
    private val adventureAdapter = CompactBookAdapter { openBookDetail(it) }
    private val historyAdapter = CompactBookAdapter { openBookDetail(it) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_member_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupRecyclerViews()
        observeViewModel()

        viewModel.loadDataIfNeeded(requireContext())
    }

    private fun initializeViews(view: View) {
        tvName = view.findViewById(R.id.tvName)
        pbGoal = view.findViewById(R.id.pbGoal)
        tvGoalStatus = view.findViewById(R.id.tvGoalStatus)
        rvRecentBooks = view.findViewById(R.id.rvRecentBooks)
        rvRecommendedBooks = view.findViewById(R.id.rvRecommendedBooks)
        rvFantasyBooks = view.findViewById(R.id.rvFantasyBooks)
        rvMysteryBooks = view.findViewById(R.id.rvMysteryBooks)
        rvSciFiBooks = view.findViewById(R.id.rvSciFiBooks)
        rvAdventureBooks = view.findViewById(R.id.rvAdventureBooks)
        rvHistoryBooks = view.findViewById(R.id.rvHistoryBooks)
        cardNewUserWelcome = view.findViewById(R.id.cardNewUserWelcome)
    }

    private fun setupRecyclerViews() {
        setupHorizontalScroll(rvRecentBooks, recentAdapter)
        setupHorizontalScroll(rvRecommendedBooks, recommendedAdapter)
        setupHorizontalScroll(rvFantasyBooks, fantasyAdapter)
        setupHorizontalScroll(rvMysteryBooks, mysteryAdapter)
        setupHorizontalScroll(rvSciFiBooks, sciFiAdapter)
        setupHorizontalScroll(rvAdventureBooks, adventureAdapter)
        setupHorizontalScroll(rvHistoryBooks, historyAdapter)
    }

    private fun setupHorizontalScroll(rv: RecyclerView?, adapter: CompactBookAdapter) {
        rv?.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            this.adapter = adapter
        }
    }

    private fun observeViewModel() {
        viewModel.userName.observe(viewLifecycleOwner) { tvName?.text = "Hello, $it!" }
        viewModel.readingProgress.observe(viewLifecycleOwner) { pbGoal?.progress = it }
        viewModel.readingGoalText.observe(viewLifecycleOwner) { tvGoalStatus?.text = it }
        
        viewModel.recentBooks.observe(viewLifecycleOwner) { 
            recentAdapter.setBooks(it)
            view?.findViewById<View>(R.id.tvRecentLabel)?.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
            rvRecentBooks?.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
        }
        
        viewModel.recommendedBooks.observe(viewLifecycleOwner) { recommendedAdapter.setBooks(it) }
        viewModel.fantasyBooks.observe(viewLifecycleOwner) { fantasyAdapter.setBooks(it) }
        viewModel.mysteryBooks.observe(viewLifecycleOwner) { mysteryAdapter.setBooks(it) }
        viewModel.sciFiBooks.observe(viewLifecycleOwner) { sciFiAdapter.setBooks(it) }
        viewModel.adventureBooks.observe(viewLifecycleOwner) { adventureAdapter.setBooks(it) }
        viewModel.historyBooks.observe(viewLifecycleOwner) { historyAdapter.setBooks(it) }
        
        viewModel.isNewUser.observe(viewLifecycleOwner) {
            cardNewUserWelcome?.visibility = if (it) View.VISIBLE else View.GONE
        }
    }

    private fun openBookDetail(book: Book) {
        val intent = Intent(requireContext(), BookDetailActivity::class.java)
        intent.putExtra("BOOK_ID", book.id)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rvRecentBooks = null
        rvRecommendedBooks = null
        rvFantasyBooks = null
        rvMysteryBooks = null
        rvSciFiBooks = null
        rvAdventureBooks = null
        rvHistoryBooks = null
    }
}
