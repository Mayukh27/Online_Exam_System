import React, { createContext, useContext, useState, useCallback } from 'react';
const AuthContext = createContext(null);
export function AuthProvider({ children }) {
  const stored = localStorage.getItem('examportal_user');
  const [user, setUser] = useState(stored ? JSON.parse(stored) : null);
  const login = useCallback((u) => { localStorage.setItem('examportal_user', JSON.stringify(u)); setUser(u); }, []);
  const logout = useCallback(() => { localStorage.removeItem('examportal_user'); setUser(null); }, []);
  const isRole = useCallback((r) => user?.role === r, [user]);
  return <AuthContext.Provider value={{ user, login, logout, isRole }}>{children}</AuthContext.Provider>;
}
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be inside AuthProvider');
  return ctx;
}
