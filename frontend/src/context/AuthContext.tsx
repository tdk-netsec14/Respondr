import React, { createContext, useContext, useState, useEffect } from 'react';
import type { User, Organization, AuthResponse } from '../types';
import { apiFetch } from '../api/apiClient';

interface AuthContextType {
  user: User | null;
  organization: Organization | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: { email: string; password: string }) => Promise<void>;
  register: (data: {
    email: string;
    password: string;
    name: string;
    orgName: string;
    orgSlug: string;
  }) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    const saved = localStorage.getItem('respondr_user');
    return saved ? JSON.parse(saved) : null;
  });

  const [organization, setOrganization] = useState<Organization | null>(() => {
    const saved = localStorage.getItem('respondr_org');
    return saved ? JSON.parse(saved) : null;
  });

  const [accessToken, setAccessToken] = useState<string | null>(() =>
    localStorage.getItem('respondr_access_token')
  );

  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    const handleAuthExpired = () => {
      logout();
    };
    window.addEventListener('respondr_auth_expired', handleAuthExpired);
    return () => window.removeEventListener('respondr_auth_expired', handleAuthExpired);
  }, []);

  const saveAuthData = (data: AuthResponse) => {
    localStorage.setItem('respondr_access_token', data.accessToken);
    localStorage.setItem('respondr_refresh_token', data.refreshToken);
    localStorage.setItem('respondr_user', JSON.stringify(data.user));
    localStorage.setItem('respondr_org', JSON.stringify(data.organization));

    setAccessToken(data.accessToken);
    setUser(data.user);
    setOrganization(data.organization);
  };

  const login = async (credentials: { email: string; password: string }) => {
    setIsLoading(true);
    try {
      const data = await apiFetch<AuthResponse>('/auth/login', {
        method: 'POST',
        body: JSON.stringify(credentials),
      });
      saveAuthData(data);
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (data: {
    email: string;
    password: string;
    name: string;
    orgName: string;
    orgSlug: string;
  }) => {
    setIsLoading(true);
    try {
      const resData = await apiFetch<AuthResponse>('/auth/register', {
        method: 'POST',
        body: JSON.stringify(data),
      });
      saveAuthData(resData);
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('respondr_access_token');
    localStorage.removeItem('respondr_refresh_token');
    localStorage.removeItem('respondr_user');
    localStorage.removeItem('respondr_org');
    setAccessToken(null);
    setUser(null);
    setOrganization(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        organization,
        accessToken,
        isAuthenticated: !!accessToken && !!user,
        isLoading,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
