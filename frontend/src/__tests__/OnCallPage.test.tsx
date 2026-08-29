import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import { OnCallPage } from '../pages/OnCallPage';

describe('OnCallPage', () => {
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
        if (url.includes('/on-call/schedules') && !url.includes('/current-responder')) {
          return Promise.resolve({
            ok: true,
            status: 200,
            json: () =>
              Promise.resolve({
                content: [
                  {
                    id: 's1',
                    orgId: 'o1',
                    teamId: 't1',
                    teamName: 'Payments Team',
                    name: 'Primary On-Call',
                    timezone: 'Asia/Kolkata',
                    rotationRules: '{}',
                    active: true,
                    createdAt: new Date().toISOString(),
                    updatedAt: new Date().toISOString(),
                  },
                ],
                totalElements: 1,
              }),
          });
        }
        if (url.includes('/current-responder')) {
          return Promise.resolve({
            ok: true,
            status: 200,
            json: () =>
              Promise.resolve({
                userId: 'u1',
                userName: 'Alice Smith',
                userEmail: 'alice@acme.com',
                scheduleId: 's1',
                scheduleName: 'Primary On-Call',
                isOverride: false,
                timestamp: new Date().toISOString(),
              }),
          });
        }
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [] }) });
      })
    );
  });

  it('renders OnCallPage and displays active schedule', async () => {
    render(
      <AuthProvider>
        <MemoryRouter>
          <OnCallPage />
        </MemoryRouter>
      </AuthProvider>
    );

    expect(screen.getByText('On-Call Schedules & Shifts')).toBeDefined();

    await waitFor(() => {
      expect(screen.getByText('Primary On-Call')).toBeDefined();
      expect(screen.getByText('Asia/Kolkata')).toBeDefined();
    });
  });
});
