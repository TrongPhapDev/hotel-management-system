import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { phongAPI, loaiPhongAPI } from '../api/api';
import { BedDouble, Plus, Edit2, Trash2, X, Search, Eye } from 'lucide-react';
import { RowContextMenu, useContextMenu } from '../components/ContextMenu';

const TRANG_THAI = ['AVAILABLE', 'OCCUPIED', 'MAINTENANCE', 'CLEANING'];

const TT_CONFIG = {
  AVAILABLE:   { label: 'Có sẵn',     cls: 'badge-available',    dot: 'var(--status-avail-dot)' },
  OCCUPIED:    { label: 'Đang thuê',   cls: 'badge-occupied',     dot: 'var(--status-occup-dot)' },
  MAINTENANCE: { label: 'Bảo trì',    cls: 'badge-maintenance',  dot: 'var(--status-maint-dot)' },
  CLEANING:    { label: 'Vệ sinh',    cls: 'badge-cleaning',     dot: 'var(--status-clean-dot)' },
};

const StatusBadge = ({ tt }) => {
  const cfg = TT_CONFIG[tt] || { label: tt, cls: '', dot: 'var(--text-muted)' };
  return (
    <span className={`badge ${cfg.cls}`}>
      <span className="badge-dot" style={{ background: cfg.dot }} />
      {cfg.label}
    </span>
  );
};

const PhongPage = () => {
  const [phongs, setPhongs] = useState([]);
  const [loaiPhongs, setLoaiPhongs] = useState([]);
  const [filterTT, setFilterTT] = useState('');
  const [filterLoai, setFilterLoai] = useState('');
  const [filterTang, setFilterTang] = useState('');
  const [kw, setKw] = useState('');
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [detailPhong, setDetailPhong] = useState(null);
  const [form, setForm] = useState({ maPhong: '', tang: 1, trangThai: 'AVAILABLE', loaiPhong: { maLoaiPhong: '' }, huongNhin: '' });
  const [err, setErr] = useState('');
  const { ctxMenu, openCtxMenu, closeCtxMenu } = useContextMenu();

  const fetchData = useCallback(() => {
    setLoading(true);
    Promise.all([
      phongAPI.getAll(),
      loaiPhongAPI.getAll()
    ]).then(([p, l]) => { setPhongs(p.data); setLoaiPhongs(l.data); })
      .catch(console.error).finally(() => setLoading(false));
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  // Unique floors from data
  const uniqueTangs = useMemo(() => [...new Set(phongs.map(p => p.tang))].sort((a,b)=>a-b), [phongs]);

  const filtered = phongs.filter(p => {
    const matchKw = !kw || p.maPhong?.toLowerCase().includes(kw.toLowerCase()) ||
      p.loaiPhong?.tenLoaiPhong?.toLowerCase().includes(kw.toLowerCase());
    const matchTT = !filterTT || p.trangThai === filterTT;
    const matchLoai = !filterLoai || p.loaiPhong?.maLoaiPhong === filterLoai;
    const matchTang = !filterTang || String(p.tang) === filterTang;
    return matchKw && matchTT && matchLoai && matchTang;
  });

  const hasFilter = kw || filterTT || filterLoai || filterTang;
  const clearFilters = () => { setKw(''); setFilterTT(''); setFilterLoai(''); setFilterTang(''); };

  const kpiData = [
    { label: 'Có sẵn',    value: phongs.filter(p => p.trangThai === 'AVAILABLE').length },
    { label: 'Đang thuê', value: phongs.filter(p => p.trangThai === 'OCCUPIED').length },
    { label: 'Vệ sinh',   value: phongs.filter(p => p.trangThai === 'CLEANING').length },
    { label: 'Bảo trì',   value: phongs.filter(p => p.trangThai === 'MAINTENANCE').length },
    { label: 'Tổng cộng', value: phongs.length },
  ];

  const openAdd = () => { setEditing(null); setForm({ maPhong: '', tang: 1, trangThai: 'AVAILABLE', loaiPhong: { maLoaiPhong: '' }, huongNhin: '' }); setErr(''); setShowModal(true); };
  const openEdit = (p) => { setEditing(p.maPhong); setForm({ ...p, loaiPhong: p.loaiPhong || { maLoaiPhong: '' } }); setErr(''); setShowModal(true); setDetailPhong(null); };

  const handleSubmit = async (e) => {
    e.preventDefault(); setErr('');
    try {
      if (editing) await phongAPI.update(editing, form);
      else await phongAPI.create(form);
      setShowModal(false); fetchData();
    } catch (e) { setErr(e.response?.data || 'Lỗi!'); }
  };

  const handleDelete = async (p) => {
    if (p.trangThai === 'OCCUPIED' || p.trangThai === 'CLEANING') {
      alert('Không thể xóa phòng đang có khách hoặc đang vệ sinh!'); return;
    }
    if (!confirm(`Xóa phòng ${p.maPhong}? Hành động này không thể hoàn tác!`)) return;
    try { await phongAPI.delete(p.maPhong); fetchData(); } catch (e) { alert(e.response?.data || 'Lỗi!'); }
  };

  const handleCtxAction = (action, p) => {
    switch (action) {
      case 'view': setDetailPhong(p); break;
      case 'edit': openEdit(p); break;
      case 'add': openAdd(); break;
      case 'delete': handleDelete(p); break;
    }
  };

  const getCtxItems = (p) => [
    { label: 'Xem chi tiết / Sửa', action: 'view' },
    { label: 'Chỉnh sửa thông tin', action: 'edit' },
    { label: 'Thêm phòng mới', action: 'add' },
    { separator: true },
    { label: 'Xóa phòng này', action: 'delete', danger: true, disabled: p?.trangThai === 'OCCUPIED' || p?.trangThai === 'CLEANING' },
  ];

  return (
    <div className="page-shell" onClick={closeCtxMenu}>
      {/* Header */}
      <div className="page-header">
        <div>
          <div className="page-label">
            Danh mục
          </div>
          <h1 className="page-title flex items-center gap-2">
            <BedDouble style={{ width: 20, height: 20, color: 'var(--text-secondary)' }} strokeWidth={2} />
            Quản lý Phòng
          </h1>
          <p className="page-subtitle">Chuột phải để tùy chọn · Double-click để sửa thông tin</p>
        </div>
        <button onClick={openAdd} className="btn-primary">
          <Plus style={{ width: 15, height: 15 }} />
          Thêm phòng
        </button>
      </div>

      {/* KPI */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-[10px]">
        {kpiData.map(kpi => (
          <div key={kpi.label} className="kpi-card">
            <span className="kpi-label">{kpi.label}</span>
            <span className="kpi-value">{kpi.value}</span>
          </div>
        ))}
      </div>

      {/* Filter Toolbar */}
      <div className="filter-bar">
        {/* Search */}
        <div className="filter-search">
          <Search className="filter-search-icon" style={{ width: 14, height: 14 }} />
          <input
            placeholder="Tìm số phòng, loại phòng..."
            value={kw}
            onChange={e => setKw(e.target.value)}
          />
          {kw && (
            <button onClick={() => setKw('')} className="filter-clear-btn">
              <X style={{ width: 10, height: 10 }} />
            </button>
          )}
        </div>

        <div className={`filter-select-wrap ${filterTT ? 'active' : ''}`}>
          <select value={filterTT} onChange={e => setFilterTT(e.target.value)}>
            <option value="">Tất cả trạng thái</option>
            {TRANG_THAI.map(tt => <option key={tt} value={tt}>{TT_CONFIG[tt]?.label || tt}</option>)}
          </select>
        </div>

        <div className={`filter-select-wrap ${filterLoai ? 'active' : ''}`}>
          <select value={filterLoai} onChange={e => setFilterLoai(e.target.value)}>
            <option value="">Tất cả loại phòng</option>
            {loaiPhongs.map(lp => <option key={lp.maLoaiPhong} value={lp.maLoaiPhong}>{lp.tenLoaiPhong}</option>)}
          </select>
        </div>

        <div className={`filter-select-wrap ${filterTang ? 'active' : ''}`}>
          <select value={filterTang} onChange={e => setFilterTang(e.target.value)}>
            <option value="">Tất cả tầng</option>
            {uniqueTangs.map(t => <option key={t} value={String(t)}>Tầng {t}</option>)}
          </select>
        </div>

        {hasFilter && (
          <button onClick={clearFilters} className="filter-reset-btn">
            Xoá lọc
          </button>
        )}

        <span className="filter-result-count">{filtered.length}/{phongs.length} phòng</span>
      </div>

      {/* Table */}
      <div className="section-box">
        <table className="data-table">
          <thead>
            <tr>
              {['Số phòng', 'Loại phòng', 'View', 'Tầng', 'Sức chứa', 'Khách hiện tại', 'Trạng thái'].map(h => (
                <th key={h}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={7} className="py-16 text-center">
                  <div className="spinner mx-auto mb-2" />
                  <div style={{ color: 'var(--text-muted)', fontSize: 12 }}>Đang tải...</div>
                </td>
              </tr>
            ) : filtered.length === 0 ? (
              <tr>
                <td colSpan={7} className="py-16 text-center" style={{ color: 'var(--text-muted)', fontSize: 13 }}>
                  <BedDouble style={{ width: 32, height: 32, opacity: 0.2, margin: '0 auto 8px' }} />
                  Không có phòng nào
                </td>
              </tr>
            ) : filtered.map(p => (
              <tr
                key={p.maPhong}
                className="cursor-pointer group"
                onDoubleClick={() => setDetailPhong(p)}
                onContextMenu={e => { e.stopPropagation(); openCtxMenu(e, p); }}
              >
                <td className="font-black" style={{ color: 'var(--accent)' }}>P.{p.maPhong}</td>
                <td style={{ color: 'var(--text-secondary)' }}>{p.loaiPhong?.tenLoaiPhong || '—'}</td>
                <td style={{ color: 'var(--text-muted)', fontSize: 12 }}>{p.huongNhin?.tenHuongNhin || '—'}</td>
                <td className="text-center" style={{ color: 'var(--text-secondary)' }}>Tầng {p.tang}</td>
                <td className="text-center" style={{ color: 'var(--text-secondary)' }}>{p.loaiPhong?.sucChua ? `${p.loaiPhong.sucChua} ng.` : '—'}</td>
                <td style={{ color: 'var(--text-secondary)' }}>{p.tenKhachHienTai || '—'}</td>
                <td className="row-actions-cell">
                  <StatusBadge tt={p.trangThai} />

                  {/* Hover Actions Panel */}
                  <div className="row-hover-actions">
                    <button onClick={(e) => { e.stopPropagation(); setDetailPhong(p); }} className="row-action-btn" title="Xem chi tiết">
                      <Eye style={{ width: 12, height: 12 }} />
                    </button>
                    <button onClick={(e) => { e.stopPropagation(); openEdit(p); }} className="row-action-btn" title="Chỉnh sửa">
                      <Edit2 style={{ width: 11, height: 11 }} />
                    </button>
                    <button onClick={(e) => { e.stopPropagation(); handleDelete(p); }} className="row-action-btn danger" title="Xóa">
                      <Trash2 style={{ width: 11, height: 11 }} />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!loading && filtered.length > 0 && (
          <div className="table-footer">
            <span>Tổng <strong style={{ color: 'var(--text-primary)' }}>{filtered.length} phòng</strong></span>
            <span>Double-click để sửa · Chuột phải để tùy chọn</span>
          </div>
        )}
      </div>

      {/* Context Menu */}
      {ctxMenu && (
        <RowContextMenu
          menu={ctxMenu}
          items={getCtxItems(ctxMenu.item)}
          onAction={handleCtxAction}
          onClose={closeCtxMenu}
          title={`Phòng ${ctxMenu.item?.maPhong}`}
          subtitle={ctxMenu.item?.loaiPhong?.tenLoaiPhong}
        />
      )}

      {/* Detail Modal */}
      {detailPhong && (
        <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) setDetailPhong(null); }}>
          <div className="modal-panel max-w-sm">
            <div className="modal-header">
              <h3 className="modal-title">Chi tiết phòng {detailPhong.maPhong}</h3>
              <button onClick={() => setDetailPhong(null)} className="row-action-btn">
                <X style={{ width: 13, height: 13 }} />
              </button>
            </div>
            <div className="modal-body">
              <StatusBadge tt={detailPhong.trangThai} />
              <div className="grid grid-cols-2 gap-2 mt-3">
                {[
                  ['Loại phòng', detailPhong.loaiPhong?.tenLoaiPhong || '—'],
                  ['Tầng', `Tầng ${detailPhong.tang}`],
                  ['Hướng nhìn', detailPhong.huongNhin?.tenHuongNhin || '—'],
                  ['Sức chứa', detailPhong.loaiPhong?.sucChua ? `${detailPhong.loaiPhong.sucChua} người` : '—'],
                  ['Khách hiện tại', detailPhong.tenKhachHienTai || '—'],
                ].map(([k, v]) => (
                  <div key={k} className="rounded-xl p-3" style={{ background: 'var(--bg-elevated)' }}>
                    <div className="text-[9px] font-bold uppercase tracking-wider mb-1" style={{ color: 'var(--text-muted)' }}>{k}</div>
                    <div className="text-[13px] font-semibold" style={{ color: 'var(--text-primary)' }}>{v}</div>
                  </div>
                ))}
              </div>
            </div>
            <div className="modal-footer">
              <button onClick={() => setDetailPhong(null)} className="btn-ghost text-[13px]">Đóng</button>
              <button onClick={() => openEdit(detailPhong)} className="btn-primary text-[13px]">Chỉnh sửa</button>
            </div>
          </div>
        </div>
      )}

      {/* Add/Edit Modal */}
      {showModal && (
        <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) setShowModal(false); }}>
          <div className="modal-panel">
            <div className="modal-header">
              <h3 className="modal-title">{editing ? 'Cập nhật Phòng' : 'Thêm Phòng Mới'}</h3>
              <button onClick={() => setShowModal(false)} className="row-action-btn">
                <X style={{ width: 13, height: 13 }} />
              </button>
            </div>
            <div className="modal-body">
              {err && <div className="alert-strip alert-error text-[12px]" style={{ color: '#f87171' }}>{err}</div>}
              <form id="phong-form" onSubmit={handleSubmit} className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="label-style">Mã phòng *</label>
                    <input type="text" required disabled={!!editing} value={form.maPhong}
                      onChange={e => setForm({ ...form, maPhong: e.target.value })} placeholder="P101"
                      className="input-style" style={{ opacity: editing ? 0.5 : 1 }} />
                  </div>
                  <div>
                    <label className="label-style">Tầng</label>
                    <input type="number" min={1} value={form.tang}
                      onChange={e => setForm({ ...form, tang: parseInt(e.target.value) })}
                      className="input-style" />
                  </div>
                </div>
                <div>
                  <label className="label-style">Loại phòng</label>
                  <select value={form.loaiPhong?.maLoaiPhong || ''}
                    onChange={e => setForm({ ...form, loaiPhong: { maLoaiPhong: e.target.value } })}
                    className="input-style">
                    <option value="">-- Chọn loại phòng --</option>
                    {loaiPhongs.map(lp => <option key={lp.maLoaiPhong} value={lp.maLoaiPhong}>{lp.tenLoaiPhong}</option>)}
                  </select>
                </div>
                <div>
                  <label className="label-style">Trạng thái</label>
                  <select value={form.trangThai}
                    onChange={e => setForm({ ...form, trangThai: e.target.value })}
                    className="input-style">
                    {TRANG_THAI.map(tt => <option key={tt} value={tt}>{TT_CONFIG[tt]?.label || tt}</option>)}
                  </select>
                </div>
              </form>
            </div>
            <div className="modal-footer">
              <button type="button" onClick={() => setShowModal(false)} className="btn-ghost text-[13px]">Hủy</button>
              <button type="submit" form="phong-form" className="btn-primary text-[13px]">Lưu</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PhongPage;
