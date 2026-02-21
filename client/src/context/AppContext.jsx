// src/context/AppContext.jsx
import React, { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';
import config from '../config';
import { tokenCookies } from '../utils/cookies';

const AppContext = createContext();

// API instance
const api = axios.create({
  baseURL: config.API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
api.interceptors.request.use(
  (config) => {
    const token = tokenCookies.getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for token refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        const refreshToken = tokenCookies.getRefreshToken();
        if (refreshToken) {
          const response = await axios.post(`${config.API_URL}/auth/refresh`, {
            refresh_token: refreshToken
          });
          
          tokenCookies.setAccessToken(response.data.access_token);
          if (response.data.refresh_token) {
            tokenCookies.setRefreshToken(response.data.refresh_token);
          }
          api.defaults.headers.common['Authorization'] = `Bearer ${response.data.access_token}`;
          originalRequest.headers['Authorization'] = `Bearer ${response.data.access_token}`;
          
          return api(originalRequest);
        }
      } catch (refreshError) {
        tokenCookies.clear();
        window.location.href = '/';
      }
    }
    
    return Promise.reject(error);
  }
);

export const AppProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [searchResults, setSearchResults] = useState([]);
  const [isSearching, setIsSearching] = useState(false);
  const [lastQuery, setLastQuery] = useState('');

  // Check authentication on mount
  useEffect(() => {
    checkAuth();
  }, []);

  const checkAuth = async () => {
    setLoading(true);
    try {
      if (tokenCookies.getAccessToken()) {
        const response = await api.get('/users/me');
        setUser(response.data);
      }
    } catch (error) {
      console.error('Auth check failed:', error);
      tokenCookies.clear();
    } finally {
      setLoading(false);
    }
  };

  // Auth methods
  const login = async (email, password) => {
    try {
      const response = await api.post('/auth/login', { email, password });
      if (response.data.access_token) {
        tokenCookies.setTokens(
          response.data.access_token,
          response.data.refresh_token ?? tokenCookies.getRefreshToken()
        );
        await checkAuth();
      }
      return { success: true, data: response.data };
    } catch (error) {
      return { 
        success: false, 
        error: error.response?.data?.detail || 'Login failed' 
      };
    }
  };

  const register = async (email, password) => {
    try {
      const response = await api.post('/auth/register', { email, password });
      return { success: true, data: response.data };
    } catch (error) {
      return { 
        success: false, 
        error: error.response?.data?.detail || 'Registration failed' 
      };
    }
  };

  const googleLogin = () => {
    window.location.href = `${config.API_URL}/auth/google/login`;
  };

  const handleGoogleCallback = () => {
    const params = new URLSearchParams(window.location.search);
    const accessToken = params.get('access_token');
    const refreshToken = params.get('refresh_token');
    
    if (accessToken) {
      tokenCookies.setTokens(accessToken, refreshToken ?? '');
      checkAuth();
      return true;
    }
    return false;
  };

  const logout = () => {
    tokenCookies.clear();
    setUser(null);
    window.location.href = '/';
  };

  // Book methods
  const searchBooks = async (query, topK = 6) => {
    setIsSearching(true);
    setLastQuery(query);
    
    try {
      const response = await api.post('/assistant/ask', {
        question: query,
        top_k: topK
      });
      console.log('Raw API response: ',response.data);
      console.log(`Sources length : ${response.data?.sources?.length}`);
      // Transform API response
      const books = response.data.sources.map(source => ({
        id: source.book_id,
        title: source.title,
        author: source.author,
        reason: source.description ? source.description.substring(0, 120) + '...' : 'No description available',
        category: source.genres?.split(',')[0]?.trim() || 'General',
        rating: 4.5,
        description: source.description,
        genres: source.genres,
        num_pages: source.num_pages,
        image_url: source.image_url,
        answer: response.data.answer,
        citations: response.data.citations
      }));
      
      console.log(`Mapped books : ${books}`)

      setSearchResults(books);
      return { success: true, books };
    } catch (error) {
      // console.error('Search error:', error);
      console.error(`Searche error : ${error.response?.data || error.message}`)
      return { success: false, error: 'Search failed' };
    } finally {
      setIsSearching(false);
    }
  };

  const getBookById = async (bookId) => {
    try {
      const response = await api.get(`/books/${bookId}`);
      return { success: true, book: response.data };
    } catch (error) {
      console.error('Failed to fetch book:', error);
      return { success: false, error: 'Book not found' };
    }
  };

  const askFollowUp = async (question, books) => {
    try {
      const bookIds = (books || []).map((b) => b.id || b.book_id).filter(Boolean);
      const response = await api.post('/assistant/ask', {
        question,
        top_k: bookIds.length || 5,
        ...(bookIds.length ? { book_ids: bookIds } : {})
      });
      return { 
        success: true, 
        answer: response.data.answer,
        citations: response.data.citations 
      };
    } catch (error) {
      console.error('Follow-up error:', error);
      return { success: false, error: 'Failed to get answer' };
    }
  };

  const getBooksByGenre = async (genre, page = 1, limit = 20) => {
    try {
      const response = await api.get(`/books/by-genre/${genre}`, {
        params: { page, limit }
      });
      return { success: true, books: response.data };
    } catch (error) {
      console.error('Failed to fetch books by genre:', error);
      return { success: false, error: 'Failed to fetch books' };
    }
  };

  const value = {
    // State
    user,
    loading,
    searchResults,
    isSearching,
    lastQuery,
    
    // Auth methods
    login,
    register,
    logout,
    googleLogin,
    handleGoogleCallback,
    isAuthenticated: () => !!user,
    
    // Book methods
    searchBooks,
    getBookById,
    askFollowUp,
    getBooksByGenre,
    setSearchResults,
  };

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
};

export const useApp = () => {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within AppProvider');
  }
  return context;
};