import React from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export const AppShell: React.FC = () => {
  const { user, organization, logout } = useAuth();

  const navItems = [
    { label: 'Dashboard', path: '/dashboard', icon: '📊', placeholder: true },
    { label: 'Incidents', path: '/incidents', icon: '🚨', placeholder: false },
    { label: 'On-Call', path: '/on-call', icon: '📞', placeholder: false },
    { label: 'Services', path: '/services', icon: '⚙️', placeholder: false },
    { label: 'Teams', path: '/teams', icon: '👥', placeholder: false },
    { label: 'Organizations', path: '/organizations', icon: '🏢', placeholder: false },
    { label: 'Integrations', path: '/integrations', icon: '🔌', placeholder: true },
    { label: 'Analytics', path: '/analytics', icon: '📈', placeholder: true },
    { label: 'Audit Log', path: '/audit', icon: '📜', placeholder: true },
  ];

  return (
    <div style={{ display: 'flex', height: '100vh', width: '100vw', overflow: 'hidden', fontFamily: 'system-ui, sans-serif' }}>
      {/* Sidebar */}
      <aside style={{ width: '240px', background: '#1e293b', color: '#f8fafc', display: 'flex', flexDirection: 'column' }}>
        <div style={{ padding: '20px', borderBottom: '1px solid #334155' }}>
          <h2 style={{ margin: 0, fontSize: '20px', fontWeight: 'bold', color: '#38bdf8' }}>⚡ Respondr</h2>
          <div style={{ fontSize: '12px', color: '#94a3b8', marginTop: '4px' }}>
            Org: {organization?.name || 'Default'}
          </div>
        </div>

        <nav style={{ flex: 1, padding: '12px', overflowY: 'auto' }}>
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              style={({ isActive }) => ({
                display: 'flex',
                alignItems: 'center',
                padding: '10px 12px',
                margin: '4px 0',
                borderRadius: '6px',
                color: isActive ? '#ffffff' : '#cbd5e1',
                background: isActive ? '#0284c7' : 'transparent',
                textDecoration: 'none',
                fontSize: '14px',
              })}
            >
              <span style={{ marginRight: '10px' }}>{item.icon}</span>
              <span style={{ flex: 1 }}>{item.label}</span>
              {item.placeholder && (
                <span
                  style={{
                    fontSize: '10px',
                    background: '#334155',
                    color: '#94a3b8',
                    padding: '2px 6px',
                    borderRadius: '4px',
                  }}
                >
                  Soon
                </span>
              )}
            </NavLink>
          ))}
        </nav>

        <div style={{ padding: '16px', borderTop: '1px solid #334155', fontSize: '13px', color: '#94a3b8' }}>
          <div style={{ fontWeight: 'bold', color: '#f1f5f9' }}>{user?.name}</div>
          <div style={{ fontSize: '11px', marginBottom: '8px' }}>{user?.email}</div>
          <button
            onClick={logout}
            style={{
              width: '100%',
              padding: '6px 12px',
              background: '#ef4444',
              color: '#ffffff',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer',
              fontSize: '12px',
            }}
          >
            Logout
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <main style={{ flex: 1, display: 'flex', flexDirection: 'column', background: '#f8fafc', overflowY: 'auto' }}>
        <header
          style={{
            height: '60px',
            background: '#ffffff',
            borderBottom: '1px solid #e2e8f0',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '0 24px',
          }}
        >
          <div style={{ fontWeight: '600', color: '#0f172a' }}>
            Real-time Incident & On-Call Management
          </div>
          <div style={{ fontSize: '13px', color: '#64748b' }}>
            Tenant Isolation: Active
          </div>
        </header>

        <div style={{ padding: '24px', flex: 1 }}>
          <Outlet />
        </div>
      </main>
    </div>
  );
};
