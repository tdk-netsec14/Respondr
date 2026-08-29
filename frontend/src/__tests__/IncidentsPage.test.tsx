import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import { IncidentsPage } from '../pages/IncidentsPage';

describe('IncidentsPage', () => {
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
        if (url.includes('/incidents')) {
          return Promise.resolve({
            ok: true,
            status: 200,
            json: () =>
              Promise.resolve({
                content: [
                  {
                    id: 'inc-1',
                    orgId: 'o1',
                    title: 'Database Outage',
                    description: 'Primary Postgres unreachable',
                    severity: 'CRITICAL',
                    status: 'OPEN',
                    createdAt: new Date().toISOString(),
                    updatedAt: new Date().toISOString(),
                  },
                ],
                totalElements: 1,
              }),
          });
        }
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [] }) });
      })
    );
  });

  it('renders IncidentsPage and displays loaded incidents', async () => {
    render(
      <AuthProvider>
        <MemoryRouter>
          <IncidentsPage />
        </MemoryRouter>
      </AuthProvider>
    );

    expect(screen.getByText('Incidents')).toBeDefined();

    await waitFor(() => {
      expect(screen.getByText('Database Outage')).toBeDefined();
      expect(screen.getByText('CRITICAL')).toBeDefined();
    });
  });
});
