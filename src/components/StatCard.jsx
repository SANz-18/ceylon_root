import React from 'react';

export default function StatCard({ label, value, delta, dir }) {
  return (
    <div className="card stat-card">
      <div className="eyebrow">{label}</div>
      <div className="val">{value}</div>
      {delta && <div className={`delta ${dir}`}>{delta}</div>}
    </div>
  );
}
