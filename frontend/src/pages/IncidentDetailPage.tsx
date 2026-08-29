import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import type { Incident, IncidentEvent, Comment } from '../types';
import { apiFetch } from '../api/apiClient';

export const IncidentDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [incident, setIncident] = useState<Incident | null>(null);
  const [timeline, setTimeline] = useState<IncidentEvent[]>([]);
  const [comments, setComments] = useState<Comment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [newComment, setNewComment] = useState('');
  const [submittingComment, setSubmittingComment] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const fetchDetail = async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      const [inc, events, comms] = await Promise.all([
        apiFetch<Incident>(`/incidents/${id}`),
        apiFetch<IncidentEvent[]>(`/incidents/${id}/timeline`),
        apiFetch<Comment[]>(`/incidents/${id}/comments`),
      ]);
      setIncident(inc);
      setTimeline(events);
      setComments(comms);
    } catch (err: any) {
      setError(err.message || 'Failed to load incident detail');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDetail();
  }, [id]);

  const handleStateTransition = async (action: 'acknowledge' | 'investigate' | 'resolve' | 'reopen' | 'cancel') => {
    if (!id) return;
    setActionError(null);
    try {
      const updated = await apiFetch<Incident>(`/incidents/${id}/${action}`, { method: 'POST' });
      setIncident(updated);
      fetchDetail();
    } catch (err: any) {
      setActionError(err.message || `Failed to ${action} incident`);
    }
  };

  const handleAddComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !newComment.trim()) return;
    setSubmittingComment(true);
    try {
      await apiFetch<Comment>(`/incidents/${id}/comments`, {
        method: 'POST',
        body: JSON.stringify({ body: newComment }),
      });
      setNewComment('');
      fetchDetail();
    } catch (err: any) {
      setActionError(err.message || 'Failed to add comment');
    } finally {
      setSubmittingComment(false);
    }
  };

  if (loading) return <div style={{ color: '#64748b' }}>Loading incident details...</div>;
  if (error || !incident) return <div style={{ color: '#ef4444' }}>{error || 'Incident not found'}</div>;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', background: '#ffffff', padding: '20px', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
        <div>
          <button
            onClick={() => navigate('/incidents')}
            style={{ background: 'none', border: 'none', color: '#0284c7', cursor: 'pointer', fontSize: '13px', marginBottom: '8px', padding: 0 }}
          >
            ← Back to Incidents
          </button>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <h1 style={{ margin: 0, fontSize: '22px', color: '#0f172a' }}>{incident.title}</h1>
            <span style={{ padding: '2px 8px', borderRadius: '4px', fontSize: '12px', fontWeight: 'bold', background: '#e2e8f0' }}>
              {incident.status}
            </span>
          </div>
          <p style={{ margin: '8px 0 0', color: '#64748b', fontSize: '14px' }}>
            {incident.description || 'No description provided.'}
          </p>
        </div>

        {/* State Control Buttons */}
        <div style={{ display: 'flex', gap: '8px' }}>
          {incident.status === 'OPEN' && (
            <>
              <button onClick={() => handleStateTransition('acknowledge')} style={btnStyle('#f59e0b')}>Acknowledge</button>
              <button onClick={() => handleStateTransition('cancel')} style={btnStyle('#64748b')}>Cancel</button>
            </>
          )}
          {incident.status === 'ACKNOWLEDGED' && (
            <>
              <button onClick={() => handleStateTransition('investigate')} style={btnStyle('#8b5cf6')}>Investigate</button>
              <button onClick={() => handleStateTransition('resolve')} style={btnStyle('#10b981')}>Resolve</button>
            </>
          )}
          {incident.status === 'INVESTIGATING' && (
            <button onClick={() => handleStateTransition('resolve')} style={btnStyle('#10b981')}>Resolve</button>
          )}
          {incident.status === 'RESOLVED' && (
            <button onClick={() => handleStateTransition('reopen')} style={btnStyle('#f97316')}>Reopen</button>
          )}
          {incident.status === 'REOPENED' && (
            <>
              <button onClick={() => handleStateTransition('acknowledge')} style={btnStyle('#f59e0b')}>Acknowledge</button>
              <button onClick={() => handleStateTransition('investigate')} style={btnStyle('#8b5cf6')}>Investigate</button>
            </>
          )}
        </div>
      </div>

      {actionError && (
        <div style={{ padding: '10px', background: '#fee2e2', color: '#b91c1c', borderRadius: '6px', fontSize: '13px' }}>
          {actionError}
        </div>
      )}

      {/* Main Content Grid: Timeline Stream + Comments */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
        {/* Timeline Events Stream */}
        <div style={{ background: '#ffffff', padding: '20px', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
          <h2 style={{ margin: '0 0 16px', fontSize: '16px', color: '#0f172a' }}>📜 Audit & Timeline Stream</h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {timeline.length === 0 && <div style={{ color: '#94a3b8', fontSize: '13px' }}>No timeline events.</div>}
            {timeline.map((ev) => (
              <div key={ev.id} style={{ borderLeft: '3px solid #0284c7', paddingLeft: '12px' }}>
                <div style={{ fontWeight: 'bold', fontSize: '13px', color: '#0f172a' }}>{ev.eventType}</div>
                <div style={{ fontSize: '11px', color: '#94a3b8' }}>{new Date(ev.createdAt).toLocaleString()}</div>
                {ev.metadata && (
                  <div style={{ fontSize: '12px', color: '#475569', marginTop: '4px', background: '#f8fafc', padding: '4px 8px', borderRadius: '4px', fontFamily: 'monospace' }}>
                    {ev.metadata}
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Comments Section */}
        <div style={{ background: '#ffffff', padding: '20px', borderRadius: '8px', border: '1px solid #e2e8f0', display: 'flex', flexDirection: 'column' }}>
          <h2 style={{ margin: '0 0 16px', fontSize: '16px', color: '#0f172a' }}>💬 Collaborative Notes & Comments</h2>
          <div style={{ flex: 1, overflowY: 'auto', marginBottom: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {comments.length === 0 && <div style={{ color: '#94a3b8', fontSize: '13px' }}>No comments yet.</div>}
            {comments.map((c) => (
              <div key={c.id} style={{ background: '#f8fafc', padding: '10px', borderRadius: '6px', border: '1px solid #f1f5f9' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', fontWeight: 'bold', color: '#0f172a' }}>
                  <span>{c.authorName || 'User'}</span>
                  <span style={{ fontSize: '11px', color: '#94a3b8', fontWeight: 'normal' }}>{new Date(c.createdAt).toLocaleTimeString()}</span>
                </div>
                <p style={{ margin: '4px 0 0', fontSize: '13px', color: '#334155' }}>{c.body}</p>
              </div>
            ))}
          </div>

          <form onSubmit={handleAddComment} style={{ display: 'flex', gap: '8px' }}>
            <input
              type="text"
              placeholder="Add a comment or incident update..."
              value={newComment}
              onChange={(e) => setNewComment(e.target.value)}
              style={{ flex: 1, padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px' }}
            />
            <button
              type="submit"
              disabled={submittingComment || !newComment.trim()}
              style={{ padding: '8px 16px', background: '#0284c7', color: '#ffffff', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: 'bold' }}
            >
              Post
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

const btnStyle = (color: string) => ({
  padding: '6px 14px',
  background: color,
  color: '#ffffff',
  border: 'none',
  borderRadius: '6px',
  fontWeight: 'bold' as const,
  cursor: 'pointer',
  fontSize: '13px',
});
