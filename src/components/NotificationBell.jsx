import React, { useEffect, useRef, useState } from 'react';
import client from '../api/client.js';

const POLL_MS = 20000;

function timeAgo(d) {
  const diff = Math.floor((Date.now() - new Date(d).getTime()) / 60000);
  if (diff < 1) return 'just now';
  if (diff < 60) return `${diff}m ago`;
  if (diff < 1440) return `${Math.floor(diff / 60)}h ago`;
  return `${Math.floor(diff / 1440)}d ago`;
}

const TYPE_DOT = {
  ORDER_CONFIRMED: 'var(--jade)',
  ORDER_STATUS: 'var(--jade)',
  ORDER_CANCELLED: 'var(--danger)',
  NEW_ORDER: 'var(--rust)',
  NEW_FEEDBACK: 'var(--gold)',
  FEEDBACK_REPLY: 'var(--jade)',
};

export default function NotificationBell() {
  const [items, setItems] = useState([]);
  const [unread, setUnread] = useState(0);
  const [open, setOpen] = useState(false);
  const boxRef = useRef(null);

  const loadCount = () => client.get('/notifications/unread-count').then((r) => setUnread(r.data.count)).catch(() => {});
  const loadList = () => client.get('/notifications').then((r) => setItems(r.data)).catch(() => {});

  useEffect(() => {
    loadCount();
    const id = setInterval(loadCount, POLL_MS);
    return () => clearInterval(id);
  }, []);

  useEffect(() => {
    if (open) loadList();
  }, [open]);

  useEffect(() => {
    const onClickOutside = (e) => { if (boxRef.current && !boxRef.current.contains(e.target)) setOpen(false); };
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, []);

  const markRead = async (n) => {
    if (n.read) return;
    await client.post(`/notifications/${n.id}/read`);
    setItems((prev) => prev.map((i) => (i.id === n.id ? { ...i, read: true } : i)));
    setUnread((c) => Math.max(0, c - 1));
  };

  const markAllRead = async () => {
    await client.post('/notifications/read-all');
    setItems((prev) => prev.map((i) => ({ ...i, read: true })));
    setUnread(0);
  };

  return (
    <div ref={boxRef} style={{ position: 'relative' }}>
      <button
        className="nav-item"
        style={{ position: 'relative', width: '100%' }}
        onClick={() => setOpen((o) => !o)}
      >
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.73 21a2 2 0 0 1-3.46 0" />
        </svg>
        <span>Notifications</span>
        {unread > 0 && (
          <span style={{
            marginLeft: 'auto', background: 'var(--rust)', color: '#fff', fontSize: 10.5,
            fontFamily: 'var(--font-mono)', padding: '1px 7px', borderRadius: 99,
          }}>
            {unread > 9 ? '9+' : unread}
          </span>
        )}
      </button>

      {open && (
        <div
          className="card"
          style={{
            position: 'absolute', left: '100%', top: 0, marginLeft: 10, width: 340,
            maxHeight: 'min(440px, calc(100vh - 40px))', overflowY: 'auto', zIndex: 80, padding: 0,
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '14px 16px', borderBottom: '1px solid var(--line)', position: 'sticky', top: 0, background: '#fff' }}>
            <strong style={{ fontSize: 13.5 }}>Notifications</strong>
            {unread > 0 && <button className="btn btn-ghost btn-sm" onClick={markAllRead}>Mark all read</button>}
          </div>

          {items.length === 0 && (
            <div style={{ padding: '30px 16px', textAlign: 'center', color: '#8a7256', fontSize: 12.5 }}>
              Nothing yet — you'll see order and feedback updates here.
            </div>
          )}

          {items.map((n) => (
            <div
              key={n.id}
              onClick={() => markRead(n)}
              style={{
                display: 'flex', gap: 10, padding: '12px 16px', borderBottom: '1px solid var(--line)',
                cursor: n.read ? 'default' : 'pointer', background: n.read ? 'transparent' : 'rgba(176,86,43,0.05)',
              }}
            >
              <span style={{ width: 7, height: 7, borderRadius: '50%', marginTop: 5, flexShrink: 0, background: n.read ? 'var(--parchment-3)' : (TYPE_DOT[n.type] || 'var(--rust)') }} />
              <div style={{ minWidth: 0 }}>
                <div style={{ fontSize: 12.5, fontWeight: n.read ? 500 : 700, color: 'var(--ink-text)' }}>{n.title}</div>
                <div style={{ fontSize: 12, color: '#6b5947', marginTop: 2, lineHeight: 1.4 }}>{n.message}</div>
                <div style={{ fontSize: 10.5, color: '#8a7256', marginTop: 4, fontFamily: 'var(--font-mono)' }}>{timeAgo(n.createdAt)}</div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
