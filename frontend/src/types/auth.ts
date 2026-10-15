export interface AuthUser {
  id: string;
  username: string;
  email?: string;
}

export interface LoginResponseData {
  token: string;
  expiresInSeconds: number;
  userId: string;
  username: string;
  email: string;
}

export interface AuthContextType {
  user: AuthUser | null;
  token: string | null;
  isLoading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  getToken: () => string | null;
}
