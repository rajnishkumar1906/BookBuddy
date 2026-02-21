// src/services/auth.js - Authentication service
import api from './api';
import config from './config';
import { tokenCookies } from '../utils/cookies';

export const authService = {
  async login(email, password) {
    const response = await api.post('/auth/login', { email, password });
    if (response.data.access_token) {
      tokenCookies.setTokens(
        response.data.access_token,
        response.data.refresh_token ?? tokenCookies.getRefreshToken()
      );
    }
    return response.data;
  },

  async register(email, password) {
    const response = await api.post('/auth/register', {
      email,
      password
    });
    return response.data;
  },

  googleLogin() {
    window.location.href = `${config.API_URL}/auth/google/login`;
  },

  logout() {
    tokenCookies.clear();
    window.location.href = '/';
  },

  handleGoogleCallback() {
    const params = new URLSearchParams(window.location.search);
    const accessToken = params.get('access_token');
    const refreshToken = params.get('refresh_token');

    if (accessToken) {
      tokenCookies.setTokens(accessToken, refreshToken ?? '');
      window.location.href = '/dashboard';
      return true;
    }
    return false;
  },

  isAuthenticated() {
    return tokenCookies.hasAccessToken();
  },

  async getCurrentUser() {
    try {
      const response = await api.get('/users/me');
      return response.data;
    } catch (error) {
      return null;
    }
  }
};