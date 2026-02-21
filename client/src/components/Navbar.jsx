// src/components/Navbar.jsx - UPDATED
import { Link } from 'react-router-dom';
import { FaSignOutAlt, FaUser, FaBell } from 'react-icons/fa';
import BookBuddyLogo from './BookBuddyLogo';
import { useApp } from '../context/AppContext';

export default function Navbar() {
  const { user, logout } = useApp();

  return (
    <nav className="bg-white/10 backdrop-blur-sm shadow-lg sticky top-0 z-50 px-6 py-4">
      <div className="max-w-7xl mx-auto flex justify-between items-center">
        <Link to="/dashboard" className="flex items-center gap-3 group">
          <div className="p-2 bg-gradient-to-r from-amber-800 to-amber-900 rounded-xl">
            <BookBuddyLogo className="w-6 h-6 text-white" />
          </div>
          <span className="text-xl font-bold text-white bg-clip-text text-transparent">
            BookBuddy
          </span>
        </Link>

        <div className="flex items-center gap-3">
          {/* Notification */}
          <button className="p-2.5 hover:bg-gray-100/80 rounded-xl transition-colors">
            <FaBell className="w-4 h-4 text-gray-600" />
          </button>

          {/* User Menu */}
          <Link to="/profile" className="flex items-center gap-2 px-3 py-2 bg-gray-100/80 backdrop-blur-sm rounded-xl hover:bg-gray-200 transition-colors">
            <div className="w-7 h-7 bg-gradient-to-r from-amber-800 to-amber-900 rounded-lg flex items-center justify-center">
              <FaUser className="w-3 h-3 text-white" />
            </div>
            <span className="text-sm font-medium text-gray-800 hidden sm:block">
              {user?.email?.split('@')[0] || 'John Doe'}
            </span>
          </Link>

          <button 
            onClick={logout}
            className="p-2.5 hover:bg-gray-100/80 rounded-xl transition-colors text-gray-600 hover:text-red-500"
          >
            <FaSignOutAlt className="w-4 h-4" />
          </button>
        </div>
      </div>
    </nav>
  );
}