import axios from 'axios';

const normalizeApiBaseUrl = (rawValue) => {
  if (!rawValue) return rawValue;
  return rawValue.endsWith('/') ? rawValue.slice(0, -1) : rawValue;
};

const configuredApiBaseUrl = normalizeApiBaseUrl(import.meta.env.VITE_API_BASE_URL);
const isBrowserLocal = typeof window !== 'undefined' && ['localhost', '127.0.0.1'].includes(window.location.hostname);
const isLanIpv4Host = typeof window !== 'undefined' && /^((10\.)|(192\.168\.)|(172\.(1[6-9]|2\d|3[0-1])\.))\d+\.\d+$/.test(window.location.hostname);

const dynamicLocalApiBaseUrl = typeof window !== 'undefined'
  ? `http://${window.location.hostname}:8080/api`
  : null;

export const API_BASE_URL = configuredApiBaseUrl
  || ((isBrowserLocal || isLanIpv4Host) ? dynamicLocalApiBaseUrl : '/api');

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

const readTokenFromStorage = () => {
  const directToken = localStorage.getItem('token') || localStorage.getItem('accessToken');
  if (directToken) {
    return directToken;
  }

  const rawUser = localStorage.getItem('user');
  if (!rawUser) {
    return null;
  }

  try {
    const parsed = JSON.parse(rawUser);
    return parsed?.accessToken || parsed?.token || null;
  } catch {
    return null;
  }
};

api.interceptors.request.use((config) => {
  const token = readTokenFromStorage();
  if (token) {
    // Keep a canonical key so all auth checks stay in sync.
    localStorage.setItem('token', token);
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    if ((status === 401 || status === 403) && readTokenFromStorage()) {
      localStorage.removeItem('token');
      localStorage.removeItem('accessToken');
      localStorage.removeItem('user');
      if (window.location.pathname !== '/login') {
        window.location.assign('/login');
      }
    }
    return Promise.reject(error);
  }
);

export default api;
