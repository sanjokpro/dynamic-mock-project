'use client';

import React, { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { AuthContextType, AuthUser, LoginResponseData } from '@/types/auth';

// ---------------------------------------------------------------------------
// In-memory token store (never touches localStorage / sessionStorage)
// ---------------------------------------------------------------------------
let _memToken: string | null = null;

function setMemToken(t: string | null) {
  _memToken = t;
}

function getMemToken(): string | null {
  return _memToken;
}

// ---------------------------------------------------------------------------
// Context
// ---------------------------------------------------------------------------
const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const hydratedRef = useRef(false);

  const applyToken = useCallback((t: string | null) => {
    setMemToken(t);
    setToken(t);
  }, []);

  // Attempt to hydrate current user from backend using existing token.
  // On first mount there is no in-memory token so /me will 401 → user stays null.
  useEffect(() => {
    if (hydratedRef.current) return;
    hydratedRef.current = true;

    const stored = getMemToken();
    if (!stored) {
      setIsLoading(false);
      return;
    }

    fetch('/api/auth/me', {
      headers: { Authorization: `Bearer ${stored}` },
    })
      .then(async (res) => {
        if (res.ok) {
          const data = await res.json();
          setUser({ id: data.id, username: data.username, email: data.email });
        } else {
          applyToken(null);
        }
      })
      .catch(() => applyToken(null))
      .finally(() => setIsLoading(false));
  }, [applyToken]);

  // Listen for 401 signals dispatched by apiClient (avoids circular dep)
  useEffect(() => {
    const handler = () => {
      applyToken(null);
      setUser(null);
    };
    window.addEventListener('auth:unauthorized', handler);
    return () => window.removeEventListener('auth:unauthorized', handler);
  }, [applyToken]);

  const login = useCallback(
    async (username: string, password: string) => {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });

      if (!res.ok) {
        let message = 'Invalid username or password';
        try {
          const body = await res.json();
          message = body.message || message;
        } catch {
          // ignore parse errors
        }
        throw new Error(message);
      }

      const data: LoginResponseData = await res.json();
      applyToken(data.token);
      setUser({ id: data.userId, username: data.username, email: data.email });
    },
    [applyToken],
  );

  const logout = useCallback(() => {
    applyToken(null);
    setUser(null);
  }, [applyToken]);

  const getToken = useCallback(() => getMemToken(), []);

  return (
    <AuthContext.Provider value={{ user, token, isLoading, login, logout, getToken }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuthContext(): AuthContextType {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuthContext must be used within <AuthProvider>');
  }
  return ctx;
}

// Expose the raw getter so apiClient (non-React module) can read the token
export { getMemToken };
