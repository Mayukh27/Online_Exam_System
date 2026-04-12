import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { login } from '../api/authApi';
import Spinner from '../components/Spinner';

export default function LoginPage() {
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login: authLogin } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(''); setLoading(true);
    try {
      const res = await login(form);
      const user = res.data.data;
      authLogin(user);
      if (user.role === 'ADMIN') navigate('/admin');
      else if (user.role === 'TEACHER') navigate('/teacher');
      else navigate('/student');
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed. Please check your credentials.');
    } finally { setLoading(false); }
  };

  return (
    <div style={{ minHeight:'100vh', background:'var(--bg)', display:'flex', alignItems:'center', justifyContent:'center', padding:20 }}>
      <div style={{ width:'100%', maxWidth:420 }}>
        <div style={{ textAlign:'center', marginBottom:32 }}>
          <div style={{ display:'inline-flex', alignItems:'center', justifyContent:'center', width:52, height:52, background:'var(--primary)', borderRadius:12, marginBottom:14 }}>
            <span style={{ fontSize:26, color:'#fff' }}>&#x1F4DD;</span>
          </div>
          <h1 style={{ fontSize:24, fontWeight:700, color:'var(--text-primary)' }}>ExamPortal</h1>
          <p style={{ color:'var(--text-muted)', marginTop:4 }}>Sign in to your account</p>
        </div>

        <div style={{ background:'#fff', borderRadius:12, boxShadow:'var(--shadow-md)', padding:32 }}>
          <form onSubmit={handleSubmit}>
            <div style={{ marginBottom:18 }}>
              <label style={{ display:'block', fontSize:13, fontWeight:600, color:'var(--text-secondary)', marginBottom:6 }}>Email address</label>
              <input type="email" value={form.email} onChange={e => setForm(f=>({...f,email:e.target.value}))}
                required placeholder="you@example.com"
                style={{ width:'100%', padding:'10px 12px', border:'1px solid var(--border)', borderRadius:6, fontSize:14, outline:'none' }} />
            </div>
            <div style={{ marginBottom:22 }}>
              <label style={{ display:'block', fontSize:13, fontWeight:600, color:'var(--text-secondary)', marginBottom:6 }}>Password</label>
              <input type="password" value={form.password} onChange={e => setForm(f=>({...f,password:e.target.value}))}
                required placeholder="••••••••"
                style={{ width:'100%', padding:'10px 12px', border:'1px solid var(--border)', borderRadius:6, fontSize:14, outline:'none' }} />
            </div>
            {error && (
              <div style={{ background:'#fef2f2', border:'1px solid #fecaca', color:'var(--danger)', padding:'10px 14px', borderRadius:6, fontSize:13, marginBottom:16 }}>
                {error}
              </div>
            )}
            <button type="submit" disabled={loading}
              style={{ width:'100%', padding:'11px', background:'var(--primary)', color:'#fff', border:'none', borderRadius:6, fontSize:15, fontWeight:600, cursor:'pointer', display:'flex', alignItems:'center', justifyContent:'center', gap:8 }}>
              {loading ? <Spinner size={18} color="#fff" /> : null}
              {loading ? 'Signing in...' : 'Sign In'}
            </button>
          </form>
          <p style={{ textAlign:'center', marginTop:20, fontSize:13, color:'var(--text-muted)' }}>
            Don't have an account? <Link to="/register" style={{ color:'var(--primary)', fontWeight:600 }}>Register</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
