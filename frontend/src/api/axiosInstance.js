import axios from 'axios';

const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
});

axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Full page load to the login screen; a separate object so tests can replace it.
export const sessionNavigation = {
  toLogin() {
    if (window.location.pathname !== '/login') {
      window.location.href = '/login';
    }
  },
};

axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    // A 401 from a logged-in request means the session is gone (expired, invalid or
    // deactivated): clear it and go to the login page. A 401 from login/register itself
    // is just "wrong email or password" and must reach the form so it can show the
    // message; redirecting there would reload the page and wipe the error.
    const isAuthRequest = error.config?.url?.startsWith('/api/auth/');
    if (error.response?.status === 401 && !isAuthRequest) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      sessionNavigation.toLogin();
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;