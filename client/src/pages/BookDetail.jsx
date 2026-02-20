import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { useState } from 'react';
import { FaArrowLeft, FaBook, FaUser, FaLayerGroup, FaFileAlt, FaCalendar, FaBuilding, FaBarcode, FaStar, FaRobot, FaQuestionCircle } from 'react-icons/fa';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';

// Fallback book data if state is not passed
const fallbackBooks = {
  "1": {
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
  }
};

export default function BookDetail() {
  const { bookId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [showFollowUp, setShowFollowUp] = useState(false);
  const [followUpQuestion, setFollowUpQuestion] = useState('');
  const [followUpAnswer, setFollowUpAnswer] = useState('');

  // Get book from location state or use fallback
  const book = location.state?.book || fallbackBooks[bookId] || fallbackBooks["1"];

  const handleFollowUpSubmit = (e) => {
    e.preventDefault();
    if (!followUpQuestion.trim()) return;

    // Simulate AI response to follow-up question
    setTimeout(() => {
      const answers = {
        default: `Based on your interest in "${book.title}", I can tell you that this book is highly recommended for readers who enjoy ${book.genres}. The author, ${book.author}, has written several other acclaimed works in this genre.`,
        summary: `"${book.title}" is a ${book.category.toLowerCase()} book that explores themes through ${book.author}'s unique perspective. Readers particularly appreciate the engaging narrative and insightful content.`,
        similar: `If you enjoyed "${book.title}", you might also like works by ${book.author} or explore other books in the ${book.genres} genre.`,
        author: `${book.author} is known for their expertise in ${book.genres}. They have received critical acclaim for their writing style and depth of research.`
      };

      let answer = answers.default;
      if (followUpQuestion.toLowerCase().includes('summary')) answer = answers.summary;
      if (followUpQuestion.toLowerCase().includes('similar')) answer = answers.similar;
      if (followUpQuestion.toLowerCase().includes('author')) answer = answers.author;

      setFollowUpAnswer(answer);
    }, 1000);
  };

  return (
    <div className="min-h-screen">
      <Navbar />

      <main className="max-w-6xl mx-auto px-4 py-8">
        {/* Back button */}
        <button
          onClick={() => navigate('/dashboard')}
          className="flex items-center gap-2 text-gray-600 hover:text-amber-800 mb-6 transition-colors group"
        >
          <FaArrowLeft className="w-4 h-4 group-hover:-translate-x-1 transition-transform" />
          Back to search results
        </button>

        {/* Book Detail Card */}
        <div className="bg-white/95 backdrop-blur-sm rounded-3xl shadow-xl border border-gray-200 overflow-hidden mb-8">
          <div className="grid md:grid-cols-3 gap-8 p-8">
            {/* Book Cover */}
            <div className="md:col-span-1">
              <div className="bg-gradient-to-br from-amber-100 to-amber-200 rounded-2xl overflow-hidden shadow-lg aspect-[2/3] flex items-center justify-center">
                {book.image_url ? (
                  <img 
                    src={book.image_url} 
                    alt={book.title} 
                    className="w-full h-full object-cover hover:scale-105 transition-transform duration-500"
                  />
                ) : (
                  <FaBook className="w-20 h-20 text-amber-800/30" />
                )}
              </div>
            </div>

            {/* Book Info */}
            <div className="md:col-span-2">
              <div className="flex items-center gap-2 mb-2">
                <span className="px-3 py-1 bg-amber-100 text-amber-900 rounded-full text-xs font-medium">
                  {book.category}
                </span>
                <div className="flex items-center gap-1 text-amber-600">
                  <FaStar className="w-4 h-4" />
                  <span className="text-sm font-medium">{book.rating}</span>
                </div>
              </div>

              <h1 className="text-4xl font-bold text-gray-900 mb-2">{book.title}</h1>
              <p className="text-xl text-gray-600 mb-6">by {book.author}</p>

              {/* Meta Info Grid */}
              <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mb-6">
                <div className="flex items-center gap-2 text-gray-600 bg-gray-50 p-3 rounded-xl">
                  <FaUser className="w-4 h-4 text-amber-700" />
                  <div>
                    <p className="text-xs text-gray-500">Author</p>
                    <p className="text-sm font-medium">{book.author}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2 text-gray-600 bg-gray-50 p-3 rounded-xl">
                  <FaLayerGroup className="w-4 h-4 text-amber-700" />
                  <div>
                    <p className="text-xs text-gray-500">Genre</p>
                    <p className="text-sm font-medium">{book.genres}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2 text-gray-600 bg-gray-50 p-3 rounded-xl">
                  <FaFileAlt className="w-4 h-4 text-amber-700" />
                  <div>
                    <p className="text-xs text-gray-500">Pages</p>
                    <p className="text-sm font-medium">{book.num_pages}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2 text-gray-600 bg-gray-50 p-3 rounded-xl">
                  <FaCalendar className="w-4 h-4 text-amber-700" />
                  <div>
                    <p className="text-xs text-gray-500">Published</p>
                    <p className="text-sm font-medium">{book.published_year}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2 text-gray-600 bg-gray-50 p-3 rounded-xl">
                  <FaBuilding className="w-4 h-4 text-amber-700" />
                  <div>
                    <p className="text-xs text-gray-500">Publisher</p>
                    <p className="text-sm font-medium">{book.publisher}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2 text-gray-600 bg-gray-50 p-3 rounded-xl">
                  <FaBarcode className="w-4 h-4 text-amber-700" />
                  <div>
                    <p className="text-xs text-gray-500">ISBN</p>
                    <p className="text-sm font-medium">{book.isbn}</p>
                  </div>
                </div>
              </div>

              {/* Description */}
              <div className="mb-8">
                <h2 className="text-lg font-semibold text-gray-800 mb-2">Description</h2>
                <p className="text-gray-600 leading-relaxed">{book.description}</p>
              </div>

              {/* Action Buttons */}
              <div className="flex flex-wrap gap-4">
                <button
                  onClick={() => setShowFollowUp(!showFollowUp)}
                  className="flex items-center gap-2 px-6 py-3 bg-gradient-to-r from-amber-800 to-amber-900 text-white font-medium rounded-xl hover:shadow-lg transition-all duration-300 transform hover:scale-105"
                >
                  <FaRobot className="w-5 h-5" />
                  Ask AI Librarian
                </button>
                <button className="flex items-center gap-2 px-6 py-3 border border-gray-300 text-gray-700 font-medium rounded-xl hover:bg-gray-50 transition-all duration-300">
                  Add to Wishlist
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Follow-up Questions Section */}
        {showFollowUp && (
          <div className="bg-white/95 backdrop-blur-sm rounded-3xl shadow-xl border border-gray-200 p-8 mb-8 animate-fadeIn">
            <div className="flex items-center gap-3 mb-6">
              <div className="w-10 h-10 bg-gradient-to-r from-amber-800 to-amber-900 rounded-xl flex items-center justify-center">
                <FaQuestionCircle className="w-5 h-5 text-white" />
              </div>
              <h2 className="text-2xl font-semibold text-gray-800">Ask a follow-up question</h2>
            </div>

            <form onSubmit={handleFollowUpSubmit} className="mb-6">
              <div className="flex gap-3">
                <input
                  type="text"
                  value={followUpQuestion}
                  onChange={(e) => setFollowUpQuestion(e.target.value)}
                  placeholder="e.g., Tell me more about this book, Similar recommendations, About the author..."
                  className="flex-1 px-4 py-3 border border-gray-300 rounded-xl bg-white/80 focus:outline-none focus:ring-2 focus:ring-amber-800"
                />
                <button
                  type="submit"
                  className="px-6 py-3 bg-gradient-to-r from-amber-800 to-amber-900 text-white font-medium rounded-xl hover:shadow-lg transition-all"
                >
                  Ask
                </button>
              </div>
            </form>

            {/* Suggested questions */}
            <div className="mb-6">
              <p className="text-sm text-gray-500 mb-2">Suggested questions:</p>
              <div className="flex flex-wrap gap-2">
                {[
                  "What's a summary of this book?",
                  "Similar books to this?",
                  "Tell me about the author",
                  "Is this book worth reading?"
                ].map((q, i) => (
                  <button
                    key={i}
                    onClick={() => {
                      setFollowUpQuestion(q);
                      handleFollowUpSubmit({ preventDefault: () => {} });
                    }}
                    className="px-3 py-1 bg-gray-100 hover:bg-gray-200 rounded-full text-xs text-gray-700 transition-colors"
                  >
                    {q}
                  </button>
                ))}
              </div>
            </div>

            {/* Answer */}
            {followUpAnswer && (
              <div className="bg-amber-50 rounded-2xl p-6 border border-amber-200">
                <div className="flex gap-3">
                  <div className="w-8 h-8 bg-gradient-to-r from-amber-800 to-amber-900 rounded-lg flex items-center justify-center flex-shrink-0">
                    <FaRobot className="w-4 h-4 text-white" />
                  </div>
                  <div>
                    <p className="text-sm text-gray-800 leading-relaxed">{followUpAnswer}</p>
                    <p className="text-xs text-gray-500 mt-2">AI Librarian • Just now</p>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* You might also like section */}
        <div className="mt-12">
          <h2 className="text-2xl font-semibold text-gray-800 mb-6">You might also like</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {[1, 2, 3, 4].map((i) => (
              <div 
                key={i} 
                className="bg-white/95 backdrop-blur-sm rounded-xl shadow-md border border-gray-200 p-4 hover:shadow-xl transition-all cursor-pointer transform hover:-translate-y-1"
                onClick={() => navigate('/book/1')}
              >
                <div className="bg-amber-100 rounded-lg h-24 mb-3 flex items-center justify-center">
                  <FaBook className="w-8 h-8 text-amber-800/30" />
                </div>
                <h3 className="font-medium text-gray-900">Another Great Book</h3>
                <p className="text-xs text-gray-500">By Popular Author</p>
              </div>
            ))}
          </div>
        </div>
      </main>

      <Footer />

      <style>{`
        @keyframes fadeIn {
          from { opacity: 0; transform: translateY(10px); }
          to { opacity: 1; transform: translateY(0); }
        }
        .animate-fadeIn {
          animation: fadeIn 0.3s ease-out;
        }
      `}</style>
    </div>
  );
}