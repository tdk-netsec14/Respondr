import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { LoginPage } from '../pages/LoginPage';

vi.stubGlobal('fetch', vi.fn());

describe('AuthFlow & ProtectedRoute', () => {
  it('redirects unauthenticated users to /login', () => {
    localStorage.clear();

    render(
      <AuthProvider>
        <MemoryRouter initialEntries={['/teams']}>
          <Routes>
            <Route path="/login" element={<div>Login Screen</div>} />
            <Route element={<ProtectedRoute />}>
              <Route path="/teams" element={<div>Protected Teams</div>} />
            </Route>
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    );

    expect(screen.getByText('Login Screen')).toBeDefined();
    expect(screen.queryByText('Protected Teams')).toBeNull();
  });

  it('renders login page correctly', () => {
    render(
      <AuthProvider>
        <MemoryRouter>
          <LoginPage />
        </MemoryRouter>
      </AuthProvider>
    );

    expect(screen.getByText('⚡ Respondr')).toBeDefined();
    expect(screen.getByText('Sign in to your Respondr Account')).toBeDefined();
  });
});
