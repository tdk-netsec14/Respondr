import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AppShell } from './components/AppShell';
import { LoginPage } from './pages/LoginPage';
import { OrganizationsPage } from './pages/OrganizationsPage';
import { TeamsPage } from './pages/TeamsPage';
import { ServicesPage } from './pages/ServicesPage';
import { IncidentsPage } from './pages/IncidentsPage';
import { IncidentDetailPage } from './pages/IncidentDetailPage';
import { OnCallPage } from './pages/OnCallPage';
import { PlaceholderPage } from './pages/PlaceholderPage';

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />

          <Route element={<ProtectedRoute />}>
            <Route element={<AppShell />}>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route
                path="/dashboard"
                element={
                  <PlaceholderPage
                    title="Dashboard"
                    description="Real-time incident metrics, MTTA/MTTR analytics, and live incident stream."
                    icon="📊"
                  />
                }
              />
              <Route path="/incidents" element={<IncidentsPage />} />
              <Route path="/incidents/:id" element={<IncidentDetailPage />} />
              <Route path="/on-call" element={<OnCallPage />} />
              <Route path="/services" element={<ServicesPage />} />
              <Route path="/teams" element={<TeamsPage />} />
              <Route path="/organizations" element={<OrganizationsPage />} />
              <Route
                path="/integrations"
                element={
                  <PlaceholderPage
                    title="Integrations & Webhooks"
                    description="Alert ingestion endpoints (Prometheus, Datadog, PagerDuty, Webhooks)."
                    icon="🔌"
                  />
                }
              />
              <Route
                path="/analytics"
                element={
                  <PlaceholderPage
                    title="Analytics & SLA Reports"
                    description="Service reliability SLA, mean-time-to-acknowledge, and resolution analytics."
                    icon="📈"
                  />
                }
              />
              <Route
                path="/audit"
                element={
                  <PlaceholderPage
                    title="Audit Log"
                    description="Immutable organization audit trail and security activity logs."
                    icon="📜"
                  />
                }
              />
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
