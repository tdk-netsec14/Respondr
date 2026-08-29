import React from 'react';

interface PlaceholderPageProps {
  title: string;
  description: string;
  icon: string;
}

export const PlaceholderPage: React.FC<PlaceholderPageProps> = ({ title, description, icon }) => {
  return (
    <div style={{ padding: '40px 20px', textAlign: 'center', background: '#ffffff', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
      <div style={{ fontSize: '48px', marginBottom: '12px' }}>{icon}</div>
      <h1 style={{ margin: '0 0 8px', fontSize: '24px', color: '#0f172a' }}>{title}</h1>
      <p style={{ margin: '0 0 20px', color: '#64748b', fontSize: '14px' }}>{description}</p>
      <span
        style={{
          display: 'inline-block',
          background: '#e0f2fe',
          color: '#0369a1',
          padding: '6px 16px',
          borderRadius: '20px',
          fontSize: '12px',
          fontWeight: 'bold',
        }}
      >
        Phase 4+ Implementation Target
      </span>
    </div>
  );
};
