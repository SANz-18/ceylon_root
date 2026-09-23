import React, { useEffect, useRef, useState } from 'react';
import client from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';

const POLL_MESSAGES_MS = 4000;
const POLL_THREADS_MS = 8000;

function timeLabel(d) {
  const date = new Date(d);
  const diffH = (Date.now() - date.getTime()) / 3600000;
  if (diffH < 24) return date.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
  return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) + ' ' + date.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
}
function timeAgoShort(d) {
  const diff = Math.floor((Date.now() - new Date(d).getTime()) / 60000);
  if (diff < 1) return 'now';
  if (diff < 60) return `${diff}m`;
  if (diff < 1440) return `${Math.floor(diff / 60)}h`;
  return `${Math.floor(diff / 1440)}d`;
}

function MessageBubble({ msg, mine }) {
  return (
    <div style={{ display: 'flex', justifyContent: mine ? 'flex-end' : 'flex-start', marginBottom: 10 }}>
      <div style={{ maxWidth: '72%' }}>
        {!mine && <div style={{ fontSize: 10.5, color: '#8a7256', marginBottom: 3, fontFamily: 'var(--font-mono)' }}>{msg.sender.name}</div>}
        <div style={{
          background: mine ? 'var(--rust)' : 'var(--parchment-2)',
          color: mine ? '#fff' : 'var(--ink-text)',
          padding: '9px 13px', borderRadius: 14,
          borderBottomRightRadius: mine ? 3 : 14, borderBottomLeftRadius: mine ? 14 : 3,
          fontSize: 13.5, lineHeight: 1.5, whiteSpace: 'pre-wrap', wordBreak: 'break-word',
        }}>
          {msg.content}
        </div>
        <div style={{ fontSize: 10, color: '#8a7256', marginTop: 3, textAlign: mine ? 'right' : 'left', fontFamily: 'var(--font-mono)' }}>
          {timeLabel(msg.createdAt)}
        </div>
      </div>
    </div>
  );
}

function MessageList({ messages, currentUserId }) {
  const bottomRef = useRef(null);
  useEffect(() => { bottomRef.current?.scrollIntoView({ block: 'end' }); }, [messages.length]);

  return (
    <div style={{ flex: 1, overflowY: 'auto', padding: '18px 20px' }}>
      {messages.length === 0 && (
        <div className="empty-state" style={{ paddingTop: 60 }}>No messages yet — say hello.</div>
      )}
      {messages.map((m) => <MessageBubble key={m.id} msg={m} mine={m.sender.id === currentUserId} />)}
      <div ref={bottomRef} />
    </div>
  );
}

function Composer({ onSend }) {
  const [value, setValue] = useState('');
  const [sending, setSending] = useState(false);

  const send = async () => {
    if (!value.trim() || sending) return;
    setSending(true);
    try {
      await onSend(value.trim());
      setValue('');
    } finally {
      setSending(false);
    }
  };
  const onKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(); }
  };

  return (
    <div style={{ display: 'flex', gap: 10, padding: '14px 20px', borderTop: '1px solid var(--line)' }}>
      <textarea
        rows={1}
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onKeyDown={onKeyDown}
        placeholder="Type a message… (Enter to send, Shift+Enter for a new line)"
        style={{ flex: 1, resize: 'none', padding: '10px 13px' }}
      />
      <button className="btn btn-primary" disabled={sending || !value.trim()} onClick={send}>Send</button>
    </div>
  );
}

// ===================== BUYER VIEW =====================
function BuyerChat({ user }) {
  const [messages, setMessages] = useState([]);
  const [loaded, setLoaded] = useState(false);

  const load = () => client.get('/chat/my-messages').then((r) => { setMessages(r.data); setLoaded(true); });

  useEffect(() => {
    load();
    const id = setInterval(load, POLL_MESSAGES_MS);
    return () => clearInterval(id);
  }, []);

  const send = async (content) => {
    await client.post('/chat/my-messages', { content });
    load();
  };

  return (
    <div>
      <div className="topbar"><div><h1>Live Chat</h1><div className="desc">Message our support team directly — usually replies within a few hours.</div></div></div>
      <div className="card" style={{ padding: 0, display: 'flex', flexDirection: 'column', height: '65vh' }}>
        {loaded && <MessageList messages={messages} currentUserId={user.id} />}
        <Composer onSend={send} />
      </div>
    </div>
  );
}

// ===================== STAFF / ADMIN VIEW =====================
function StaffChat({ user }) {
  const [threads, setThreads] = useState([]);
  const [selected, setSelected] = useState(null); // buyer object
  const [messages, setMessages] = useState([]);

  const loadThreads = () => client.get('/chat/threads').then((r) => setThreads(r.data));
  useEffect(() => {
    loadThreads();
    const id = setInterval(loadThreads, POLL_THREADS_MS);
    return () => clearInterval(id);
  }, []);

  const loadMessages = (buyerId) => client.get(`/chat/threads/${buyerId}/messages`).then((r) => setMessages(r.data));
  useEffect(() => {
    if (!selected) return;
    loadMessages(selected.id);
    const id = setInterval(() => loadMessages(selected.id), POLL_MESSAGES_MS);
    return () => clearInterval(id);
  }, [selected?.id]);

  const openThread = (buyer) => { setSelected(buyer); loadThreads(); };

  const send = async (content) => {
    await client.post(`/chat/threads/${selected.id}/messages`, { content });
    loadMessages(selected.id);
    loadThreads();
  };

  return (
    <div>
      <div className="topbar"><div><h1>Live Chat</h1><div className="desc">{threads.length} conversation{threads.length !== 1 ? 's' : ''}</div></div></div>
      <div className="card" style={{ padding: 0, display: 'grid', gridTemplateColumns: '280px 1fr', height: '65vh', overflow: 'hidden' }}>
        <div style={{ borderRight: '1px solid var(--line)', overflowY: 'auto' }}>
          {threads.length === 0 && <div className="empty-state" style={{ padding: '30px 16px' }}>No conversations yet.</div>}
          {threads.map((t) => (
            <div
              key={t.buyer.id}
              onClick={() => openThread(t.buyer)}
              style={{
                padding: '13px 16px', borderBottom: '1px solid var(--line)', cursor: 'pointer',
                background: selected?.id === t.buyer.id ? 'var(--parchment)' : 'transparent',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <strong style={{ fontSize: 13 }}>{t.buyer.name}</strong>
                {t.unreadCount > 0 && (
                  <span style={{ background: 'var(--rust)', color: '#fff', fontSize: 10, fontFamily: 'var(--font-mono)', padding: '1px 6px', borderRadius: 99 }}>
                    {t.unreadCount}
                  </span>
                )}
              </div>
              <div style={{ fontSize: 12, color: '#6b5947', marginTop: 3, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {t.lastSenderRole !== 'BUYER' ? 'You: ' : ''}{t.lastMessage}
              </div>
              <div style={{ fontSize: 10, color: '#8a7256', marginTop: 3, fontFamily: 'var(--font-mono)' }}>{timeAgoShort(t.lastMessageAt)} ago</div>
            </div>
          ))}
        </div>

        <div style={{ display: 'flex', flexDirection: 'column' }}>
          {selected ? (
            <>
              <div style={{ padding: '13px 20px', borderBottom: '1px solid var(--line)' }}>
                <strong style={{ fontSize: 13.5 }}>{selected.name}</strong>
                <span style={{ fontSize: 11.5, color: '#8a7256', marginLeft: 8 }}>{selected.email}</span>
              </div>
              <MessageList messages={messages} currentUserId={user.id} />
              <Composer onSend={send} />
            </>
          ) : (
            <div className="empty-state" style={{ margin: 'auto' }}>Select a conversation to view messages.</div>
          )}
        </div>
      </div>
    </div>
  );
}

export default function Chat() {
  const { user } = useAuth();
  return user.role === 'BUYER' ? <BuyerChat user={user} /> : <StaffChat user={user} />;
}
