import React, { useEffect, useState } from 'react';
import client from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';

const BLANK = {
  code: '', description: '', discountType: 'PERCENTAGE',
  percentage: '', fixedUsd: '', fixedLkr: '',
  minOrderUsd: '', minOrderLkr: '', maxUses: '', expiresAt: '',
};

function summarize(c) {
  return c.discountType === 'PERCENTAGE'
    ? `${c.percentage}% off`
    : [c.fixedUsd ? `$${c.fixedUsd} off` : null, c.fixedLkr ? `Rs.${c.fixedLkr} off` : null].filter(Boolean).join(' / ');
}

export default function CouponsAdmin() {
  const { showToast } = useAuth();
  const [coupons, setCoupons] = useState([]);
  const [modalId, setModalId] = useState(null); // null closed, 'new' create, id edit
  const [form, setForm] = useState(BLANK);
  const [error, setError] = useState('');

  const load = () => client.get('/coupons').then((r) => setCoupons(r.data));
  useEffect(() => { load(); }, []);

  const openNew = () => { setForm(BLANK); setError(''); setModalId('new'); };
  const openEdit = (c) => {
    setForm({
      code: c.code, description: c.description || '', discountType: c.discountType,
      percentage: c.percentage ?? '', fixedUsd: c.fixedUsd ?? '', fixedLkr: c.fixedLkr ?? '',
      minOrderUsd: c.minOrderUsd ?? '', minOrderLkr: c.minOrderLkr ?? '', maxUses: c.maxUses ?? '',
      expiresAt: c.expiresAt ? c.expiresAt.slice(0, 16) : '',
    });
    setError('');
    setModalId(c.id);
  };
  const close = () => setModalId(null);

  const save = async () => {
    if (!form.code.trim()) { setError('Coupon code is required.'); return; }
    const payload = {
      code: form.code.trim().toUpperCase(),
      description: form.description,
      discountType: form.discountType,
      percentage: form.percentage === '' ? null : parseFloat(form.percentage),
      fixedUsd: form.fixedUsd === '' ? null : parseFloat(form.fixedUsd),
      fixedLkr: form.fixedLkr === '' ? null : parseFloat(form.fixedLkr),
      minOrderUsd: form.minOrderUsd === '' ? null : parseFloat(form.minOrderUsd),
      minOrderLkr: form.minOrderLkr === '' ? null : parseFloat(form.minOrderLkr),
      maxUses: form.maxUses === '' ? null : parseInt(form.maxUses, 10),
      expiresAt: form.expiresAt === '' ? null : form.expiresAt,
    };
    try {
      if (modalId === 'new') await client.post('/coupons', payload);
      else await client.put(`/coupons/${modalId}`, payload);
      showToast('Coupon saved.', 'ok');
      close();
      load();
    } catch (err) {
      setError(err.response?.data?.message || 'Could not save coupon.');
    }
  };

  const toggleActive = async (id) => {
    try { await client.put(`/coupons/${id}/toggle-active`); load(); }
    catch (err) { showToast(err.response?.data?.message || 'Could not update coupon.', 'err'); }
  };

  const remove = async (id) => {
    try { await client.delete(`/coupons/${id}`); showToast('Coupon deleted.', 'err'); load(); }
    catch (err) { showToast(err.response?.data?.message || 'Could not delete coupon.', 'err'); }
  };

  return (
    <div>
      <div className="topbar">
        <div><h1>Coupons</h1><div className="desc">Discount codes for buyer checkout.</div></div>
        <button className="btn btn-primary btn-sm" onClick={openNew}>+ New coupon</button>
      </div>

      <div className="card">
        <div className="table-wrap">
          <table>
            <thead><tr><th>Code</th><th>Discount</th><th>Min. order</th><th>Uses</th><th>Expires</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {coupons.map((c) => {
                const expired = c.expiresAt && new Date(c.expiresAt) < new Date();
                const exhausted = c.maxUses != null && c.usedCount >= c.maxUses;
                return (
                  <tr key={c.id}>
                    <td><strong className="mono">{c.code}</strong>{c.description && <div style={{ fontSize: 11, color: '#8a7256', marginTop: 2 }}>{c.description}</div>}</td>
                    <td className="mono">{summarize(c)}</td>
                    <td className="mono">
                      {c.minOrderUsd || c.minOrderLkr
                        ? [c.minOrderUsd ? `$${c.minOrderUsd}` : null, c.minOrderLkr ? `Rs.${c.minOrderLkr}` : null].filter(Boolean).join(' / ')
                        : '—'}
                    </td>
                    <td className="mono">{c.usedCount}{c.maxUses != null ? ` / ${c.maxUses}` : ''}</td>
                    <td className="mono">{c.expiresAt ? new Date(c.expiresAt).toLocaleDateString() : 'Never'}</td>
                    <td>
                      {!c.active ? <span className="pill" style={{ background: '#F6DEDE', color: 'var(--danger)' }}>Disabled</span>
                        : expired ? <span className="pill" style={{ background: '#F6E9DC', color: '#8a5320' }}>Expired</span>
                        : exhausted ? <span className="pill" style={{ background: '#F6E9DC', color: '#8a5320' }}>Exhausted</span>
                        : <span className="pill" style={{ background: '#DFF0E4', color: '#237a45' }}>Active</span>}
                    </td>
                    <td style={{ display: 'flex', gap: 6 }}>
                      <button className="btn btn-ghost btn-sm" onClick={() => openEdit(c)}>Edit</button>
                      <button className="btn btn-ghost btn-sm" onClick={() => toggleActive(c.id)}>{c.active ? 'Disable' : 'Enable'}</button>
                      <button className="btn btn-danger btn-sm" onClick={() => remove(c.id)}>Delete</button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        {!coupons.length && <div className="empty-state">No coupons yet — create your first discount code.</div>}
      </div>

      {modalId !== null && (
        <div className="modal-bg" onClick={(e) => e.target === e.currentTarget && close()}>
          <div className="modal" style={{ maxWidth: 480 }}>
            <h3>{modalId === 'new' ? 'New coupon' : 'Edit coupon'}</h3>
            <div className="grid grid-2">
              <div className="field"><label>Code</label><input value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} placeholder="WELCOME10" style={{ textTransform: 'uppercase' }} /></div>
              <div className="field"><label>Type</label>
                <select value={form.discountType} onChange={(e) => setForm({ ...form, discountType: e.target.value })}>
                  <option value="PERCENTAGE">Percentage</option>
                  <option value="FIXED">Fixed amount</option>
                </select>
              </div>
            </div>
            <div className="field"><label>Description</label><input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} placeholder="e.g. First order discount" /></div>

            {form.discountType === 'PERCENTAGE' ? (
              <div className="field"><label>Percentage off</label><input type="number" min="1" max="100" value={form.percentage} onChange={(e) => setForm({ ...form, percentage: e.target.value })} placeholder="10" /></div>
            ) : (
              <div className="grid grid-2">
                <div className="field"><label>Fixed off (USD)</label><input type="number" value={form.fixedUsd} onChange={(e) => setForm({ ...form, fixedUsd: e.target.value })} placeholder="5" /></div>
                <div className="field"><label>Fixed off (LKR)</label><input type="number" value={form.fixedLkr} onChange={(e) => setForm({ ...form, fixedLkr: e.target.value })} placeholder="1500" /></div>
              </div>
            )}

            <div className="grid grid-2">
              <div className="field"><label>Min. order (USD)</label><input type="number" value={form.minOrderUsd} onChange={(e) => setForm({ ...form, minOrderUsd: e.target.value })} placeholder="Optional" /></div>
              <div className="field"><label>Min. order (LKR)</label><input type="number" value={form.minOrderLkr} onChange={(e) => setForm({ ...form, minOrderLkr: e.target.value })} placeholder="Optional" /></div>
            </div>
            <div className="grid grid-2">
              <div className="field"><label>Max uses</label><input type="number" value={form.maxUses} onChange={(e) => setForm({ ...form, maxUses: e.target.value })} placeholder="Unlimited" /></div>
              <div className="field"><label>Expires</label><input type="datetime-local" value={form.expiresAt} onChange={(e) => setForm({ ...form, expiresAt: e.target.value })} /></div>
            </div>

            {error && <div className="field-error" style={{ display: 'block' }}>{error}</div>}
            <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 10 }}>
              <button className="btn btn-ghost" onClick={close}>Cancel</button>
              <button className="btn btn-primary" onClick={save}>Save</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
