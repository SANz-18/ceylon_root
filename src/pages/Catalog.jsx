import React, { useEffect, useState } from 'react';
import client from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';

const CART_KEY = 'cr_cart';
function getCart() { return JSON.parse(localStorage.getItem(CART_KEY) || '[]'); }
function setCart(cart) { localStorage.setItem(CART_KEY, JSON.stringify(cart)); }

export default function Catalog() {
  const { showToast } = useAuth();
  const [products, setProducts] = useState([]);
  const [qty, setQty] = useState({});
  const [cartCount, setCartCount] = useState(getCart().length);

  useEffect(() => { client.get('/products').then((r) => setProducts(r.data)); }, []);

  const stepQty = (id, delta) => {
    setQty((q) => ({ ...q, [id]: Math.max(1, (q[id] ?? 10) + delta) }));
  };

  const addToCart = (product) => {
    const cart = getCart();
    const q = qty[product.id] ?? 10;
    const existing = cart.find((c) => c.productId === product.id);
    if (existing) existing.qty += q; else cart.push({ productId: product.id, qty: q });
    setCart(cart);
    setCartCount(cart.length);
    showToast('Added to cart.', 'ok');
  };

  return (
    <div>
      <div className="topbar">
        <div><h1>Export Catalog</h1><div className="desc">Ceylon cinnamon quills, graded to export standards.</div></div>
        <div className="pill" style={{ background: 'var(--parchment-2)' }}>{cartCount} in cart</div>
      </div>
      <div className="grid grid-3">
        {products.map((p) => (
          <div className="card grade-card" key={p.id}>
            <div className="grade-stamp">{p.grade.split(' ')[0]}</div>
            <div className="quill-swatch" />
            <h4>{p.name}</h4>
            <div className="desc">{p.description}</div>
            <div className="grade-meta"><span>Stock: {p.stockKg}kg</span><span>{p.stockKg > 100 ? 'In stock' : 'Low stock'}</span></div>
            <div className="grade-price">${p.priceUsd.toFixed(2)} <span style={{ fontSize: 12, color: '#8a7256' }}>/ kg</span></div>
            <div style={{ fontSize: 11.5, color: '#8a7256', fontFamily: 'var(--font-mono)' }}>or Rs. {p.priceLkr.toLocaleString()} / kg (local)</div>
            <div className="qty-row">
              <button onClick={() => stepQty(p.id, -10)}>−</button>
              <input type="number" value={qty[p.id] ?? 10} onChange={(e) => setQty((q) => ({ ...q, [p.id]: Math.max(1, parseInt(e.target.value) || 1) }))} />
              <button onClick={() => stepQty(p.id, 10)}>+</button>
              <button className="btn btn-primary btn-sm" style={{ marginLeft: 'auto' }} onClick={() => addToCart(p)}>Add</button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export { getCart, setCart, CART_KEY };
