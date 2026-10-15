import axios from 'axios';

const isServer = typeof window === 'undefined';
const baseURL = isServer
  ? (process.env.BACKEND_URL || 'http://localhost:8080') + '/api'
  : '/api';

const apiClient = axios.create({ baseURL });

// ---------------------------------------------------------------------------
// Request interceptor — attach JWT Bearer token (in-memory) + API key fallback
// ---------------------------------------------------------------------------
apiClient.interceptors.request.use((config) => {
  // Lazy import to avoid circular dependency at module initialisation time.
  // getMemToken() reads the module-level variable set by AuthContext.
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const { getMemToken } = require('@/context/AuthContext') as {
    getMemToken: () => string | null;
  };

  const token = getMemToken();
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  } else {
    // Fallback: API key for unauthenticated / legacy dev routes
    const apiKey = process.env.NEXT_PUBLIC_API_KEY || 'default-dev-key';
    if (apiKey) {
      config.headers['X-API-KEY'] = apiKey;
    }
  }
  return config;
});

// ---------------------------------------------------------------------------
// Response interceptor — handle 401 by clearing auth state and redirecting
// ---------------------------------------------------------------------------
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && typeof window !== 'undefined') {
      // Signal the AuthProvider to clear its state
      window.dispatchEvent(new CustomEvent('auth:unauthorized'));
      // Redirect to login only if not already there
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  },
);

export default apiClient;
