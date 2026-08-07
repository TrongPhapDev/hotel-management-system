import React, { useState, useEffect, useRef } from 'react';
import LoginPage from './pages/LoginPage';
import TongQuanPage from './pages/TongQuanPage';
import ThuePhongPage from './pages/ThuePhongPage';
import PhongPage from './pages/PhongPage';
import LoaiPhongPage from './pages/LoaiPhongPage';
import KhachHangPage from './pages/KhachHangPage';
import DatPhongPage from './pages/DatPhongPage';
import DichVuList from './components/DichVuList';
import HoaDonPage from './pages/HoaDonPage';
import NhanVienPage from './pages/NhanVienPage';
import KhuyenMaiPage from './pages/KhuyenMaiPage';
import ThongKePage from './pages/ThongKePage';
import BangGiaPage from './pages/BangGiaPage';
import LogPage from './pages/LogPage';
import KeHoachPage from './pages/KeHoachPage';
import GiaoCaPage from './pages/GiaoCaPage';
import { authAPI } from './api/api';
import {
  LayoutDashboard, BedDouble, MapPin, LayoutGrid, CalendarCheck,
  Bookmark, Users, Receipt, UserCog, Tag, LogOut, BarChart3,
  Building2, Bell, ChevronRight, DollarSign, ClipboardList, Calendar, Key,
  Sun, Moon, X, User, Lock, ChevronDown, MoreVertical
} from 'lucide-react';

const MENU_GROUPS = [
  {
    group: 'Vận hành',
    items: [
      { id: 'tongquan',  label: 'Tổng Quan',     icon: LayoutDashboard },
      { id: 'thuephong', label: 'Sơ Đồ Phòng',   icon: BedDouble },
      { id: 'kehoach',   label: 'Lịch Trình',     icon: Calendar },
      { id: 'datphong',  label: 'Đặt Phòng',      icon: CalendarCheck },
      { id: 'hoadon',    label: 'Hóa Đơn',        icon: Receipt },
    ],
  },
  {
    group: 'Danh mục',
    items: [
      { id: 'phong',     label: 'Quản lý Phòng',  icon: MapPin },
      { id: 'loaiphong', label: 'Loại Phòng',     icon: LayoutGrid },
      { id: 'dichvu',    label: 'Dịch Vụ',        icon: Bookmark },
      { id: 'banggia',   label: 'Bảng Giá',       icon: DollarSign },
      { id: 'khuyenmai', label: 'Khuyến Mãi',     icon: Tag },
    ],
  },
  {
    group: 'Nhân sự & Khách hàng',
    items: [
      { id: 'khachhang', label: 'Khách Hàng',     icon: Users },
      { id: 'nhanvien',  label: 'Nhân Viên',      icon: UserCog },
      { id: 'giaoca',    label: 'Giao Ca',        icon: Key },
    ],
  },
  {
    group: 'Báo cáo',
    items: [
      { id: 'thongke',   label: 'Thống Kê',       icon: BarChart3 },
      { id: 'log',       label: 'Nhật Ký',        icon: ClipboardList },
    ],
  },
];

function App() {
  const [user, setUser] = useState(null);
  const [tab, setTab] = useState('tongquan');
  const [darkMode, setDarkMode] = useState(() => localStorage.getItem('theme') === 'dark');
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const [showProfileModal, setShowProfileModal] = useState(false);
  const [showPwdModal, setShowPwdModal] = useState(false);
  const profileMenuRef = useRef(null);

  useEffect(() => {
    if (darkMode) {
      document.documentElement.classList.add('dark');
      localStorage.setItem('theme', 'dark');
    } else {
      document.documentElement.classList.remove('dark');
      localStorage.setItem('theme', 'light');
    }
  }, [darkMode]);

  useEffect(() => {
    const handleOutside = (e) => {
      if (profileMenuRef.current && !profileMenuRef.current.contains(e.target))
        setShowProfileMenu(false);
    };
    document.addEventListener('mousedown', handleOutside);
    return () => document.removeEventListener('mousedown', handleOutside);
  }, []);

  if (!user) return <LoginPage onLogin={setUser} />;

  const renderPage = () => {
    switch (tab) {
      case 'tongquan':  return <TongQuanPage user={user} setTab={setTab} />;
      case 'thuephong': return <ThuePhongPage />;
      case 'kehoach':   return <KeHoachPage setTab={setTab} />;
      case 'phong':     return <PhongPage />;
      case 'loaiphong': return <LoaiPhongPage />;
      case 'datphong':  return <DatPhongPage />;
      case 'dichvu':    return <DichVuList />;
      case 'khachhang': return <KhachHangPage />;
      case 'hoadon':    return <HoaDonPage />;
      case 'nhanvien':  return <NhanVienPage />;
      case 'khuyenmai': return <KhuyenMaiPage />;
      case 'banggia':   return <BangGiaPage />;
      case 'giaoca':    return <GiaoCaPage />;
      case 'thongke':   return <ThongKePage />;
      case 'log':       return <LogPage />;
      default:          return <TongQuanPage user={user} setTab={setTab} />;
    }
  };

  const currentLabel = MENU_GROUPS.flatMap(g => g.items).find(m => m.id === tab)?.label || 'Hệ thống';
  const initials = (user.nhanVien?.hoTen || user.tenDangNhap || 'U').charAt(0).toUpperCase();

  const getHeaderDate = () => {
    const days = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'];
    const now = new Date();
    const dayName = days[now.getDay()];
    const dateStr = `${now.getDate()}/${now.getMonth() + 1}/${now.getFullYear()}`;
    return `${dayName}, ${dateStr}`;
  };

  return (
    <div className="flex h-screen overflow-hidden" style={{ background: 'var(--bg-root)', color: 'var(--text-primary)' }}>

      {/* ── Sidebar ── */}
      <aside
        className="w-[220px] flex flex-col shrink-0 overflow-y-auto"
        style={{
          background: 'var(--bg-sidebar)',
          borderRight: '1px solid var(--border)',
        }}
      >
        {/* Brand */}
        <div
          className="h-[60px] px-5 flex items-center gap-3 shrink-0"
          style={{ borderBottom: '1px solid var(--border)' }}
        >
          <div
            className="w-8 h-8 rounded-xl flex items-center justify-center shrink-0"
            style={{
              background: 'var(--accent)',
              boxShadow: 'var(--shadow-accent)',
            }}
          >
            <Building2 className="w-4 h-4" style={{ color: '#ffffff' }} strokeWidth={2} />
          </div>
          <div className="min-w-0">
            <div className="text-[13px] font-black tracking-wider" style={{ color: 'var(--text-primary)', letterSpacing: '0.06em' }}>
              OHNO
            </div>
            <div className="text-[9px] font-bold uppercase tracking-[0.18em]" style={{ color: 'var(--text-accent)', opacity: 0.8 }}>
              Hotel PMS
            </div>
          </div>
        </div>

        {/* Nav */}
        <nav className="flex-1 px-2.5 py-4 space-y-5 overflow-y-auto">
          {MENU_GROUPS.map(({ group, items }) => (
            <div key={group}>
              <div
                className="px-2.5 mb-2 text-[10px] font-medium uppercase"
                style={{ color: 'var(--text-tertiary)', letterSpacing: '0.7px' }}
              >
                {group}
              </div>
              <div className="space-y-0.5">
                {items.map(({ id, label, icon: Icon }) => {
                  const active = tab === id;
                  return (
                    <button
                      key={id}
                      onClick={() => setTab(id)}
                      className="w-full flex items-center gap-[10px] px-[10px] py-[8px] rounded-[7px] text-[13px] font-semibold transition-all duration-200 text-left group"
                      style={{
                        background: active ? '#EFF6FF' : 'transparent',
                        color: active ? '#1D4ED8' : 'var(--text-secondary)',
                      }}
                      onMouseEnter={e => {
                        if (!active) {
                          e.currentTarget.style.background = 'var(--bg-hover)';
                          e.currentTarget.style.color = 'var(--text-primary)';
                        }
                      }}
                      onMouseLeave={e => {
                        if (!active) {
                          e.currentTarget.style.background = 'transparent';
                          e.currentTarget.style.color = 'var(--text-secondary)';
                        }
                      }}
                    >
                      <Icon
                        className="shrink-0"
                        style={{ width: 16, height: 16, color: active ? '#1D4ED8' : 'currentColor' }}
                        strokeWidth={active ? 2.0 : 1.8}
                      />
                      <span className="truncate">{label}</span>
                    </button>
                  );
                })}
              </div>
            </div>
          ))}
        </nav>

        {/* Sidebar Footer: User Block */}
        <div
          className="p-3 shrink-0 flex items-center gap-2.5 relative"
          style={{ borderTop: '1px solid var(--border)' }}
        >
          {/* Circular Initials Avatar 32x32px */}
          <div
            className="w-8 h-8 rounded-full flex items-center justify-center text-[12px] font-black shrink-0"
            style={{ background: '#EFF6FF', color: '#1D4ED8', border: '1px solid #c3ddfd' }}
          >
            {initials}
          </div>
          <div className="flex-1 min-w-0">
            <div className="text-[12px] font-semibold truncate" style={{ color: 'var(--text-primary)' }}>
              {user.nhanVien?.hoTen || user.tenDangNhap}
            </div>
            <div className="text-[10px] text-[var(--text-secondary)] font-medium truncate">
              {user.vaiTro || 'Nhân viên'}
            </div>
          </div>
          <button
            onClick={() => setShowProfileMenu(!showProfileMenu)}
            className="w-7 h-7 flex items-center justify-center rounded-lg text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-[var(--bg-hover)] transition-all duration-200"
            title="Tùy chọn tài khoản"
          >
            <MoreVertical style={{ width: 14, height: 14 }} />
          </button>
        </div>
      </aside>

      {/* ── Main Area ── */}
      <div className="flex-1 flex flex-col overflow-hidden">

        {/* ── Topbar ── */}
        <header
          className="h-[52px] px-6 flex items-center justify-between shrink-0"
          style={{
            background: 'var(--bg-sidebar)',
            borderBottom: '0.5px solid var(--border-tertiary, var(--border))',
          }}
        >
          {/* Breadcrumb & Live Date & Live Badge */}
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2 text-[13px] font-medium">
              <span style={{ color: 'var(--text-muted)' }}>OHNO</span>
              <span style={{ color: 'var(--text-muted)' }}>›</span>
              <span style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{currentLabel}</span>
              <span style={{ color: 'var(--text-muted)', margin: '0 2px' }}>·</span>
              <span style={{ color: 'var(--text-secondary)' }}>{getHeaderDate()}</span>
            </div>

            {/* Live Badge */}
            <div
              className="flex items-center gap-1.5 px-2.5 py-0.5 rounded-[20px] text-[10px] font-bold"
              style={{
                background: '#E1F5EE',
                color: '#0F6E56',
              }}
            >
              <span
                className="w-1.5 h-1.5 rounded-full inline-block"
                style={{ background: '#10b981', animation: 'ping-soft 2s ease-in-out infinite' }}
              />
              LIVE
            </div>
          </div>

          {/* Right Actions */}
          <div className="flex items-center gap-2">
            {/* Theme Toggle */}
            <button
              onClick={() => setDarkMode(!darkMode)}
              className="flex items-center justify-center transition-all duration-200"
              style={{
                width: 32,
                height: 32,
                borderRadius: 8,
                border: '0.5px solid var(--border-tertiary, var(--border))',
                color: 'var(--text-secondary)',
              }}
              onMouseEnter={e => { e.currentTarget.style.background = 'var(--bg-hover)'; e.currentTarget.style.color = 'var(--text-primary)'; }}
              onMouseLeave={e => { e.currentTarget.style.background = 'transparent'; e.currentTarget.style.color = 'var(--text-secondary)'; }}
              title={darkMode ? 'Giao diện sáng' : 'Giao diện tối'}
            >
              {darkMode ? <Sun style={{ width: 14, height: 14 }} /> : <Moon style={{ width: 14, height: 14 }} />}
            </button>

            {/* Notification */}
            <button
              className="flex items-center justify-center transition-all duration-200 relative"
              style={{
                width: 32,
                height: 32,
                borderRadius: 8,
                border: '0.5px solid var(--border-tertiary, var(--border))',
                color: 'var(--text-secondary)',
              }}
              onMouseEnter={e => { e.currentTarget.style.background = 'var(--bg-hover)'; e.currentTarget.style.color = 'var(--text-primary)'; }}
              onMouseLeave={e => { e.currentTarget.style.background = 'transparent'; e.currentTarget.style.color = 'var(--text-secondary)'; }}
            >
              <Bell style={{ width: 14, height: 14 }} />
            </button>

            {/* Divider */}
            <div className="w-px h-5 mx-1" style={{ background: 'var(--border)' }} />

            {/* Profile */}
            <div className="relative" ref={profileMenuRef}>
              <button
                onClick={() => setShowProfileMenu(!showProfileMenu)}
                className="flex items-center gap-2 px-2 py-1.5 rounded-lg transition-all duration-200"
                style={{ color: 'var(--text-primary)' }}
                onMouseEnter={e => e.currentTarget.style.background = 'var(--bg-hover)'}
                onMouseLeave={e => { if (!showProfileMenu) e.currentTarget.style.background = 'transparent'; }}
              >
                {/* Avatar */}
                <div
                  className="w-7 h-7 rounded-lg flex items-center justify-center text-[11px] font-black"
                  style={{ background: 'var(--accent)', color: '#0d0f14', flexShrink: 0 }}
                >
                  {initials}
                </div>
                <div className="hidden sm:block text-left">
                  <div className="text-[12px] font-bold leading-tight" style={{ color: 'var(--text-primary)' }}>
                    {user.nhanVien?.hoTen || user.tenDangNhap}
                  </div>
                  <div className="text-[9px] font-bold uppercase tracking-wider" style={{ color: 'var(--text-muted)' }}>
                    {user.vaiTro || 'Staff'}
                  </div>
                </div>
                <ChevronDown
                  style={{ width: 12, height: 12, color: 'var(--text-muted)', transition: 'transform 0.2s ease' }}
                  className={showProfileMenu ? 'rotate-180' : ''}
                />
              </button>

              {showProfileMenu && (
                <div
                  className="dropdown-menu absolute right-0 top-full mt-2 w-48 animate-fade-in"
                  style={{ zIndex: 100 }}
                >
                  <div className="px-4 py-3" style={{ borderBottom: '1px solid var(--border)' }}>
                    <div className="text-[12px] font-bold" style={{ color: 'var(--text-primary)' }}>
                      {user.nhanVien?.hoTen || user.tenDangNhap}
                    </div>
                    <div className="text-[10px] font-medium mt-0.5" style={{ color: 'var(--text-muted)' }}>
                      {user.vaiTro || 'Nhân viên'}
                    </div>
                  </div>
                  <div className="py-1.5">
                    <button
                      onClick={() => { setShowProfileMenu(false); setShowProfileModal(true); }}
                      className="dropdown-item"
                    >
                      <User style={{ width: 13, height: 13, color: 'var(--text-muted)' }} strokeWidth={2} />
                      Thông tin cá nhân
                    </button>
                    <button
                      onClick={() => { setShowProfileMenu(false); setShowPwdModal(true); }}
                      className="dropdown-item"
                    >
                      <Lock style={{ width: 13, height: 13, color: 'var(--text-muted)' }} strokeWidth={2} />
                      Đổi mật khẩu
                    </button>
                  </div>
                  <div className="py-1.5" style={{ borderTop: '1px solid var(--border)' }}>
                    <button
                      onClick={() => { setShowProfileMenu(false); setUser(null); }}
                      className="dropdown-item danger"
                    >
                      <LogOut style={{ width: 13, height: 13 }} strokeWidth={2} />
                      Đăng xuất
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </header>

        {/* ── Content ── */}
        <main
          className="flex-1 overflow-y-auto p-6"
          style={{ background: 'var(--bg-root)' }}
        >
          <div className="animate-fade-in">
            {renderPage()}
          </div>
        </main>
      </div>

      {/* ── Modals ── */}
      {showProfileModal && <ProfileModal user={user} onClose={() => setShowProfileModal(false)} />}
      {showPwdModal && <ChangePasswordModal user={user} onClose={() => setShowPwdModal(false)} />}
    </div>
  );
}

/* ── Profile Modal ── */
const ProfileModal = ({ user, onClose }) => {
  const nv = user.nhanVien || {};
  const initials = (nv.hoTen || user.tenDangNhap || 'U').charAt(0).toUpperCase();
  const fields = [
    { label: 'Mã nhân viên', value: nv.maNhanVien },
    { label: 'Số điện thoại', value: nv.sdt },
    { label: 'Email', value: nv.email },
    { label: 'Địa chỉ', value: nv.diaChi },
    { label: 'Tên đăng nhập', value: user.tenDangNhap },
  ];

  return (
    <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="modal-panel max-w-sm">
        <div className="modal-header">
          <h3 className="modal-title">Thông tin cá nhân</h3>
          <button
            onClick={onClose}
            className="w-7 h-7 flex items-center justify-center rounded-lg transition-all duration-200"
            style={{ color: 'var(--text-muted)' }}
            onMouseEnter={e => { e.currentTarget.style.background = 'var(--bg-hover)'; e.currentTarget.style.color = 'var(--text-primary)'; }}
            onMouseLeave={e => { e.currentTarget.style.background = 'transparent'; e.currentTarget.style.color = 'var(--text-muted)'; }}
          >
            <X style={{ width: 15, height: 15 }} />
          </button>
        </div>

        <div className="modal-body">
          {/* Avatar */}
          <div className="flex flex-col items-center gap-2 py-3">
            <div
              className="w-16 h-16 rounded-2xl flex items-center justify-center text-3xl font-black"
              style={{ background: 'var(--accent)', color: '#0d0f14', boxShadow: 'var(--shadow-accent)' }}
            >
              {initials}
            </div>
            <div>
              <div className="text-center font-bold text-[15px]" style={{ color: 'var(--text-primary)' }}>
                {nv.hoTen || user.tenDangNhap}
              </div>
              <div className="text-center text-[10px] font-bold uppercase tracking-wider mt-0.5" style={{ color: 'var(--accent)', opacity: 0.9 }}>
                {user.vaiTro || 'Nhân viên'}
              </div>
            </div>
          </div>

          {/* Fields */}
          <div
            className="rounded-xl overflow-hidden"
            style={{ border: '1px solid var(--border)' }}
          >
            {fields.map(({ label, value }, i) => (
              <div
                key={label}
                className="flex items-center justify-between px-4 py-3"
                style={{
                  borderBottom: i < fields.length - 1 ? '1px solid var(--border)' : 'none',
                  background: i % 2 === 0 ? 'transparent' : 'var(--bg-elevated)',
                }}
              >
                <span className="text-[11px] font-semibold" style={{ color: 'var(--text-muted)' }}>{label}</span>
                <span className="text-[12px] font-bold" style={{ color: 'var(--text-primary)' }}>{value || '—'}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="modal-footer">
          <button onClick={onClose} className="btn-primary text-[13px]">Đóng</button>
        </div>
      </div>
    </div>
  );
};

/* ── Change Password Modal ── */
const ChangePasswordModal = ({ user, onClose }) => {
  const [form, setForm] = useState({ matKhauCu: '', matKhauMoi: '', xacNhanMatKhau: '' });
  const [err, setErr] = useState('');
  const [success, setSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErr(''); setSuccess('');
    if (form.matKhauMoi !== form.xacNhanMatKhau) {
      setErr('Mật khẩu mới và xác nhận không khớp.');
      return;
    }
    setSubmitting(true);
    try {
      await authAPI.doiMatKhau({
        tenDangNhap: user.tenDangNhap,
        matKhauCu: form.matKhauCu,
        matKhauMoi: form.matKhauMoi,
      });
      setSuccess('Đổi mật khẩu thành công.');
      setForm({ matKhauCu: '', matKhauMoi: '', xacNhanMatKhau: '' });
      setTimeout(onClose, 1500);
    } catch (err) {
      setErr(err.response?.data || 'Đổi mật khẩu thất bại. Kiểm tra lại mật khẩu cũ.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="modal-panel max-w-sm">
        <div className="modal-header">
          <h3 className="modal-title">Đổi mật khẩu</h3>
          <button
            onClick={onClose}
            className="w-7 h-7 flex items-center justify-center rounded-lg transition-all duration-200"
            style={{ color: 'var(--text-muted)' }}
            onMouseEnter={e => { e.currentTarget.style.background = 'var(--bg-hover)'; e.currentTarget.style.color = 'var(--text-primary)'; }}
            onMouseLeave={e => { e.currentTarget.style.background = 'transparent'; e.currentTarget.style.color = 'var(--text-muted)'; }}
          >
            <X style={{ width: 15, height: 15 }} />
          </button>
        </div>

        <div className="modal-body">
          {err && (
            <div className="alert-strip alert-error text-[12px]" style={{ color: '#f87171' }}>
              {err}
            </div>
          )}
          {success && (
            <div className="alert-strip alert-success text-[12px]" style={{ color: '#34d399' }}>
              {success}
            </div>
          )}

          <form id="pwd-form" onSubmit={handleSubmit} className="space-y-3.5">
            {[
              { name: 'matKhauCu', label: 'Mật khẩu hiện tại' },
              { name: 'matKhauMoi', label: 'Mật khẩu mới' },
              { name: 'xacNhanMatKhau', label: 'Xác nhận mật khẩu mới' },
            ].map(({ name, label }) => (
              <div key={name}>
                <label className="label-style">{label} *</label>
                <input
                  type="password"
                  required
                  minLength={name !== 'matKhauCu' ? 4 : undefined}
                  value={form[name]}
                  onChange={e => setForm({ ...form, [name]: e.target.value })}
                  className="input-style"
                  placeholder="••••••••"
                />
              </div>
            ))}
          </form>
        </div>

        <div className="modal-footer">
          <button
            type="button"
            onClick={onClose}
            className="btn-ghost text-[13px]"
          >
            Hủy
          </button>
          <button
            type="submit"
            form="pwd-form"
            disabled={submitting}
            className="btn-primary text-[13px]"
            style={{ opacity: submitting ? 0.6 : 1 }}
          >
            {submitting ? 'Đang lưu...' : 'Lưu mật khẩu'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default App;
