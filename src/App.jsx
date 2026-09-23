import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import Sidebar from './components/Sidebar.jsx';
import Toast from './components/Toast.jsx';

import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Catalog from './pages/Catalog.jsx';
import Cart from './pages/Cart.jsx';
import Payment from './pages/Payment.jsx';
import Orders from './pages/Orders.jsx';
import Logistics from './pages/Logistics.jsx';
import ProductsAdmin from './pages/ProductsAdmin.jsx';
import UsersAdmin from './pages/UsersAdmin.jsx';
import Feedback from './pages/Feedback.jsx';
import Reports from './pages/Reports.jsx';
import CouponsAdmin from './pages/CouponsAdmin.jsx';
import Refunds from './pages/Refunds.jsx';
import Chat from './pages/Chat.jsx';

export default function App() {
  const { user, toast } = useAuth();

  if (!user) {
    return (
      <>
        <Routes>
          <Route path="/register" element={<Register />} />
          <Route path="*" element={<Login />} />
        </Routes>
        {toast && <Toast {...toast} />}
      </>
    );
  }

  return (
    <div id="app-shell">
      <Sidebar />
      <div className="main">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route
            path="/catalog"
            element={<ProtectedRoute roles={['BUYER']}><Catalog /></ProtectedRoute>}
          />
          <Route
            path="/cart"
            element={<ProtectedRoute roles={['BUYER']}><Cart /></ProtectedRoute>}
          />
          <Route
            path="/payment"
            element={<ProtectedRoute roles={['BUYER']}><Payment /></ProtectedRoute>}
          />
          <Route path="/orders" element={<Orders />} />
          <Route
            path="/logistics"
            element={<ProtectedRoute roles={['STAFF', 'ADMIN']}><Logistics /></ProtectedRoute>}
          />
          <Route
            path="/products"
            element={<ProtectedRoute roles={['ADMIN']}><ProductsAdmin /></ProtectedRoute>}
          />
          <Route
            path="/users"
            element={<ProtectedRoute roles={['ADMIN']}><UsersAdmin /></ProtectedRoute>}
          />
          <Route
            path="/feedback"
            element={<ProtectedRoute roles={['BUYER', 'ADMIN']}><Feedback /></ProtectedRoute>}
          />
          <Route
            path="/reports"
            element={<ProtectedRoute roles={['ADMIN']}><Reports /></ProtectedRoute>}
          />
          <Route
            path="/coupons"
            element={<ProtectedRoute roles={['ADMIN']}><CouponsAdmin /></ProtectedRoute>}
          />
          <Route
            path="/refunds"
            element={<ProtectedRoute roles={['STAFF', 'ADMIN']}><Refunds /></ProtectedRoute>}
          />
          <Route path="/chat" element={<Chat />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </div>
      {toast && <Toast {...toast} />}
    </div>
  );
}
