import React, { useState, useEffect } from 'react';
import { authAPI } from '../api/api';
import { Building2, Eye, EyeOff, AlertCircle, ArrowRight } from 'lucide-react';

const LoginPage = ({ onLogin }) => {
  const [form, setForm] = useState({ tenDangNhap: '', matKhau: '' });
  const [showPass, setShowPass] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await authAPI.login(form);
      onLogin(res.data);
    } catch (err) {
      setError(err.response?.data || 'Không thể kết nối. Kiểm tra Spring Boot đang chạy.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="min-h-screen flex overflow-hidden"
      style={{ background: 'var(--bg-root)', fontFamily: "'Outfit', system-ui, sans-serif" }}
    >
      {/* ── Left decorative panel ── */}
      <div
        className="hidden lg:flex lg:w-[55%] relative flex-col justify-between p-12 overflow-hidden"
        style={{ background: '#0a0c12' }}
      >
        {/* Radial glow orbs */}
        <div
          className="absolute pointer-events-none"
          style={{
            width: 500, height: 500,
            borderRadius: '50%',
            background: 'radial-gradient(circle, rgba(245,200,66,0.12) 0%, transparent 70%)',
            top: '-80px', left: '-80px',
          }}
        />
        <div
          className="absolute pointer-events-none"
          style={{
            width: 400, height: 400,
            borderRadius: '50%',
            background: 'radial-gradient(circle, rgba(99,102,241,0.1) 0%, transparent 70%)',
            bottom: '60px', right: '-60px',
          }}
        />

        {/* Grid overlay */}
        <div
          className="absolute inset-0 pointer-events-none"
          style={{
            backgroundImage:
              'linear-gradient(rgba(255,255,255,0.025) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.025) 1px, transparent 1px)',
            backgroundSize: '60px 60px',
          }}
        />

        {/* Brand mark */}
        <div className="relative z-10 flex items-center gap-3">
          <div
            className="w-10 h-10 rounded-xl flex items-center justify-center"
            style={{ background: 'var(--accent)', boxShadow: '0 8px 32px -4px rgba(245,200,66,0.4)' }}
          >
            <Building2 style={{ width: 18, height: 18, color: '#0a0c12' }} strokeWidth={2.5} />
          </div>
          <div>
            <div style={{ color: '#f0f2f7', fontSize: 16, fontWeight: 800, letterSpacing: '0.06em' }}>OHNO HOTEL</div>
            <div style={{ color: 'var(--accent)', fontSize: 9, fontWeight: 700, letterSpacing: '0.2em', textTransform: 'uppercase', opacity: 0.8 }}>Management System</div>
          </div>
        </div>

        {/* Hero copy */}
        <div className="relative z-10 space-y-6">
          <div
            className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-[10px] font-bold uppercase tracking-widest"
            style={{
              background: 'rgba(245,200,66,0.1)',
              border: '1px solid rgba(245,200,66,0.25)',
              color: 'var(--accent)',
            }}
          >
            <span
              className="w-1.5 h-1.5 rounded-full"
              style={{
                background: 'var(--accent)',
                animation: 'ping-soft 2s ease-in-out infinite',
                display: 'inline-block',
              }}
            />
            Hệ thống vận hành 24/7
          </div>

          <h1
            style={{
              fontSize: 'clamp(2rem, 3.5vw, 3.25rem)',
              fontWeight: 900,
              lineHeight: 1.08,
              color: '#f0f2f7',
              letterSpacing: '-0.02em',
            }}
          >
            Quản lý khách sạn{' '}
            <span style={{ color: 'var(--accent)' }}>chuyên nghiệp</span>
            {' '}trong tầm tay.
          </h1>

          <p style={{ color: '#7c849a', fontSize: 14, lineHeight: 1.7, maxWidth: 380 }}>
            Kiểm soát toàn bộ hoạt động vận hành — từ đặt phòng, check-in, hóa đơn
            đến báo cáo doanh thu — trên một nền tảng duy nhất.
          </p>

          {/* Feature chips */}
          <div className="flex flex-wrap gap-2 pt-2">
            {['Đặt phòng thời gian thực', 'Báo cáo doanh thu', 'Quản lý nhân viên', 'Hóa đơn tự động'].map(feat => (
              <span
                key={feat}
                className="px-3 py-1.5 rounded-lg text-[11px] font-semibold"
                style={{
                  background: 'rgba(255,255,255,0.04)',
                  border: '1px solid rgba(255,255,255,0.08)',
                  color: '#a0a8bb',
                }}
              >
                {feat}
              </span>
            ))}
          </div>
        </div>

        {/* Bottom stats */}
        <div
          className="relative z-10 flex items-center gap-8 pt-8"
          style={{ borderTop: '1px solid rgba(255,255,255,0.06)' }}
        >
          {[
            { value: '99.8%', label: 'Uptime' },
            { value: '< 200ms', label: 'Response' },
            { value: '24/7', label: 'Hỗ trợ' },
          ].map(({ value, label }) => (
            <div key={label}>
              <div style={{ color: '#f0f2f7', fontSize: 18, fontWeight: 800, fontVariantNumeric: 'tabular-nums' }}>{value}</div>
              <div style={{ color: '#4a5068', fontSize: 10, fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.1em', marginTop: 2 }}>{label}</div>
            </div>
          ))}
        </div>
      </div>

      {/* ── Right login panel ── */}
      <div
        className="flex-1 flex items-center justify-center p-8"
        style={{ background: 'var(--bg-root)' }}
      >
        <div className="w-full max-w-[380px] space-y-8 animate-fade-in">

          {/* Mobile brand */}
          <div className="flex lg:hidden items-center gap-3 mb-2">
            <div
              className="w-9 h-9 rounded-xl flex items-center justify-center"
              style={{ background: 'var(--accent)' }}
            >
              <Building2 style={{ width: 16, height: 16, color: '#0d0f14' }} strokeWidth={2.5} />
            </div>
            <div style={{ color: 'var(--text-primary)', fontSize: 15, fontWeight: 800 }}>OHNO HOTEL</div>
          </div>

          {/* Heading */}
          <div>
            <h2
              style={{
                fontSize: 26,
                fontWeight: 800,
                color: 'var(--text-primary)',
                letterSpacing: '-0.02em',
              }}
            >
              Đăng nhập
            </h2>
            <p style={{ color: 'var(--text-muted)', fontSize: 13, marginTop: 6 }}>
              Nhập thông tin tài khoản để tiếp tục
            </p>
          </div>

          {/* Error */}
          {error && (
            <div
              className="flex items-start gap-3 px-4 py-3.5 rounded-xl animate-fade-in"
              style={{
                background: 'rgba(239,68,68,0.08)',
                border: '1px solid rgba(239,68,68,0.2)',
              }}
            >
              <AlertCircle style={{ width: 14, height: 14, color: '#f87171', marginTop: 1, flexShrink: 0 }} />
              <span style={{ color: '#f87171', fontSize: 12, lineHeight: 1.6 }}>{error}</span>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="label-style">Tên đăng nhập</label>
              <input
                type="text"
                required
                value={form.tenDangNhap}
                onChange={e => setForm({ ...form, tenDangNhap: e.target.value })}
                placeholder="admin, manager, recept1..."
                className="input-style"
                style={{ fontSize: 14 }}
                autoComplete="username"
              />
            </div>

            <div>
              <label className="label-style">Mật khẩu</label>
              <div className="relative">
                <input
                  type={showPass ? 'text' : 'password'}
                  required
                  value={form.matKhau}
                  onChange={e => setForm({ ...form, matKhau: e.target.value })}
                  placeholder="••••••••"
                  className="input-style pr-11"
                  style={{ fontSize: 14 }}
                  autoComplete="current-password"
                />
                <button
                  type="button"
                  onClick={() => setShowPass(!showPass)}
                  className="absolute right-3.5 top-1/2 -translate-y-1/2 transition-colors duration-200"
                  style={{ color: 'var(--text-muted)' }}
                  onMouseEnter={e => e.currentTarget.style.color = 'var(--text-primary)'}
                  onMouseLeave={e => e.currentTarget.style.color = 'var(--text-muted)'}
                >
                  {showPass ? <EyeOff style={{ width: 15, height: 15 }} /> : <Eye style={{ width: 15, height: 15 }} />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full flex items-center justify-center gap-2.5 py-3 rounded-xl font-bold text-[14px] transition-all duration-300"
              style={{
                background: loading ? 'rgba(245,200,66,0.5)' : 'var(--accent)',
                color: '#0d0f14',
                boxShadow: loading ? 'none' : '0 6px 24px -4px rgba(245,200,66,0.45)',
                cursor: loading ? 'not-allowed' : 'pointer',
                transform: 'scale(1)',
                transition: 'all 0.25s cubic-bezier(0.32, 0.72, 0, 1)',
              }}
              onMouseEnter={e => { if (!loading) e.currentTarget.style.filter = 'brightness(1.08)'; }}
              onMouseLeave={e => { e.currentTarget.style.filter = 'none'; }}
              onMouseDown={e => { if (!loading) e.currentTarget.style.transform = 'scale(0.97)'; }}
              onMouseUp={e => { e.currentTarget.style.transform = 'scale(1)'; }}
            >
              {loading ? (
                <>
                  <div
                    className="w-4 h-4 rounded-full border-2 border-[#0d0f14]/30 border-t-[#0d0f14] animate-spin"
                  />
                  Đang xác thực...
                </>
              ) : (
                <>
                  Đăng nhập
                  <ArrowRight style={{ width: 15, height: 15 }} />
                </>
              )}
            </button>
          </form>

          {/* Hint */}
          <div
            className="text-center pt-2"
            style={{ borderTop: '1px solid var(--border)' }}
          >
            <p style={{ color: 'var(--text-muted)', fontSize: 11, lineHeight: 1.6 }}>
              Tài khoản mặc định:{' '}
              <span
                className="font-mono-data px-2 py-0.5 rounded"
                style={{
                  color: 'var(--text-primary)',
                  background: 'var(--bg-elevated)',
                  border: '1px solid var(--border)',
                  fontSize: 11,
                }}
              >
                admin / admin
              </span>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
