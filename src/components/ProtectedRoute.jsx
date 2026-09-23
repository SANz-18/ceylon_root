import React from 'react';
import { useAuth } from '../context/AuthContext.jsx';

export default function ProtectedRoute({ roles, children }) {
  const { user } = useAuth();
  if (!roles.includes(user.role)) {
    return (
      <div className="card" style={{ maxWidth: 480, margin: '40px auto', textAlign: 'center', padding: 32 }}>
        <h3>Not available for your role</h3>
        <p style={{ color: '#6b5947', marginTop: 8, fontSize: 13.5 }}>
          This section is restricted to {roles.join(' / ')}. Real authorization is also enforced
          server-side — this client check is just for a smoother UI.
        </p>
      </div>
    );
  }
  return children;
}
