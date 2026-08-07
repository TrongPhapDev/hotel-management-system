import React, { useEffect, useState, useRef, useCallback } from 'react';
import { datPhongAPI, khachHangAPI, phongAPI, loaiPhongAPI } from '../api/api';
import { CalendarCheck, Plus, Search, X, Eye, Edit2, CheckCircle, XCircle, UserX, Trash2, ChevronDown, ChevronLeft, ChevronRight, Filter, Info, CreditCard, Users, Phone } from 'lucide-react';
import { MasterBillDialog } from '../components/MasterBillDialog';

const TT_CONFIG = {
  PENDING:              { label: 'Chờ xác nhận',  cls: 'badge-cleaning'    },
  CONFIRMED:            { label: 'Đã xác nhận',   cls: 'badge-occupied'    },
  PARTIALLY_CHECKED_IN: { label: 'Đang check-in', cls: 'badge-occupied'    },
  CHECKED_IN:           { label: 'Đang ở',         cls: 'badge-available'   },
  CHECKED_OUT:          { label: 'Đã trả phòng',  cls: ''                  },
  CANCELLED:            { label: 'Đã hủy',         cls: 'badge-maintenance' },
  NO_SHOW:              { label: 'Không đến',      cls: 'badge-maintenance' },
  WAITLIST:             { label: 'Chờ xếp phòng', cls: 'badge-cleaning'    },
};

const STATUS_FILTERS = [
  { key: '', label: 'Tất cả' },
  { key: 'PENDING', label: 'Chờ xác nhận' },
  { key: 'CONFIRMED', label: 'Đã xác nhận' },
  { key: 'CHECKED_IN', label: 'Đang ở' },
  { key: 'WAITLIST', label: 'Chờ xếp phòng' },
  { key: 'CANCELLED', label: 'Đã hủy' },
  { key: 'NO_SHOW', label: 'Không đến' },
];

const fmtDate = (dt) => dt ? new Date(dt).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '—';
const fmtMoney = (n) => (n || 0).toLocaleString('vi-VN') + 'đ';

// ===================== STATUS BADGE =====================
const StatusBadge = ({ status }) => {
  const cfg = TT_CONFIG[status] || { label: status, cls: '' };
  return (
    <span className={`badge ${cfg.cls}`}>{cfg.label}</span>
  );
};

// ===================== DETAIL MODAL =====================
const DetailModal = ({ dp, onClose, onAction }) => {
  if (!dp) return null;
  const cfg = TT_CONFIG[dp.trangThai] || {};
  const rooms = dp.dsChiTiet || [];

  return (
    <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="modal-panel" style={{ maxWidth: 672 }}>
        <div className="px-5 py-4 border-b border-[var(--border-color)] flex items-center justify-between">
          <div>
            <h3 className="font-bold text-[var(--text-primary)] text-base flex items-center gap-2">
              <CalendarCheck size={16} className="text-blue-500"/>Chi tiết đặt phòng: {dp.maDatPhong}
            </h3>
            <div className="mt-1"><StatusBadge status={dp.trangThai}/></div>
          </div>
          <button onClick={onClose} className="text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition-colors"><X size={18}/></button>
        </div>

        <div className="p-5 overflow-y-auto max-h-[75vh] space-y-4">
          {/* Guest Info */}
          <div className="grid grid-cols-2 gap-3">
            <div className="bg-[var(--bg-main)] rounded-xl p-3">
              <div className="text-[10px] font-bold text-[var(--text-secondary)] uppercase mb-1">Khách hàng</div>
              <div className="font-bold text-[var(--text-primary)]">{dp.khachHang?.hoTen || '—'}</div>
              <div className="text-xs text-[var(--text-secondary)] mt-0.5">{dp.khachHang?.sdt || '—'}</div>
            </div>
            <div className="bg-[var(--bg-main)] rounded-xl p-3">
              <div className="text-[10px] font-bold text-[var(--text-secondary)] uppercase mb-1">Loại khách</div>
              <div className="font-bold text-[var(--text-primary)]">{dp.loaiKhach === 'DOAN' ? '🏢 Khách đoàn' : '👤 Khách lẻ'}</div>
              {dp.tenDoan && <div className="text-xs text-[var(--text-secondary)] mt-0.5">{dp.tenDoan}</div>}
            </div>
            <div className="bg-[var(--bg-main)] rounded-xl p-3">
              <div className="text-[10px] font-bold text-[var(--text-secondary)] uppercase mb-1">Ngày nhận</div>
              <div className="font-semibold text-[var(--text-primary)]">{fmtDate(dp.ngayNhanDuKien)}</div>
            </div>
            <div className="bg-[var(--bg-main)] rounded-xl p-3">
              <div className="text-[10px] font-bold text-[var(--text-secondary)] uppercase mb-1">Ngày trả</div>
              <div className="font-semibold text-[var(--text-primary)]">{fmtDate(dp.ngayTraDuKien)}</div>
            </div>
            <div className="bg-[var(--bg-main)] rounded-xl p-3">
              <div className="text-[10px] font-bold text-[var(--text-secondary)] uppercase mb-1">Số người</div>
              <div className="font-semibold text-[var(--text-primary)]">{dp.soNguoi || '—'}</div>
            </div>
            <div className="bg-[var(--bg-main)] rounded-xl p-3">
              <div className="text-[10px] font-bold text-[var(--text-secondary)] uppercase mb-1">Tiền đặt cọc</div>
              <div className="font-semibold text-blue-600 dark:text-blue-400">{fmtMoney(dp.tienDatCoc)}</div>
            </div>
          </div>

          {/* Rooms */}
          {rooms.length > 0 && (
            <div>
              <div className="text-xs font-bold text-[var(--text-secondary)] uppercase mb-2">Danh sách phòng ({rooms.length})</div>
              <div className="space-y-1.5">
                {rooms.map((r, i) => (
                  <div key={i} className="bg-blue-50 dark:bg-blue-950/20 border border-blue-100 dark:border-blue-800/30 rounded-xl px-3 py-2 flex justify-between items-center text-xs">
                    <span className="font-bold text-blue-700 dark:text-blue-300">P.{r.phong?.maPhong}</span>
                    <span className="text-[var(--text-secondary)]">{r.phong?.loaiPhong?.tenLoaiPhong || '—'}</span>
                    <span className="font-semibold text-[var(--text-primary)]">{fmtMoney(r.giaThucTeChot)}/đêm</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {dp.ghiChu && (
            <div className="bg-yellow-50 dark:bg-yellow-950/20 border border-yellow-200 dark:border-yellow-800/30 rounded-xl p-3">
              <div className="text-[10px] font-bold text-yellow-600 dark:text-yellow-400 uppercase mb-1">Ghi chú</div>
              <div className="text-xs text-[var(--text-primary)]">{dp.ghiChu}</div>
            </div>
          )}

          {/* Action buttons */}
          <div className="flex flex-wrap gap-2 pt-2 border-t border-[var(--border-color)]">
            {['CONFIRMED', 'PARTIALLY_CHECKED_IN'].includes(dp.trangThai) && (
              <button onClick={() => { onAction('checkin', dp); onClose(); }}
                className="flex items-center gap-1.5 px-3 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold rounded-xl">
                <CheckCircle size={12}/>Nhận phòng
              </button>
            )}
            {dp.loaiKhach === 'DOAN' && ['CHECKED_IN', 'PARTIALLY_CHECKED_IN'].includes(dp.trangThai) && (
              <button onClick={() => { onAction('masterbill', dp); onClose(); }}
                className="flex items-center gap-1.5 px-3 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold rounded-xl">
                <CreditCard size={12}/>Thanh toán đoàn (Master Bill)
              </button>
            )}
            {['PENDING', 'CONFIRMED'].includes(dp.trangThai) && (
              <button onClick={() => { onAction('edit', dp); onClose(); }}
                className="flex items-center gap-1.5 px-3 py-2 bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold rounded-xl">
                <Edit2 size={12}/>Sửa đặt phòng
              </button>
            )}
            {['PENDING', 'CONFIRMED', 'WAITLIST'].includes(dp.trangThai) && (
              <button onClick={() => { onAction('cancel', dp); onClose(); }}
                className="flex items-center gap-1.5 px-3 py-2 bg-red-50 dark:bg-red-950/20 border border-red-200 dark:border-red-800/50 text-red-600 dark:text-red-400 text-xs font-bold rounded-xl">
                <XCircle size={12}/>Hủy đặt phòng
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

// ===================== EDIT MODAL =====================
const EditModal = ({ dp, khachHangs, onClose, onSaved }) => {
  const [form, setForm] = useState({
    soNguoi: dp.soNguoi || 1,
    ngayNhanDuKien: dp.ngayNhanDuKien ? dp.ngayNhanDuKien.substring(0, 10) : '',
    ngayTraDuKien: dp.ngayTraDuKien ? dp.ngayTraDuKien.substring(0, 10) : '',
    tienDatCoc: dp.tienDatCoc || 0,
    ghiChu: dp.ghiChu || '',
  });
  const [submitting, setSubmitting] = useState(false);
  const [err, setErr] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault(); setErr('');
    setSubmitting(true);
    try {
      const payload = { ...dp, ...form, ngayNhanDuKien: form.ngayNhanDuKien + 'T14:00:00', ngayTraDuKien: form.ngayTraDuKien + 'T12:00:00' };
      await datPhongAPI.update(dp.maDatPhong, payload);
      onSaved();
      onClose();
    } catch(e) { setErr(e.response?.data || 'Lỗi cập nhật'); }
    finally { setSubmitting(false); }
  };

  return (
    <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="modal-panel" style={{ maxWidth: 448 }}>
        <div className="flex items-center justify-between mb-4 pb-3 border-b border-[var(--border-color)]">
          <h3 className="font-bold text-[var(--text-primary)] flex items-center gap-2"><Edit2 size={16} className="text-blue-500"/>Sửa đặt phòng: {dp.maDatPhong}</h3>
          <button onClick={onClose}><X size={16} className="text-[var(--text-secondary)]"/></button>
        </div>
        {err && <div className="mb-3 p-3 bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-800/50 text-red-600 dark:text-red-400 text-xs rounded-xl">{err}</div>}
        <form onSubmit={handleSubmit} className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-[10px] font-bold text-[var(--text-secondary)] uppercase">Ngày nhận</label>
              <input type="date" value={form.ngayNhanDuKien} onChange={e => setForm({...form, ngayNhanDuKien: e.target.value})}
                className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] text-[var(--input-text)] rounded-xl px-3 py-2 text-sm text-[var(--text-primary)] mt-1 focus:border-blue-500 outline-none"/>
            </div>
            <div>
              <label className="text-[10px] font-bold text-[var(--text-secondary)] uppercase">Ngày trả</label>
              <input type="date" value={form.ngayTraDuKien} onChange={e => setForm({...form, ngayTraDuKien: e.target.value})}
                className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] text-[var(--input-text)] rounded-xl px-3 py-2 text-sm text-[var(--text-primary)] mt-1 focus:border-blue-500 outline-none"/>
            </div>
            <div>
              <label className="text-[10px] font-bold text-[var(--text-secondary)] uppercase">Số người</label>
              <input type="number" min={1} value={form.soNguoi} onChange={e => setForm({...form, soNguoi: parseInt(e.target.value)})}
                className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] text-[var(--input-text)] rounded-xl px-3 py-2 text-sm text-[var(--text-primary)] mt-1 focus:border-blue-500 outline-none"/>
            </div>
            <div>
              <label className="text-[10px] font-bold text-[var(--text-secondary)] uppercase">Tiền đặt cọc</label>
              <input type="number" min={0} value={form.tienDatCoc} onChange={e => setForm({...form, tienDatCoc: parseFloat(e.target.value)})}
                className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] text-[var(--input-text)] rounded-xl px-3 py-2 text-sm text-[var(--text-primary)] mt-1 focus:border-blue-500 outline-none"/>
            </div>
          </div>
          <div>
            <label className="text-[10px] font-bold text-[var(--text-secondary)] uppercase">Ghi chú</label>
            <textarea value={form.ghiChu} onChange={e => setForm({...form, ghiChu: e.target.value})} rows={2}
              className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] text-[var(--input-text)] rounded-xl px-3 py-2 text-sm text-[var(--text-primary)] mt-1 focus:border-blue-500 outline-none resize-none"/>
          </div>
          <div className="flex justify-end gap-2 pt-2 border-t border-[var(--border-color)]">
            <button type="button" onClick={onClose} className="px-4 py-2 bg-[var(--bg-main)] text-[var(--text-secondary)] text-xs font-semibold rounded-xl">Hủy</button>
            <button type="submit" disabled={submitting} className="px-5 py-2 bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold rounded-xl disabled:opacity-60">
              {submitting ? 'Đang lưu...' : 'Lưu thay đổi'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

// ===================== ADD MODAL =====================
const AddModal = ({ khachHangs, onClose, onSaved }) => {
  const [step, setStep] = useState(1);
  const [ngayNhan, setNgayNhan] = useState('');
  const [ngayTra, setNgayTra] = useState('');
  const [availableRooms, setAvailableRooms] = useState([]);
  const [loadingRooms, setLoadingRooms] = useState(false);
  const [selectedRooms, setSelectedRooms] = useState([]); // Array of Phong objects
  
  // Filters
  const [filterTang, setFilterTang] = useState('');
  const [filterLoaiPhong, setFilterLoaiPhong] = useState('');
  const [loaiPhongs, setLoaiPhongs] = useState([]);
  
  // Form fields for step 3
  const [form, setForm] = useState({
    khachHang: { maKhachHang: '' },
    soNguoi: 1,
    ghiChu: '',
    loaiKhach: 'CA_NHAN',
    tenDoan: '',
    tienDatCoc: 0
  });

  const [err, setErr] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Load loai phong
  useEffect(() => {
    loaiPhongAPI.getAll().then(r => setLoaiPhongs(r.data)).catch(console.error);
  }, []);

  const handleSearchRooms = async (e) => {
    e.preventDefault();
    if (!ngayNhan || !ngayTra) {
      setErr('Vui lòng chọn ngày nhận và ngày trả!');
      return;
    }
    if (new Date(ngayNhan) >= new Date(ngayTra)) {
      setErr('Ngày nhận phải trước ngày trả!');
      return;
    }
    setErr('');
    setLoadingRooms(true);
    try {
      const rNhan = ngayNhan + 'T14:00:00';
      const rTra = ngayTra + 'T12:00:00';
      const res = await phongAPI.getAvailableInRange(rNhan, rTra);
      setAvailableRooms(res.data);
      setSelectedRooms([]);
      setStep(2);
    } catch (error) {
      setErr(error.response?.data || 'Không thể tìm phòng trống');
    } finally {
      setLoadingRooms(false);
    }
  };

  const toggleRoom = (room) => {
    if (selectedRooms.find(r => r.maPhong === room.maPhong)) {
      setSelectedRooms(selectedRooms.filter(r => r.maPhong !== room.maPhong));
    } else {
      setSelectedRooms([...selectedRooms, room]);
    }
  };

  const handleCreateBooking = async (e) => {
    e.preventDefault();
    if (!form.khachHang.maKhachHang) {
      setErr('Vui lòng chọn khách hàng!');
      return;
    }
    if (selectedRooms.length === 0) {
      setErr('Vui lòng chọn ít nhất một phòng!');
      return;
    }
    setErr('');
    setSubmitting(true);
    try {
      const rNhan = ngayNhan + 'T14:00:00';
      const rTra  = ngayTra  + 'T12:00:00';

      // Calculate per-night price for each room
      const getRoomPrice = (room) => {
        const basePrice = room.loaiPhong?.giaTheoNgay || 0;
        const multiplier = room.huongNhin?.heSoGia || 1.0;
        return Math.round(basePrice * multiplier);
      };

      const dsChiTiet = selectedRooms.map(room => ({
        phong: { maPhong: room.maPhong },
        giaThucTeChot: getRoomPrice(room)
      }));

      const tongTienTamTinh = selectedRooms.reduce(
        (sum, r) => sum + getRoomPrice(r) * numDays, 0
      );

      const payload = {
        khachHang:       { maKhachHang: form.khachHang.maKhachHang },
        soNguoi:         form.soNguoi,
        ghiChu:          form.ghiChu,
        loaiKhach:       form.loaiKhach,
        tenDoan:         form.loaiKhach === 'DOAN' ? form.tenDoan : null,
        tienDatCoc:      form.tienDatCoc || 0,
        ngayNhanDuKien:  rNhan,
        ngayTraDuKien:   rTra,
        dsChiTiet,
        tongTienTamTinh,
      };

      console.log('[DatPhong] Tạo đặt phòng payload:', payload);
      await datPhongAPI.create(payload);
      onSaved();
      onClose();
    } catch (e) {
      console.error('[DatPhong] Lỗi tạo đặt phòng:', e.response?.data || e.message);
      const errMsg = e.response?.data;
      if (typeof errMsg === 'string') setErr(errMsg);
      else if (errMsg?.message) setErr(errMsg.message);
      else setErr('Lỗi tạo đặt phòng. Vui lòng kiểm tra lại thông tin!');
    } finally {
      setSubmitting(false);
    }
  };

  // Group rooms by Floor (tang)
  const roomsByFloor = availableRooms
    .filter(r => !filterTang || r.tang.toString() === filterTang)
    .filter(r => !filterLoaiPhong || r.loaiPhong?.maLoaiPhong === filterLoaiPhong)
    .reduce((groups, room) => {
      const f = room.tang;
      if (!groups[f]) groups[f] = [];
      groups[f].push(room);
      return groups;
    }, {});

  const uniqueFloors = [...new Set(availableRooms.map(r => r.tang))].sort((a,b) => a-b);

  const calculateDays = () => {
    if (!ngayNhan || !ngayTra) return 0;
    const diffTime = Math.abs(new Date(ngayTra) - new Date(ngayNhan));
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays || 1;
  };

  const numDays = calculateDays();

  // Helper: price per night for a room
  const getRoomPrice = (room) => {
    const basePrice = room.loaiPhong?.giaTheoNgay || 0;
    const multiplier = room.huongNhin?.heSoGia || 1.0;
    return Math.round(basePrice * multiplier);
  };

  const tongTienPhong = selectedRooms.reduce((sum, r) => sum + getRoomPrice(r) * numDays, 0);

  return (
    <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="modal-panel flex flex-col" style={{ maxWidth: 896, maxHeight: '90vh' }}>
        {/* Header */}
        <div className="px-6 py-4 border-b border-[var(--border-color)] flex items-center justify-between">
          <div>
            <h3 className="text-lg font-bold text-[var(--text-primary)] flex items-center gap-2">
              <CalendarCheck size={20} className="text-blue-500"/>
              Đặt phòng mới (Wizard)
            </h3>
            {/* Steps indicator */}
            <div className="flex items-center gap-2 mt-2">
              <span className="text-[11px] font-bold px-3 py-1 rounded-lg transition-all"
                style={{ background: step === 1 ? 'var(--accent)' : 'var(--bg-elevated)', color: step === 1 ? '#0d0f14' : 'var(--text-muted)' }}>1. Chọn ngày</span>
              <span style={{ color: 'var(--text-muted)', fontSize: 11 }}>→</span>
              <span className="text-[11px] font-bold px-3 py-1 rounded-lg transition-all"
                style={{ background: step === 2 ? 'var(--accent)' : 'var(--bg-elevated)', color: step === 2 ? '#0d0f14' : 'var(--text-muted)' }}>2. Chọn phòng ({selectedRooms.length})</span>
              <span style={{ color: 'var(--text-muted)', fontSize: 11 }}>→</span>
              <span className="text-[11px] font-bold px-3 py-1 rounded-lg transition-all"
                style={{ background: step === 3 ? 'var(--accent)' : 'var(--bg-elevated)', color: step === 3 ? '#0d0f14' : 'var(--text-muted)' }}>3. Thông tin & Lưu</span>
            </div>
          </div>
          <button onClick={onClose} className="text-[var(--text-secondary)] hover:text-[var(--text-primary)]"><X size={18}/></button>
        </div>

        {err && (
          <div className="mx-6 mt-4 p-3 bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-800/50 text-red-600 dark:text-red-400 text-xs rounded-xl flex items-center gap-2">
            <Info size={14} className="shrink-0"/>
            <span>{err}</span>
          </div>
        )}

        {/* Content Area */}
        <div className="p-6 overflow-y-auto flex-1">
          {/* STEP 1: Select Checkin / Checkout Dates */}
          {step === 1 && (
            <form onSubmit={handleSearchRooms} className="space-y-6 max-w-md mx-auto py-4">
              <div className="bg-[var(--input-bg)] border border-[var(--input-border)] text-[var(--input-text)] rounded-2xl p-5 space-y-4">
                <div>
                  <label className="block text-xs font-bold text-[var(--text-secondary)] uppercase tracking-wider mb-2">Ngày nhận phòng *</label>
                  <input required type="date" value={ngayNhan} onChange={e => setNgayNhan(e.target.value)}
                    className="w-full bg-[var(--bg-sidebar)] border border-[var(--border-color)] rounded-xl px-3 py-2.5 text-sm text-[var(--text-primary)] focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none transition-all"/>
                </div>
                <div>
                  <label className="block text-xs font-bold text-[var(--text-secondary)] uppercase tracking-wider mb-2">Ngày trả phòng *</label>
                  <input required type="date" value={ngayTra} onChange={e => setNgayTra(e.target.value)}
                    className="w-full bg-[var(--bg-sidebar)] border border-[var(--border-color)] rounded-xl px-3 py-2.5 text-sm text-[var(--text-primary)] focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none transition-all"/>
                </div>
              </div>

              <div className="flex justify-end gap-3 pt-3 border-t border-[var(--border-color)]">
                <button type="button" onClick={onClose} className="px-5 py-2.5 bg-[var(--bg-main)] text-[var(--text-secondary)] text-xs font-semibold rounded-xl hover:bg-[var(--border-color)]">Hủy</button>
                <button type="submit" disabled={loadingRooms} className="px-6 py-2.5 bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold rounded-xl shadow-lg shadow-blue-600/20 disabled:opacity-60 flex items-center gap-1.5">
                  {loadingRooms ? 'Đang tìm...' : 'Tìm phòng trống'}
                  <ChevronRight size={14}/>
                </button>
              </div>
            </form>
          )}

          {/* STEP 2: Choose Rooms */}
          {step === 2 && (
            <div className="space-y-4 flex flex-col h-full">
              {/* Filters & Summary */}
              <div className="flex flex-wrap items-center justify-between gap-3 bg-[var(--input-bg)] border border-[var(--input-border)] text-[var(--input-text)] rounded-xl p-3">
                <div className="flex flex-wrap items-center gap-3">
                  {/* Floor filter */}
                  <div className="flex items-center gap-1.5">
                    <Filter size={12} className="text-[var(--text-secondary)]"/>
                    <select value={filterTang} onChange={e => setFilterTang(e.target.value)}
                      className="bg-[var(--bg-sidebar)] border border-[var(--border-color)] rounded-lg px-2.5 py-1 text-xs text-[var(--text-secondary)] focus:outline-none">
                      <option value="">Tất cả tầng</option>
                      {uniqueFloors.map(f => <option key={f} value={f}>Tầng {f}</option>)}
                    </select>
                  </div>
                  {/* Type filter */}
                  <div className="flex items-center gap-1.5">
                    <select value={filterLoaiPhong} onChange={e => setFilterLoaiPhong(e.target.value)}
                      className="bg-[var(--bg-sidebar)] border border-[var(--border-color)] rounded-lg px-2.5 py-1 text-xs text-[var(--text-secondary)] focus:outline-none">
                      <option value="">Tất cả loại phòng</option>
                      {loaiPhongs.map(lp => <option key={lp.maLoaiPhong} value={lp.maLoaiPhong}>{lp.tenLoaiPhong}</option>)}
                    </select>
                  </div>
                </div>
                <div className="text-xs text-[var(--text-secondary)] font-semibold">
                  Đã chọn: <span className="text-blue-600 dark:text-blue-400 font-bold">{selectedRooms.length} phòng</span> | Thời gian: <span className="font-bold">{numDays} đêm</span>
                </div>
              </div>

              {/* Room Grid */}
              {Object.keys(roomsByFloor).length === 0 ? (
                <div className="text-center py-12 text-[var(--text-secondary)] bg-[var(--bg-main)]/30 dark:bg-[var(--bg-main)]/20 border border-[var(--border-color)] rounded-2xl">
                  Không tìm thấy phòng trống phù hợp với điều kiện lọc.
                </div>
              ) : (
                <div className="space-y-6">
                  {Object.keys(roomsByFloor).sort().map(floor => (
                    <div key={floor} className="space-y-2">
                      <h4 className="text-xs font-bold text-[var(--text-secondary)] uppercase tracking-wider">Tầng {floor}</h4>
                      <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 gap-3">
                        {roomsByFloor[floor].map(room => {
                            const isSelected = selectedRooms.some(r => r.maPhong === room.maPhong);
                          const giaPhong = room.loaiPhong?.giaTheoNgay
                            ? Math.round(room.loaiPhong.giaTheoNgay * (room.huongNhin?.heSoGia || 1.0))
                            : 0;
                          return (
                            <div key={room.maPhong} onClick={() => toggleRoom(room)}
                              className="cursor-pointer rounded-xl p-3.5 relative overflow-hidden flex flex-col gap-1.5 transition-all"
                              style={{
                                background: isSelected ? 'var(--accent-dim)' : 'var(--bg-elevated)',
                                border: `1px solid ${isSelected ? 'var(--accent)' : 'var(--border)'}`,
                                boxShadow: isSelected ? 'var(--shadow-accent)' : 'none',
                              }}>
                              {isSelected && <div className="absolute top-0 right-0 w-4 h-4 rounded-bl-lg flex items-center justify-center text-[9px] font-black" style={{ background: 'var(--accent)', color: '#0d0f14' }}>✓</div>}
                              <div className="font-extrabold text-[13px]" style={{ color: 'var(--text-primary)' }}>P.{room.maPhong}</div>
                              <div className="text-[10px] truncate" style={{ color: 'var(--text-muted)' }}>{room.loaiPhong?.tenLoaiPhong}</div>
                              <div className="text-[11px] font-bold tabular-nums mt-1" style={{ color: 'var(--accent)' }}>{fmtMoney(giaPhong)}</div>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {/* Bottom Actions */}
              <div className="flex justify-between items-center pt-4 border-t border-[var(--border-color)] mt-auto">
                <button type="button" onClick={() => setStep(1)} className="px-4 py-2 bg-[var(--bg-main)] text-[var(--text-secondary)] text-xs font-semibold rounded-xl hover:bg-[var(--border-color)] flex items-center gap-1.5">
                  <ChevronLeft size={14}/> Quay lại
                </button>
                <div className="flex items-center gap-3">
                  <div className="text-right hidden sm:block">
                    <div className="text-[10px] text-[var(--text-secondary)] uppercase font-bold">Tổng tạm tính</div>
                    <div className="text-sm font-bold text-blue-600 dark:text-blue-400">{fmtMoney(tongTienPhong)}</div>
                  </div>
                  <button type="button" onClick={() => {
                    if (selectedRooms.length === 0) {
                      setErr('Vui lòng chọn ít nhất một phòng!');
                      return;
                    }
                    setErr('');
                    setStep(3);
                  }} className="px-6 py-2.5 bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold rounded-xl shadow-lg shadow-blue-600/20 flex items-center gap-1.5">
                    Tiếp tục
                    <ChevronRight size={14}/>
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* STEP 3: Customer Information & Confirm */}
          {step === 3 && (
            <form onSubmit={handleCreateBooking} className="space-y-6 max-w-2xl mx-auto py-2">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* Left col - Customer Select */}
                <div className="space-y-4">
                  <div>
                    <label className="block text-xs font-bold text-[var(--text-secondary)] uppercase tracking-wider mb-2">Khách hàng đại diện *</label>
                    <select required value={form.khachHang.maKhachHang} onChange={e => setForm({...form, khachHang: {maKhachHang: e.target.value}})}
                      className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] text-[var(--input-text)] rounded-xl px-3 py-2.5 text-sm text-[var(--text-primary)] focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none transition-all">
                      <option value="">-- Chọn khách hàng --</option>
                      {khachHangs.map(kh => <option key={kh.maKhachHang} value={kh.maKhachHang}>{kh.hoTen} ({kh.sdt})</option>)}
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-[var(--text-secondary)] uppercase tracking-wider mb-2">Loại khách hàng</label>
                    <select value={form.loaiKhach} onChange={e => setForm({...form, loaiKhach: e.target.value})}
                      className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] text-[var(--input-text)] rounded-xl px-3 py-2.5 text-sm text-[var(--text-primary)] focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none transition-all">
                      <option value="CA_NHAN">👤 Khách lẻ</option>
                      <option value="DOAN">🏢 Khách đoàn</option>
                    </select>
                  </div>
                  {form.loaiKhach === 'DOAN' && (
                    <div>
                      <label className="block text-xs font-bold text-[var(--text-secondary)] uppercase tracking-wider mb-2">Tên đoàn</label>
                      <input type="text" value={form.tenDoan} onChange={e => setForm({...form, tenDoan: e.target.value})} placeholder="VD: Đoàn Công ty ABC..."
                        className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] text-[var(--input-text)] rounded-xl px-3 py-2.5 text-sm text-[var(--text-primary)] focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none transition-all"/>
                    </div>
                  )}
                </div>

                {/* Right col - Numbers & Notes */}
                <div className="space-y-4">
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-xs font-bold text-[var(--text-secondary)] uppercase tracking-wider mb-2">Số người</label>
                      <input type="number" min={1} value={form.soNguoi} onChange={e => setForm({...form, soNguoi: parseInt(e.target.value)})}
                        className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] text-[var(--input-text)] rounded-xl px-3 py-2.5 text-sm text-[var(--text-primary)] focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none transition-all"/>
                    </div>
                    <div>
                      <label className="block text-xs font-bold text-[var(--text-secondary)] uppercase tracking-wider mb-2">Tiền đặt cọc</label>
                      <input type="number" min={0} value={form.tienDatCoc} onChange={e => setForm({...form, tienDatCoc: parseFloat(e.target.value)})}
                        className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] text-[var(--input-text)] rounded-xl px-3 py-2.5 text-sm text-[var(--text-primary)] focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none transition-all"/>
                    </div>
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-[var(--text-secondary)] uppercase tracking-wider mb-2">Ghi chú</label>
                    <textarea value={form.ghiChu} onChange={e => setForm({...form, ghiChu: e.target.value})} rows={3} placeholder="Ghi chú thêm..."
                      className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] text-[var(--input-text)] rounded-xl px-3 py-2.5 text-sm text-[var(--text-primary)] focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none resize-none transition-all"/>
                  </div>
                </div>
              </div>

              {/* Order summary card */}
              <div className="bg-blue-50/50 dark:bg-[var(--bg-sidebar)] border border-blue-100 rounded-2xl p-4 space-y-2.5">
                <h4 className="text-xs font-extrabold text-blue-700 dark:text-blue-400 uppercase tracking-wider flex items-center gap-1.5"><CreditCard size={14}/> Tóm tắt đặt phòng</h4>
                <div className="grid grid-cols-2 gap-y-1.5 text-xs text-[var(--text-secondary)]">
                  <div>Thời gian:</div>
                  <div className="font-semibold text-slate-800 dark:text-[var(--text-primary)] text-right">{fmtDate(ngayNhan)} ➔ {fmtDate(ngayTra)} ({numDays} đêm)</div>
                  <div>Phòng chọn:</div>
                  <div className="font-semibold text-slate-800 dark:text-[var(--text-primary)] text-right">{selectedRooms.map(r => `P.${r.maPhong}`).join(', ')}</div>
                  <div className="border-t border-[var(--border-color)] pt-2 font-bold text-[var(--text-primary)]">Tổng tiền phòng:</div>
                  <div className="border-t border-[var(--border-color)] pt-2 font-extrabold text-blue-600 dark:text-blue-400 text-right">{fmtMoney(tongTienPhong)}</div>
                </div>
              </div>

              {/* Form Buttons */}
              <div className="flex justify-between items-center pt-4 border-t border-[var(--border-color)]">
                <button type="button" onClick={() => setStep(2)} className="px-4 py-2 bg-[var(--bg-main)] text-[var(--text-secondary)] text-xs font-semibold rounded-xl hover:bg-[var(--border-color)] flex items-center gap-1.5">
                  <ChevronLeft size={14}/> Quay lại
                </button>
                <div className="flex gap-2">
                  <button type="button" onClick={onClose} className="px-4 py-2 bg-[var(--bg-main)] text-[var(--text-secondary)] text-xs font-semibold rounded-xl hover:bg-[var(--border-color)]">Hủy</button>
                  <button type="submit" disabled={submitting} className="px-6 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold rounded-xl shadow-lg shadow-emerald-600/20 disabled:opacity-60 flex items-center gap-1.5">
                    {submitting ? 'Đang tạo...' : 'Xác nhận tạo đặt phòng'}
                    <CheckCircle size={14}/>
                  </button>
                </div>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};

// ===================== CONTEXT MENU =====================
const ContextMenu = ({ menu, onAction, onClose }) => {
  const { dp } = menu;
  const ref = useRef(null);

  useEffect(() => {
    const handleClick = (e) => { if (ref.current && !ref.current.contains(e.target)) onClose(); };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, [onClose]);

  const style = { position: 'fixed', top: menu.y, left: menu.x, zIndex: 9999 };
  if (menu.x + 230 > window.innerWidth) style.left = menu.x - 230;

  const items = [
    { label: 'Xem chi tiết', action: 'detail', color: 'text-[var(--text-secondary)]', always: true },
  ];

  if (['CONFIRMED', 'PARTIALLY_CHECKED_IN'].includes(dp.trangThai)) {
    items.push({ separator: true });
    items.push({ label: 'Nhận phòng (Check-in)', action: 'checkin', color: 'text-emerald-600 dark:text-emerald-400', bold: true });
  }
  if (dp.loaiKhach === 'DOAN' && ['CHECKED_IN', 'PARTIALLY_CHECKED_IN'].includes(dp.trangThai)) {
    items.push({ separator: true });
    items.push({ label: 'Thanh toán đoàn (Master Bill)', action: 'masterbill', color: 'text-emerald-600 dark:text-emerald-400', bold: true });
  }
  if (['PENDING', 'CONFIRMED'].includes(dp.trangThai)) {
    if (items.length < 3) items.push({ separator: true });
    items.push({ label: 'Sửa đặt phòng', action: 'edit', color: 'text-blue-600 dark:text-blue-400' });
  }
  if (['PENDING', 'WAITLIST'].includes(dp.trangThai)) {
    items.push({ label: 'Thu tiền cọc', action: 'deposit', color: 'text-amber-600 dark:text-amber-400' });
    items.push({ label: 'Xác nhận đặt phòng', action: 'confirm', color: 'text-emerald-600 dark:text-emerald-400', bold: true });
  }
  if (dp.trangThai === 'CONFIRMED') {
    items.push({ label: 'Đánh dấu Không đến', action: 'noshow', color: 'text-red-600 dark:text-red-400' });
  }
  if (['PENDING', 'CONFIRMED', 'WAITLIST'].includes(dp.trangThai)) {
    items.push({ separator: true });
    items.push({ label: 'Hủy đặt phòng', action: 'cancel', color: 'text-rose-600 dark:text-rose-400' });
  }
  if (['PENDING', 'CANCELLED', 'WAITLIST'].includes(dp.trangThai)) {
    if (dp.trangThai === 'CANCELLED') items.push({ separator: true });
    items.push({ label: 'Xóa vĩnh viễn', action: 'delete', color: 'text-red-700 dark:text-red-500' });
  }

  return (
    <div ref={ref} style={style}
      className="bg-[var(--bg-sidebar)] border border-[var(--border-color)] rounded-xl shadow-2xl py-1 min-w-[220px] overflow-hidden">
      <div className="px-3 py-2 border-b border-[var(--border-color)]">
        <span className="text-xs font-bold text-[var(--text-secondary)] font-mono">{dp.maDatPhong}</span>
        <span className="ml-2"><StatusBadge status={dp.trangThai}/></span>
      </div>
      {items.map((item, idx) => item.separator ? (
        <div key={idx} className="my-1 border-t border-[var(--border-color)]"/>
      ) : (
        <button key={idx} onClick={() => onAction(item.action, dp)}
          className={`w-full text-left px-3 py-2 text-xs font-semibold hover:bg-[var(--bg-main)] dark:hover:bg-[var(--bg-main)]/50 transition-colors ${item.color} ${item.bold ? 'font-extrabold' : ''}`}>
          {item.label}
        </button>
      ))}
    </div>
  );
};

// ===================== CONFIRM DIALOG =====================
const ConfirmDialog = ({ data, onConfirm, onClose }) => (
  <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) onClose(); }}>
    <div className="modal-panel" style={{ maxWidth: 384, padding: 24 }}>
      <div className="text-base font-bold text-[var(--text-primary)] mb-2">{data.title}</div>
      <div className="text-sm text-[var(--text-secondary)] mb-5">{data.message}</div>
      {data.input && (
        <input type="text" value={data.inputValue || ''} onChange={e => data.onInputChange(e.target.value)}
          placeholder={data.inputPlaceholder}
          className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] text-[var(--input-text)] rounded-xl px-3 py-2 text-sm text-[var(--text-primary)] mb-4 focus:border-blue-500 outline-none"/>
      )}
      <div className="flex justify-end gap-3">
        <button onClick={onClose} className="px-4 py-2 bg-[var(--bg-main)] text-[var(--text-secondary)] text-xs font-semibold rounded-xl hover:bg-[var(--border-color)]">Hủy</button>
        <button onClick={onConfirm} className={`px-5 py-2 text-white text-xs font-bold rounded-xl ${data.danger ? 'bg-red-600 hover:bg-red-500' : 'bg-blue-600 hover:bg-blue-500'}`}>
          {data.confirmText || 'Xác nhận'}
        </button>
      </div>
    </div>
  </div>
);

// ===================== MAIN PAGE =====================
const DatPhongPage = () => {
  const [items, setItems] = useState([]);
  const [kw, setKw] = useState('');
  const [filter, setFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [khachHangs, setKhachHangs] = useState([]);

  // Active dialogs
  const [showAdd, setShowAdd] = useState(false);
  const [detailDp, setDetailDp] = useState(null);
  const [editDp, setEditDp] = useState(null);
  const [ctxMenu, setCtxMenu] = useState(null);
  const [confirmDlg, setConfirmDlg] = useState(null);
  const [cancelReason, setCancelReason] = useState('');
  const [showMasterBill, setShowMasterBill] = useState(false);
  const [masterBillMaDP, setMasterBillMaDP] = useState(null);

  const fetchData = useCallback(() => {
    setLoading(true);
    datPhongAPI.getAll(kw, filter).then(r => setItems(r.data)).catch(console.error).finally(() => setLoading(false));
  }, [kw, filter]);

  useEffect(() => { fetchData(); }, [fetchData]);
  useEffect(() => { khachHangAPI.getAll().then(r => setKhachHangs(r.data)).catch(console.error); }, []);

  // KPI stats
  const kpiData = [
    { label: 'Tổng đặt phòng', value: items.length },
    { label: 'Chờ xác nhận',   value: items.filter(i => i.trangThai === 'PENDING').length },
    { label: 'Đã xác nhận',    value: items.filter(i => i.trangThai === 'CONFIRMED').length },
    { label: 'Đang ở',         value: items.filter(i => ['CHECKED_IN', 'PARTIALLY_CHECKED_IN'].includes(i.trangThai)).length },
  ];

  const handleAction = async (action, dp) => {
    setCtxMenu(null);
    switch (action) {
      case 'detail': setDetailDp(dp); break;
      case 'edit': setEditDp(dp); break;

      case 'checkin':
        try { await datPhongAPI.checkIn(dp.maDatPhong); fetchData(); }
        catch(e) { alert(e.response?.data || 'Lỗi check-in'); }
        break;

      case 'confirm':
        setConfirmDlg({
          title: 'Xác nhận đặt phòng',
          message: `Xác nhận đặt phòng cho "${dp.khachHang?.hoTen}"?`,
          confirmText: 'Xác nhận',
          onConfirm: async () => {
            try { await datPhongAPI.update(dp.maDatPhong, { ...dp, trangThai: 'CONFIRMED' }); fetchData(); }
            catch(e) { alert(e.response?.data || 'Lỗi'); }
            setConfirmDlg(null);
          }
        });
        break;

      case 'noshow':
        setConfirmDlg({
          title: 'Đánh dấu Khách không đến',
          message: `Đánh dấu đặt phòng "${dp.maDatPhong}" của "${dp.khachHang?.hoTen}" là No-show?`,
          confirmText: 'Xác nhận No-show',
          danger: true,
          onConfirm: async () => {
            try { await datPhongAPI.update(dp.maDatPhong, { ...dp, trangThai: 'NO_SHOW' }); fetchData(); }
            catch(e) { alert(e.response?.data || 'Lỗi'); }
            setConfirmDlg(null);
          }
        });
        break;

      case 'deposit':
        alert(`Chức năng thu tiền cọc cho đặt phòng ${dp.maDatPhong}`);
        break;

      case 'cancel':
        setCancelReason('');
        setConfirmDlg({
          title: 'Hủy đặt phòng',
          message: `Hủy đặt phòng "${dp.maDatPhong}" của "${dp.khachHang?.hoTen}"?`,
          input: true,
          inputPlaceholder: 'Nhập lý do hủy...',
          inputValue: cancelReason,
          onInputChange: (v) => { setCancelReason(v); },
          confirmText: 'Hủy đặt phòng',
          danger: true,
          onConfirm: async () => {
            try { await datPhongAPI.huy(dp.maDatPhong, cancelReason || 'Không có lý do'); fetchData(); }
            catch(e) { alert(e.response?.data || 'Lỗi hủy'); }
            setConfirmDlg(null);
          }
        });
        break;

      case 'delete':
        setConfirmDlg({
          title: '⚠️ Xóa vĩnh viễn',
          message: `Xóa hoàn toàn đặt phòng "${dp.maDatPhong}" của "${dp.khachHang?.hoTen}"? Hành động này không thể hoàn tác!`,
          confirmText: 'Xóa vĩnh viễn',
          danger: true,
          onConfirm: async () => {
            try { /* datPhongAPI.delete(dp.maDatPhong) */ fetchData(); }
            catch(e) { alert(e.response?.data || 'Lỗi xóa'); }
            setConfirmDlg(null);
          }
        });
        break;

      case 'masterbill':
        setMasterBillMaDP(dp.maDatPhong);
        setShowMasterBill(true);
        break;

      default: break;
    }
  };

  return (
    <div className="page-shell" onClick={() => ctxMenu && setCtxMenu(null)}>
      {/* Header */}
      <div className="page-header">
        <div>
          <div className="page-label">Vận hành</div>
          <h1 className="page-title flex items-center gap-2">
            <CalendarCheck style={{ width: 20, height: 20, color: 'var(--text-secondary)' }} strokeWidth={2} />
            Đặt phòng trước
          </h1>
          <p className="page-subtitle">Chuột phải để tùy chọn · Double-click để xem chi tiết</p>
        </div>
        <button onClick={() => setShowAdd(true)} className="btn-primary">
          <Plus style={{ width: 15, height: 15 }} />Đặt phòng mới
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
 
      {/* Filter bar */}
      <div className="bg-[var(--bg-sidebar)] p-5 rounded-2xl border border-[var(--border-color)] flex flex-wrap items-center gap-4">
        {/* Search */}
        <div className="relative flex-1 min-w-[200px]">
          <Search size={14} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[var(--text-secondary)]"/>
          <input type="text" placeholder="Tìm mã đặt phòng, tên khách, SĐT..." value={kw} onChange={e => setKw(e.target.value)}
            className="input-style pl-10 h-10 w-full" />
          {kw && (
            <button onClick={() => setKw('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition-colors">
              <X size={12}/>
            </button>
          )}
        </div>

        {/* Status filter */}
        <div className="w-52">
          <select value={filter} onChange={e => setFilter(e.target.value)}
            className="h-10 px-3 bg-[var(--bg-sidebar)] border border-[var(--border-color)] rounded-xl text-xs text-[var(--text-primary)] focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 font-bold transition-all w-full">
            {STATUS_FILTERS.map(sf => (
              <option key={sf.key} value={sf.key}>{sf.key === '' ? 'Tất cả trạng thái' : sf.label}</option>
            ))}
          </select>
        </div>

        {/* Result count */}
        <span className="ml-auto text-[11px] font-semibold text-[var(--text-muted)] px-3 py-1.5 rounded-full bg-[var(--bg-main)] border border-[var(--border-color)] whitespace-nowrap">
          {items.length} đặt phòng
        </span>
      </div>

      {/* Table */}
      <div className="section-box">
        <table className="data-table">
          <thead>
            <tr>
              <th>Mã ĐP</th>
              <th>Khách hàng</th>
              <th>Loại</th>
              <th>Phòng</th>
              <th>Check-in</th>
              <th>Check-out</th>
              <th className="text-center">Số người</th>
              <th className="text-right">Đặt cọc</th>
              <th>Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={9} className="py-16 text-center">
                <div className="spinner mx-auto mb-2" />
                <div style={{ color: 'var(--text-muted)', fontSize: 12 }}>Đang tải...</div>
              </td></tr>
            ) : items.length === 0 ? (
              <tr><td colSpan={9} className="py-14 text-center" style={{ color: 'var(--text-muted)', fontSize: 13 }}>
                Không có dữ liệu
              </td></tr>
            ) : items.map(dp => (
              <tr key={dp.maDatPhong} className="cursor-pointer group"
                onDoubleClick={() => setDetailDp(dp)}
                onContextMenu={e => { e.preventDefault(); setCtxMenu({ x: e.clientX, y: e.clientY, dp }); }}>
                <td className="font-mono-data font-bold text-[12px]" style={{ color: 'var(--accent)' }}>{dp.maDatPhong}</td>
                <td>
                  <div className="flex items-center gap-2.5">
                    <div className="w-[26px] h-[26px] rounded-full flex items-center justify-center text-[10px] font-black shrink-0"
                      style={{ background: 'var(--accent-dim)', color: 'var(--accent)', border: '1px solid var(--accent-border)' }}>
                      {dp.khachHang?.hoTen?.charAt(0) || '?'}
                    </div>
                    <div>
                      <div className="font-semibold text-[13px]" style={{ color: 'var(--text-primary)' }}>{dp.khachHang?.hoTen || '—'}</div>
                      <div className="text-[11px] flex items-center gap-0.5" style={{ color: 'var(--text-secondary)' }}>
                        {dp.khachHang?.sdt ? <span className="flex items-center gap-0.5"><Phone size={9} />{dp.khachHang.sdt}</span> : '—'}
                      </div>
                    </div>
                  </div>
                </td>
                <td>
                  <span className="text-[10px] font-semibold px-2 py-0.5 rounded-md"
                    style={{ background: 'var(--bg-elevated)', color: 'var(--text-secondary)', border: '1px solid var(--border)' }}>
                    {dp.loaiKhach === 'DOAN' ? 'Đoàn' : 'Lẻ'}
                  </span>
                </td>
                <td className="font-mono-data text-[11px]" style={{ color: 'var(--text-secondary)' }}>{dp.dsChiTiet?.length > 0 ? dp.dsChiTiet.map(c => c.phong?.maPhong).join(', ') : '—'}</td>
                <td className="font-mono-data text-[12px]" style={{ color: 'var(--text-secondary)' }}>{fmtDate(dp.ngayNhanDuKien)}</td>
                <td className="font-mono-data text-[12px]" style={{ color: 'var(--text-secondary)' }}>{fmtDate(dp.ngayTraDuKien)}</td>
                <td className="text-center tabular-nums" style={{ color: 'var(--text-secondary)' }}>{dp.soNguoi || '—'}</td>
                <td className="text-right tabular-nums font-semibold" style={{ color: 'var(--text-primary)' }}>{dp.tienDatCoc > 0 ? fmtMoney(dp.tienDatCoc) : '—'}</td>
                <td className="row-actions-cell">
                  <StatusBadge status={dp.trangThai} />

                  {/* Hover Actions Panel */}
                  <div className="row-hover-actions">
                    <button onClick={(e) => { e.stopPropagation(); setDetailDp(dp); }} className="row-action-btn" title="Xem chi tiết">
                      <Eye style={{ width: 12, height: 12 }} />
                    </button>
                    <button onClick={(e) => { e.stopPropagation(); setEditDp(dp); }} className="row-action-btn" title="Chỉnh sửa">
                      <Edit2 style={{ width: 11, height: 11 }} />
                    </button>
                    {['PENDING', 'CONFIRMED'].includes(dp.trangThai) && (
                      <button onClick={(e) => { e.stopPropagation(); handleAction('cancel', dp); }} className="row-action-btn danger" title="Hủy đặt">
                        <X style={{ width: 11, height: 11 }} />
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!loading && items.length > 0 && (
          <div className="table-footer">
            <span>Tổng cộng: <strong style={{ color: 'var(--text-primary)' }}>{items.length} đặt phòng</strong></span>
            <span>Double-click để xem chi tiết · Chuột phải để tùy chọn</span>
          </div>
        )}
      </div>

      {/* Context Menu */}
      {ctxMenu && <ContextMenu menu={ctxMenu} onAction={handleAction} onClose={() => setCtxMenu(null)}/>}

      {/* Detail Modal */}
      {detailDp && <DetailModal dp={detailDp} onClose={() => setDetailDp(null)} onAction={handleAction}/>}

      {/* Edit Modal */}
      {editDp && <EditModal dp={editDp} khachHangs={khachHangs} onClose={() => setEditDp(null)} onSaved={fetchData}/>}

      {/* Add Modal */}
      {showAdd && <AddModal khachHangs={khachHangs} onClose={() => setShowAdd(false)} onSaved={fetchData}/>}

      {/* Confirm Dialog */}
      {confirmDlg && <ConfirmDialog data={confirmDlg} onConfirm={confirmDlg.onConfirm} onClose={() => setConfirmDlg(null)}/>}

      {/* Master Bill Dialog */}
      {showMasterBill && (
        <MasterBillDialog 
          isOpen={showMasterBill} 
          onClose={() => { setShowMasterBill(false); setMasterBillMaDP(null); }} 
          maDatPhong={masterBillMaDP} 
          onCheckoutSuccess={fetchData}
        />
      )}
    </div>
  );
};

export default DatPhongPage;
