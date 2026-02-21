import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import SearchBar from '../components/SearchBar';
import BookCard from '../components/BookCard';
import { useApp } from '../context/AppContext';

export default function Dashboard() {
  const navigate = useNavigate();
  const { searchBooks, searchResults, isSearching, lastQuery } = useApp();
  const [searchError, setSearchError] = useState('');

  const handleSearch = async (query) => {
    const trimmedQuery = query.trim();
    if (!trimmedQuery) return;

    setSearchError('');
    const result = await searchBooks(trimmedQuery, 12);
    console.log("[Dashboard] Results updated:", results.length, " | Query:", lastQuery);
    
    if (!result.success) {
      setSearchError(result.error || 'Something went wrong. Please try again.');
    }
  };

  const handleBookClick = (book) => {
    if (book?.id) {
      navigate(`/book/${book.id}`, { state: { book } });
    }
  };

  const results = searchResults;
  const aiAnswer = results.length > 0 ? results[0]?.answer : null;
  const hasSearched = !!lastQuery;

  return (
    <div className="min-h-screen flex flex-col ">
      <Navbar />

      <main className="flex-grow max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        {/* Hero Section */}
        <div className="text-center mb-16 md:mb-20">
          <h1 className="text-5xl md:text-6xl lg:text-7xl font-extrabold mb-6 tracking-tight">
            <span className="text-gray-900">Find Your</span>
            <br />
            <span className="bg-gradient-to-r from-amber-700 via-amber-800 to-amber-900 bg-clip-text text-transparent">
              Next Great Read
            </span>
          </h1>
          <div className="max-w-3xl mx-auto bg-white/80 backdrop-blur-md shadow-xl px-8 py-6 rounded-3xl border border-amber-100">
            <p className="text-lg md:text-xl text-gray-700 leading-relaxed">
              Tell me what mood you're in or what kind of story you're craving — get AI-powered book recommendations tailored just for you.
            </p>
          </div>
        </div>

        {/* Search Bar */}
        <div className="max-w-4xl mx-auto mb-16">
          <div className="bg-white/90 backdrop-blur-lg shadow-2xl rounded-3xl p-8 border border-amber-100">
            <SearchBar onSearch={handleSearch} />
          </div>
        </div>

        {/* Error Message */}
        {searchError && (
          <div className="max-w-4xl mx-auto mb-8 p-5 bg-red-50 border border-red-200 text-red-700 rounded-2xl shadow-sm text-center md:text-left">
            {searchError}
          </div>
        )}

        {/* Main Content Area */}
        {isSearching ? (
          <div className="flex flex-col items-center justify-center py-24 text-gray-600">
            <div className="relative mb-6">
              <div className="w-20 h-20 border-4 border-amber-200 border-t-amber-800 rounded-full animate-spin"></div>
              <div className="absolute inset-0 flex items-center justify-center">
                <div className="w-10 h-10 bg-amber-100 rounded-full opacity-40 animate-pulse"></div>
              </div>
            </div>
            <p className="text-xl font-medium text-amber-900">Looking for the perfect books...</p>
            <p className="mt-3 text-gray-500">This usually takes just a few seconds</p>
          </div>
        ) : results.length > 0 ? (
          <section className="mt-8">
            {/* Results Header */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-10 gap-4">
              <div>
                <h2 className="text-3xl md:text-4xl font-bold text-gray-800">
                  Books Picked for You
                </h2>
                {lastQuery && (
                  <p className="mt-3 text-gray-600 text-lg">
                    Based on: <span className="font-medium italic text-amber-800">"{lastQuery}"</span>
                  </p>
                )}
              </div>
              <div className="inline-flex px-6 py-3 bg-amber-50 text-amber-800 rounded-full text-base font-medium border border-amber-200 shadow-sm">
                {results.length} {results.length === 1 ? 'book' : 'books'}
              </div>
            </div>

            {/* AI Answer */}
            {aiAnswer && (
              <div className="mb-12 p-7 bg-gradient-to-br from-amber-50 to-amber-100 rounded-2xl border border-amber-200 shadow-md">
                <div className="flex items-center gap-3 mb-4">
                  <span className="text-2xl">📚</span>
                  <h3 className="text-lg font-semibold text-amber-900">Your AI Librarian recommends:</h3>
                </div>
                <p className="text-gray-800 leading-relaxed whitespace-pre-line">
                  {aiAnswer}
                </p>
              </div>
            )}

            {/* Book Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 md:gap-8">
              {results.map((book, index) => (
                <div
                  key={book.id || `book-${index}`}
                  onClick={() => handleBookClick(book)}
                  className="cursor-pointer transition-all duration-300 hover:-translate-y-3 hover:shadow-2xl focus:outline-none focus:ring-2 focus:ring-amber-500 rounded-2xl overflow-hidden"
                  role="button"
                  tabIndex={0}
                  aria-label={`View details for ${book.title || 'book'}`}
                >
                  <BookCard {...book} index={index} />
                </div>
              ))}
            </div>
          </section>
        ) : hasSearched ? (
          <div className="mt-16 text-center py-20 bg-white/70 backdrop-blur-md rounded-3xl border border-amber-100 shadow-xl max-w-3xl mx-auto">
            <h3 className="text-3xl font-bold text-gray-800 mb-6">
              No matches found
            </h3>
            <p className="text-lg text-gray-700 mb-10 px-6">
              We couldn't find books matching <span className="font-semibold">"{lastQuery}"</span>.<br />
              Try describing it differently or pick a popular category below.
            </p>
            <div className="flex flex-wrap justify-center gap-4">
              {['Fantasy', 'Romance', 'Mystery', 'Science Fiction', 'Thriller', 'Non-Fiction', 'Historical', 'Young Adult'].map((cat) => (
                <button
                  key={cat}
                  onClick={() => handleSearch(cat)}
                  className="px-7 py-3.5 bg-white shadow-md hover:shadow-lg hover:border-amber-400 hover:text-amber-900 rounded-full text-gray-700 transition-all border border-amber-200 focus:outline-none focus:ring-2 focus:ring-amber-400"
                  aria-label={`Search for ${cat} books`}
                >
                  {cat}
                </button>
              ))}
            </div>
          </div>
        ) : (
          <div className="mt-16 text-center">
            <h3 className="text-2xl md:text-3xl font-bold text-gray-800 mb-10 bg-white/80 backdrop-blur-md shadow-lg px-10 py-5 rounded-3xl inline-block border border-amber-100">
              Start Exploring
            </h3>
            <div className="flex flex-wrap justify-center gap-4 max-w-5xl mx-auto">
              {['Fantasy', 'Romance', 'Mystery & Thriller', 'Science Fiction', 'Self-Help', 'Historical Fiction', 'Young Adult', 'Non-Fiction'].map((cat) => (
                <button
                  key={cat}
                  onClick={() => handleSearch(cat)}
                  className="px-8 py-4 bg-white/90 backdrop-blur-sm shadow-lg hover:shadow-2xl hover:border-amber-400 hover:text-amber-900 rounded-full text-gray-700 font-medium transition-all duration-300 border border-amber-200 focus:outline-none focus:ring-2 focus:ring-amber-400"
                  aria-label={`Explore ${cat} books`}
                >
                  {cat}
                </button>
              ))}
            </div>
            <p className="mt-10 text-gray-600 text-base">
              Or just describe the kind of story you're in the mood for above ↑
            </p>
          </div>
        )}
      </main>

      <Footer />
    </div>
  );
}