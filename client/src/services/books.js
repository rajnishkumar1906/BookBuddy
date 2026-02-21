// src/services/books.js - Books service
import api from './api';

export const booksService = {
  async searchBooks(query, topK = 10) {
    const response = await api.post('/assistant/ask', {
      question: query,
      top_k: topK
    });
    return response.data;
  },

  async getBookById(bookId) {
    const response = await api.get(`/books/${bookId}`);
    return response.data;
  },

  async listBooks(page = 1, limit = 20) {
    const response = await api.get('/books/', {
      params: { page, limit }
    });
    return response.data;
  },

  async getBooksByGenre(genre, page = 1, limit = 20) {
    const response = await api.get(`/books/by-genre/${genre}`, {
      params: { page, limit }
    });
    return response.data;
  },

  async askFollowUp(question, books) {
    const response = await api.post('/assistant/ask', {
      question,
      top_k: books.length
    });
    return response.data;
  }
};