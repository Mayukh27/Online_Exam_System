import axios from 'axios';
const api = axios.create({ baseURL: window.location.origin,});
api.interceptors.request.use((config) => {
  const stored = localStorage.getItem('examportal_user');
  if (stored) { const { token } = JSON.parse(stored); if (token) config.headers.Authorization = 'Bearer ' + token; }
  return config;
});
api.interceptors.response.use((res) => res, (err) => {
  if (err.response?.status === 401) { localStorage.removeItem('examportal_user'); window.location.href = '/login'; }
  return Promise.reject(err);
});
export default api;
