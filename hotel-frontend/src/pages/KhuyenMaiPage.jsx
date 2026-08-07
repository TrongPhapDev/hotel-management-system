import React, { useEffect, useState } from 'react';
import { khuyenMaiAPI } from '../api/api';
import { Tag, Plus, Edit2, Trash2, X, Search, Eye } from 'lucide-react';
import { RowContextMenu, useContextMenu } from '../components/ContextMenu';

const fmtDate = (dt) => {
  if (!dt) return '—';
  const d = new Date(dt);
  const pad = (n) => String(n).padStart(2, '0');
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

const fmtMoney = (n) => (n || 0).toLocaleString('vi-VN') + ' ₫';

const STATUS_BADGE = {
  'HOẠT ĐỘNG':   'badge-available',
  'SẮP DIỄN RA': 'badge-occupied',
  'HẾT HẠN':     'badge-maintenance',
  'HẾT LƯỢT':    'badge-maintenance',
  'TẠM DỪNG':    'badge-cleaning',
};

const KhuyenMaiPage = () => {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [err, setErr] = useState('');
  const { ctxMenu, openCtxMenu, closeCtxMenu } = useContextMenu();

  const [searchKw, setSearchKw] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('ALL');
  const [filterLoaiGiam, setFilterLoaiGiam] = useState('ALL');

  const init = {
    maKhuyenMai: '', tenKhuyenMai: '', loaiGiam: 'PERCENT', giaTriGiam: 10,
    ngayBatDau: '', ngayKetThuc: '', dieuKienApDung: '',
    soLuong: 100, dieuKienToiThieu: 0, trangThai: true
  };
  const [form, setForm] = useState(init);

  const fetchData = () => {
    setLoading(true);
    khuyenMaiAPI.getAll().then(r => setItems(r.data)).catch(console.error).finally(() => setLoading(false));
  };
  useEffect(() => { fetchData(); }, []);

  const openAdd = () => {
    setEditing(null);
    setForm({
      ...init,
      ngayBatDau: new Date().toISOString().substring(0, 16),
      ngayKetThuc: new Date(Date.now() + 30 * 24 * 3600 * 1000).toISOString().substring(0, 16)
    });
    setErr(''); setShowModal(true);
  };

  const openEdit = (km) => {
    setEditing(km.maKhuyenMai);
    setForm({ ...init, ...km, ngayBatDau: km.ngayBatDau?.substring(0, 16) || '', ngayKetThuc: km.ngayKetThuc?.substring(0, 16) || '' });
    setErr(''); setShowModal(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault(); setErr('');
    try {
      if (editing) await khuyenMaiAPI.update(editing, form);
      else await khuyenMaiAPI.create(form);
      setShowModal(false); fetchData();
    } catch (e) { setErr(e.response?.data || 'Có lỗi xảy ra!'); }
  };

  const del = async (id) => {
    const km = items.find(x => x.maKhuyenMai === id);
    if (km && km.daDung > 0) { alert('Không thể xóa khuyến mãi đã được sử dụng!'); return; }
    if (!confirm(`Xóa khuyến mãi: ${id}?`)) return;
    try { await khuyenMaiAPI.delete(id); fetchData(); } catch (e) { alert(e.response?.data || 'Lỗi!'); }
  };

  const determineStatus = (km) => {
    if (!km.trangThai) return 'TẠM DỪNG';
    const now = new Date();
    if (now < new Date(km.ngayBatDau)) return 'SẮP DIỄN RA';
    if (now > new Date(km.ngayKetThuc)) return 'HẾT HẠN';
    if ((km.daDung || 0) >= (km.soLuong || 0)) return 'HẾT LƯỢT';
    return 'HOẠT ĐỘNG';
  };

  const handleCtxAction = (action, km) => {
    switch (action) {
      case 'edit': openEdit(km); break;
      case 'delete': del(km.maKhuyenMai); break;
      case 'toggle_status':
        khuyenMaiAPI.update(km.maKhuyenMai, { ...km, trangThai: !km.trangThai })
          .then(fetchData).catch(() => alert('Không thể đổi trạng thái!'));
        break;
    }
  };

  const getCtxItems = (km) => [
    { label: 'Chỉnh sửa khuyến mãi', action: 'edit' },
    { label: km.trangThai ? 'Tạm dừng hoạt động' : 'Kích hoạt', action: 'toggle_status' },
    { separator: true },
    { label: 'Xóa khuyến mãi', action: 'delete', danger: true },
  ];

  const filteredItems = items.filter(km => {
    const kw = searchKw.trim().toLowerCase();
    const matchKw = !kw || km.maKhuyenMai?.toLowerCase().includes(kw) || km.tenKhuyenMai?.toLowerCase().includes(kw);
    const status = determineStatus(km);
    const matchStatus = selectedStatus === 'ALL' || status === selectedStatus;
    const matchLoai = filterLoaiGiam === 'ALL' || km.loaiGiam === filterLoaiGiam;
    return matchKw && matchStatus && matchLoai;
  });

  const hasFilter = searchKw || selectedStatus !== 'ALL' || filterLoaiGiam !== 'ALL';
  const clearFilters = () => { setSearchKw(''); setSelectedStatus('ALL'); setFilterLoaiGiam('ALL'); };

  return (
    <div className="page-shell" onClick={() => closeCtxMenu()}>
      {/* Header */}
      <div className="page-header">
        <div>
          <div className="page-label">
            Vận hành
          </div>
          <h1 className="page-title flex items-center gap-2">
            <Tag style={{ width: 20, height: 20, color: 'var(--text-secondary)' }} strokeWidth={2} />
            Quản lý Khuyến mãi & Voucher
          </h1>
          <p className="page-subtitle">Quản lý các chương trình ưu đãi và mã giảm giá toàn hệ thống</p>
        </div>
        <button onClick={openAdd} className="btn-primary">
          <Plus style={{ width: 15, height: 15 }} />
          Thêm Khuyến Mãi
        </button>
      </div>

      {/* Filter Bar */}
      <div className="filter-bar">
        <div className="filter-search">
          <Search className="filter-search-icon" style={{ width: 14, height: 14 }} />
          <input type="text" placeholder="Tìm mã KM, tên chương trình..."
            value={searchKw} onChange={e => setSearchKw(e.target.value)} />
          {searchKw && (
            <button onClick={() => setSearchKw('')} className="filter-clear-btn">
              <X style={{ width: 10, height: 10 }} />
            </button>
          )}
        </div>

        <div className={`filter-select-wrap ${selectedStatus !== 'ALL' ? 'active' : ''}`}>
          <select value={selectedStatus} onChange={e => setSelectedStatus(e.target.value)}>
            <option value="ALL">Tất cả trạng thái</option>
            <option value="HOẠT ĐỘNG">Hoạt động</option>
            <option value="TẠM DỪNG">Tạm dừng</option>
            <option value="HẾT HẠN">Hết hạn</option>
            <option value="SẮP DIỄN RA">Sắp diễn ra</option>
            <option value="HẾT LƯỢT">Hết lượt</option>
          </select>
        </div>

        <div className={`filter-select-wrap ${filterLoaiGiam !== 'ALL' ? 'active' : ''}`}>
          <select value={filterLoaiGiam} onChange={e => setFilterLoaiGiam(e.target.value)}>
            <option value="ALL">Tất cả loại giảm</option>
            <option value="PERCENT">Phần trăm (%)</option>
            <option value="FIXED">Cố định (₫)</option>
          </select>
        </div>

        {hasFilter && (
          <button onClick={clearFilters} className="filter-reset-btn">
            Xoá lọc
          </button>
        )}

        <span className="filter-result-count">{filteredItems.length}/{items.length} khuyến mãi</span>
      </div>

      {/* Table */}
      <div className="section-box overflow-x-auto">
        <table className="data-table">
          <thead>
            <tr>
              {['Mã KM', 'Tên chương trình', 'Loại', 'Giá trị', 'Bắt đầu', 'Kết thúc', 'Đơn tối thiểu', 'Đã dùng / Tổng', 'Trạng thái'].map(h => (
                <th key={h}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={9} className="py-16 text-center">
                <div className="spinner mx-auto mb-2" />
                <div style={{ color: 'var(--text-muted)', fontSize: 12 }}>Đang tải...</div>
              </td></tr>
            ) : filteredItems.length === 0 ? (
              <tr><td colSpan={9} className="py-14 text-center" style={{ color: 'var(--text-muted)', fontSize: 13 }}>Không có dữ liệu</td></tr>
            ) : filteredItems.map(km => {
              const status = determineStatus(km);
              return (
                <tr key={km.maKhuyenMai} className="cursor-pointer group"
                  onDoubleClick={() => openEdit(km)}
                  onContextMenu={e => { e.stopPropagation(); openCtxMenu(e, km); }}>
                  <td className="font-mono-data font-bold text-[12px]" style={{ color: 'var(--accent)' }}>{km.maKhuyenMai}</td>
                  <td className="font-medium text-[13px]" style={{ color: 'var(--text-primary)' }}>{km.tenKhuyenMai || '—'}</td>
                  <td className="text-[11px]" style={{ color: 'var(--text-muted)' }}>{km.loaiGiam === 'PERCENT' ? 'PHẦN TRĂM (%)' : 'CỐ ĐỊNH (₫)'}</td>
                  <td className="font-bold tabular-nums" style={{ color: 'var(--accent)' }}>
                    {km.loaiGiam === 'PERCENT' ? `${km.giaTriGiam}%` : fmtMoney(km.giaTriGiam)}
                  </td>
                  <td className="font-mono-data text-[11px]" style={{ color: 'var(--text-muted)' }}>{fmtDate(km.ngayBatDau)}</td>
                  <td className="font-mono-data text-[11px]" style={{ color: 'var(--text-muted)' }}>{fmtDate(km.ngayKetThuc)}</td>
                  <td className="tabular-nums" style={{ color: 'var(--text-secondary)' }}>{fmtMoney(km.dieuKienToiThieu)}</td>
                  <td className="tabular-nums font-bold" style={{ color: (km.daDung || 0) >= (km.soLuong || 0) ? '#f87171' : 'var(--text-secondary)' }}>
                    {km.daDung || 0} / {km.soLuong || 0}
                  </td>
                  <td className="row-actions-cell">
                    <span className={`badge ${STATUS_BADGE[status] || ''}`}>{status}</span>

                    {/* Hover Actions Panel */}
                    <div className="row-hover-actions">
                      <button onClick={(e) => { e.stopPropagation(); openEdit(km); }} className="row-action-btn" title="Xem chi tiết"><Eye style={{ width: 12, height: 12 }} /></button>
                      <button onClick={(e) => { e.stopPropagation(); openEdit(km); }} className="row-action-btn" title="Chỉnh sửa"><Edit2 style={{ width: 11, height: 11 }} /></button>
                      <button onClick={(e) => { e.stopPropagation(); del(km.maKhuyenMai); }} className="row-action-btn danger" title="Xóa"><Trash2 style={{ width: 11, height: 11 }} /></button>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Modal */}
      {showModal && (
        <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) setShowModal(false); }}>
          <div className="modal-panel">
            <div className="modal-header">
              <h3 className="modal-title">{editing ? 'Sửa Khuyến Mãi' : 'Thêm Khuyến Mãi Mới'}</h3>
              <button onClick={() => setShowModal(false)} className="row-action-btn"><X style={{ width: 13, height: 13 }} /></button>
            </div>
            <div className="modal-body" style={{ maxHeight: '70vh', overflowY: 'auto' }}>
              {err && <div className="alert-strip alert-error text-[12px]" style={{ color: '#f87171' }}>{err}</div>}
              <form id="khuyenmai-form" onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="label-style">Mã khuyến mãi (Voucher Code) *</label>
                  <input required type="text" disabled={!!editing} value={form.maKhuyenMai}
                    onChange={e => setForm({ ...form, maKhuyenMai: e.target.value })}
                    className="input-style" placeholder="VD: SUMMER2025"
                    style={{ opacity: editing ? 0.5 : 1 }} />
                </div>
                <div>
                  <label className="label-style">Tên chương trình *</label>
                  <input required type="text" value={form.tenKhuyenMai || ''}
                    onChange={e => setForm({ ...form, tenKhuyenMai: e.target.value })}
                    className="input-style" placeholder="VD: Giảm 10% mùa hè rực rỡ" />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="label-style">Loại giảm</label>
                    <select value={form.loaiGiam} onChange={e => setForm({ ...form, loaiGiam: e.target.value })} className="input-style">
                      <option value="PERCENT">Phần trăm (%)</option>
                      <option value="FIXED">Cố định (₫)</option>
                    </select>
                  </div>
                  <div>
                    <label className="label-style">Giá trị giảm *</label>
                    <input required type="number" min={0} value={form.giaTriGiam}
                      onChange={e => setForm({ ...form, giaTriGiam: parseFloat(e.target.value) })} className="input-style" />
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="label-style">Ngày bắt đầu *</label>
                    <input required type="datetime-local" value={form.ngayBatDau}
                      onChange={e => setForm({ ...form, ngayBatDau: e.target.value })} className="input-style" />
                  </div>
                  <div>
                    <label className="label-style">Ngày kết thúc *</label>
                    <input required type="datetime-local" value={form.ngayKetThuc}
                      onChange={e => setForm({ ...form, ngayKetThuc: e.target.value })} className="input-style" />
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="label-style">Số lượng phát hành *</label>
                    <input required type="number" min={1} value={form.soLuong || 100}
                      onChange={e => setForm({ ...form, soLuong: parseInt(e.target.value) })} className="input-style" />
                  </div>
                  <div>
                    <label className="label-style">Đơn tối thiểu (VNĐ)</label>
                    <input type="number" min={0} value={form.dieuKienToiThieu || 0}
                      onChange={e => setForm({ ...form, dieuKienToiThieu: parseFloat(e.target.value) })} className="input-style" />
                  </div>
                </div>
                <div>
                  <label className="label-style">Điều kiện áp dụng</label>
                  <textarea value={form.dieuKienApDung || ''} onChange={e => setForm({ ...form, dieuKienApDung: e.target.value })}
                    rows={2} className="input-style resize-none" placeholder="VD: Áp dụng cho đơn phòng trên 500k..." />
                </div>
                <div className="flex items-center gap-2">
                  <input type="checkbox" id="km-trangThai" checked={form.trangThai}
                    onChange={e => setForm({ ...form, trangThai: e.target.checked })}
                    className="w-4 h-4 rounded cursor-pointer" style={{ accentColor: 'var(--accent)' }} />
                  <label htmlFor="km-trangThai" className="text-[13px] font-medium cursor-pointer" style={{ color: 'var(--text-primary)' }}>
                    Kích hoạt chương trình này
                  </label>
                </div>
              </form>
            </div>
            <div className="modal-footer">
              <button type="button" onClick={() => setShowModal(false)} className="btn-ghost text-[13px]">Hủy</button>
              <button type="submit" form="khuyenmai-form" className="btn-primary text-[13px]">Lưu</button>
            </div>
          </div>
        </div>
      )}

      {/* Context Menu */}
      {ctxMenu && (
        <RowContextMenu menu={ctxMenu} items={getCtxItems(ctxMenu.item)} onAction={handleCtxAction} onClose={closeCtxMenu}
          title={ctxMenu.item?.tenKhuyenMai} subtitle={ctxMenu.item?.maKhuyenMai} />
      )}
    </div>
  );
};

export default KhuyenMaiPage;
