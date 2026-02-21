// src/services/api.js - API client with interceptors
import axios from 'axios';
import config from './config';
import { tokenCookies } from '../utils/cookies';

const api = axios.create({
  baseURL: config.API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token from cookie
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

export default api;