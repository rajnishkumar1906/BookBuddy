import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import SearchBar from '../components/SearchBar';
import BookCard from '../components/BookCard';

// Enhanced dummy books with more details
const dummyBooks = [
  {
    id: "1",
    title: "Atomic Habits",
    author: "James Clear",
    reason: "Build better habits effortlessly",
    category: "Self-Improvement",
    rating: 4.8,
    genres: "Self-Help, Productivity",
    num_pages: 320,
    image_url: "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400",
    description: "No matter your goals, Atomic Habits offers a proven framework for improving every day. James Clear, one of the world's leading experts on habit formation, reveals practical strategies that will teach you exactly how to form good habits, break bad ones, and master the tiny behaviors that lead to remarkable results.",
    published_year: 2018,
    publisher: "Avery",
    isbn: "978-0735211292"
  },
  {
    id: "2",
    title: "The Psychology of Money",
    author: "Morgan Housel",
    reason: "Timeless lessons on wealth, greed, and happiness",
    category: "Finance",
    rating: 4.7,
    genres: "Finance, Psychology",
    num_pages: 256,
    image_url: "https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?w=400",
    description: "Timeless lessons on wealth, greed, and happiness doing well with money isn't necessarily about what you know. It's about how you behave. And behavior is hard to teach, even to really smart people.",
    published_year: 2020,
    publisher: "Harriman House",
    isbn: "978-0857197689"
  },
  {
    id: "3",
    title: "Sapiens",
    author: "Yuval Noah Harari",
    reason: "A brief history of humankind",
    category: "History",
    rating: 4.9,
    genres: "History, Anthropology",
    num_pages: 464,
    image_url: "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=400",
    description: "A brief history of humankind. Sapiens integrates history and science to reconsider accepted narratives, connect past developments with contemporary concerns, and examine specific events within the context of larger ideas.",
    published_year: 2015,
    publisher: "Harper Perennial",
    isbn: "978-0062316097"
  },
  {
    id: "4",
    title: "Thinking, Fast and Slow",
    author: "Daniel Kahneman",
    reason: "Understand decision making and cognitive biases",
    category: "Psychology",
    rating: 4.6,
    genres: "Psychology, Economics",
    num_pages: 512,
    image_url: "https://images.unsplash.com/photo-1507842217343-583bb7270b66?w=400",
    description: "Daniel Kahneman, recipient of the Nobel Prize in Economic Sciences for his work on decision-making, takes us on a groundbreaking tour of the mind and explains the two systems that drive the way we think.",
    published_year: 2013,
    publisher: "Farrar, Straus and Giroux",
    isbn: "978-0374533557"
  },
  {
    id: "5",
    title: "The Alchemist",
    author: "Paulo Coelho",
    reason: "Follow your dreams and listen to your heart",
    category: "Fiction",
    rating: 4.7,
    genres: "Fiction, Philosophy",
    num_pages: 208,
    image_url: "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=400",
    description: "A magical story about following your dreams. The Alchemist tells the story of Santiago, a young shepherd who embarks on a journey to find a treasure he has dreamed about.",
    published_year: 1993,
    publisher: "HarperOne",
    isbn: "978-0062502174"
  },
  {
    id: "6",
    title: "Dune",
    author: "Frank Herbert",
    reason: "Epic sci-fi about politics, religion, and ecology",
    category: "Science Fiction",
    rating: 4.8,
    genres: "Science Fiction, Fantasy",
    num_pages: 688,
    image_url: "https://images.unsplash.com/photo-1532012197267-da84d127e765?w=400",
    description: "Set on the desert planet Arrakis, Dune is the story of the boy Paul Atreides, who would become the mysterious Muad'Dib and lead a great army to a cosmic confrontation.",
    published_year: 1965,
    publisher: "Ace Books",
    isbn: "978-0441172719"
  }
];

// Function to match query with books (semantic matching simulation)
const matchBooksToQuery = (query) => {
  const queryLower = query.toLowerCase();
  
  // Simple keyword matching - in real app this would be backend API call
  return dummyBooks.filter(book => {
    const searchableText = `${book.title} ${book.author} ${book.category} ${book.genres} ${book.reason}`.toLowerCase();
    
    // Check for keyword matches
    if (searchableText.includes(queryLower)) return true;
    
    // Check for category matches
    if (book.category.toLowerCase().includes(queryLower)) return true;
    
    // Check for genre matches
    if (book.genres.toLowerCase().includes(queryLower)) return true;
    
    // Check for author matches
    if (book.author.toLowerCase().includes(queryLower)) return true;
    
    // Check for title matches
    if (book.title.toLowerCase().includes(queryLower)) return true;
    
    return false;
  });
};

export default function Dashboard() {
  const navigate = useNavigate();
  const [results, setResults] = useState([]);
  const [isSearching, setIsSearching] = useState(false);
  const [lastQuery, setLastQuery] = useState('');

  const handleSearch = (query) => {
    setLastQuery(query);
    setIsSearching(true);
    
    // Simulate API call delay
    setTimeout(() => {
      const matchedBooks = matchBooksToQuery(query);
      setResults(matchedBooks);
      setIsSearching(false);
    }, 800);
  };

  const handleBookClick = (book) => {
    // Navigate to book detail page with the book data
    navigate(`/book/${book.id}`, { state: { book } });
  };

  return (
    <div className="min-h-screen">
      <Navbar />

      <main className="max-w-6xl mx-auto px-4 py-12">
        {/* Hero Section */}
        <div className="text-center mb-16">
          <h1 className="text-5xl md:text-7xl font-bold mb-6">
            <span className="text-gray-900">Find Your Next</span>
            <br />
            <span className="bg-gradient-to-r from-amber-800 to-amber-900 bg-clip-text text-transparent">
              Great Read
            </span>
          </h1>
          <div className="bg-white/95 backdrop-blur-sm shadow-xl max-w-3xl mx-auto px-8 py-4 rounded-2xl border border-gray-200">
            <p className="text-xl text-gray-700 leading-relaxed">
              Describe what you're in the mood for — get personalized suggestions
              powered by AI that understands your reading preferences.
            </p>
          </div>
        </div>

        {/* Search Section */}
        <div className="max-w-4xl mx-auto mb-20">
          <div className="bg-white/95 backdrop-blur-sm shadow-2xl rounded-3xl p-8 border border-gray-200">
            <SearchBar onSearch={handleSearch} />
          </div>
        </div>

        {/* Results Section */}
        {results.length > 0 && (
          <section>
            <div className="flex items-center justify-between mb-10">
              <div>
                <h2 className="text-3xl font-semibold text-gray-800 bg-white/95 backdrop-blur-sm shadow-lg px-6 py-3 rounded-2xl border border-gray-200">
                  Recommended for You
                </h2>
                {lastQuery && (
                  <p className="text-sm text-gray-500 mt-2 ml-2">
                    Based on: "{lastQuery}"
                  </p>
                )}
              </div>
              <span className="px-4 py-2 bg-white/95 backdrop-blur-sm shadow-md rounded-full text-sm text-gray-600 border border-gray-200">
                {results.length} books found
              </span>
            </div>

            {isSearching ? (
              <div className="flex justify-center items-center py-20">
                <div className="bg-white/95 backdrop-blur-sm shadow-xl p-8 rounded-3xl border border-gray-200">
                  <div className="w-16 h-16 border-4 border-amber-200 border-t-amber-800 rounded-full animate-spin"></div>
                </div>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                {results.map((book, i) => (
                  <div key={book.id} onClick={() => handleBookClick(book)} className="cursor-pointer transform transition-all duration-300 hover:-translate-y-2 hover:shadow-2xl">
                    <BookCard {...book} index={i} />
                  </div>
                ))}
              </div>
            )}
          </section>
        )}

        {/* Quick Categories */}
        {results.length === 0 && !isSearching && (
          <div className="mt-20 text-center">
            <h3 className="text-xl text-gray-700 mb-8 bg-white/95 backdrop-blur-sm shadow-lg px-6 py-3 rounded-2xl inline-block border border-gray-200">
              Popular Categories
            </h3>
            <div className="flex flex-wrap justify-center gap-4">
              {['Fiction', 'Science Fiction', 'Self-Help', 'Biography', 'History', 'Fantasy', 'Mystery', 'Romance'].map((cat, i) => (
                <button
                  key={i}
                  onClick={() => handleSearch(cat)}
                  className="px-6 py-3 bg-white/95 backdrop-blur-sm shadow-md hover:shadow-xl rounded-full text-gray-700 hover:text-amber-800 hover:border-amber-800 hover:-translate-y-1 transition-all border border-gray-200"
                >
                  {cat}
                </button>
              ))}
            </div>
            <p className="mt-6 text-gray-500 text-sm">
              Click any category to see recommendations
            </p>
          </div>
        )}
      </main>

      <Footer />
    </div>
  );
}