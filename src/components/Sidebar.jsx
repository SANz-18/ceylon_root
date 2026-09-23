import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import NotificationBell from './NotificationBell.jsx';

const ROLE_LABEL = { ADMIN: 'Administrator', STAFF: 'Logistics Staff', BUYER: 'Buyer / Importer' };

const NAV_ITEMS = [
  { to: '/', label: 'Dashboard', roles: ['ADMIN', 'STAFF', 'BUYER'] },
  { to: '/catalog', label: 'Catalog', roles: ['BUYER'] },
  { to: '/cart', label: 'Cart & Checkout', roles: ['BUYER'] },
  { to: '/orders', label: 'Orders', roles: ['ADMIN', 'STAFF', 'BUYER'] },
  { to: '/chat', label: 'Live Chat', roles: ['ADMIN', 'STAFF', 'BUYER'] },
  { to: '/logistics', label: 'Logistics Queue', roles: ['STAFF', 'ADMIN'] },
  { to: '/products', label: 'Manage Products', roles: ['ADMIN'] },
  { to: '/coupons', label: 'Coupons', roles: ['ADMIN'] },
  { to: '/refunds', label: 'Refunds', roles: ['STAFF', 'ADMIN'] },
  { to: '/users', label: 'Users', roles: ['ADMIN'] },
  { to: '/feedback', label: 'Feedback', roles: ['ADMIN', 'BUYER'] },
  { to: '/reports', label: 'Reports', roles: ['ADMIN'] },
];

export default function Sidebar() {
  const { user, logout } = useAuth();
  const items = NAV_ITEMS.filter((i) => i.roles.includes(user.role));

  return (
    <div className="sidebar">
      <div className="brandmark"><span>Ceylon Roots</span></div>
      <div className="sidebar-role">
        <div className="name">{user.name}</div>
        <div className="role">{ROLE_LABEL[user.role]}</div>
      </div>

      <NotificationBell />

      <div className="nav-group">
        {items.map((i) => (
          <NavLink key={i.to} to={i.to} end={i.to === '/'} className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            {i.label}
          </NavLink>
        ))}
      </div>
      <div className="sidebar-foot">
        <button className="nav-item" onClick={logout}>Sign out</button>
      </div>
    </div>
  );
}
