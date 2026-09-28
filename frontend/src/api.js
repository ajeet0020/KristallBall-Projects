import axios from 'axios';

export const api = axios.create({ baseURL: '/api' });

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export function messageFromError(error) {
  return error?.response?.data?.message || error?.response?.data?.error || error.message || 'Request failed';
}
