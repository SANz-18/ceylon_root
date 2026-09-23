import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import client from '../api/client.js';
import QuillTracker, { STAGE_LABEL } from '../components/QuillTracker.jsx';

function timeAgo(d) {
  const diff = Math.floor((Date.now() - new Date(d).getTime()) / 3600000);
  if (diff < 1) return 'just now';
  if (diff < 24) return `${diff}h ago`;
  return `${Math.floor(diff / 24)}d ago`;
}

function money(v, cur) {
  return cur === 'USD'
    ? `$${Number(v).toLocaleString(undefined, { maximumFractionDigits: 2 })}`
    : `Rs. ${Number(v).toLocaleString()}`;
}

function TrackingModal({ order, onClose }) {
  const latest = order.history?.[order.history.length - 1];
  return (
    <div className="modal-bg" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" style={{ maxWidth: 560 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <div className="eyebrow" style={{ color: '#8a7256' }}>Tracking code</div>
            <h3 className="mono" style={{ marginTop: 2 }}>{order.trackingCode}</h3>
          </div>
          <button className="btn btn-ghost btn-sm" onClick={onClose}>Close</button>
        </div>

        <div style={{ display: 'flex', gap: 18, flexWrap: 'wrap', marginTop: 14, fontSize: 12.5, color: '#6b5947' }}>
          <div><strong style={{ color: 'var(--ink-text)' }}>{order.orderCode}</strong></div>
          <div>{order.destinationType === 'INTERNATIONAL' ? 'Export' : 'Local'} · {order.country}</div>
          <div>{order.items?.reduce((a, i) => a + i.qty, 0)}kg</div>
          <div className="mono">{money(order.total, order.currency)}</div>
        </div>

        {order.status === 'CANCELLED'
          ? <div className="role-note" style={{ marginTop: 18 }}>This order was cancelled.</div>
          : <QuillTracker status={order.status} />}

        {latest && (
          <div style={{ marginTop: 10, fontSize: 12.5, color: '#8a7256' }}>
            <strong style={{ color: 'var(--ink-text)' }}>{STAGE_LABEL[latest.status]}</strong> — {latest.note} · {timeAgo(latest.updatedAt)}
          </div>
        )}

        <div className="section-title" style={{ margin: '20px 0 8px' }}><h3 style={{ fontSize: 14 }}>Shipment contents</h3></div>
        <div className="card" style={{ padding: '6px 16px' }}>
          {order.items?.map((it) => (
            <div key={it.id} className="cart-item">
              <span>{it.grade}</span>
              <span className="mono">{it.qty}kg</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [trackCode, setTrackCode] = useState('');
  const [trackResult, setTrackResult] = useState(null);
  const [trackError, setTrackError] = useState('');
  const [trackLoading, setTrackLoading] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await login(email, password);
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.message || 'Incorrect email or password.');
    }
  };

  const quickLogin = async (demoEmail, demoPassword) => {
    setError('');
    try {
      await login(demoEmail, demoPassword);
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed.');
    }
  };

  const guestTrack = async (e) => {
    e.preventDefault();
    if (!trackCode.trim()) return;
    setTrackError('');
    setTrackLoading(true);
    try {
      const { data } = await client.get(`/orders/track/${trackCode.trim().toUpperCase()}`);
      setTrackResult(data);
    } catch {
      setTrackError('No shipment found for that tracking code.');
    } finally {
      setTrackLoading(false);
    }
  };

  return (
    <div id="login-screen">
      <div className="login-art">
        <div className="brandmark"><span>Ceylon Roots</span></div>
        <div className="login-headline">
          <div className="eyebrow">Export &amp; Ordering Platform</div>
          <h1>From Matale bark<br />to <em>your</em> doorstep.</h1>
          <p>A single system for grading, ordering, secure payment, shipment tracking and buyer feedback.</p>
        </div>
        <div style={{ color: 'rgba(241,231,211,0.6)', fontSize: 12 }}>React + Spring Boot demo</div>
      </div>
      <div className="login-panel">
        <div className="login-box">
          <h2>Welcome back</h2>
          <div className="sub">Sign in to manage orders, shipments and analytics.</div>
          <form onSubmit={submit}>
            <div className="field">
              <label>Email</label>
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@company.com" />
            </div>
            <div className="field">
              <label>Password</label>
              <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" />
              {error && <div className="field-error" style={{ display: 'block' }}>{error}</div>}
            </div>
            <button className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }} type="submit">Sign in</button>
          </form>
          <div style={{ marginTop: 14, fontSize: 12.5 }}>
            No account? <Link to="/register" style={{ color: 'var(--jade)' }}>Register as a buyer</Link>
          </div>
          <div className="demo-row">
            <button className="demo-chip" onClick={() => quickLogin('admin@ceylonroots.lk', 'admin123')}>Admin demo</button>
            <button className="demo-chip" onClick={() => quickLogin('staff@ceylonroots.lk', 'staff123')}>Staff demo</button>
            <button className="demo-chip" onClick={() => quickLogin('buyer@ceylonroots.lk', 'buyer123')}>Buyer demo</button>
          </div>

          <div className="track-box">
            <label style={{ fontSize: 12, fontWeight: 600, fontFamily: 'var(--font-mono)', textTransform: 'uppercase', letterSpacing: '0.05em', color: '#4a3a29' }}>
              Track a shipment
            </label>
            <div style={{ fontSize: 11.5, color: '#8a7256', marginTop: 2, marginBottom: 8 }}>
              No account needed — enter the tracking code from your order confirmation.
            </div>
            <form className="row" onSubmit={guestTrack}>
              <input
                className="mono"
                placeholder="CYN-XXXXXX"
                value={trackCode}
                onChange={(e) => { setTrackCode(e.target.value); setTrackError(''); }}
                style={{ textTransform: 'uppercase', letterSpacing: '0.04em' }}
              />
              <button className="btn btn-jade btn-sm" type="submit" disabled={trackLoading}>
                {trackLoading ? 'Tracking…' : 'Track'}
              </button>
            </form>
            {trackError && <div className="field-error" style={{ display: 'block', marginTop: 10 }}>{trackError}</div>}
          </div>
        </div>
      </div>

      {trackResult && <TrackingModal order={trackResult} onClose={() => setTrackResult(null)} />}
    </div>
  );
}
