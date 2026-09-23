import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Line, Doughnut, Bar, Pie } from 'react-chartjs-2';
import { Chart as ChartJS, registerables } from 'chart.js';
import client from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';
import StatCard from '../components/StatCard.jsx';
import { STAGE_LABEL } from '../components/QuillTracker.jsx';

ChartJS.register(...registerables);

function money(v, cur) {
  return cur === 'USD' ? `$${Number(v).toLocaleString(undefined, { maximumFractionDigits: 2 })}` : `Rs. ${Number(v).toLocaleString()}`;
}
function timeAgo(d) {
  const diff = Math.floor((Date.now() - new Date(d).getTime()) / 3600000);
  if (diff < 1) return 'just now';
  if (diff < 24) return `${diff}h ago`;
  return `${Math.floor(diff / 24)}d ago`;
}

export default function Dashboard() {
  const { user } = useAuth(); //gets the logged in user
  const [orders, setOrders] = useState([]);
  const [analytics, setAnalytics] = useState(null);

  useEffect(() => {
    client.get('/orders').then((r) => setOrders(r.data));
    if (user.role !== 'BUYER') {
      client.get('/analytics/dashboard').then((r) => setAnalytics(r.data));
    }
  }, [user.role]);

  const recent = [...orders].reverse().slice(0, 6);

  return (
    <div>
      <div className="topbar">
        <div>
          <h1>{user.role === 'BUYER' ? `Welcome back, ${user.name.split(' ')[0]}` : 'Operations Dashboard'}</h1>
          <div className="desc">
            {user.role === 'BUYER' ? 'Track your shipments and manage new orders.' : 'Live view across exports, payments, shipments and feedback.'}
          </div>
        </div>
      </div>

      {user.role === 'BUYER' ? (
        <div className="grid grid-3">
          <StatCard label="Active shipments" value={orders.filter((o) => !['DELIVERED', 'CANCELLED'].includes(o.status)).length} />
          <StatCard label="Delivered orders" value={orders.filter((o) => o.status === 'DELIVERED').length} />
          <StatCard label="Total orders" value={orders.length} />
        </div>
      ) : analytics && (
        <>
          <div className="grid grid-4">
            <StatCard label="Export revenue (USD)" value={`$${analytics.revenueUsdTotal.toLocaleString()}`} />
            <StatCard label="Local revenue (LKR)" value={`Rs. ${analytics.revenueLkrTotal.toLocaleString()}`} />
            <StatCard label="Active shipments" value={analytics.activeShipments} />
            <StatCard label="Avg. buyer rating" value={`${analytics.avgRating} / 5`} />
          </div>

          <div className="section-title"><h3>Revenue &amp; order flow</h3></div>
          <div className="grid grid-2">
            <div className="card">
              <div className="eyebrow" style={{ color: '#8a7256' }}>Paid revenue by month (USD, international)</div>
              <div className="chart-wrap">
                <Line
                  data={{ labels: analytics.revenueMonths, datasets: [{ label: 'USD revenue', data: analytics.revenueUsd, borderColor: '#B0562B', backgroundColor: 'rgba(176,86,43,0.15)', fill: true, tension: 0.35 }] }}
                  options={{ maintainAspectRatio: false, plugins: { legend: { display: false } } }}
                />
              </div>
            </div>
            <div className="card">
              <div className="eyebrow" style={{ color: '#8a7256' }}>Orders by status</div>
              <div className="chart-wrap">
                <Doughnut
                  data={{ labels: Object.keys(analytics.ordersByStatus).map((s) => STAGE_LABEL[s] || s), datasets: [{ data: Object.values(analytics.ordersByStatus), backgroundColor: ['#F3E7CE', '#E4EEEC', '#EAE3F5', '#E3ECF7', '#DCEFF6', '#FDEBDD', '#F6E9DC', '#DFF0E4', '#F6DEDE'] }] }}
                  options={{ maintainAspectRatio: false, plugins: { legend: { position: 'bottom', labels: { boxWidth: 10, font: { size: 10.5 } } } } }}
                />
              </div>
            </div>
          </div>
          <div className="grid grid-2" style={{ marginTop: 16 }}>
            <div className="card">
              <div className="eyebrow" style={{ color: '#8a7256' }}>Sales volume by grade (kg)</div>
              <div className="chart-wrap">
                <Bar
                  data={{ labels: Object.keys(analytics.salesByGrade), datasets: [{ label: 'kg sold', data: Object.values(analytics.salesByGrade), backgroundColor: '#2F6E62', borderRadius: 6 }] }}
                  options={{ maintainAspectRatio: false, plugins: { legend: { display: false } } }}
                />
              </div>
            </div>
            <div className="card">
              <div className="eyebrow" style={{ color: '#8a7256' }}>Local vs. international split</div>
              <div className="chart-wrap">
                <Pie
                  data={{ labels: ['International', 'Local'], datasets: [{ data: [analytics.international, analytics.local], backgroundColor: ['#B0562B', '#C9A34E'] }] }}
                  options={{ maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } }}
                />
              </div>
            </div>
          </div>
        </>
      )}

      <div className="section-title">
        <h3>{user.role === 'BUYER' ? 'Recent orders' : 'Recent activity'}</h3>
        <Link className="btn btn-ghost btn-sm" to="/orders">View all</Link>
      </div>
      <div className="card">
        {recent.length ? (
          <div className="table-wrap">
            <table>
              <thead><tr><th>Order</th><th>Destination</th><th>Items</th><th>Total</th><th>Status</th><th>Updated</th></tr></thead>
              <tbody>
                {recent.map((o) => (
                  <tr key={o.id}>
                    <td className="mono">{o.orderCode}</td>
                    <td>{o.country}</td>
                    <td>{o.items.reduce((a, i) => a + i.qty, 0)}kg</td>
                    <td className="mono">{money(o.total, o.currency)}</td>
                    <td><span className={`pill pill-${o.status.toLowerCase()}`}>{STAGE_LABEL[o.status]}</span></td>
                    <td>{timeAgo(o.history[o.history.length - 1]?.updatedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : <div className="empty-state">No orders yet.</div>}
      </div>
    </div>
  );
}
