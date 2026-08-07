import React, { useEffect, useState, useCallback } from 'react';
import { nhanVienAPI } from '../api/api';
import { UserCog, Plus, Edit2, UserMinus, X, Search, Eye, Trash2 } from 'lucide-react';
import { RowContextMenu, useContextMenu } from '../components/ContextMenu';

const CHUCVU_CONFIG = {
  'ADMIN':        { label: 'Quản trị viên', cls: 'badge-maintenance' },
  'MANAGER':      { label: 'Quản lý',       cls: 'badge-available' },
  'RECEPTIONIST': { label: 'Lễ tân',        cls: 'badge-occupied' },
};

const NhanVienPage = () => {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [err, setErr] = useState('');
  const [kw, setKw] = useState('');
  const [filter, setFilter] = useState('Tất cả');
  const [filterTrangThai, setFilterTrangThai] = useState('all'); // 'all' | 'active' | 'inactive'
  const [detailNV, setDetailNV] = useState(null);
  const { ctxMenu, openCtxMenu, closeCtxMenu } = useContextMenu();

  const init = {
    maNhanVien: '', hoTen: '', sdt: '', chucVu: 'RECEPTIONIST',
    email: '', cccd: '', diaChi: '', luongCoBan: 0,
    matKhau: '', ngaySinh: '', gioiTinh: 'Nam',
    ngayVaoLam: new Date().toISOString().split('T')[0],
    dangLamViec: true
  };
  const [form, setForm] = useState(init);

  const fetchData = useCallback(() => {
    setLoading(true);
    nhanVienAPI.getAll().then(r => setItems(r.data)).catch(console.error).finally(() => setLoading(false));
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  const filtered = items.filter(nv => {
    const matchKw = !kw || nv.hoTen?.toLowerCase().includes(kw.toLowerCase()) ||
      nv.maNhanVien?.toLowerCase().includes(kw.toLowerCase()) ||
      nv.sdt?.includes(kw) || nv.email?.toLowerCase().includes(kw.toLowerCase());
    const matchFilter = filter === 'Tất cả' || nv.chucVu === filter;
    const matchTrangThai = filterTrangThai === 'all' ||
      (filterTrangThai === 'active' && nv.dangLamViec) ||
      (filterTrangThai === 'inactive' && !nv.dangLamViec);
    return matchKw && matchFilter && matchTrangThai;
  });

  const hasFilter = kw || filter !== 'Tất cả' || filterTrangThai !== 'all';
  const clearFilters = () => { setKw(''); setFilter('Tất cả'); setFilterTrangThai('all'); };

  const kpiData = [
    { label: 'Tổng nhân lực',  value: items.length },
    { label: 'Đang làm việc',  value: items.filter(i => i.dangLamViec).length },
    { label: 'Đã nghỉ việc',   value: items.filter(i => !i.dangLamViec).length },
  ];

  const openAdd = () => { setEditing(null); setForm(init); setErr(''); setShowModal(true); };
  const openEdit = (nv) => { setEditing(nv.maNhanVien); setForm({ ...init, ...nv }); setErr(''); setShowModal(true); setDetailNV(null); };

  const handleSubmit = async (e) => {
    e.preventDefault(); setErr('');
    try {
      if (editing) await nhanVienAPI.update(editing, form);
      else await nhanVienAPI.create(form);
      setShowModal(false); fetchData();
    } catch (e) { setErr(e.response?.data || 'Lỗi!'); }
  };

  const handleNghi = async (nv) => {
    if (!confirm(`Đánh dấu nghỉ việc: "${nv.hoTen}" (${nv.maNhanVien})?`)) return;
    try { await nhanVienAPI.nghi(nv.maNhanVien); fetchData(); } catch (e) { alert(e.response?.data || 'Lỗi!'); }
  };

  const handleCtxAction = (action, nv) => {
    switch (action) {
      case 'view': setDetailNV(nv); break;
      case 'edit': openEdit(nv); break;
      case 'nghi': handleNghi(nv); break;
    }
  };

  const getCtxItems = (nv) => [
    { label: 'Xem hồ sơ nhân viên', action: 'view' },
    { label: 'Chỉnh sửa thông tin', action: 'edit' },
    ...(nv?.dangLamViec ? [
      { separator: true },
      { label: 'Đánh dấu nghỉ việc', action: 'nghi', danger: true },
    ] : []),
  ];

  const FILTERS = ['Tất cả', 'ADMIN', 'MANAGER', 'RECEPTIONIST'];

  const initials = (name) => (name || '?').charAt(0).toUpperCase();

  return (
    <div className="page-shell" onClick={() => closeCtxMenu()}>
      {/* Header */}
      <div className="page-header">
        <div>
          <div className="page-label">
            Nhân sự & Khách hàng
          </div>
          <h1 className="page-title flex items-center gap-2">
            <UserCog style={{ width: 20, height: 20, color: 'var(--text-secondary)' }} strokeWidth={2} />
            Nhân sự & Tài khoản
          </h1>
          <p className="page-subtitle">Chuột phải để tùy chọn · Double-click để xem/sửa thông tin</p>
        </div>
        <button onClick={openAdd} className="btn-primary">
          <Plus style={{ width: 15, height: 15 }} />
          Thêm nhân viên
        </button>
      </div>

      {/* KPI */}
      <div className="grid grid-cols-2 md:grid-cols-3 gap-[10px]">
        {kpiData.map(kpi => (
          <div key={kpi.label} className="kpi-card">
            <span className="kpi-label">{kpi.label}</span>
            <span className="kpi-value">{kpi.value}</span>
          </div>
        ))}
      </div>

      {/* Filter Bar */}
      <div className="filter-bar">
        <div className="filter-search">
          <Search className="filter-search-icon" style={{ width: 14, height: 14 }} />
          <input type="text" placeholder="Tìm tên, mã NV, SĐT, email..." value={kw}
            onChange={e => setKw(e.target.value)} />
          {kw && (
            <button onClick={() => setKw('')} className="filter-clear-btn">
              <X style={{ width: 10, height: 10 }} />
            </button>
          )}
        </div>

        <div className={`filter-select-wrap ${filter !== 'Tất cả' ? 'active' : ''}`}>
          <select value={filter} onChange={e => setFilter(e.target.value)}>
            <option value="Tất cả">Tất cả chức vụ</option>
            {FILTERS.filter(f => f !== 'Tất cả').map(f => (
              <option key={f} value={f}>{CHUCVU_CONFIG[f]?.label || f}</option>
            ))}
          </select>
        </div>

        <div className={`filter-select-wrap ${filterTrangThai !== 'all' ? 'active' : ''}`}>
          <select value={filterTrangThai} onChange={e => setFilterTrangThai(e.target.value)}>
            <option value="all">Tất cả trạng thái</option>
            <option value="active">Đang làm việc</option>
            <option value="inactive">Đã nghỉ việc</option>
          </select>
        </div>

        {hasFilter && (
          <button onClick={clearFilters} className="filter-reset-btn">
            Xoá lọc
          </button>
        )}

        <span className="filter-result-count">{filtered.length}/{items.length} nhân viên</span>
      </div>

      {/* Table */}
      <div className="section-box">
        <table className="data-table">
          <thead>
            <tr>
              {['Mã NV', 'Họ tên', 'Chức vụ', 'SĐT', 'Email', 'Trạng thái'].map(h => (
                <th key={h}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={6} className="py-16 text-center">
                  <div className="spinner mx-auto mb-2" />
                  <div style={{ color: 'var(--text-muted)', fontSize: 12 }}>Đang tải...</div>
                </td>
              </tr>
            ) : filtered.length === 0 ? (
              <tr>
                <td colSpan={6} className="py-14 text-center" style={{ color: 'var(--text-muted)', fontSize: 13 }}>Không có dữ liệu</td>
              </tr>
            ) : filtered.map(nv => (
              <tr
                key={nv.maNhanVien}
                className="cursor-pointer group"
                onDoubleClick={() => setDetailNV(nv)}
                onContextMenu={e => { e.stopPropagation(); openCtxMenu(e, nv); }}
              >
                <td className="font-mono-data text-[12px] font-bold" style={{ color: 'var(--accent)' }}>{nv.maNhanVien}</td>
                <td>
                  <div className="flex items-center gap-2.5">
                    <div
                      className="w-8 h-8 rounded-lg flex items-center justify-center text-[12px] font-black shrink-0"
                      style={{ background: 'var(--accent)', color: '#0d0f14' }}
                    >
                      {initials(nv.hoTen)}
                    </div>
                    <div>
                      <div className="font-semibold text-[13px]" style={{ color: 'var(--text-primary)' }}>{nv.hoTen}</div>
                      {nv.email && <div className="text-[11px]" style={{ color: 'var(--text-muted)' }}>{nv.email}</div>}
                    </div>
                  </div>
                </td>
                <td>
                  {(() => {
                    const cfg = CHUCVU_CONFIG[nv.chucVu];
                    return cfg
                      ? <span className={`badge ${cfg.cls}`}>{cfg.label}</span>
                      : <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>{nv.chucVu || '—'}</span>;
                  })()}
                </td>
                <td style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{nv.sdt || '—'}</td>
                <td style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{nv.email || '—'}</td>
                <td className="row-actions-cell">
                  <span className={`badge ${nv.dangLamViec ? 'badge-available' : ''}`}
                    style={!nv.dangLamViec ? { background: 'var(--bg-elevated)', color: 'var(--text-muted)', border: '1px solid var(--border)' } : undefined}>
                    <span className="badge-dot" style={{ background: nv.dangLamViec ? 'var(--status-avail-dot)' : 'var(--text-muted)' }} />
                    {nv.dangLamViec ? 'Đang làm' : 'Đã nghỉ'}
                  </span>

                  {/* Hover Actions Panel */}
                  <div className="row-hover-actions">
                    <button onClick={(e) => { e.stopPropagation(); setDetailNV(nv); }} className="row-action-btn" title="Xem hồ sơ">
                      <Eye style={{ width: 12, height: 12 }} />
                    </button>
                    <button onClick={(e) => { e.stopPropagation(); openEdit(nv); }} className="row-action-btn" title="Chỉnh sửa">
                      <Edit2 style={{ width: 11, height: 11 }} />
                    </button>
                    {nv.dangLamViec && (
                      <button onClick={(e) => { e.stopPropagation(); handleNghi(nv); }} className="row-action-btn danger" title="Nghỉ việc">
                        <UserMinus style={{ width: 11, height: 11 }} />
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!loading && filtered.length > 0 && (
          <div className="table-footer">
            <span>Tổng <strong style={{ color: 'var(--text-primary)' }}>{filtered.length} nhân viên</strong></span>
            <span>Double-click để sửa · Chuột phải để tùy chọn</span>
          </div>
        )}
      </div>

      {/* Context Menu */}
      {ctxMenu && (
        <RowContextMenu menu={ctxMenu} items={getCtxItems(ctxMenu.item)} onAction={handleCtxAction} onClose={closeCtxMenu}
          title={ctxMenu.item?.hoTen} subtitle={ctxMenu.item?.maNhanVien} />
      )}

      {/* Detail Modal */}
      {detailNV && (
        <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) setDetailNV(null); }}>
          <div className="modal-panel" style={{ maxWidth: 460 }}>
            <div className="modal-header">
              <h3 className="modal-title">Hồ sơ nhân viên</h3>
              <button onClick={() => setDetailNV(null)} className="row-action-btn"><X style={{ width: 13, height: 13 }} /></button>
            </div>
            <div className="modal-body">
              <div className="flex items-center gap-4">
                <div className="w-14 h-14 rounded-2xl flex items-center justify-center text-2xl font-black shrink-0"
                  style={{ background: 'var(--accent)', color: '#0d0f14', boxShadow: 'var(--shadow-accent)' }}>
                  {initials(detailNV.hoTen)}
                </div>
                <div>
                  <div className="text-[16px] font-bold" style={{ color: 'var(--text-primary)' }}>{detailNV.hoTen}</div>
                  <div className="font-mono-data text-[11px]" style={{ color: 'var(--text-muted)' }}>{detailNV.maNhanVien}</div>
                  <span className={`badge mt-1.5 inline-flex ${detailNV.dangLamViec ? 'badge-available' : ''}`}
                    style={!detailNV.dangLamViec ? { background: 'var(--bg-elevated)', color: 'var(--text-muted)', border: '1px solid var(--border)' } : undefined}>
                    {detailNV.dangLamViec ? 'Đang làm việc' : 'Đã nghỉ việc'}
                  </span>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-2 mt-4">
                {[
                  ['Chức vụ', detailNV.chucVu || '—'],
                  ['SĐT', detailNV.sdt || '—'],
                  ['Email', detailNV.email || '—'],
                  ['CCCD', detailNV.cccd || '—'],
                  ['Lương cơ bản', detailNV.luongCoBan ? `${(detailNV.luongCoBan).toLocaleString('vi-VN')}đ` : '—'],
                  ['Ngày sinh', detailNV.ngaySinh || '—'],
                  ['Giới tính', detailNV.gioiTinh || '—'],
                  ['Ngày vào làm', detailNV.ngayVaoLam || '—'],
                  ['Địa chỉ', detailNV.diaChi || '—'],
                ].map(([k, v]) => (
                  <div key={k} className="rounded-xl p-2.5" style={{ background: 'var(--bg-elevated)' }}>
                    <div className="text-[9px] font-bold uppercase tracking-wider" style={{ color: 'var(--text-muted)' }}>{k}</div>
                    <div className="text-[12px] font-semibold mt-0.5 truncate" style={{ color: 'var(--text-primary)' }}>{v}</div>
                  </div>
                ))}
              </div>
            </div>
            <div className="modal-footer">
              <button onClick={() => setDetailNV(null)} className="btn-ghost text-[13px]">Đóng</button>
              <button onClick={() => openEdit(detailNV)} className="btn-primary text-[13px]">Chỉnh sửa</button>
            </div>
          </div>
        </div>
      )}

      {/* Add/Edit Modal */}
      {showModal && (
        <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) setShowModal(false); }}>
          <div className="modal-panel" style={{ maxWidth: 540 }}>
            <div className="modal-header">
              <h3 className="modal-title">{editing ? 'Cập nhật' : 'Thêm'} Nhân viên</h3>
              <button onClick={() => setShowModal(false)} className="row-action-btn"><X style={{ width: 13, height: 13 }} /></button>
            </div>
            <div className="modal-body" style={{ maxHeight: '70vh', overflowY: 'auto' }}>
              {err && <div className="alert-strip alert-error text-[12px]" style={{ color: '#f87171' }}>{err}</div>}
              <form id="nhanvien-form" onSubmit={handleSubmit} className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="label-style">Mã NV</label>
                    <input type="text" disabled={!!editing} value={form.maNhanVien}
                      onChange={e => setForm({ ...form, maNhanVien: e.target.value })} placeholder="Tự sinh"
                      className="input-style" style={{ opacity: editing ? 0.5 : 1 }} />
                  </div>
                  <div>
                    <label className="label-style">Chức vụ</label>
                    <select value={form.chucVu} onChange={e => setForm({ ...form, chucVu: e.target.value })} className="input-style">
                      <option value="RECEPTIONIST">Lễ tân</option>
                      <option value="MANAGER">Quản lý</option>
                      <option value="ADMIN">Quản trị viên</option>
                      <option value="Buồng phòng">Buồng phòng</option>
                      <option value="Bảo vệ">Bảo vệ</option>
                    </select>
                  </div>
                </div>
                <div>
                  <label className="label-style">Họ tên *</label>
                  <input required type="text" value={form.hoTen} onChange={e => setForm({ ...form, hoTen: e.target.value })} className="input-style" />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="label-style">SĐT</label>
                    <input type="text" value={form.sdt} onChange={e => setForm({ ...form, sdt: e.target.value })} className="input-style" />
                  </div>
                  <div>
                    <label className="label-style">Email</label>
                    <input type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} className="input-style" />
                  </div>
                  <div>
                    <label className="label-style">CCCD</label>
                    <input type="text" value={form.cccd} onChange={e => setForm({ ...form, cccd: e.target.value })} className="input-style" />
                  </div>
                  <div>
                    <label className="label-style">Lương cơ bản</label>
                    <input type="number" min={0} value={form.luongCoBan}
                      onChange={e => setForm({ ...form, luongCoBan: parseFloat(e.target.value) })} className="input-style" />
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="label-style">Ngày sinh</label>
                    <input type="date" value={form.ngaySinh || ''} onChange={e => setForm({ ...form, ngaySinh: e.target.value })} className="input-style" />
                  </div>
                  <div>
                    <label className="label-style">Giới tính</label>
                    <select value={form.gioiTinh || 'Nam'} onChange={e => setForm({ ...form, gioiTinh: e.target.value })} className="input-style">
                      <option value="Nam">Nam</option>
                      <option value="Nữ">Nữ</option>
                      <option value="Khác">Khác</option>
                    </select>
                  </div>
                  <div>
                    <label className="label-style">Ngày vào làm</label>
                    <input type="date" value={form.ngayVaoLam || ''} onChange={e => setForm({ ...form, ngayVaoLam: e.target.value })} className="input-style" />
                  </div>
                  <div>
                    <label className="label-style">Trạng thái</label>
                    <div className="flex items-center mt-3 gap-2">
                      <input type="checkbox" id="dangLamViec" checked={form.dangLamViec}
                        onChange={e => setForm({ ...form, dangLamViec: e.target.checked })}
                        className="w-4 h-4 rounded cursor-pointer" style={{ accentColor: 'var(--accent)' }} />
                      <label htmlFor="dangLamViec" className="text-[13px] font-medium cursor-pointer" style={{ color: 'var(--text-primary)' }}>
                        Đang làm việc
                      </label>
                    </div>
                  </div>
                </div>
                <div>
                  <label className="label-style">Địa chỉ</label>
                  <textarea value={form.diaChi || ''} onChange={e => setForm({ ...form, diaChi: e.target.value })} rows={2} className="input-style resize-none" />
                </div>
                {!editing && (
                  <div>
                    <label className="label-style">Mật khẩu *</label>
                    <input required type="password" value={form.matKhau} onChange={e => setForm({ ...form, matKhau: e.target.value })} className="input-style" />
                  </div>
                )}
                {editing && (
                  <div>
                    <label className="label-style">Đổi mật khẩu mới (bỏ trống để giữ nguyên)</label>
                    <input type="password" value={form.matKhau || ''} onChange={e => setForm({ ...form, matKhau: e.target.value })} className="input-style" />
                  </div>
                )}
              </form>
            </div>
            <div className="modal-footer">
              <button type="button" onClick={() => setShowModal(false)} className="btn-ghost text-[13px]">Hủy</button>
              <button type="submit" form="nhanvien-form" className="btn-primary text-[13px]">Lưu</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default NhanVienPage;
