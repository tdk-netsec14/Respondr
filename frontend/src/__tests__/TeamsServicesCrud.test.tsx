import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import { TeamsPage } from '../pages/TeamsPage';

describe('Teams & Services CRUD Integration', () => {
  beforeEach(() => {
    localStorage.setItem('respondr_access_token', 'mock_access_token');
    localStorage.setItem(
      'respondr_user',
      JSON.stringify({ id: 'u1', email: 'admin@acme.com', name: 'Admin', status: 'ACTIVE' })
    );
    localStorage.setItem(
      'respondr_org',
      JSON.stringify({ id: 'o1', name: 'Acme Corp', slug: 'acme' })
    );

    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation((url) => {
        if (url.includes('/teams')) {
          return Promise.resolve({
            ok: true,
            status: 200,
            json: () =>
              Promise.resolve({
                content: [
                  { id: 't1', orgId: 'o1', name: 'Platform Engineering', description: 'Infra team' },
                ],
                totalElements: 1,
              }),
          });
        }
        return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
      })
    );
  });

  it('renders TeamsPage and displays loaded teams', async () => {
    render(
      <AuthProvider>
        <MemoryRouter>
          <TeamsPage />
        </MemoryRouter>
      </AuthProvider>
    );

    expect(screen.getByText('Teams')).toBeDefined();

    await waitFor(() => {
      expect(screen.getByText('Platform Engineering')).toBeDefined();
    });
  });
});
