import React, { createContext, useContext, useState, useCallback } from 'react';
import client from '../api/client.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('cr_user');
    return raw ? JSON.parse(raw) : null;
  });
  const [toast, setToast] = useState(null);

  const showToast = useCallback((msg, kind = 'ok') => {
    setToast({ msg, kind });
    setTimeout(() => setToast(null), 3200);
  }, []);

  const persist = (token, u) => {
    localStorage.setItem('cr_token', token);
    localStorage.setItem('cr_user', JSON.stringify(u));
    setUser(u);
  };

  const login = async (email, password) => {
    const { data } = await client.post('/auth/login', { email, password });
    persist(data.token, data.user);
    return data.user;
  };

  const register = async (payload) => {
    const { data } = await client.post('/auth/register', payload);
    persist(data.token, data.user);
    return data.user;
  };

  const logout = () => {
    localStorage.removeItem('cr_token');
    localStorage.removeItem('cr_user');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, setUser, login, register, logout, toast, showToast }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
