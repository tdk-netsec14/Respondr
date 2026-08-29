import React, { useEffect, useState } from 'react';
import type { OnCallSchedule, ScheduleOverride, ResponderResponse, Team, Page } from '../types';
import { apiFetch } from '../api/apiClient';
import { useAuth } from '../context/AuthContext';

export const OnCallPage: React.FC = () => {
  const { organization } = useAuth();
  const [schedules, setSchedules] = useState<OnCallSchedule[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [responders, setResponders] = useState<Record<string, ResponderResponse>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [showScheduleModal, setShowScheduleModal] = useState(false);
  const [teamId, setTeamId] = useState('');
  const [name, setName] = useState('');
  const [timezone, setTimezone] = useState('UTC');
  const [shiftHours, setShiftHours] = useState('8');
  const [createSchedError, setCreateSchedError] = useState<string | null>(null);

  const [selectedScheduleId, setSelectedScheduleId] = useState<string | null>(null);
  const [overrides, setOverrides] = useState<ScheduleOverride[]>([]);
  const [showOverrideModal, setShowOverrideModal] = useState(false);
  const [startAt, setStartAt] = useState('');
  const [endAt, setEndAt] = useState('');
  const [reason, setReason] = useState('');
  const [overrideError, setOverrideError] = useState<string | null>(null);

  const fetchData = async () => {
    if (!organization) return;
    setLoading(true);
    setError(null);
    try {
      const [schedData, teamData] = await Promise.all([
        apiFetch<Page<OnCallSchedule>>('/on-call/schedules'),
        apiFetch<Page<Team>>(`/organizations/${organization.id}/teams`),
      ]);
      setSchedules(schedData.content);
      setTeams(teamData.content);

      // Fetch current responder for each schedule
      const resMap: Record<string, ResponderResponse> = {};
      for (const s of schedData.content) {
        try {
          const r = await apiFetch<ResponderResponse>(`/on-call/schedules/${s.id}/current-responder`);
          resMap[s.id] = r;
        } catch (ignored) {}
      }
      setResponders(resMap);
    } catch (err: any) {
      setError(err.message || 'Failed to load on-call schedules');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [organization?.id]);

  const handleCreateSchedule = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!teamId) return;
    setCreateSchedError(null);
    try {
      await apiFetch<OnCallSchedule>(`/teams/${teamId}/on-call/schedules`, {
        method: 'POST',
        body: JSON.stringify({
          name,
          timezone,
          participantUserIds: [organization?.id || '00000000-0000-0000-0000-000000000000'],
          shiftLengthHours: parseInt(shiftHours, 10),
          rotationStartAt: new Date().toISOString(),
        }),
      });
      setShowScheduleModal(false);
      setName('');
      fetchData();
    } catch (err: any) {
      setCreateSchedError(err.message || 'Failed to create schedule');
    }
  };

  const handleFetchOverrides = async (scheduleId: string) => {
    setSelectedScheduleId(scheduleId);
    try {
      const data = await apiFetch<ScheduleOverride[]>(`/on-call/schedules/${scheduleId}/overrides`);
      setOverrides(data);
    } catch (ignored) {}
  };

  const handleCreateOverride = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedScheduleId || !startAt || !endAt) return;
    setOverrideError(null);
    try {
      await apiFetch<ScheduleOverride>(`/on-call/schedules/${selectedScheduleId}/overrides`, {
        method: 'POST',
        body: JSON.stringify({
          userId: organization?.id || '00000000-0000-0000-0000-000000000000',
          startAt: new Date(startAt).toISOString(),
          endAt: new Date(endAt).toISOString(),
          reason,
        }),
      });
      setShowOverrideModal(false);
      handleFetchOverrides(selectedScheduleId);
      fetchData();
    } catch (err: any) {
      setOverrideError(err.message || 'Failed to create schedule override');
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <div>
          <h1 style={{ margin: 0, fontSize: '24px', color: '#0f172a' }}>On-Call Schedules & Shifts</h1>
          <p style={{ margin: '4px 0 0', color: '#64748b', fontSize: '14px' }}>
            Deterministic responder rotations and shift overrides
          </p>
        </div>
        <button
          onClick={() => setShowScheduleModal(true)}
          style={{ padding: '8px 16px', background: '#0284c7', color: '#ffffff', border: 'none', borderRadius: '6px', fontWeight: 'bold', cursor: 'pointer' }}
        >
          + Create Schedule
        </button>
      </div>

      {loading && <div style={{ color: '#64748b' }}>Loading schedules...</div>}
      {error && <div style={{ padding: '12px', background: '#fee2e2', color: '#b91c1c', borderRadius: '6px', marginBottom: '16px' }}>{error}</div>}

      {!loading && !error && schedules.length === 0 && (
        <div style={{ padding: '32px', textAlign: 'center', background: '#ffffff', borderRadius: '8px', border: '1px solid #e2e8f0', color: '#64748b' }}>
          No on-call schedules configured yet. Click "+ Create Schedule" to configure team shifts.
        </div>
      )}

      {!loading && !error && schedules.length > 0 && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '20px' }}>
          {schedules.map((s) => {
            const resp = responders[s.id];
            return (
              <div key={s.id} style={{ background: '#ffffff', padding: '20px', borderRadius: '8px', border: '1px solid #e2e8f0', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div>
                    <h3 style={{ margin: 0, color: '#0f172a' }}>{s.name}</h3>
                    <div style={{ fontSize: '12px', color: '#64748b' }}>Team: {s.teamName}</div>
                  </div>
                  <span style={{ fontSize: '11px', background: '#e0f2fe', color: '#0369a1', padding: '2px 8px', borderRadius: '12px', fontWeight: 'bold' }}>
                    {s.timezone}
                  </span>
                </div>

                {/* Current Responder Card */}
                <div style={{ background: '#f8fafc', padding: '12px', borderRadius: '6px', border: '1px solid #f1f5f9' }}>
                  <div style={{ fontSize: '11px', color: '#64748b', fontWeight: 'bold', textTransform: 'uppercase' }}>
                    Current Responder (Active)
                  </div>
                  {resp ? (
                    <div style={{ marginTop: '4px' }}>
                      <div style={{ fontWeight: 'bold', color: '#0f172a' }}>
                        👤 {resp.userName} {resp.isOverride && <span style={{ color: '#d97706', fontSize: '11px' }}>(OVERRIDE)</span>}
                      </div>
                      <div style={{ fontSize: '12px', color: '#64748b' }}>{resp.userEmail}</div>
                    </div>
                  ) : (
                    <div style={{ fontSize: '12px', color: '#94a3b8', marginTop: '4px' }}>No responder active</div>
                  )}
                </div>

                <div style={{ display: 'flex', gap: '8px', marginTop: 'auto' }}>
                  <button
                    onClick={() => {
                      handleFetchOverrides(s.id);
                      setShowOverrideModal(true);
                    }}
                    style={{ flex: 1, padding: '6px 12px', background: '#f1f5f9', border: '1px solid #cbd5e1', borderRadius: '4px', cursor: 'pointer', fontSize: '12px' }}
                  >
                    Manage Overrides
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Modal: Create Schedule */}
      {showScheduleModal && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ background: '#ffffff', padding: '24px', borderRadius: '8px', width: '420px' }}>
            <h2 style={{ margin: '0 0 16px', fontSize: '18px' }}>Create On-Call Schedule</h2>
            {createSchedError && <div style={{ color: '#ef4444', fontSize: '13px', marginBottom: '12px' }}>{createSchedError}</div>}
            <form onSubmit={handleCreateSchedule} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 'bold', marginBottom: '4px' }}>Team</label>
                <select
                  required
                  value={teamId}
                  onChange={(e) => setTeamId(e.target.value)}
                  style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #cbd5e1', boxSizing: 'border-box' }}
                >
                  <option value="">Select Team</option>
                  {teams.map((t) => (
                    <option key={t.id} value={t.id}>{t.name}</option>
                  ))}
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 'bold', marginBottom: '4px' }}>Schedule Name</label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #cbd5e1', boxSizing: 'border-box' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 'bold', marginBottom: '4px' }}>Timezone</label>
                <select
                  value={timezone}
                  onChange={(e) => setTimezone(e.target.value)}
                  style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #cbd5e1', boxSizing: 'border-box' }}
                >
                  <option value="UTC">UTC</option>
                  <option value="Asia/Kolkata">Asia/Kolkata (IST)</option>
                  <option value="America/New_York">America/New_York (EST)</option>
                  <option value="Europe/London">Europe/London (GMT)</option>
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 'bold', marginBottom: '4px' }}>Shift Length (Hours)</label>
                <input
                  type="number"
                  required
                  value={shiftHours}
                  onChange={(e) => setShiftHours(e.target.value)}
                  style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #cbd5e1', boxSizing: 'border-box' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '12px' }}>
                <button
                  type="button"
                  onClick={() => setShowScheduleModal(false)}
                  style={{ padding: '8px 16px', background: '#e2e8f0', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  style={{ padding: '8px 16px', background: '#0284c7', color: '#ffffff', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
                >
                  Create Schedule
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Manage Overrides */}
      {showOverrideModal && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ background: '#ffffff', padding: '24px', borderRadius: '8px', width: '460px' }}>
            <h2 style={{ margin: '0 0 16px', fontSize: '18px' }}>Schedule Overrides</h2>
            {overrideError && <div style={{ color: '#ef4444', fontSize: '13px', marginBottom: '12px' }}>{overrideError}</div>}

            <div style={{ maxHeight: '150px', overflowY: 'auto', marginBottom: '16px', border: '1px solid #e2e8f0', borderRadius: '6px', padding: '8px' }}>
              {overrides.length === 0 && <div style={{ color: '#94a3b8', fontSize: '12px' }}>No active overrides.</div>}
              {overrides.map((o) => (
                <div key={o.id} style={{ fontSize: '12px', marginBottom: '6px', borderBottom: '1px solid #f1f5f9', paddingBottom: '4px' }}>
                  <strong>{o.userName}</strong>: {new Date(o.startAt).toLocaleString()} → {new Date(o.endAt).toLocaleString()}
                </div>
              ))}
            </div>

            <h3 style={{ fontSize: '14px', margin: '0 0 8px' }}>Create Override</h3>
            <form onSubmit={handleCreateOverride} style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '11px', fontWeight: 'bold' }}>Start Time</label>
                <input
                  type="datetime-local"
                  required
                  value={startAt}
                  onChange={(e) => setStartAt(e.target.value)}
                  style={{ width: '100%', padding: '6px', borderRadius: '4px', border: '1px solid #cbd5e1', boxSizing: 'border-box' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '11px', fontWeight: 'bold' }}>End Time</label>
                <input
                  type="datetime-local"
                  required
                  value={endAt}
                  onChange={(e) => setEndAt(e.target.value)}
                  style={{ width: '100%', padding: '6px', borderRadius: '4px', border: '1px solid #cbd5e1', boxSizing: 'border-box' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '11px', fontWeight: 'bold' }}>Reason</label>
                <input
                  type="text"
                  placeholder="e.g. Covering shift"
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  style={{ width: '100%', padding: '6px', borderRadius: '4px', border: '1px solid #cbd5e1', boxSizing: 'border-box' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '12px' }}>
                <button
                  type="button"
                  onClick={() => setShowOverrideModal(false)}
                  style={{ padding: '6px 12px', background: '#e2e8f0', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
                >
                  Close
                </button>
                <button
                  type="submit"
                  style={{ padding: '6px 12px', background: '#0284c7', color: '#ffffff', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
                >
                  Add Override
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
