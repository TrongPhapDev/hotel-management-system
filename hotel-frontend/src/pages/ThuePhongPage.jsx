import React, { useEffect, useState, useCallback, useRef } from 'react';
import { phongAPI, loaiPhongAPI, chiTietDatPhongAPI, thuePhongAPI, dichVuAPI } from '../api/api';
import { BedDouble, Search, X, Plus, Info, ArrowLeftRight, UtensilsCrossed, Clock, Wrench, CheckCircle2, ChevronRight } from 'lucide-react';
import { CheckinDialog, CheckoutDialog } from '../components/ThuePhongDialogs';

const TT_COLOR = {
  AVAILABLE:   { border: '#1D9E75', text: '#0F6E56', bg: '#E1F5EE', label: 'Có sẵn' },
  OCCUPIED:    { border: '#1D4ED8', text: '#185FA5', bg: '#EFF6FF', label: 'Đang thuê' },
  CLEANING:    { border: '#EF9F27', text: '#854F0B', bg: '#FAEEDA', label: 'Vệ sinh' },
  MAINTENANCE: { border: '#E24B4A', text: '#A32D2D', bg: '#FCEBEB', label: 'Bảo trì' },
};

const FILTERS = [
  { key: 'ALL', label: 'Tất cả', color: '#64748B' },
  { key: 'AVAILABLE', label: 'Có sẵn', color: '#10B981' },
  { key: 'OCCUPIED', label: 'Đang thuê', color: '#3B82F6' },
  { key: 'CLEANING', label: 'Vệ sinh', color: '#F59E0B' },
  { key: 'MAINTENANCE', label: 'Bảo trì', color: '#EF4444' },
];

const fmt = (n) => (n || 0).toLocaleString('vi-VN');

// === ROOM DETAIL MODAL ===
const RoomDetailModal = ({ room, onClose }) => {
  if (!room) return null;
  const ttc = TT_COLOR[room.trangThai] || { border: '#94A3B8', text: '#94A3B8', bg: '#F8FAFC', label: room.trangThai };
  return (
    <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="modal-panel" style={{ maxWidth: 448 }}>
        <div className="px-5 py-4 border-b border-[var(--border-color)] flex items-center justify-between">
          <h3 className="font-bold text-[var(--text-primary)] text-base">Chi tiết phòng {room.maPhong}</h3>
          <button onClick={onClose} className="text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition-colors"><X size={18} /></button>
        </div>
        <div className="p-5 space-y-3">
          <div className="flex items-center gap-3">
            <div style={{ backgroundColor: ttc.border }} className="w-4 h-4 rounded-full" />
            <span style={{ color: ttc.text }} className="text-sm font-bold">{ttc.label}</span>
          </div>
          <div className="grid grid-cols-2 gap-3 text-xs">
            {[
              ['Số phòng', room.maPhong],
              ['Loại phòng', room.loaiPhong?.tenLoaiPhong || '—'],
              ['Tầng', room.tang ? `Tầng ${room.tang}` : '—'],
              ['Hướng view', room.view || '—'],
              ['Sức chứa', room.sucChua ? `${room.sucChua} người` : '—'],
              ['Giá/đêm', room.loaiPhong?.giaTheoNgay ? `${fmt(room.loaiPhong.giaTheoNgay)}đ` : '—'],
            ].map(([k, v]) => (
              <div key={k} className="bg-[var(--bg-main)] rounded-xl p-3">
                <div className="text-[10px] font-bold text-[var(--text-secondary)] uppercase">{k}</div>
                <div className="font-semibold text-[var(--text-primary)] mt-0.5">{v}</div>
              </div>
            ))}
          </div>
          {room.trangThai === 'OCCUPIED' && room.tenKhachHienTai && (
            <div className="bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-800/50 rounded-xl p-3">
              <div className="text-[10px] font-bold text-blue-400 uppercase mb-1">Khách đang ở</div>
              <div className="font-bold text-blue-700 dark:text-blue-300">{room.tenKhachHienTai}</div>
            </div>
          )}
        </div>
        <div className="px-5 pb-4 flex justify-end">
          <button onClick={onClose} className="px-4 py-2 bg-[var(--bg-main)] hover:bg-[var(--border-color)] text-[var(--text-primary)] text-xs font-semibold rounded-xl">Đóng</button>
        </div>
      </div>
    </div>
  );
};

// === QUICK ADD SERVICE DIALOG (from context menu) ===
const QuickAddServiceDialog = ({ room, onClose, onSuccess }) => {
  const [services, setServices] = useState([]);
  const [stayDetail, setStayDetail] = useState(null);
  const [form, setForm] = useState({ maDV: '', soLuong: 1 });
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    Promise.all([dichVuAPI.getAll(), chiTietDatPhongAPI.getActive()]).then(([sRes, aRes]) => {
      setServices(sRes.data);
      const stay = aRes.data.find(s => s.phong?.maPhong === room.maPhong);
      setStayDetail(stay || null);
    }).catch(console.error).finally(() => setLoading(false));
  }, [room]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!stayDetail) return alert('Không tìm thấy thông tin lượt thuê!');
    setSubmitting(true);
    try {
      const selected = services.find(s => s.maDV === form.maDV);
      await thuePhongAPI.themDichVu({ maChiTiet: stayDetail.maChiTiet, maDV: form.maDV, soLuong: form.soLuong, donGia: selected?.donGia || 0 });
      onSuccess();
      onClose();
    } catch (e) { alert(e.response?.data || 'Lỗi thêm dịch vụ'); } finally { setSubmitting(false); }
  };

  return (
    <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="modal-panel" style={{ maxWidth: 384, padding: 20 }}>
        <div className="flex items-center justify-between">
          <h4 className="font-bold text-[var(--text-primary)] flex items-center gap-2"><UtensilsCrossed size={16} className="text-blue-500" />Thêm dịch vụ — P.{room.maPhong}</h4>
          <button onClick={onClose}><X size={16} className="text-[var(--text-secondary)]" /></button>
        </div>
        {loading ? <div className="text-center py-6 text-[var(--text-secondary)] text-sm">Đang tải...</div> : !stayDetail ? (
          <div className="text-center py-6 text-red-400 text-sm">Không tìm thấy thông tin lượt thuê!</div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-3">
            <div>
              <label className="text-[10px] font-bold text-[var(--text-secondary)] uppercase">Dịch vụ</label>
              <select required value={form.maDV} onChange={e => setForm({ ...form, maDV: e.target.value })}
                className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] text-[var(--input-text)] rounded-xl px-3 py-2 text-sm text-[var(--text-primary)] mt-1 focus:border-blue-500 outline-none">
                <option value="">-- Chọn dịch vụ --</option>
                {services.map(s => <option key={s.maDV} value={s.maDV}>{s.tenDV} ({fmt(s.donGia)}đ)</option>)}
              </select>
            </div>
            <div>
              <label className="text-[10px] font-bold text-[var(--text-secondary)] uppercase">Số lượng</label>
              <input type="number" min={1} value={form.soLuong} onChange={e => setForm({ ...form, soLuong: Number(e.target.value) })}
                className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] text-[var(--input-text)] rounded-xl px-3 py-2 text-sm text-[var(--text-primary)] mt-1 focus:border-blue-500 outline-none" />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button type="button" onClick={onClose} className="px-4 py-2 bg-[var(--bg-main)] text-[var(--text-secondary)] text-xs font-semibold rounded-xl">Hủy</button>
              <button type="submit" disabled={submitting} className="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold rounded-xl disabled:opacity-60">
                {submitting ? 'Đang xử lý...' : 'Thêm dịch vụ'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};

// === QUICK EXTEND STAY DIALOG ===
const QuickExtendDialog = ({ room, onClose, onSuccess }) => {
  const [stayDetail, setStayDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [ngayTraMoi, setNgayTraMoi] = useState(new Date(Date.now() + 24 * 3600 * 1000).toISOString().substring(0, 16));
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    chiTietDatPhongAPI.getActive().then(res => {
      const stay = res.data.find(s => s.phong?.maPhong === room.maPhong);
      setStayDetail(stay || null);
      if (stay?.datPhong?.ngayTraDuKien) {
        setNgayTraMoi(new Date(new Date(stay.datPhong.ngayTraDuKien).getTime() + 24 * 3600 * 1000).toISOString().substring(0, 16));
      }
    }).catch(console.error).finally(() => setLoading(false));
  }, [room]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!stayDetail) return;
    setSubmitting(true);
    try {
      await thuePhongAPI.giaHan({ maChiTiet: stayDetail.maChiTiet, ngayTraMoi: new Date(ngayTraMoi).toISOString() });
      onSuccess();
      onClose();
    } catch (e) { alert(e.response?.data || 'Lỗi gia hạn'); } finally { setSubmitting(false); }
  };

  return (
    <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="modal-panel" style={{ maxWidth: 384, padding: 20 }}>
        <div className="flex items-center justify-between">
          <h4 className="font-bold text-[var(--text-primary)] flex items-center gap-2"><Clock size={16} className="text-amber-500" />Gia hạn lưu trú — P.{room.maPhong}</h4>
          <button onClick={onClose}><X size={16} className="text-[var(--text-secondary)]" /></button>
        </div>
        {loading ? <div className="text-center py-6 text-[var(--text-secondary)] text-sm">Đang tải...</div> : !stayDetail ? (
          <div className="text-center py-6 text-red-400 text-sm">Không tìm thấy thông tin lượt thuê!</div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-3">
            <div className="bg-amber-50 dark:bg-amber-950/20 border border-amber-200 dark:border-amber-800/40 rounded-xl p-3 text-xs text-amber-700 dark:text-amber-300">
              Ngày trả hiện tại: <strong>{stayDetail.datPhong?.ngayTraDuKien ? new Date(stayDetail.datPhong.ngayTraDuKien).toLocaleString('vi-VN') : '—'}</strong>
            </div>
            <div>
              <label className="text-[10px] font-bold text-[var(--text-secondary)] uppercase">Ngày trả mới</label>
              <input type="datetime-local" required value={ngayTraMoi} onChange={e => setNgayTraMoi(e.target.value)}
                className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] text-[var(--input-text)] rounded-xl px-3 py-2 text-sm text-[var(--text-primary)] mt-1 focus:border-blue-500 outline-none" />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button type="button" onClick={onClose} className="px-4 py-2 bg-[var(--bg-main)] text-[var(--text-secondary)] text-xs font-semibold rounded-xl">Hủy</button>
              <button type="submit" disabled={submitting} className="px-4 py-2 bg-amber-600 hover:bg-amber-500 text-white text-xs font-bold rounded-xl disabled:opacity-60">
                {submitting ? 'Đang xử lý...' : 'Gia hạn'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};

// === QUICK SWITCH ROOM DIALOG ===
const QuickSwitchRoomDialog = ({ room, onClose, onSuccess }) => {
  const [stayDetail, setStayDetail] = useState(null);
  const [availableRooms, setAvailableRooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [maPhongMoi, setMaPhongMoi] = useState('');
  const [giuNguyenGia, setGiuNguyenGia] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    Promise.all([chiTietDatPhongAPI.getActive(), phongAPI.getAll('AVAILABLE')]).then(([aRes, pRes]) => {
      const stay = aRes.data.find(s => s.phong?.maPhong === room.maPhong);
      setStayDetail(stay || null);
      setAvailableRooms(pRes.data);
    }).catch(console.error).finally(() => setLoading(false));
  }, [room]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!stayDetail || !maPhongMoi) return;
    setSubmitting(true);
    try {
      await thuePhongAPI.doiPhong({ maChiTiet: stayDetail.maChiTiet, maPhongMoi, giuNguyenGia });
      onSuccess();
      onClose();
    } catch (e) { alert(e.response?.data || 'Lỗi đổi phòng'); } finally { setSubmitting(false); }
  };

  return (
    <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="modal-panel" style={{ maxWidth: 384, padding: 20 }}>
        <div className="flex items-center justify-between">
          <h4 className="font-bold text-[var(--text-primary)] flex items-center gap-2"><ArrowLeftRight size={16} className="text-blue-500" />Đổi phòng — P.{room.maPhong}</h4>
          <button onClick={onClose}><X size={16} className="text-[var(--text-secondary)]" /></button>
        </div>
        {loading ? <div className="text-center py-6 text-[var(--text-secondary)] text-sm">Đang tải...</div> : !stayDetail ? (
          <div className="text-center py-6 text-red-400 text-sm">Không tìm thấy thông tin lượt thuê!</div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-3">
            <div>
              <label className="text-[10px] font-bold text-[var(--text-secondary)] uppercase">Phòng trống khả dụng</label>
              <select required value={maPhongMoi} onChange={e => setMaPhongMoi(e.target.value)}
                className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] text-[var(--input-text)] rounded-xl px-3 py-2 text-sm text-[var(--text-primary)] mt-1 focus:border-blue-500 outline-none">
                <option value="">-- Chọn phòng trống --</option>
                {availableRooms.map(r => <option key={r.maPhong} value={r.maPhong}>P.{r.maPhong} — {r.loaiPhong?.tenLoaiPhong} ({fmt(r.loaiPhong?.giaTheoNgay)}đ/đêm)</option>)}
              </select>
            </div>
            <label className="flex items-center gap-2 cursor-pointer">
              <input type="checkbox" checked={giuNguyenGia} onChange={e => setGiuNguyenGia(e.target.checked)} className="w-4 h-4 text-blue-600 rounded" />
              <span className="text-xs text-[var(--text-secondary)] font-semibold">Giữ nguyên giá phòng cũ</span>
            </label>
            <div className="flex justify-end gap-2 pt-2">
              <button type="button" onClick={onClose} className="px-4 py-2 bg-[var(--bg-main)] text-[var(--text-secondary)] text-xs font-semibold rounded-xl">Hủy</button>
              <button type="submit" disabled={submitting} className="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold rounded-xl disabled:opacity-60">
                {submitting ? 'Đang xử lý...' : 'Chuyển phòng'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};

// === STATUS CONFIRM DIALOG ===
const StatusConfirmDialog = ({ data, onConfirm, onClose }) => (
  <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) onClose(); }}>
    <div className="modal-panel" style={{ maxWidth: 384, padding: 24 }}>
      <div className="text-base font-bold text-[var(--text-primary)] mb-2">{data.title || 'Xác nhận'}</div>
      <div className="text-sm text-[var(--text-secondary)] mb-5">{data.label}</div>
      <div className="flex justify-end gap-3">
        <button onClick={onClose} className="px-4 py-2 bg-[var(--bg-main)] text-[var(--text-secondary)] text-xs font-semibold rounded-xl hover:bg-[var(--border-color)]">Hủy</button>
        <button onClick={onConfirm} className="px-5 py-2 bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold rounded-xl">{data.confirmText || 'Xác nhận'}</button>
      </div>
    </div>
  </div>
);

// === CONTEXT MENU COMPONENT ===
const RoomContextMenu = ({ menu, onAction, onClose }) => {
  const { phong: p } = menu;
  const menuRef = useRef(null);

  useEffect(() => {
    const handleClick = (e) => { if (menuRef.current && !menuRef.current.contains(e.target)) onClose(); };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, [onClose]);

  const menuItems = [];

  // Always: View details
  menuItems.push({ label: 'Xem chi tiết phòng', action: 'detail', color: 'text-[var(--text-secondary)]' });

  if (p.trangThai === 'AVAILABLE') {
    menuItems.push(null); // separator
    menuItems.push({ label: 'Nhận phòng (Check-in)', action: 'checkin', color: 'text-green-600 dark:text-green-400' });
    menuItems.push({ label: 'Đưa vào bảo trì', action: 'maintenance_start', color: 'text-orange-600 dark:text-orange-400' });
  }

  if (p.trangThai === 'OCCUPIED') {
    menuItems.push(null);
    menuItems.push({ label: 'Đổi phòng cho khách', action: 'switch', color: 'text-blue-600 dark:text-blue-400' });
    menuItems.push({ label: 'Thêm dịch vụ sử dụng', action: 'service', color: 'text-violet-600 dark:text-violet-400' });
    menuItems.push({ label: 'Gia hạn lưu trú', action: 'extend', color: 'text-amber-600 dark:text-amber-400' });
    menuItems.push(null);
    menuItems.push({ label: 'Thanh toán & Trả phòng', action: 'checkout', color: 'text-blue-600 dark:text-blue-400', bold: true });
    menuItems.push({ label: 'Đưa vào bảo trì', action: 'maintenance_start', color: 'text-orange-600 dark:text-orange-400' });
  }

  if (p.trangThai === 'CLEANING') {
    menuItems.push(null);
    menuItems.push({ label: 'Dọn xong — Sẵn sàng đón khách', action: 'cleaning_done', color: 'text-green-600 dark:text-green-400' });
    menuItems.push({ label: 'Đưa vào bảo trì', action: 'maintenance_start', color: 'text-orange-600 dark:text-orange-400' });
  }

  if (p.trangThai === 'MAINTENANCE') {
    menuItems.push(null);
    menuItems.push({ label: 'Bảo trì xong — Mở lại phòng', action: 'maintenance_done', color: 'text-green-600 dark:text-green-400' });
  }

  // Position adjustment
  const menuStyle = { position: 'fixed', top: menu.y, left: menu.x, zIndex: 9999 };
  // Make sure menu doesn't overflow screen
  if (menu.x + 220 > window.innerWidth) menuStyle.left = menu.x - 220;
  if (menu.y + menuItems.length * 36 > window.innerHeight) menuStyle.top = menu.y - menuItems.length * 36;

  return (
    <div ref={menuRef} style={menuStyle}
      className="bg-[var(--bg-sidebar)] border border-[var(--border-color)] rounded-xl shadow-2xl py-1 min-w-[220px] overflow-hidden">
      <div className="px-3 py-2 border-b border-[var(--border-color)]">
        <span className="text-xs font-bold text-[var(--text-secondary)]">Phòng {p.maPhong}</span>
        <span className="ml-2 text-[10px] px-1.5 py-0.5 rounded-md font-bold"
          style={{ backgroundColor: TT_COLOR[p.trangThai]?.bg || '#F1F5F9', color: TT_COLOR[p.trangThai]?.text || '#94A3B8' }}>
          {TT_COLOR[p.trangThai]?.label || p.trangThai}
        </span>
      </div>
      {menuItems.map((item, idx) => item === null ? (
        <div key={idx} className="my-1 border-t border-[var(--border-color)]" />
      ) : (
        <button key={idx} onClick={() => onAction(item.action, p)}
          className={`w-full text-left px-3 py-2 text-xs font-semibold hover:bg-[var(--bg-main)] dark:hover:bg-[var(--bg-main)]/60 transition-colors ${item.color} ${item.bold ? 'font-extrabold' : ''}`}>
          {item.label}
        </button>
      ))}
    </div>
  );
};

// ====================== MAIN PAGE ======================
const ThuePhongPage = ({ onNavigate }) => {
  const [phongs, setPhongs] = useState([]);
  const [loaiPhongs, setLoaiPhongs] = useState([]);
  const [filter, setFilter] = useState('ALL');
  const [floorFilter, setFloorFilter] = useState(0);
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [searchKw, setSearchKw] = useState('');
  const [loading, setLoading] = useState(true);
  const [ctxMenu, setCtxMenu] = useState(null);
  const [statusDlg, setStatusDlg] = useState(null);

  // Active dialogs
  const [selectedRoom, setSelectedRoom] = useState(null);
  const [showCheckin, setShowCheckin] = useState(false);
  const [showCheckout, setShowCheckout] = useState(false);
  const [showDetail, setShowDetail] = useState(false);
  const [showService, setShowService] = useState(false);
  const [showExtend, setShowExtend] = useState(false);
  const [showSwitch, setShowSwitch] = useState(false);

  const fetchData = useCallback(() => {
    setLoading(true);
    Promise.all([phongAPI.getAll(), loaiPhongAPI.getAll()])
      .then(([p, l]) => { setPhongs(p.data); setLoaiPhongs(l.data); })
      .catch(console.error).finally(() => setLoading(false));
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  const floors = [...new Set(phongs.map(p => p.tang))].sort((a, b) => a - b);
  const stats = { ALL: phongs.length };
  FILTERS.slice(1).forEach(f => { stats[f.key] = phongs.filter(p => p.trangThai === f.key).length; });

  const filtered = phongs.filter(p => {
    const matchStatus = filter === 'ALL' || p.trangThai === filter;
    const matchFloor = floorFilter === 0 || p.tang === floorFilter;
    const matchType = typeFilter === 'ALL' || (p.loaiPhong?.maLoaiPhong === typeFilter);
    const matchSearch = !searchKw || p.maPhong?.toLowerCase().includes(searchKw.toLowerCase())
      || p.tenKhachHienTai?.toLowerCase().includes(searchKw.toLowerCase());
    return matchStatus && matchFloor && matchType && matchSearch;
  });

  const byFloor = {};
  filtered.forEach(p => { if (!byFloor[p.tang]) byFloor[p.tang] = []; byFloor[p.tang].push(p); });

  const handleRoomClick = (p) => {
    setSelectedRoom(p);
    if (p.trangThai === 'AVAILABLE') setShowCheckin(true);
    else if (p.trangThai === 'OCCUPIED') setShowCheckout(true);
    else if (p.trangThai === 'CLEANING') setStatusDlg({ phong: p, newStatus: 'AVAILABLE', title: 'Xác nhận hoàn tất vệ sinh', label: `Đánh dấu phòng ${p.maPhong} đã dọn xong và sẵn sàng đón khách?`, confirmText: 'Xác nhận' });
  };

  const handleContextMenu = (e, p) => {
    e.preventDefault();
    e.stopPropagation();
    setCtxMenu({ x: e.clientX, y: e.clientY, phong: p });
  };

  const handleContextAction = async (action, p) => {
    setCtxMenu(null);
    setSelectedRoom(p);
    switch (action) {
      case 'detail': setShowDetail(true); break;
      case 'checkin': setShowCheckin(true); break;
      case 'checkout': setShowCheckout(true); break;
      case 'service': setShowService(true); break;
      case 'extend': setShowExtend(true); break;
      case 'switch': setShowSwitch(true); break;
      case 'cleaning_done':
        setStatusDlg({ phong: p, newStatus: 'AVAILABLE', title: 'Xác nhận hoàn tất vệ sinh', label: `Phòng ${p.maPhong} đã dọn xong. Chuyển sang sẵn sàng đón khách?`, confirmText: 'Xác nhận' });
        break;
      case 'maintenance_start':
        setStatusDlg({ phong: p, newStatus: 'MAINTENANCE', title: 'Đánh dấu bảo trì', label: `Bạn muốn chuyển phòng ${p.maPhong} sang trạng thái Bảo trì?`, confirmText: 'Xác nhận bảo trì' });
        break;
      case 'maintenance_done':
        setStatusDlg({ phong: p, newStatus: 'AVAILABLE', title: 'Hoàn tất bảo trì', label: `Phòng ${p.maPhong} đã bảo trì xong. Chuyển sang sẵn sàng đón khách?`, confirmText: 'Mở lại phòng' });
        break;
      default: break;
    }
  };

  const handleStatusChange = async () => {
    if (!statusDlg) return;
    try {
      await phongAPI.capNhatTrangThai(statusDlg.phong.maPhong, statusDlg.newStatus);
      fetchData();
    } catch (e) { alert(e.response?.data || 'Lỗi!'); }
    setStatusDlg(null);
  };

  return (
    <div className="page-shell" onClick={() => ctxMenu && setCtxMenu(null)}>
      {/* Header */}
      <div className="page-header">
        <div>
          <div className="page-label">Vận hành</div>
          <h1 className="page-title flex items-center gap-2">
            <BedDouble style={{ width: 20, height: 20, color: 'var(--text-secondary)' }} strokeWidth={2} />Sơ đồ phòng
          </h1>
          <p className="page-subtitle">
            Click vào phòng để nhận/trả phòng · Chuột phải để xem thêm tùy chọn
          </p>
        </div>
        <button onClick={() => { setSelectedRoom(null); setShowCheckin(true); }}
          className="btn-primary">
          <Plus size={15} />Nhận phòng
        </button>
      </div>

      {/* Filter bar */}
      <div className="bg-[var(--bg-sidebar)] border border-[var(--border-color)] p-3 rounded-xl flex flex-col gap-3 shadow-sm text-xs">
        {/* Row 1: Filters & Search */}
        <div className="flex flex-wrap items-center gap-3">
          {/* Status chips */}
          <div className="flex flex-wrap items-center gap-2">
            {FILTERS.map(f => {
              const isActive = filter === f.key;
              return (
                <button key={f.key} onClick={() => setFilter(f.key)}
                  className="flex items-center gap-2 px-3 py-1.5 rounded-xl font-bold border transition-all duration-200"
                  style={{
                    background: isActive ? '#EFF6FF' : 'var(--bg-sidebar)',
                    borderColor: isActive ? '#1D4ED8' : 'var(--border)',
                    color: isActive ? '#1D4ED8' : 'var(--text-secondary)'
                  }}
                  onMouseEnter={e => {
                    if (!isActive) e.currentTarget.style.borderColor = '#1D4ED8';
                  }}
                  onMouseLeave={e => {
                    if (!isActive) e.currentTarget.style.borderColor = 'var(--border)';
                  }}
                >
                  {f.label}
                  <span
                    style={{
                      background: '#E1F5EE',
                      color: '#0F6E56',
                      padding: '1px 6px',
                      borderRadius: '10px',
                      fontSize: '10px'
                    }}
                  >
                    {stats[f.key]}
                  </span>
                </button>
              );
            })}
          </div>

          <div className="h-4 w-px bg-[var(--border-color)] hidden md:block" />

          {/* Floor tabs */}
          <div className="flex items-center gap-1.5">
            <span className="text-[10px] text-[var(--text-secondary)] font-extrabold uppercase tracking-wide">Tầng:</span>
            {[0, ...floors].map(f => {
              const isActive = floorFilter === f;
              return (
                <button key={f} onClick={() => setFloorFilter(f)}
                  className="px-2.5 py-1 rounded-md text-[11px] font-bold transition-all border"
                  style={{
                    background: isActive ? '#EFF6FF' : 'var(--bg-main)',
                    borderColor: isActive ? '#1D4ED8' : 'var(--border)',
                    color: isActive ? '#1D4ED8' : 'var(--text-secondary)'
                  }}
                  onMouseEnter={e => {
                    if (!isActive) e.currentTarget.style.borderColor = '#1D4ED8';
                  }}
                  onMouseLeave={e => {
                    if (!isActive) e.currentTarget.style.borderColor = 'var(--border)';
                  }}
                >
                  {f === 0 ? 'Tất cả' : `T${f}`}
                </button>
              );
            })}
          </div>

          <div className="flex-1" />

          {/* Search Room */}
          <div className="flex items-center gap-2 px-3 py-1.5 bg-[var(--bg-main)]/50 border border-[var(--border-color)] rounded-lg w-52">
            <Search className="text-[var(--text-secondary)] w-3.5 h-3.5 shrink-0" />
            <input type="text" placeholder="Tìm số phòng, khách..." value={searchKw} onChange={e => setSearchKw(e.target.value)}
              className="bg-transparent text-[var(--text-primary)] placeholder-slate-400 text-xs focus:outline-none w-full font-semibold" />
            {searchKw && <button onClick={() => setSearchKw('')}><X size={12} className="text-[var(--text-secondary)]" /></button>}
          </div>

          {/* Type Filter select */}
          <select value={typeFilter} onChange={e => setTypeFilter(e.target.value)}
            className="px-2.5 py-1.5 bg-[var(--bg-sidebar)] border border-[var(--border-color)] rounded-lg text-xs text-[var(--text-primary)] focus:outline-none focus:border-blue-500 font-bold">
            <option value="ALL">Tất cả loại phòng</option>
            {loaiPhongs.map(lp => <option key={lp.maLoaiPhong} value={lp.maLoaiPhong}>{lp.tenLoaiPhong}</option>)}
          </select>

          {(filter !== 'ALL' || floorFilter !== 0 || typeFilter !== 'ALL' || searchKw) && (
            <button onClick={() => { setFilter('ALL'); setFloorFilter(0); setTypeFilter('ALL'); setSearchKw(''); }}
              className="flex items-center gap-1 px-2.5 py-1.5 bg-red-50 dark:bg-red-950/20 border border-red-200 dark:border-red-800/40 text-red-600 dark:text-red-400 rounded-lg text-xs font-bold hover:bg-red-100 dark:hover:bg-red-950/40">
              <X size={11} /> Xóa bộ lọc
            </button>
          )}
        </div>
      </div>

      {/* Room Grid grouped by floor */}
      {loading ? (
        <div className="text-center py-20 text-[var(--text-secondary)] font-semibold">
          <div className="animate-spin w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full mx-auto mb-3" />
          Đang tải sơ đồ phòng...
        </div>
      ) : Object.keys(byFloor).length === 0 ? (
        <div className="text-center py-20 text-[var(--text-secondary)]">
          <BedDouble className="w-12 h-12 mx-auto mb-3 opacity-30" />
          <div className="font-semibold text-sm">Không có phòng nào phù hợp bộ lọc</div>
        </div>
      ) : (
        Object.entries(byFloor).sort(([a], [b]) => parseInt(a) - parseInt(b)).map(([floor, roomList]) => (
          <div key={floor} className="space-y-2.5">
            <div className="flex items-center gap-2.5">
              <span className="text-xs font-black text-[var(--text-primary)] uppercase tracking-wider">Tầng {floor}</span>
              <span className="text-[10px] text-[var(--text-secondary)] font-bold">({roomList.length} phòng)</span>
              <div className="flex-1 h-px bg-[var(--border-color)]" />
            </div>
            <div className="flex flex-wrap gap-2.5">
              {roomList.map(p => {
                const ttc = TT_COLOR[p.trangThai] || { bg: 'var(--bg-sidebar)', border: 'var(--border-color)', text: 'var(--text-secondary)', label: p.trangThai };
                return (
                  <div key={p.maPhong}
                    onClick={() => handleRoomClick(p)}
                    onContextMenu={(e) => handleContextMenu(e, p)}
                    style={{
                      borderRadius: '10px',
                      padding: '12px 14px',
                      background: 'var(--bg-elevated)',
                      border: '1px solid var(--border)',
                      borderLeft: `3px solid ${ttc.border}`,
                      minHeight: 95,
                    }}
                    className="w-[145px] cursor-pointer flex flex-col justify-between hover:shadow-md hover:scale-[1.02] transition-all duration-200 select-none relative overflow-hidden"
                    title="Click để thực hiện · Chuột phải để xem tùy chọn">
                    <div>
                      <div className="flex items-start justify-between mb-1">
                        <span style={{ color: 'var(--text-primary)' }} className="text-sm font-black tracking-wide">P.{p.maPhong}</span>
                        {p.trangThai === 'DELETED' && (
                          <span className="text-[8px] bg-red-50 dark:bg-red-950/20 text-red-600 dark:text-red-400 border border-red-200 dark:border-red-900 px-1 py-0.5 rounded font-extrabold uppercase tracking-wider">Xóa</span>
                        )}
                      </div>
                      <div className="text-[8px] text-[var(--text-secondary)] font-bold uppercase tracking-wider truncate">{p.loaiPhong?.tenLoaiPhong || '—'}</div>
                      
                      {p.trangThai === 'OCCUPIED' && p.tenKhachHienTai && (
                        <div className="text-[10px] font-medium truncate mt-1" style={{ color: 'var(--text-tertiary)' }}>
                          {p.tenKhachHienTai}
                        </div>
                      )}
                    </div>
                    <div>
                      <div className="mt-1">
                        <span className="inline-block px-1.5 py-0.5 rounded-[20px] text-[8px] font-bold uppercase tracking-wider"
                          style={{ backgroundColor: ttc.bg, color: ttc.text }}>
                          {ttc.label}
                        </span>
                      </div>
                      <div className="text-[9px] text-[var(--text-secondary)] font-semibold mt-1.5 pt-1 border-t border-black/5 dark:border-white/5">
                        {p.loaiPhong?.giaTheoNgay ? `${fmt(p.loaiPhong.giaTheoNgay)}đ` : '—'}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        ))
      )}

      {/* Legend */}
      <div className="flex flex-wrap gap-4 pt-2 border-t border-[var(--border-color)]">
        {Object.entries(TT_COLOR).map(([k, v]) => (
          <div key={k} className="flex items-center gap-2">
            <div style={{ backgroundColor: v.border }} className="w-2.5 h-2.5 rounded-full" />
            <span className="text-[10px] text-[var(--text-secondary)] font-extrabold uppercase tracking-wider">{v.label}</span>
          </div>
        ))}
        <span className="text-[10px] text-[var(--text-secondary)] font-bold">· Chuột phải lên ô phòng để xem thêm tùy chọn ca trực</span>
      </div>

      {/* Context Menu */}
      {ctxMenu && (
        <RoomContextMenu menu={ctxMenu} onAction={handleContextAction} onClose={() => setCtxMenu(null)} />
      )}

      {/* Status Confirm Dialog */}
      {statusDlg && (
        <StatusConfirmDialog data={statusDlg} onConfirm={handleStatusChange} onClose={() => setStatusDlg(null)} />
      )}

      {/* Detail Modal */}
      {showDetail && <RoomDetailModal room={selectedRoom} onClose={() => setShowDetail(false)} />}

      {/* Quick Service Dialog */}
      {showService && selectedRoom && (
        <QuickAddServiceDialog room={selectedRoom} onClose={() => setShowService(false)} onSuccess={fetchData} />
      )}

      {/* Quick Extend Dialog */}
      {showExtend && selectedRoom && (
        <QuickExtendDialog room={selectedRoom} onClose={() => setShowExtend(false)} onSuccess={fetchData} />
      )}

      {/* Quick Switch Room Dialog */}
      {showSwitch && selectedRoom && (
        <QuickSwitchRoomDialog room={selectedRoom} onClose={() => setShowSwitch(false)} onSuccess={fetchData} />
      )}

      {/* Check-in / Check-out */}
      <CheckinDialog
        isOpen={showCheckin}
        onClose={() => { setShowCheckin(false); setSelectedRoom(null); }}
        room={selectedRoom}
        onCheckinSuccess={fetchData}
      />
      <CheckoutDialog
        isOpen={showCheckout}
        onClose={() => { setShowCheckout(false); setSelectedRoom(null); }}
        room={selectedRoom}
        onCheckoutSuccess={fetchData}
      />
    </div>
  );
};

export default ThuePhongPage;
