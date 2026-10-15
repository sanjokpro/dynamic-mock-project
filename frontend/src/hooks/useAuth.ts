import { useAuthContext } from '@/context/AuthContext';

/**
 * Thin convenience hook — re-exports everything from AuthContext.
 * Consumers: useAuth().user, useAuth().login(...), useAuth().logout(), etc.
 */
export function useAuth() {
  return useAuthContext();
}
