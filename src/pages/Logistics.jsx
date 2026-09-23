import React, { useEffect, useState } from 'react';
import client from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';
import { STAGE_LABEL } from '../components/QuillTracker.jsx';

function timeAgo(d) {
  const diff = Math.floor((Date.now() - new Date(d).getTime()) / 3600000);
  if (diff < 1) return 'just now';
  if (diff < 24) return `${diff}h ago`;
  return `${Math.floor(diff / 24)}d ago`;
}

export default function Logistics() {
  const { showToast } = useAuth();
  const [orders, setOrders] = useState([]);

  const load = () => client.get('/orders/logistics').then((r) => setOrders(r.data));
  useEffect(() => { load(); }, []);

  const advance = async (id) => {
    try {
      await client.post(`/orders/${id}/advance`);
      showToast('Order advanced to next stage.', 'ok');
      load();
    } catch (err) { showToast(err.response?.data?.message || 'Could not update order.', 'err'); }
  };

  return (
    <div>
      <div className="topbar"><div><h1>Logistics Queue</h1><div className="desc">{orders.length} shipment{orders.length !== 1 ? 's' : ''} in motion — oldest stage first.</div></div></div>
      <div className="card">
        {orders.length ? (
          <div className="table-wrap">
            <table>
              <thead><tr><th>Order</th><th>Destination</th><th>Stage</th><th>Weight</th><th>Since</th><th></th></tr></thead>
              <tbody>
                {orders.map((o) => (
                  <tr key={o.id}>
                    <td className="mono">{o.orderCode}</td>
                    <td>{o.destinationType === 'INTERNATIONAL' ? '🌍' : '🏦'} {o.country}</td>
                    <td><span className={`pill pill-${o.status.toLowerCase()}`}>{STAGE_LABEL[o.status]}</span></td>
                    <td>{o.items.reduce((a, i) => a + i.qty, 0)}kg</td>
                    <td>{timeAgo(o.history[o.history.length - 1]?.updatedAt)}</td>
                    <td><button className="btn btn-jade btn-sm" onClick={() => advance(o.id)}>Advance →</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : <div className="empty-state">Queue is empty — nothing currently in transit.</div>}
      </div>
    </div>
  );
}
