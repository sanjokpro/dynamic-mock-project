import axios from 'axios';

const isServer = typeof window === 'undefined';
const baseURL = isServer ? (process.env.BACKEND_URL || 'http://localhost:8080') + '/api' : '/api';

const apiClient = axios.create({
  baseURL,
});

// Add a request interceptor to inject the API key
apiClient.interceptors.request.use((config) => {
  const apiKey = process.env.NEXT_PUBLIC_API_KEY || 'default-dev-key';
  if (apiKey) {
    config.headers['X-API-KEY'] = apiKey;
  }
  return config;
});

// Add a response interceptor to handle 401s
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      console.error('API Key Authentication Failed (401). Check NEXT_PUBLIC_API_KEY.');
    }
    return Promise.reject(error);
  }
);

export default apiClient;
