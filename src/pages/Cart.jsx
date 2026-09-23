import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import client from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';
import { getCart, setCart } from './Catalog.jsx';

function money(v, cur) {
  return cur === 'USD' ? `$${v.toLocaleString(undefined, { maximumFractionDigits: 2 })}` : `Rs. ${v.toLocaleString()}`;
}

export default function Cart() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [products, setProducts] = useState([]);
  const [cart, setCartState] = useState(getCart());
  const [currency, setCurrency] = useState(user.country === 'Sri Lanka' ? 'LKR' : 'USD');
  const [couponInput, setCouponInput] = useState('');
  const [coupon, setCoupon] = useState(null); // { code, description, discountAmount }
  const [couponError, setCouponError] = useState('');
  const [checkingCoupon, setCheckingCoupon] = useState(false);

  useEffect(() => { client.get('/products').then((r) => setProducts(r.data)); }, []);

  const lines = cart.map((c) => {
    const product = products.find((p) => p.id === c.productId);
    if (!product) return null;
    const priceEach = currency === 'USD' ? product.priceUsd : product.priceLkr;
    return { ...c, product, priceEach };
  }).filter(Boolean);

  const subtotal = lines.reduce((a, l) => a + l.priceEach * l.qty, 0);
  const destinationType = currency === 'USD' ? 'international' : 'local';
  const shipping = destinationType === 'international' ? (currency === 'USD' ? 85 : 25500) : (currency === 'USD' ? 0 : 1200);
  const discount = coupon?.discountAmount || 0;
  const total = subtotal - discount + shipping;

  const remove = (productId) => {
    const next = cart.filter((c) => c.productId !== productId);
    setCart(next);
    setCartState(next);
  };

  const changeCurrency = (v) => {
    setCurrency(v);
    setCoupon(null); // discount amounts can differ by currency — re-check required
    setCouponError('');
  };

  const applyCoupon = async () => {
    if (!couponInput.trim()) return;
    setCheckingCoupon(true);
    setCouponError('');
    try {
      const { data } = await client.post('/coupons/validate', { code: couponInput.trim(), subtotal, currency });
      setCoupon(data);
    } catch (err) {
      setCoupon(null);
      setCouponError(err.response?.data?.message || 'Invalid coupon code.');
    } finally {
      setCheckingCoupon(false);
    }
  };

  const removeCoupon = () => { setCoupon(null); setCouponInput(''); setCouponError(''); };

  const proceed = () => {
    sessionStorage.setItem('cr_checkout_currency', currency);
    sessionStorage.setItem('cr_checkout_coupon', coupon ? coupon.code : '');
    navigate('/payment');
  };

  return (
    <div>
      <div className="topbar"><div><h1>Cart</h1><div className="desc">Review quantities before checkout.</div></div></div>
      <div className="grid" style={{ gridTemplateColumns: '1.6fr 1fr', alignItems: 'start' }}>
        <div className="card">
          {lines.length ? lines.map((l) => (
            <div className="cart-item" key={l.productId}>
              <div>
                <strong>{l.product.name}</strong>
                <div style={{ fontSize: 12, color: '#8a7256' }}>{l.qty}kg × {money(l.priceEach, currency)}</div>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                <span className="mono">{money(l.priceEach * l.qty, currency)}</span>
                <button className="btn btn-ghost btn-sm" onClick={() => remove(l.productId)}>✕</button>
              </div>
            </div>
          )) : <div className="empty-state">Your cart is empty — visit the catalog to add cinnamon grades.</div>}
        </div>
        <div className="card">
          <h4 style={{ fontSize: 15, marginBottom: 12 }}>Order summary</h4>
          <div className="field">
            <label>Currency / destination</label>
            <select value={currency} onChange={(e) => changeCurrency(e.target.value)}>
              <option value="USD">USD — International export</option>
              <option value="LKR">LKR — Local (Sri Lanka)</option>
            </select>
          </div>

          <div className="field">
            <label>Coupon code</label>
            {coupon ? (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '9px 12px', background: '#F0F7F5', borderRadius: 9, border: '1px solid var(--jade)' }}>
                <div>
                  <div className="mono" style={{ fontSize: 12.5, fontWeight: 700, color: 'var(--jade-dark)' }}>{coupon.code}</div>
                  {coupon.description && <div style={{ fontSize: 11, color: '#6b5947' }}>{coupon.description}</div>}
                </div>
                <button className="btn btn-ghost btn-sm" onClick={removeCoupon}>Remove</button>
              </div>
            ) : (
              <div style={{ display: 'flex', gap: 8 }}>
                <input value={couponInput} onChange={(e) => setCouponInput(e.target.value)} placeholder="e.g. WELCOME10" style={{ textTransform: 'uppercase' }} />
                <button className="btn btn-jade btn-sm" disabled={checkingCoupon} onClick={applyCoupon}>{checkingCoupon ? 'Checking…' : 'Apply'}</button>
              </div>
            )}
            {couponError && <div className="field-error" style={{ display: 'block' }}>{couponError}</div>}
          </div>

          <div className="summary-row"><span>Subtotal</span><span className="mono">{money(subtotal, currency)}</span></div>
          {discount > 0 && <div className="summary-row" style={{ color: 'var(--jade-dark)' }}><span>Discount ({coupon.code})</span><span className="mono">−{money(discount, currency)}</span></div>}
          <div className="summary-row"><span>Shipping</span><span className="mono">{shipping ? money(shipping, currency) : 'Free'}</span></div>
          <div className="summary-row total"><span>Total</span><span className="mono">{money(total, currency)}</span></div>
          <button className="btn btn-primary" style={{ width: '100%', justifyContent: 'center', marginTop: 14 }} disabled={!lines.length} onClick={proceed}>
            Proceed to payment
          </button>
        </div>
      </div>
    </div>
  );
}
