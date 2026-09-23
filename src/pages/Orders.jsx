import React, { useEffect, useState } from 'react';
import client from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';
import QuillTracker, { STAGE_LABEL } from '../components/QuillTracker.jsx';

function money(v, cur) {
  return cur === 'USD' ? `$${v.toLocaleString(undefined, { maximumFractionDigits: 2 })}` : `Rs. ${v.toLocaleString()}`;
}
function timeAgo(d) {
  const diff = Math.floor((Date.now() - new Date(d).getTime()) / 3600000);
  if (diff < 1) return 'just now';
  if (diff < 24) return `${diff}h ago`;
  return `${Math.floor(diff / 24)}d ago`;
}

const STAGES = ['CONFIRMED', 'PROCESSING', 'PACKED', 'SHIPPED', 'IN_TRANSIT', 'CUSTOMS', 'DELIVERED'];
const REFUND_PILL = {
  PENDING: { bg: '#F3E7CE', color: '#8a5c17', label: 'Refund: Pending' },
  APPROVED: { bg: '#DFF0E4', color: '#237a45', label: 'Refund: Approved' },
  REJECTED: { bg: '#F6DEDE', color: 'var(--danger)', label: 'Refund: Rejected' },
};
const MAX_IMAGES = 3;

export default function Orders() {
  const { user, showToast } = useAuth();
  const [orders, setOrders] = useState([]);
  const [feedbackOrderIds, setFeedbackOrderIds] = useState(new Set());
  const [refundsByOrder, setRefundsByOrder] = useState({}); // orderId -> refundRequest
  const [modalOrder, setModalOrder] = useState(null);
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [confirmAction, setConfirmAction] = useState(null); // { type: 'advance' | 'cancel', order, nextLabel }
  const [confirmBusy, setConfirmBusy] = useState(false);

  const [refundOrder, setRefundOrder] = useState(null);
  const [refundReason, setRefundReason] = useState('');
  const [refundFiles, setRefundFiles] = useState([]);
  const [refundError, setRefundError] = useState('');
  const [refundSubmitting, setRefundSubmitting] = useState(false);

  const load = () => {
    client.get('/orders').then((r) => setOrders(r.data));
    client.get('/refunds').then((r) => {
      const map = {};
      r.data.forEach((rq) => { map[rq.order.id] = rq; });
      setRefundsByOrder(map);
    });
    if (user.role === 'BUYER') {
      client.get('/feedback').then((r) => setFeedbackOrderIds(new Set(r.data.map((f) => f.order.id))));
    }
  };
  useEffect(load, [user.role]);

  const advance = async (id) => {
    try {
      await client.post(`/orders/${id}/advance`);
      showToast('Order updated.', 'ok');
      load();
    } catch (err) { showToast(err.response?.data?.message || 'Could not update order.', 'err'); }
  };
  const cancel = async (id) => {
    try {
      await client.post(`/orders/${id}/cancel`);
      showToast('Order cancelled.', 'err');
      load();
    } catch (err) { showToast(err.response?.data?.message || 'Could not cancel order.', 'err'); }
  };

  const runConfirmedAction = async () => {
    if (!confirmAction) return;
    setConfirmBusy(true);
    try {
      if (confirmAction.type === 'advance') await advance(confirmAction.order.id);
      else await cancel(confirmAction.order.id);
    } finally {
      setConfirmBusy(false);
      setConfirmAction(null);
    }
  };

  const submitFeedback = async () => {
    if (!comment.trim()) { showToast('Please add a short comment.', 'err'); return; }
    try {
      await client.post('/feedback', { orderId: modalOrder.id, rating, comment });
      showToast('Thanks for your feedback!', 'ok');
      setModalOrder(null); setComment(''); setRating(5);
      load();
    } catch (err) { showToast(err.response?.data?.message || 'Could not submit feedback.', 'err'); }
  };

  const openRefund = (o) => {
    setRefundOrder(o); setRefundReason(''); setRefundFiles([]); setRefundError('');
  };
  const addRefundFiles = (fileList) => {
    const incoming = Array.from(fileList);
    const combined = [...refundFiles, ...incoming].slice(0, MAX_IMAGES);
    setRefundFiles(combined);
  };
  const removeRefundFile = (idx) => setRefundFiles(refundFiles.filter((_, i) => i !== idx));

  const submitRefund = async () => {
    if (!refundReason.trim()) { setRefundError('Please describe the reason for the refund.'); return; }
    setRefundSubmitting(true);
    setRefundError('');
    try {
      const form = new FormData();
      form.append('orderId', refundOrder.id);
      form.append('reason', refundReason.trim());
      refundFiles.forEach((f) => form.append('images', f));
      await client.post('/refunds', form);
      showToast('Refund request submitted.', 'ok');
      setRefundOrder(null);
      load();
    } catch (err) {
      setRefundError(err.response?.data?.message || 'Could not submit refund request.');
    } finally {
      setRefundSubmitting(false);
    }
  };

  const [invoiceLoadingId, setInvoiceLoadingId] = useState(null);
  const downloadInvoice = async (order) => {
    setInvoiceLoadingId(order.id);
    try {
      const res = await client.get(`/invoices/${order.id}`, { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.download = `invoice-${order.orderCode}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      showToast(err.response?.data?.message || 'Could not generate invoice.', 'err');
    } finally {
      setInvoiceLoadingId(null);
    }
  };

  const sorted = [...orders].reverse();
  const canManage = user.role === 'STAFF' || user.role === 'ADMIN';

  return (
    <div>
      <div className="topbar"><div><h1>{user.role === 'BUYER' ? 'My Orders' : 'All Orders'}</h1><div className="desc">{sorted.length} order{sorted.length !== 1 ? 's' : ''}</div></div></div>

      {sorted.length ? sorted.map((o) => {
        const idx = STAGES.indexOf(o.status);
        const next = idx >= 0 && idx < STAGES.length - 1 ? STAGES[idx + 1] : null;
        const hasFeedback = feedbackOrderIds.has(o.id);
        const refund = refundsByOrder[o.id];
        return (
          <div className="card" style={{ marginBottom: 16 }} key={o.id}>
            <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 10 }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
                  <strong className="mono">{o.orderCode}</strong>
                  <span className={`pill pill-${o.status.toLowerCase()}`}>{STAGE_LABEL[o.status]}</span>
                  {refund && (
                    <span className="pill" style={{ background: REFUND_PILL[refund.status].bg, color: REFUND_PILL[refund.status].color }}>
                      {REFUND_PILL[refund.status].label}
                    </span>
                  )}
                </div>
                <div style={{ fontSize: 12.5, color: '#8a7256', marginTop: 4 }}>{o.buyer.name} · {o.country} · Tracking: <span className="mono">{o.trackingCode}</span></div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div className="mono" style={{ fontSize: 16, fontWeight: 700 }}>{money(o.total, o.currency)}</div>
                <div style={{ fontSize: 11.5, color: '#8a7256' }}>{o.paymentMethod} · {o.paymentStatus}</div>
              </div>
            </div>
            <div style={{ fontSize: 12.5, color: '#6b5947', marginTop: 10 }}>{o.items.map((i) => `${i.grade} × ${i.qty}kg`).join(' · ')}</div>
            <QuillTracker status={o.status} />

            {refund && refund.staffNote && (
              <div style={{ fontSize: 12, color: '#6b5947', marginTop: 10, padding: '9px 12px', background: 'var(--parchment)', borderRadius: 8 }}>
                <strong>Refund note:</strong> {refund.staffNote}
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 14, flexWrap: 'wrap', gap: 8 }}>
              <div style={{ fontSize: 11.5, color: '#8a7256' }}>
                Last update: {timeAgo(o.history[o.history.length - 1]?.updatedAt)} — {o.history[o.history.length - 1]?.note}
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <button className="btn btn-ghost btn-sm" disabled={invoiceLoadingId === o.id} onClick={() => downloadInvoice(o)}>
                  {invoiceLoadingId === o.id ? 'Generating…' : 'Download invoice'}
                </button>
                {canManage && o.status !== 'CANCELLED' && next && (
                  <button className="btn btn-jade btn-sm" onClick={() => setConfirmAction({ type: 'advance', order: o, nextLabel: STAGE_LABEL[next] })}>
                    Mark as {STAGE_LABEL[next]}
                  </button>
                )}
                {canManage && o.status !== 'CANCELLED' && o.status !== 'DELIVERED' && (
                  <button className="btn btn-danger btn-sm" onClick={() => setConfirmAction({ type: 'cancel', order: o })}>Cancel</button>
                )}
                {user.role === 'BUYER' && o.status === 'DELIVERED' && !hasFeedback && <button className="btn btn-primary btn-sm" onClick={() => setModalOrder(o)}>Leave feedback</button>}
                {user.role === 'BUYER' && o.status === 'DELIVERED' && !refund && <button className="btn btn-ghost btn-sm" onClick={() => openRefund(o)}>Request refund</button>}
              </div>
            </div>
          </div>
        );
      }) : <div className="card"><div className="empty-state">No orders to show yet.</div></div>}

      {confirmAction && (
        <div className="modal-bg" onClick={(e) => e.target === e.currentTarget && !confirmBusy && setConfirmAction(null)}>
          <div className="modal" style={{ maxWidth: 400 }}>
            <h3>{confirmAction.type === 'advance' ? 'Advance this order?' : 'Cancel this order?'}</h3>
            <p style={{ fontSize: 13.5, color: '#6b5947', lineHeight: 1.55, marginTop: 10 }}>
              {confirmAction.type === 'advance'
                ? <>Order <strong className="mono">{confirmAction.order.orderCode}</strong> will move to <strong>{confirmAction.nextLabel}</strong>. This will be visible to the buyer immediately and can't be undone.</>
                : <>Order <strong className="mono">{confirmAction.order.orderCode}</strong> will be cancelled. This can't be undone.</>}
            </p>
            <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 18 }}>
              <button className="btn btn-ghost" disabled={confirmBusy} onClick={() => setConfirmAction(null)}>Go back</button>
              <button
                className={confirmAction.type === 'advance' ? 'btn btn-jade' : 'btn btn-danger'}
                style={confirmAction.type === 'cancel' ? { background: 'var(--danger)', color: '#fff', border: 'none' } : {}}
                disabled={confirmBusy}
                onClick={runConfirmedAction}
              >
                {confirmBusy ? 'Please wait…' : confirmAction.type === 'advance' ? `Yes, mark as ${confirmAction.nextLabel}` : 'Yes, cancel order'}
              </button>
            </div>
          </div>
        </div>
      )}

      {modalOrder && (
        <div className="modal-bg" onClick={(e) => e.target === e.currentTarget && setModalOrder(null)}>
          <div className="modal">
            <h3>Leave feedback</h3>
            <div className="sub" style={{ color: '#6b5947', fontSize: 13, marginBottom: 14 }}>Order {modalOrder.orderCode}</div>
            <div className="field">
              <label>Rating</label>
              <div className="stars" style={{ gap: 8 }}>
                {[1, 2, 3, 4, 5].map((n) => (
                  <span key={n} onClick={() => setRating(n)} style={{ cursor: 'pointer', fontSize: 20, color: n <= rating ? 'var(--gold)' : 'var(--parchment-3)' }}>★</span>
                ))}
              </div>
            </div>
            <div className="field"><label>Comment</label><textarea rows={3} value={comment} onChange={(e) => setComment(e.target.value)} placeholder="How was the quality, packaging and delivery?" /></div>
            <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
              <button className="btn btn-ghost" onClick={() => setModalOrder(null)}>Cancel</button>
              <button className="btn btn-primary" onClick={submitFeedback}>Submit</button>
            </div>
          </div>
        </div>
      )}

      {refundOrder && (
        <div className="modal-bg" onClick={(e) => e.target === e.currentTarget && !refundSubmitting && setRefundOrder(null)}>
          <div className="modal" style={{ maxWidth: 480 }}>
            <h3>Request Refund</h3>
            <div className="sub" style={{ color: '#6b5947', fontSize: 13, marginBottom: 14 }}>Order {refundOrder.orderCode}</div>

            <div className="field">
              <label>Reason</label>
              <textarea rows={3} value={refundReason} onChange={(e) => setRefundReason(e.target.value)} placeholder="What went wrong with this order?" />
            </div>

            <div className="field">
              <label>Upload images {refundFiles.length > 0 ? `(${refundFiles.length}/${MAX_IMAGES})` : `(optional, up to ${MAX_IMAGES})`}</label>
              {refundFiles.length < MAX_IMAGES && (
                <input type="file" accept="image/png,image/jpeg,image/webp" multiple
                  onChange={(e) => { addRefundFiles(e.target.files); e.target.value = ''; }} />
              )}
              {refundFiles.length > 0 && (
                <div style={{ display: 'flex', gap: 8, marginTop: 10, flexWrap: 'wrap' }}>
                  {refundFiles.map((f, i) => (
                    <div key={i} style={{ position: 'relative' }}>
                      <img src={URL.createObjectURL(f)} alt={f.name}
                        style={{ width: 64, height: 64, objectFit: 'cover', borderRadius: 8, border: '1px solid var(--line)' }} />
                      <button
                        onClick={() => removeRefundFile(i)}
                        style={{ position: 'absolute', top: -6, right: -6, width: 18, height: 18, borderRadius: '50%', border: 'none', background: 'var(--danger)', color: '#fff', fontSize: 11, lineHeight: '18px', cursor: 'pointer' }}
                      >✕</button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {refundError && <div className="field-error" style={{ display: 'block' }}>{refundError}</div>}

            <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 10 }}>
              <button className="btn btn-ghost" disabled={refundSubmitting} onClick={() => setRefundOrder(null)}>Cancel</button>
              <button className="btn btn-primary" disabled={refundSubmitting} onClick={submitRefund}>
                {refundSubmitting ? 'Submitting…' : 'Submit request'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
