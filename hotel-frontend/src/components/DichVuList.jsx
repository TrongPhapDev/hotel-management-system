import React, { useState, useEffect } from 'react';
import { dichVuAPI } from '../api/api';
import { Search, Plus, Edit2, Trash2, SlidersHorizontal, AlertTriangle, X, Eye } from 'lucide-react';
import { RowContextMenu, useContextMenu } from './ContextMenu';

const DichVuList = () => {
  const [dichVus, setDichVus] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedType, setSelectedType] = useState('Tất cả loại');
  const [filterTrangThai, setFilterTrangThai] = useState('all'); // 'all' | 'active' | 'inactive'
  const [avgPrice, setAvgPrice] = useState(0);
  const { ctxMenu, openCtxMenu, closeCtxMenu } = useContextMenu();

  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({
    maDichVu: '', tenDichVu: '', loai: 'Ăn uống',
    donGia: 0, donViTinh: 'chai', soLuongMin: 1, moTa: '', trangThai: 1
  });
  const [errorMessage, setErrorMessage] = useState('');

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await dichVuAPI.getAll();
      setDichVus(res.data);
      const avgRes = await dichVuAPI.giaTrungBinh();
      setAvgPrice(avgRes.data);
      setErrorMessage('');
    } catch (err) {
      setErrorMessage('Không thể kết nối đến Backend Spring Boot.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  // Client-side filtering
  const dichVusFiltered = dichVus.filter(dv => {
    const matchKw = !searchTerm || dv.tenDichVu?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      dv.maDichVu?.toLowerCase().includes(searchTerm.toLowerCase());
    const matchType = selectedType === 'Tất cả loại' || dv.loai === selectedType;
    const matchTT = filterTrangThai === 'all' ||
      (filterTrangThai === 'active' && dv.trangThai === 1) ||
      (filterTrangThai === 'inactive' && dv.trangThai !== 1);
    return matchKw && matchType && matchTT;
  });

  const hasFilter = searchTerm || selectedType !== 'Tất cả loại' || filterTrangThai !== 'all';
  const clearFilters = () => { setSearchTerm(''); setSelectedType('Tất cả loại'); setFilterTrangThai('all'); };

  const handleOpenAdd = () => {
    setEditingId(null);
    setFormData({ maDichVu: '', tenDichVu: '', loai: 'Ăn uống', donGia: 0, donViTinh: 'chai', soLuongMin: 1, moTa: '', trangThai: 1 });
    setErrorMessage(''); setShowModal(true);
  };

  const handleOpenEdit = (dv) => {
    setEditingId(dv.maDichVu);
    setFormData({ maDichVu: dv.maDichVu, tenDichVu: dv.tenDichVu, loai: dv.loai || 'Ăn uống', donGia: dv.donGia, donViTinh: dv.donViTinh || 'chai', soLuongMin: dv.soLuongMin || 1, moTa: dv.moTa || '', trangThai: dv.trangThai });
    setErrorMessage(''); setShowModal(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm(`Xóa dịch vụ ${id}?`)) return;
    try { await dichVuAPI.delete(id); fetchData(); } catch { alert('Lỗi khi xóa dịch vụ!'); }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingId) await dichVuAPI.update(editingId, formData);
      else await dichVuAPI.create(formData);
      setShowModal(false); fetchData();
    } catch (err) {
      setErrorMessage(err.response?.data || 'Đã xảy ra lỗi khi lưu thông tin.');
    }
  };

  const handleCtxAction = (action, dv) => {
    switch (action) {
      case 'edit': handleOpenEdit(dv); break;
      case 'delete': handleDelete(dv.maDichVu); break;
      case 'toggle_status':
        dichVuAPI.update(dv.maDichVu, { ...dv, trangThai: dv.trangThai === 1 ? 0 : 1 })
          .then(fetchData).catch(() => alert('Không thể thay đổi trạng thái!'));
        break;
    }
  };

  const getCtxItems = (dv) => [
    { label: 'Chỉnh sửa dịch vụ', action: 'edit' },
    { label: dv.trangThai === 1 ? 'Tạm ngừng cung cấp' : 'Kích hoạt lại', action: 'toggle_status' },
    { separator: true },
    { label: 'Xóa dịch vụ', action: 'delete', danger: true },
  ];

  const categories = ['Tất cả loại', 'Ăn uống', 'Vận chuyển', 'Giặt ủi', 'Giải trí', 'Dịch vụ khác'];

  return (
    <div className="page-shell" onClick={() => closeCtxMenu()}>
      {/* Header */}
      <div className="page-header">
        <div>
          <div className="text-[10px] font-bold uppercase tracking-[0.15em] mb-1" style={{ color: 'var(--accent)', opacity: 0.9 }}>
            Danh mục
          </div>
          <h2 className="page-title">Quản lý Dịch vụ</h2>
          <p className="page-subtitle">Xem, thêm mới, cập nhật danh mục dịch vụ phục vụ khách lưu trú.</p>
        </div>
        <button onClick={handleOpenAdd} className="btn-primary">
          <Plus style={{ width: 15, height: 15 }} />
          Thêm dịch vụ mới
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3.5">
        <div className="kpi-card">
          <span className="kpi-label">Tổng số dịch vụ</span>
          <span className="kpi-value">{dichVus.length}</span>
        </div>
        <div className="kpi-card">
          <span className="kpi-label">Giá trung bình</span>
          <div className="text-[20px] font-black tabular-nums" style={{ color: 'var(--accent)' }}>
            {avgPrice.toLocaleString('vi-VN')}
            <span className="text-[11px] font-normal ml-1" style={{ color: 'var(--text-muted)' }}>₫ / lượt</span>
          </div>
        </div>
        <div className="kpi-card">
          <span className="kpi-label">Trạng thái hệ thống</span>
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full" style={{ background: 'var(--status-avail-dot)', animation: 'ping-soft 2s ease-in-out infinite' }} />
            <span className="text-[13px] font-semibold" style={{ color: 'var(--text-primary)' }}>Backend hoạt động</span>
          </div>
        </div>
      </div>

      {/* Error */}
      {errorMessage && (
        <div className="alert-strip alert-error">
          <AlertTriangle style={{ width: 14, height: 14, color: '#f87171', flexShrink: 0 }} />
          <span style={{ color: '#f87171', fontSize: 12 }}>{errorMessage}</span>
        </div>
      )}

      {/* Search & Filter */}
      <div className="filter-bar">
        <div className="filter-search">
          <Search className="filter-search-icon" style={{ width: 14, height: 14 }} />
          <input type="text" placeholder="Tìm tên dịch vụ, mã DV..."
            value={searchTerm} onChange={e => setSearchTerm(e.target.value)} />
          {searchTerm && (
            <button onClick={() => setSearchTerm('')} className="filter-clear-btn">
              <X style={{ width: 10, height: 10 }} />
            </button>
          )}
        </div>

        <div className={`filter-select-wrap ${selectedType !== 'Tất cả loại' ? 'active' : ''}`}>
          <select value={selectedType} onChange={e => setSelectedType(e.target.value)}>
            {categories.map(cat => (
              <option key={cat} value={cat}>{cat === 'Tất cả loại' ? 'Tất cả phân loại' : cat}</option>
            ))}
          </select>
        </div>

        <div className={`filter-select-wrap ${filterTrangThai !== 'all' ? 'active' : ''}`}>
          <select value={filterTrangThai} onChange={e => setFilterTrangThai(e.target.value)}>
            <option value="all">Tất cả trạng thái</option>
            <option value="active">Hoạt động</option>
            <option value="inactive">Tạm ngưng</option>
          </select>
        </div>

        {hasFilter && (
          <button onClick={clearFilters} className="filter-reset-btn">
            Xoá lọc
          </button>
        )}

        <span className="filter-result-count">{dichVusFiltered.length}/{dichVus.length} dịch vụ</span>
      </div>

      {/* Table */}
      <div className="section-box">
        <div className="overflow-x-auto">
          <table className="data-table">
            <thead>
              <tr>
                {['Mã DV', 'Tên dịch vụ', 'Phân loại', 'Đơn giá', 'Đơn vị tính', 'Tối thiểu', 'Trạng thái'].map(h => (
                  <th key={h}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} className="py-16 text-center">
                    <div className="spinner mx-auto mb-2" />
                    <div style={{ color: 'var(--text-muted)', fontSize: 12 }}>Đang tải dịch vụ...</div>
                  </td>
                </tr>
              ) : dichVusFiltered.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-14 text-center" style={{ color: 'var(--text-muted)', fontSize: 13 }}>
                    Không tìm thấy dịch vụ nào phù hợp.
                  </td>
                </tr>
              ) : dichVusFiltered.map(dv => (
                <tr
                  key={dv.maDichVu}
                  className="cursor-pointer group"
                  onDoubleClick={() => handleOpenEdit(dv)}
                  onContextMenu={e => { e.stopPropagation(); openCtxMenu(e, dv); }}
                >
                  <td className="font-mono-data font-bold text-[12px]" style={{ color: 'var(--accent)' }}>{dv.maDichVu}</td>
                  <td>
                    <div className="font-semibold text-[13px]" style={{ color: 'var(--text-primary)' }}>{dv.tenDichVu}</div>
                    {dv.moTa && <div className="text-[11px] max-w-xs truncate mt-0.5" style={{ color: 'var(--text-muted)' }}>{dv.moTa}</div>}
                  </td>
                  <td>
                    <span className="text-[11px] px-2 py-0.5 rounded-md font-medium"
                      style={{ background: 'var(--bg-elevated)', color: 'var(--text-secondary)', border: '1px solid var(--border)' }}>
                      {dv.loai}
                    </span>
                  </td>
                  <td className="text-right tabular-nums font-bold" style={{ color: 'var(--accent)' }}>
                    {dv.donGia.toLocaleString('vi-VN')} ₫
                  </td>
                  <td style={{ color: 'var(--text-secondary)' }}>{dv.donViTinh}</td>
                  <td className="tabular-nums" style={{ color: 'var(--text-secondary)' }}>{dv.soLuongMin}</td>
                  <td className="row-actions-cell">
                    <span className={`badge ${dv.trangThai === 1 ? 'badge-available' : 'badge-maintenance'}`}>
                      <span className="badge-dot" style={{ background: dv.trangThai === 1 ? 'var(--status-avail-dot)' : 'var(--status-maint-dot)' }} />
                      {dv.trangThai === 1 ? 'Hoạt động' : 'Tạm ngừng'}
                    </span>

                    {/* Hover Actions Panel */}
                    <div className="row-hover-actions">
                      <button onClick={(e) => { e.stopPropagation(); handleOpenEdit(dv); }} className="row-action-btn" title="Xem chi tiết">
                        <Eye style={{ width: 12, height: 12 }} />
                      </button>
                      <button onClick={(e) => { e.stopPropagation(); handleOpenEdit(dv); }} className="row-action-btn" title="Chỉnh sửa">
                        <Edit2 style={{ width: 11, height: 11 }} />
                      </button>
                      <button onClick={(e) => { e.stopPropagation(); handleDelete(dv.maDichVu); }} className="row-action-btn danger" title="Xóa">
                        <Trash2 style={{ width: 11, height: 11 }} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal */}
      {showModal && (
        <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) setShowModal(false); }}>
          <div className="modal-panel" style={{ maxWidth: 540 }}>
            <div className="modal-header">
              <h3 className="modal-title">{editingId ? 'Cập nhật Dịch vụ' : 'Thêm Dịch vụ Mới'}</h3>
              <button onClick={() => setShowModal(false)} className="row-action-btn">
                <X style={{ width: 13, height: 13 }} />
              </button>
            </div>
            <div className="modal-body">
              {errorMessage && (
                <div className="alert-strip alert-error text-[12px]" style={{ color: '#f87171' }}>{errorMessage}</div>
              )}
              <form id="dichvu-form" onSubmit={handleSubmit} className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="label-style">Mã dịch vụ</label>
                    <input type="text" value={formData.maDichVu}
                      onChange={e => setFormData({ ...formData, maDichVu: e.target.value })}
                      placeholder="Tự động sinh" disabled={!!editingId}
                      className="input-style" style={{ opacity: editingId ? 0.5 : 1 }} />
                  </div>
                  <div>
                    <label className="label-style">Phân loại</label>
                    <select value={formData.loai} onChange={e => setFormData({ ...formData, loai: e.target.value })} className="input-style">
                      <option value="Ăn uống">Ăn uống</option>
                      <option value="Vận chuyển">Vận chuyển</option>
                      <option value="Giặt ủi">Giặt ủi</option>
                      <option value="Giải trí">Giải trí</option>
                      <option value="Dịch vụ khác">Dịch vụ khác</option>
                    </select>
                  </div>
                </div>
                <div>
                  <label className="label-style">Tên dịch vụ *</label>
                  <input required type="text" value={formData.tenDichVu}
                    onChange={e => setFormData({ ...formData, tenDichVu: e.target.value })}
                    placeholder="Ví dụ: Cà phê sữa đá" className="input-style" />
                </div>
                <div className="grid grid-cols-3 gap-4">
                  <div className="col-span-2">
                    <label className="label-style">Đơn giá *</label>
                    <div className="relative mt-1">
                      <input required type="number" min={0} value={formData.donGia}
                        onChange={e => setFormData({ ...formData, donGia: parseFloat(e.target.value) })}
                        className="input-style pr-12" />
                      <span className="absolute right-3 top-1/2 -translate-y-1/2 text-[10px] font-bold" style={{ color: 'var(--text-muted)' }}>VND</span>
                    </div>
                  </div>
                  <div>
                    <label className="label-style">Đơn vị</label>
                    <input type="text" value={formData.donViTinh}
                      onChange={e => setFormData({ ...formData, donViTinh: e.target.value })}
                      placeholder="chai, lon..." className="input-style" />
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="label-style">SL tối thiểu</label>
                    <input type="number" min={1} value={formData.soLuongMin}
                      onChange={e => setFormData({ ...formData, soLuongMin: parseInt(e.target.value) })}
                      className="input-style" />
                  </div>
                  <div>
                    <label className="label-style">Trạng thái</label>
                    <select value={formData.trangThai}
                      onChange={e => setFormData({ ...formData, trangThai: parseInt(e.target.value) })}
                      className="input-style">
                      <option value={1}>Hoạt động</option>
                      <option value={0}>Tạm ngừng</option>
                    </select>
                  </div>
                </div>
                <div>
                  <label className="label-style">Mô tả chi tiết</label>
                  <textarea value={formData.moTa} onChange={e => setFormData({ ...formData, moTa: e.target.value })}
                    placeholder="Thông tin thêm về dịch vụ..." rows={3} className="input-style resize-none" />
                </div>
              </form>
            </div>
            <div className="modal-footer">
              <button type="button" onClick={() => setShowModal(false)} className="btn-ghost text-[13px]">Hủy</button>
              <button type="submit" form="dichvu-form" className="btn-primary text-[13px]">Lưu thay đổi</button>
            </div>
          </div>
        </div>
      )}

      {/* Context Menu */}
      {ctxMenu && (
        <RowContextMenu
          menu={ctxMenu}
          items={getCtxItems(ctxMenu.item)}
          onAction={handleCtxAction}
          onClose={closeCtxMenu}
          title={ctxMenu.item?.tenDichVu}
          subtitle={ctxMenu.item?.maDichVu}
        />
      )}
    </div>
  );
};

export default DichVuList;
