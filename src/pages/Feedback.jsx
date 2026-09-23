import React, { useEffect, useState } from 'react';
import { Doughnut, Bar } from 'react-chartjs-2';
import { Chart as ChartJS, registerables } from 'chart.js';
import client from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';
import StatCard from '../components/StatCard.jsx';

ChartJS.register(...registerables);

function timeAgo(d) {
  const diff = Math.floor((Date.now() - new Date(d).getTime()) / 3600000);
  if (diff < 1) return 'just now';
  if (diff < 24) return `${diff}h ago`;
  return `${Math.floor(diff / 24)}d ago`;
}
function Stars({ n }) {
  return (
    <div className="stars">
      {[1, 2, 3, 4, 5].map((i) => (
        <span key={i} style={{ color: i <= n ? 'var(--gold)' : 'var(--parchment-3)', fontSize: 14 }}>★</span>
      ))}
    </div>
  );
}

function FeedbackItem({ f, adminView, onReply }) {
  const [replying, setReplying] = useState(false);
  const [replyText, setReplyText] = useState('');
  const keywords = (f.matchedKeywords || '').split(',').filter(Boolean);

  return (
    <div className="feedback-item">
      <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8 }}>
        <div>
          <strong>{f.buyer.name}</strong>{' '}
          <span style={{ fontSize: 11.5, color: '#8a7256' }}>· {f.order.orderCode} · {timeAgo(f.createdAt)}</span>
          <div style={{ marginTop: 4 }}><Stars n={f.rating} /></div>
        </div>
        <span className={`sentiment-tag sent-${f.sentimentLabel.toLowerCase()}`}>{f.sentimentLabel.toLowerCase()}</span>
      </div>
      <p style={{ marginTop: 8, fontSize: 13.5, lineHeight: 1.55 }}>{f.comment}</p>
      {!!keywords.length && (
        <div style={{ marginTop: 6 }}>
          {keywords.map((k) => <span key={k} className="keyword-chip">{k}</span>)}
        </div>
      )}
      {f.adminReply && (
        <div style={{ marginTop: 10, padding: '10px 12px', background: 'var(--parchment)', borderRadius: 8, fontSize: 12.5 }}>
          <strong>Ceylon Roots:</strong> {f.adminReply}
        </div>
      )}
      {adminView && !f.adminReply && !replying && (
        <button className="btn btn-ghost btn-sm" style={{ marginTop: 8 }} onClick={() => setReplying(true)}>Reply</button>
      )}
      {replying && (
        <div style={{ marginTop: 8 }}>
          <textarea rows={2} value={replyText} onChange={(e) => setReplyText(e.target.value)} placeholder="Write a reply…" />
          <div style={{ display: 'flex', gap: 8, marginTop: 6, justifyContent: 'flex-end' }}>
            <button className="btn btn-ghost btn-sm" onClick={() => setReplying(false)}>Cancel</button>
            <button className="btn btn-primary btn-sm" onClick={() => { onReply(f.id, replyText); setReplying(false); }}>Send</button>
          </div>
        </div>
      )}
    </div>
  );
}

export default function Feedback() {
  const { user, showToast } = useAuth();
  const [list, setList] = useState([]);

  const load = () => client.get('/feedback').then((r) => setList(r.data));
useEffect(() => { load(); }, []);

  const reply = async (id, text) => {
    if (!text.trim()) return;
    try {
      await client.post(`/feedback/${id}/reply`, { reply: text });
      showToast('Reply posted.', 'ok');
      load();
    } catch (err) {
      showToast(err.response?.data?.message || 'Could not send reply.', 'err');
    }
  };

  if (user.role === 'BUYER') {
    return (
      <div>
        <div className="topbar"><div><h1>My Feedback</h1><div className="desc">Reviews you've left on delivered orders.</div></div></div>
        <div className="card">
          {list.length
            ? list.map((f) => <FeedbackItem key={f.id} f={f} adminView={false} onReply={reply} />)
            : <div className="empty-state">No feedback submitted yet — leave a review from a delivered order.</div>}
        </div>
      </div>
    );
  }

  // admin: aggregate stats + charts + full list
  const pos = list.filter((f) => f.sentimentLabel === 'POSITIVE').length;
  const neu = list.filter((f) => f.sentimentLabel === 'NEUTRAL').length;
  const neg = list.filter((f) => f.sentimentLabel === 'NEGATIVE').length;
  const avg = list.length ? (list.reduce((a, f) => a + f.rating, 0) / list.length).toFixed(1) : '—';
  const ratingCounts = [1, 2, 3, 4, 5].map((n) => list.filter((f) => f.rating === n).length);

  return (
    <div>
      <div className="topbar"><div><h1>Feedback &amp; Sentiment Analysis</h1><div className="desc">Keyword-based sentiment scoring across buyer reviews.</div></div></div>

      <div className="grid grid-4">
        <StatCard label="Average rating" value={`${avg} / 5`} />
        <StatCard label="Positive" value={pos} />
        <StatCard label="Neutral" value={neu} />
        <StatCard label="Negative" value={neg} />
      </div>

      <div className="grid grid-2" style={{ marginTop: 16 }}>
        <div className="card">
          <div className="eyebrow" style={{ color: '#8a7256' }}>Sentiment distribution</div>
          <div className="chart-wrap" style={{ height: 220 }}>
            <Doughnut
              data={{ labels: ['Positive', 'Neutral', 'Negative'], datasets: [{ data: [pos, neu, neg], backgroundColor: ['#2F6E62', '#C9A34E', '#B23A3A'] }] }}
              options={{ maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } }}
            />
          </div>
        </div>
        <div className="card">
          <div className="eyebrow" style={{ color: '#8a7256' }}>Rating distribution</div>
          <div className="chart-wrap" style={{ height: 220 }}>
            <Bar
              data={{ labels: ['1★', '2★', '3★', '4★', '5★'], datasets: [{ data: ratingCounts, backgroundColor: '#B0562B', borderRadius: 6 }] }}
              options={{ maintainAspectRatio: false, plugins: { legend: { display: false } } }}
            />
          </div>
        </div>
      </div>

      <div className="section-title"><h3>All reviews</h3></div>
      <div className="card">
        {list.length
          ? list.map((f) => <FeedbackItem key={f.id} f={f} adminView onReply={reply} />)
          : <div className="empty-state">No feedback submitted yet.</div>}
      </div>
    </div>
  );
}
