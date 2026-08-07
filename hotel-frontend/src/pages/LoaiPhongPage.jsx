import React, { useEffect, useState } from 'react';
import { loaiPhongAPI } from '../api/api';
import { LayoutGrid, Plus, Edit2, Trash2, X } from 'lucide-react';
import { RowContextMenu, useContextMenu } from '../components/ContextMenu';

const LoaiPhongPage = () => {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [err, setErr] = useState('');
  const init = { maLoaiPhong: '', tenLoaiPhong: '', soNguoiToiDa: 2, giaTheoNgay: 0, moTa: '' };
  const [form, setForm] = useState(init);
  const { ctxMenu, openCtxMenu, closeCtxMenu } = useContextMenu();

  const fetchData = () => {
    setLoading(true);
    loaiPhongAPI.getAll().then(r => setItems(r.data)).catch(console.error).finally(() => setLoading(false));
  };
  useEffect(() => { fetchData(); }, []);

  const openAdd = () => { setEditing(null); setForm(init); setErr(''); setShowModal(true); };
  const openEdit = (lp) => { setEditing(lp.maLoaiPhong); setForm({ ...init, ...lp }); setErr(''); setShowModal(true); };

  const handleSubmit = async (e) => {
    e.preventDefault(); setErr('');
    try {
      if (editing) await loaiPhongAPI.update(editing, form);
      else await loaiPhongAPI.create(form);
      setShowModal(false); fetchData();
    } catch (e) { setErr(e.response?.data || 'Lỗi!'); }
  };

  const del = async (id) => {
    if (!confirm(`Xóa loại phòng ${id}?`)) return;
    try { await loaiPhongAPI.delete(id); fetchData(); } catch (e) { alert(e.response?.data || 'Lỗi!'); }
  };

  const handleCtxAction = (action, lp) => {
    switch (action) {
      case 'edit': openEdit(lp); break;
      case 'delete': del(lp.maLoaiPhong); break;
    }
  };

  const getCtxItems = () => [
    { label: 'Chỉnh sửa loại phòng', action: 'edit' },
    { separator: true },
    { label: 'Xóa loại phòng', action: 'delete', danger: true },
  ];

  return (
    <div className="page-shell" onClick={() => closeCtxMenu()}>
      {/* Header */}
      <div className="page-header">
        <div>
          <div className="page-label">
            Danh mục
          </div>
          <h1 className="page-title flex items-center gap-2">
            <LayoutGrid style={{ width: 20, height: 20, color: 'var(--text-secondary)' }} strokeWidth={2} />
            Loại phòng
          </h1>
          <p className="page-subtitle">{items.length} loại phòng · Double-click để sửa</p>
        </div>
        <button onClick={openAdd} className="btn-primary">
          <Plus style={{ width: 15, height: 15 }} />
          Thêm loại phòng
        </button>
      </div>

      {/* Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {loading ? (
          Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="skeleton h-36 rounded-2xl" />
          ))
        ) : items.map(lp => (
          <div
            key={lp.maLoaiPhong}
            onDoubleClick={() => openEdit(lp)}
            onContextMenu={e => { e.stopPropagation(); openCtxMenu(e, lp); }}
            className="card p-5 flex flex-col gap-3 cursor-pointer group"
          >
            <div className="flex items-start justify-between">
              <div>
                <div className="text-[14px] font-bold" style={{ color: 'var(--text-primary)' }}>{lp.tenLoaiPhong}</div>
                <div className="font-mono-data text-[10px] mt-0.5" style={{ color: 'var(--text-muted)' }}>{lp.maLoaiPhong}</div>
              </div>
              <div className="flex gap-1 shrink-0 opacity-0 group-hover:opacity-100 transition-opacity">
                <button onClick={() => openEdit(lp)} className="row-action-btn" title="Chỉnh sửa">
                  <Edit2 style={{ width: 11, height: 11 }} />
                </button>
                <button onClick={() => del(lp.maLoaiPhong)} className="row-action-btn danger" title="Xóa">
                  <Trash2 style={{ width: 11, height: 11 }} />
                </button>
              </div>
            </div>

            <div className="text-[20px] font-black tabular-nums" style={{ color: 'var(--accent)' }}>
              {(lp.giaTheoNgay || 0).toLocaleString('vi-VN')}
              <span className="text-[11px] font-normal ml-1" style={{ color: 'var(--text-muted)' }}>₫/đêm</span>
            </div>

            <div className="flex items-center gap-2">
              <span
                className="text-[10px] font-semibold px-2.5 py-1 rounded-full"
                style={{
                  background: 'var(--bg-elevated)',
                  border: '1px solid var(--border)',
                  color: 'var(--text-secondary)',
                }}
              >
                Tối đa {lp.soNguoiToiDa} khách
              </span>
            </div>

            {lp.moTa && (
              <p className="text-[12px] line-clamp-2 leading-relaxed" style={{ color: 'var(--text-muted)' }}>
                {lp.moTa}
              </p>
            )}
          </div>
        ))}
      </div>

      {/* Context Menu */}
      {ctxMenu && (
        <RowContextMenu
          menu={ctxMenu}
          items={getCtxItems(ctxMenu.item)}
          onAction={handleCtxAction}
          onClose={closeCtxMenu}
          title={ctxMenu.item?.tenLoaiPhong}
          subtitle={ctxMenu.item?.maLoaiPhong}
        />
      )}

      {/* Add/Edit Modal */}
      {showModal && (
        <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) setShowModal(false); }}>
          <div className="modal-panel">
            <div className="modal-header">
              <h3 className="modal-title">{editing ? 'Cập nhật' : 'Thêm'} Loại phòng</h3>
              <button onClick={() => setShowModal(false)} className="row-action-btn">
                <X style={{ width: 13, height: 13 }} />
              </button>
            </div>
            <div className="modal-body">
              {err && <div className="alert-strip alert-error text-[12px]" style={{ color: '#f87171' }}>{err}</div>}
              <form id="loaiphong-form" onSubmit={handleSubmit} className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="label-style">Mã loại phòng *</label>
                    <input required type="text" disabled={!!editing} value={form.maLoaiPhong}
                      onChange={e => setForm({ ...form, maLoaiPhong: e.target.value })}
                      className="input-style" placeholder="LP001"
                      style={{ opacity: editing ? 0.5 : 1 }} />
                  </div>
                  <div>
                    <label className="label-style">Sức chứa tối đa</label>
                    <input type="number" min={1} value={form.soNguoiToiDa}
                      onChange={e => setForm({ ...form, soNguoiToiDa: parseInt(e.target.value) })}
                      className="input-style" />
                  </div>
                </div>
                <div>
                  <label className="label-style">Tên loại phòng *</label>
                  <input required type="text" value={form.tenLoaiPhong}
                    onChange={e => setForm({ ...form, tenLoaiPhong: e.target.value })}
                    className="input-style" placeholder="Standard Double" />
                </div>
                <div>
                  <label className="label-style">Giá theo ngày (VND)</label>
                  <input type="number" min={0} value={form.giaTheoNgay}
                    onChange={e => setForm({ ...form, giaTheoNgay: parseFloat(e.target.value) })}
                    className="input-style" />
                </div>
                <div>
                  <label className="label-style">Mô tả</label>
                  <textarea value={form.moTa} onChange={e => setForm({ ...form, moTa: e.target.value })}
                    rows={3} className="input-style resize-none" />
                </div>
              </form>
            </div>
            <div className="modal-footer">
              <button type="button" onClick={() => setShowModal(false)} className="btn-ghost text-[13px]">Hủy</button>
              <button type="submit" form="loaiphong-form" className="btn-primary text-[13px]">Lưu</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default LoaiPhongPage;
