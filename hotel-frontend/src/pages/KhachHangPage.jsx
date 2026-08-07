import React, { useEffect, useState, useRef, useCallback } from 'react';
import { khachHangAPI } from '../api/api';
import { Users, Plus, Edit2, Trash2, Search, X, Star, Globe, Phone, CreditCard, Copy, ClipboardCopy, Eye } from 'lucide-react';

const HANG_CONFIG = {
  'VIP': { label: 'VIP', cls: 'badge-occupied' },
  'Thân thiết': { label: 'Thân thiết', cls: 'badge-available' },
  'Thường': { label: 'Thường', cls: '' },
};

const HangBadge = ({ hang }) => {
  const cfg = hang && HANG_CONFIG[hang] ? HANG_CONFIG[hang] : { label: hang || 'Thường', cls: '' };
  return <span className={`badge ${cfg.cls}`}>{cfg.label}</span>;
};

const ContextMenu = ({ menu, onAction, onClose }) => {
  const { kh } = menu;
  const ref = useRef(null);
  useEffect(() => {
    const handler = (e) => { if (ref.current && !ref.current.contains(e.target)) onClose(); };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [onClose]);

  const style = { position: 'fixed', top: menu.y, left: menu.x, zIndex: 9999 };
  if (menu.x + 220 > window.innerWidth) style.left = menu.x - 220;
  if (menu.y + 200 > window.innerHeight) style.top = menu.y - 200;

  return (
    <div ref={ref} style={style}
      className="bg-[var(--bg-sidebar)] border border-[var(--border-color)] rounded-xl shadow-2xl py-1 min-w-[210px] overflow-hidden">
      <div className="px-3 py-2 border-b border-[var(--border-color)]">
        <div className="text-xs font-bold text-[var(--text-primary)]">{kh.hoTen}</div>
        <div className="text-[10px] text-[var(--text-secondary)] font-mono">{kh.maKhachHang}</div>
      </div>
      {[
        { label: 'Xem hồ sơ chi tiết', action: 'view', color: 'text-[var(--text-secondary)]' },
        { label: 'Chỉnh sửa thông tin', action: 'edit', color: 'text-blue-600 dark:text-blue-400' },
        { separator: true },
        { label: 'Sao chép mã khách hàng', action: 'copy', color: 'text-[var(--text-secondary)]' },
        { separator: true },
        { label: 'Vô hiệu hóa hồ sơ', action: 'delete', color: 'text-red-600 dark:text-red-400' },
      ].map((item, idx) => item.separator ? (
        <div key={idx} className="my-1 border-t border-[var(--border-color)]" />
      ) : (
        <button key={idx} onClick={() => onAction(item.action, kh)}
          className={`w-full text-left px-3 py-2 text-xs font-semibold hover:bg-[var(--bg-main)] dark:hover:bg-[var(--bg-main)]/50 transition-colors ${item.color}`}>
          {item.label}
        </button>
      ))}
    </div>
  );
};

const DetailModal = ({ kh, onClose, onEdit }) => (
  <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) onClose(); }}>
    <div className="modal-panel" style={{ maxWidth: 440 }}>
      <div className="px-5 py-4 border-b border-[var(--border-color)] flex items-center justify-between">
        <h3 className="font-bold text-[var(--text-primary)] flex items-center gap-2"><Users size={16} className="text-blue-500" />Hồ sơ khách hàng</h3>
        <button onClick={onClose}><X size={18} className="text-[var(--text-secondary)] hover:text-[var(--text-primary)]" /></button>
      </div>
      <div className="p-5 space-y-4">
        <div className="flex items-center gap-4">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-blue-400 to-violet-600 flex items-center justify-center text-white text-2xl font-black">
            {kh.hoTen?.charAt(0) || '?'}
          </div>
          <div>
            <div className="font-bold text-lg text-[var(--text-primary)]">{kh.hoTen}</div>
            <div className="flex items-center gap-2 mt-1">
              <HangBadge hang={kh.hangKhachHang} />
              <span className="text-xs text-[var(--text-secondary)] font-mono">{kh.maKhachHang}</span>
            </div>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-2 text-xs">
          {[
            ['SĐT', kh.sdt || '—'],
            ['Giới tính', kh.gioiTinh || '—'],
            ['Quốc tịch', kh.quocTich || 'Việt Nam'],
            ['Loại giấy tờ', kh.loaiGiayTo || '—'],
            ['CCCD/CMND', kh.cccd || '—'],
            ['Hộ chiếu', kh.soHoChieu || '—'],
            ['Số lần ở', kh.soLanO || 0],
            ['Tổng chi tiêu', kh.tongChiTieu ? `${(kh.tongChiTieu).toLocaleString('vi-VN')}đ` : '0đ'],
          ].map(([k, v]) => (
            <div key={k} className="bg-[var(--bg-main)] rounded-xl p-2.5">
              <div className="text-[10px] font-bold text-[var(--text-secondary)] uppercase">{k}</div>
              <div className="font-semibold text-[var(--text-primary)] mt-0.5 truncate">{v}</div>
            </div>
          ))}
        </div>
      </div>
      <div className="px-5 pb-4 flex justify-end gap-2">
        <button onClick={onClose} className="px-4 py-2 bg-[var(--bg-main)] text-[var(--text-secondary)] text-xs font-semibold rounded-xl">Đóng</button>
        <button onClick={onEdit} className="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold rounded-xl">Chỉnh sửa</button>
      </div>
    </div>
  </div>
);

const KhachHangPage = () => {
  const [items, setItems] = useState([]);
  const [kw, setKw] = useState('');
  const [filterHang, setFilterHang] = useState('all'); // 'all' | 'VIP' | 'Thân thiết' | 'Thường'
  const [filterQuoc, setFilterQuoc] = useState('all'); // 'all' | 'domestic' | 'foreign'
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [err, setErr] = useState('');
  const [ctxMenu, setCtxMenu] = useState(null);
  const [detailKH, setDetailKH] = useState(null);
  const [copyMsg, setCopyMsg] = useState('');

  const init = { maKhachHang: '', hoTen: '', sdt: '', cccd: '', gioiTinh: 'Nam', quocTich: 'Việt Nam', loaiGiayTo: 'CCCD', soHoChieu: '' };
  const [form, setForm] = useState(init);

  const fetchData = useCallback(() => {
    setLoading(true);
    khachHangAPI.getAll().then(r => setItems(r.data)).catch(console.error).finally(() => setLoading(false));
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  // Client-side filtering
  const filtered = items.filter(kh => {
    const matchKw = !kw || kh.hoTen?.toLowerCase().includes(kw.toLowerCase()) ||
      kh.sdt?.includes(kw) || kh.cccd?.includes(kw) ||
      kh.maKhachHang?.toLowerCase().includes(kw.toLowerCase());
    const matchHang = filterHang === 'all' || kh.hangKhachHang === filterHang;
    const matchQuoc = filterQuoc === 'all' ||
      (filterQuoc === 'domestic' && (!kh.quocTich || kh.quocTich === 'Việt Nam' || kh.quocTich === 'Viet Nam')) ||
      (filterQuoc === 'foreign' && kh.quocTich && kh.quocTich !== 'Việt Nam' && kh.quocTich !== 'Viet Nam');
    return matchKw && matchHang && matchQuoc;
  });

  const hasFilter = kw || filterHang !== 'all' || filterQuoc !== 'all';
  const clearFilters = () => { setKw(''); setFilterHang('all'); setFilterQuoc('all'); };

  // KPI stats
  const kpiData = [
    { label: 'Tổng khách hàng', value: items.length, icon: '👥', color: '#6366F1', bg: '#EEF2FF' },
    { label: 'Khách VIP', value: items.filter(i => i.hangKhachHang?.includes('VIP')).length, icon: '⭐', color: '#D97706', bg: '#FEF9C3' },
    { label: 'Khách quen', value: items.filter(i => i.soLanO > 1).length, icon: '🤝', color: '#3B82F6', bg: '#DBEAFE' },
    { label: 'Quốc tế', value: items.filter(i => i.quocTich && i.quocTich !== 'Việt Nam' && i.quocTich !== 'Viet Nam').length, icon: '🌍', color: '#10B981', bg: '#ECFDF5' },
  ];

  const openAdd = () => { setEditing(null); setForm(init); setErr(''); setShowModal(true); };
  const openEdit = (kh) => { setEditing(kh.maKhachHang); setForm({ ...init, ...kh }); setErr(''); setShowModal(true); setDetailKH(null); };

  const handleSubmit = async (e) => {
    e.preventDefault(); setErr('');
    try {
      if (editing) await khachHangAPI.update(editing, form);
      else await khachHangAPI.create(form);
      setShowModal(false); fetchData();
    } catch (e) { setErr(e.response?.data || 'Lỗi!'); }
  };

  const del = async (id, name) => {
    if (!confirm(`Vô hiệu hóa hồ sơ khách hàng "${name}" (${id})?`)) return;
    try { await khachHangAPI.delete(id); fetchData(); } catch (e) { alert(e.response?.data || 'Lỗi!'); }
  };

  const handleContextMenu = (e, kh) => {
    e.preventDefault();
    setCtxMenu({ x: e.clientX, y: e.clientY, kh });
  };

  const handleContextAction = (action, kh) => {
    setCtxMenu(null);
    switch (action) {
      case 'view': setDetailKH(kh); break;
      case 'edit': openEdit(kh); break;
      case 'copy':
        navigator.clipboard.writeText(kh.maKhachHang).then(() => {
          setCopyMsg(`Đã sao chép: ${kh.maKhachHang}`);
          setTimeout(() => setCopyMsg(''), 2000);
        });
        break;
      case 'delete': del(kh.maKhachHang, kh.hoTen); break;
    }
  };

  return (
    <div className="page-shell" onClick={() => ctxMenu && setCtxMenu(null)}>
      {/* Header */}
      <div className="page-header">
        <div>
          <div className="page-label">Nhân sự &amp; Khách hàng</div>
          <h1 className="page-title flex items-center gap-2">
            <Users style={{ width: 20, height: 20, color: 'var(--text-secondary)' }} strokeWidth={2} />
            Quản lý Khách hàng
          </h1>
          <p className="page-subtitle">Chuột phải để xem tùy chọn · Double-click để xem hồ sơ</p>
        </div>
        <button onClick={openAdd} className="btn-primary">
          <Plus style={{ width: 15, height: 15 }} />Thêm khách hàng
        </button>
      </div>

      {/* KPI Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-[10px]">
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
          <input type="text" placeholder="Tìm tên, SĐT, CCCD, mã khách hàng..." value={kw}
            onChange={e => setKw(e.target.value)} />
          {kw && (
            <button onClick={() => setKw('')} className="filter-clear-btn">
              <X size={10} />
            </button>
          )}
        </div>

        <div className={`filter-select-wrap ${filterHang !== 'all' ? 'active' : ''}`}>
          <select value={filterHang} onChange={e => setFilterHang(e.target.value)}>
            <option value="all">Tất cả hạng KH</option>
            <option value="VIP">VIP</option>
            <option value="Thân thiết">Thân thiết</option>
            <option value="Thường">Thường</option>
          </select>
        </div>

        <div className={`filter-select-wrap ${filterQuoc !== 'all' ? 'active' : ''}`}>
          <select value={filterQuoc} onChange={e => setFilterQuoc(e.target.value)}>
            <option value="all">Tất cả quốc tịch</option>
            <option value="domestic">Trong nước</option>
            <option value="foreign">Quốc tế</option>
          </select>
        </div>

        {hasFilter && (
          <button onClick={clearFilters} className="filter-reset-btn">
            Xoá lọc
          </button>
        )}

        <span className="filter-result-count">{filtered.length}/{items.length} khách</span>
      </div>

      {/* Toast */}
      {copyMsg && (
        <div className="fixed bottom-5 right-5 z-50 flex items-center gap-2 px-4 py-2.5 bg-emerald-600 text-white text-sm font-bold rounded-xl shadow-2xl">
          <ClipboardCopy size={14} />{copyMsg}
        </div>
      )}

      {/* Table */}
      <div className="section-box">
        <table className="data-table">
          <thead>
            <tr>
              {['Mã KH', 'Khách hàng', 'Giấy tờ', 'Quốc tịch', 'Hạng', 'Số lần ở'].map(h => (
                <th key={h}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={6} className="py-16 text-center">
                <div className="spinner mx-auto mb-2" />
                <div style={{ color: 'var(--text-muted)', fontSize: 12 }}>Đang tải...</div>
              </td></tr>
            ) : filtered.length === 0 ? (
              <tr><td colSpan={6} className="py-14 text-center" style={{ color: 'var(--text-muted)', fontSize: 13 }}>
                Không có khách hàng nào phù hợp
              </td></tr>
            ) : filtered.map(kh => (
              <tr key={kh.maKhachHang} className="cursor-pointer group"
                onDoubleClick={() => setDetailKH(kh)}
                onContextMenu={e => handleContextMenu(e, kh)}>
                <td className="font-mono-data font-bold text-[12px]" style={{ color: 'var(--accent)' }}>{kh.maKhachHang}</td>
                <td>
                  <div className="flex items-center gap-2.5">
                    <div className="w-[26px] h-[26px] rounded-full flex items-center justify-center text-[10px] font-black shrink-0"
                      style={{ background: 'var(--accent-dim)', color: 'var(--accent)', border: '1px solid var(--accent-border)' }}>
                      {kh.hoTen?.charAt(0) || '?'}
                    </div>
                    <div>
                      <div className="font-semibold text-[13px]" style={{ color: 'var(--text-primary)' }}>{kh.hoTen}</div>
                      <div className="text-[11px] flex items-center gap-1.5" style={{ color: 'var(--text-secondary)' }}>
                        {kh.sdt ? <span className="flex items-center gap-0.5"><Phone size={9} />{kh.sdt}</span> : '—'}
                        {kh.email && <span> · {kh.email}</span>}
                      </div>
                    </div>
                  </div>
                </td>
                <td className="font-mono-data text-[11px]" style={{ color: 'var(--text-muted)' }}>
                  {kh.soHoChieu ? `HC: ${kh.soHoChieu}` : kh.cccd ? `CCCD: ${kh.cccd}` : '—'}
                </td>
                <td>
                  {kh.quocTich && kh.quocTich !== 'Việt Nam' && kh.quocTich !== 'Viet Nam' ? (
                    <span className="flex items-center gap-1 text-[11px] font-semibold" style={{ color: 'var(--accent)' }}>
                      <Globe size={10} />{kh.quocTich}
                    </span>
                  ) : (
                    <span className="text-[11px]" style={{ color: 'var(--text-muted)' }}>Việt Nam</span>
                  )}
                </td>
                <td><HangBadge hang={kh.hangKhachHang} /></td>
                <td className="row-actions-cell text-center tabular-nums font-semibold" style={{ color: 'var(--text-secondary)' }}>
                  {kh.soLanO || 0}

                  {/* Hover Actions Panel */}
                  <div className="row-hover-actions">
                    <button onClick={(e) => { e.stopPropagation(); setDetailKH(kh); }} className="row-action-btn" title="Xem hồ sơ"><Eye size={12} /></button>
                    <button onClick={(e) => { e.stopPropagation(); openEdit(kh); }} className="row-action-btn" title="Chỉnh sửa"><Edit2 size={11} /></button>
                    <button onClick={(e) => { e.stopPropagation(); del(kh.maKhachHang, kh.hoTen); }} className="row-action-btn danger" title="Xóa"><Trash2 size={11} /></button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!loading && items.length > 0 && (
          <div className="table-footer">
            <span>Tổng cộng: <strong style={{ color: 'var(--text-primary)' }}>{filtered.length} khách hàng</strong></span>
            <span>Double-click để xem hồ sơ · Chuột phải để tùy chọn</span>
          </div>
        )}
      </div>

      {/* Context Menu */}
      {ctxMenu && <ContextMenu menu={ctxMenu} onAction={handleContextAction} onClose={() => setCtxMenu(null)} />}

      {/* Detail Modal */}
      {detailKH && <DetailModal kh={detailKH} onClose={() => setDetailKH(null)} onEdit={() => openEdit(detailKH)} />}

      {/* Add/Edit Modal */}
      {showModal && (
        <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) setShowModal(false); }}>
          <div className="modal-panel" style={{ maxWidth: 500 }}>
            <div className="modal-header">
              <h3 className="modal-title">{editing ? 'Cập nhật' : 'Thêm'} Khách hàng</h3>
              <button onClick={() => setShowModal(false)} className="row-action-btn"><X style={{ width: 13, height: 13 }} /></button>
            </div>
            <div className="modal-body" style={{ maxHeight: '70vh', overflowY: 'auto' }}>
              {err && <div className="alert-strip alert-error text-[12px]" style={{ color: '#f87171' }}>{err}</div>}
              <form id="khachhang-form" onSubmit={handleSubmit} className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="label-style">Mã KH</label>
                    <input type="text" disabled={!!editing} value={form.maKhachHang} onChange={e => setForm({ ...form, maKhachHang: e.target.value })} placeholder="Tự sinh"
                      className="input-style" style={{ opacity: editing ? 0.5 : 1 }} />
                  </div>
                  <div>
                    <label className="label-style">Giới tính</label>
                    <select value={form.gioiTinh} onChange={e => setForm({ ...form, gioiTinh: e.target.value })} className="input-style">
                      <option>Nam</option><option>Nữ</option><option>Khác</option>
                    </select>
                  </div>
                </div>
                <div>
                  <label className="label-style">Họ tên *</label>
                  <input required type="text" value={form.hoTen} onChange={e => setForm({ ...form, hoTen: e.target.value })} placeholder="Nguyễn Văn An" className="input-style" />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="label-style">Số điện thoại</label>
                    <input type="text" value={form.sdt} onChange={e => setForm({ ...form, sdt: e.target.value })} className="input-style" />
                  </div>
                  <div>
                    <label className="label-style">Quốc tịch</label>
                    <input type="text" value={form.quocTich} onChange={e => setForm({ ...form, quocTich: e.target.value })} className="input-style" />
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="label-style">Loại giấy tờ</label>
                    <select value={form.loaiGiayTo} onChange={e => setForm({ ...form, loaiGiayTo: e.target.value })} className="input-style">
                      <option value="CCCD">CCCD</option><option value="CMND">CMND</option><option value="PASSPORT">Hộ chiếu</option>
                    </select>
                  </div>
                  <div>
                    <label className="label-style">CCCD</label>
                    <input type="text" value={form.cccd} onChange={e => setForm({ ...form, cccd: e.target.value })} className="input-style" />
                  </div>
                </div>
                {form.loaiGiayTo === 'PASSPORT' && (
                  <div>
                    <label className="label-style">Số hộ chiếu</label>
                    <input type="text" value={form.soHoChieu} onChange={e => setForm({ ...form, soHoChieu: e.target.value })} className="input-style" />
                  </div>
                )}
              </form>
            </div>
            <div className="modal-footer">
              <button type="button" onClick={() => setShowModal(false)} className="btn-ghost text-[13px]">Hủy</button>
              <button type="submit" form="khachhang-form" className="btn-primary text-[13px]">Lưu</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default KhachHangPage;
