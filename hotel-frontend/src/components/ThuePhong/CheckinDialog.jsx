import React, { useEffect, useState } from 'react';
import { khachHangAPI, datPhongAPI, thuePhongAPI, chiTietDatPhongAPI } from '../../api/api';
import { X, Search, Plus, ShieldCheck } from 'lucide-react';

export const CheckinDialog = ({ isOpen, onClose, room, onCheckinSuccess }) => {
  const [khachHangs, setKhachHangs] = useState([]);
  const [searchKH, setSearchKH] = useState('');
  const [selectedKH, setSelectedKH] = useState(null);
  const [sharingWarning, setSharingWarning] = useState('');

  useEffect(() => {
    if (selectedKH) {
      chiTietDatPhongAPI.getActive().then(res => {
        const activeStays = res.data;
        const sharingStay = activeStays.find(s => s.khachHang?.maKhachHang === selectedKH.maKhachHang);
        if (sharingStay) {
          setSharingWarning(`Khách hàng ${selectedKH.hoTen} đang đại diện cho phòng P.${sharingStay.phong?.maPhong}!`);
        } else {
          setSharingWarning('');
        }
      }).catch(console.error);
    } else {
      setSharingWarning('');
    }
  }, [selectedKH]);

  // Form fields
  const [ngayTraDuKien, setNgayTraDuKien] = useState(
    new Date(Date.now() + 24 * 3600 * 1000).toISOString().substring(0, 16)
  );
  const [tienCoc, setTienCoc] = useState(0);
  const [giaChot, setGiaChot] = useState(room?.loaiPhong?.giaTheoNgay || 400000);
  const [soNguoi, setSoNguoi] = useState(1);
  const [ghiChu, setGhiChu] = useState('');
  const [loaiKhach, setLoaiKhach] = useState('CA_NHAN');
  const [tenDoan, setTenDoan] = useState('');

  // Create new customer form
  const [showNewKHForm, setShowNewKHForm] = useState(false);
  const [newKH, setNewKH] = useState({ hoTen: '', soDienThoai: '', email: '', cccd: '', quocTich: 'Vietnam' });

  useEffect(() => {
    if (isOpen) {
      searchCustomers();
      if (room) setGiaChot(room.loaiPhong?.giaTheoNgay || 400000);
    }
  }, [isOpen, room]);

  const searchCustomers = async () => {
    try {
      const res = await khachHangAPI.getAll(searchKH);
      setKhachHangs(res.data);
    } catch (e) { console.error(e); }
  };

  const handleCreateCustomer = async (e) => {
    e.preventDefault();
    try {
      const res = await khachHangAPI.create(newKH);
      setSelectedKH(res.data);
      setShowNewKHForm(false);
      alert("Đã thêm khách hàng mới!");
    } catch (e) { alert(e.response?.data || "Lỗi tạo khách hàng"); }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!selectedKH) {
      alert("Vui lòng chọn hoặc thêm khách hàng đại diện!");
      return;
    }

    try {
      // 1. Create DatPhong first to get maDatPhong
      const dpData = {
        khachHang: selectedKH,
        ngayNhanDuKien: new Date().toISOString(),
        ngayTraDuKien: new Date(ngayTraDuKien).toISOString(),
        soNguoi: soNguoi,
        tienDatCoc: Number(tienCoc),
        ghiChu: ghiChu,
        loaiKhach: loaiKhach,
        tenDoan: loaiKhach === 'DOAN' ? tenDoan : null
      };

      const dpRes = await datPhongAPI.create(dpData);
      const savedDp = dpRes.data;

      // 2. Perform check-in with ChiTietDatPhong
      const ctdpData = {
        datPhong: savedDp,
        phong: room,
        khachHang: selectedKH,
        giaThucTeChot: Number(giaChot),
        phuPhiPhatSinh: 0,
        daThanhToan: false
      };

      await thuePhongAPI.checkIn(ctdpData);
      alert("Check-in thành công!");
      onCheckinSuccess();
      onClose();
    } catch (e) {
      alert(e.response?.data || "Có lỗi xảy ra trong quá trình check-in!");
    }
  };

  if (!isOpen) return null;

  return (
    <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="modal-panel flex flex-col" style={{ maxWidth: 672, maxHeight: '90vh' }}>
        <div className="px-5 py-4 border-b border-[var(--border-color)] flex items-center justify-between shrink-0">
          <h3 className="font-bold text-white text-base">Check-in Nhận Phòng: P.{room?.maPhong}</h3>
          <button onClick={onClose} className="text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition-colors"><X size={18} /></button>
        </div>

        <div className="p-5 overflow-y-auto space-y-4 flex-1">
          {/* Section 1: Customer Selection */}
          <div className="bg-[var(--bg-sidebar)] p-4 rounded-xl border border-[var(--border-color)] space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-[var(--text-secondary)] uppercase">Khách đại diện</span>
              <button type="button" onClick={() => setShowNewKHForm(!showNewKHForm)}
                className="text-xs text-blue-400 hover:underline flex items-center gap-1 font-semibold">
                <Plus size={14} /> {showNewKHForm ? 'Quay lại tìm kiếm' : 'Thêm khách hàng mới'}
              </button>
            </div>

            {showNewKHForm ? (
              <form onSubmit={handleCreateCustomer} className="grid grid-cols-2 gap-3 text-xs">
                <div>
                  <label className="text-[10px] text-[var(--text-secondary)] font-bold uppercase">Họ Tên</label>
                  <input type="text" required value={newKH.hoTen} onChange={e => setNewKH({ ...newKH, hoTen: e.target.value })}
                    className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-lg px-2 py-1.5 text-[var(--text-primary)] focus:border-blue-500 outline-none mt-1" />
                </div>
                <div>
                  <label className="text-[10px] text-[var(--text-secondary)] font-bold uppercase">Số Điện Thoại</label>
                  <input type="text" required value={newKH.soDienThoai} onChange={e => setNewKH({ ...newKH, soDienThoai: e.target.value })}
                    className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-lg px-2 py-1.5 text-[var(--text-primary)] focus:border-blue-500 outline-none mt-1" />
                </div>
                <div>
                  <label className="text-[10px] text-[var(--text-secondary)] font-bold uppercase">CCCD / Hộ chiếu</label>
                  <input type="text" required value={newKH.cccd} onChange={e => setNewKH({ ...newKH, cccd: e.target.value })}
                    className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-lg px-2 py-1.5 text-[var(--text-primary)] focus:border-blue-500 outline-none mt-1" />
                </div>
                <div>
                  <label className="text-[10px] text-[var(--text-secondary)] font-bold uppercase">Quốc tịch</label>
                  <input type="text" value={newKH.quocTich} onChange={e => setNewKH({ ...newKH, quocTich: e.target.value })}
                    className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-lg px-2 py-1.5 text-[var(--text-primary)] focus:border-blue-500 outline-none mt-1" />
                </div>
                <div className="col-span-2 flex justify-end">
                  <button type="submit" className="px-3 py-1.5 bg-blue-600 hover:bg-blue-500 text-white rounded-lg font-bold">Lưu khách đại diện</button>
                </div>
              </form>
            ) : selectedKH ? (
              <div className="space-y-2">
                <div className="p-3 bg-blue-50 dark:bg-blue-950/20 border border-blue-200 dark:border-blue-500/25 rounded-xl flex items-center justify-between">
                  <div>
                    <h4 className="font-bold text-white text-sm">{selectedKH.hoTen}</h4>
                    <div className="text-xs text-[var(--text-secondary)] flex items-center gap-3 mt-1">
                      <span>📞 {selectedKH.soDienThoai}</span>
                      <span>🪪 {selectedKH.cccd}</span>
                    </div>
                  </div>
                  <button onClick={() => setSelectedKH(null)} className="text-xs text-red-400 hover:underline">Thay đổi</button>
                </div>
                {sharingWarning && (
                  <div className="p-3 bg-amber-500/10 border border-amber-500/20 text-amber-500 rounded-xl text-xs font-semibold flex items-center gap-2">
                    <span>⚠️</span>
                    <span>{sharingWarning}</span>
                  </div>
                )}
              </div>
            ) : (
              <div className="space-y-2">
                <div className="flex gap-2">
                  <input type="text" placeholder="Nhập tên hoặc số điện thoại khách..." value={searchKH} onChange={e => setSearchKH(e.target.value)}
                    className="flex-1 bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2 text-sm text-[var(--text-primary)] focus:border-blue-500 outline-none" />
                  <button onClick={searchCustomers} className="px-3 bg-[var(--bg-sidebar)] hover:bg-[var(--border-color)] text-[var(--text-primary)] border border-[var(--border-color)] rounded-xl transition-all"><Search size={16} /></button>
                </div>
                <div className="max-h-32 overflow-y-auto divide-y divide-[var(--border-color)] text-xs">
                  {khachHangs.map(kh => (
                    <div key={kh.maKhachHang} onClick={() => setSelectedKH(kh)}
                      className="p-2 hover:bg-[var(--bg-main)]/40 cursor-pointer flex justify-between items-center text-[var(--text-secondary)]">
                      <span>{kh.hoTen} - {kh.soDienThoai}</span>
                      <span className="text-[10px] text-[var(--text-secondary)]">ID: {kh.maKhachHang}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Section 2: Booking Info */}
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-[10px] text-[var(--text-secondary)] font-bold uppercase">Loại khách</label>
                <select value={loaiKhach} onChange={e => setLoaiKhach(e.target.value)}
                  className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2 text-sm text-[var(--text-primary)] mt-1.5 focus:border-blue-500 outline-none">
                  <option value="CA_NHAN">Khách lẻ</option>
                  <option value="DOAN">Khách đoàn</option>
                </select>
              </div>
              {loaiKhach === 'DOAN' ? (
                <div>
                  <label className="text-[10px] text-[var(--text-secondary)] font-bold uppercase">Tên Đoàn</label>
                  <input type="text" required value={tenDoan} onChange={e => setTenDoan(e.target.value)} placeholder="Tên đoàn khách..."
                    className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2 text-sm text-[var(--text-primary)] mt-1.5 focus:border-blue-500 outline-none" />
                </div>
              ) : (
                <div>
                  <label className="text-[10px] text-[var(--text-secondary)] font-bold uppercase">Số người</label>
                  <input type="number" min={1} max={10} value={soNguoi} onChange={e => setSoNguoi(Number(e.target.value))}
                    className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2 text-sm text-[var(--text-primary)] mt-1.5 focus:border-blue-500 outline-none" />
                </div>
              )}

              <div>
                <label className="text-[10px] text-[var(--text-secondary)] font-bold uppercase">Giá chốt (Thực tế)</label>
                <input type="number" value={giaChot} onChange={e => setGiaChot(Number(e.target.value))}
                  className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2 text-sm text-[var(--text-primary)] mt-1.5 focus:border-blue-500 outline-none" />
              </div>
              <div>
                <label className="text-[10px] text-[var(--text-secondary)] font-bold uppercase">Tiền đặt cọc</label>
                <input type="number" value={tienCoc} onChange={e => setTienCoc(Number(e.target.value))}
                  className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2 text-sm text-[var(--text-primary)] mt-1.5 focus:border-blue-500 outline-none" />
              </div>

              <div className="col-span-2">
                <label className="text-[10px] text-[var(--text-secondary)] font-bold uppercase">Ngày trả dự kiến</label>
                <input type="datetime-local" required value={ngayTraDuKien} onChange={e => setNgayTraDuKien(e.target.value)}
                  className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2 text-sm text-[var(--text-primary)] mt-1.5 focus:border-blue-500 outline-none" />
              </div>

              <div className="col-span-2">
                <label className="text-[10px] text-[var(--text-secondary)] font-bold uppercase">Ghi chú check-in</label>
                <textarea value={ghiChu} onChange={e => setGhiChu(e.target.value)} rows={2}
                  className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2 text-sm text-[var(--text-primary)] mt-1.5 focus:border-blue-500 outline-none" />
              </div>
            </div>

            <div className="flex justify-end gap-2.5 pt-3 border-t border-[var(--border-color)]">
              <button type="button" onClick={onClose}
                className="px-4 py-2 bg-[var(--bg-main)] hover:bg-[var(--border-color)] text-[var(--text-secondary)] rounded-xl font-bold transition-all text-xs border border-[var(--border-color)]">Hủy</button>
              <button type="submit"
                className="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl font-bold transition-all text-xs shadow-lg shadow-blue-600/20 flex items-center gap-1.5">
                <ShieldCheck size={14} /> Xác nhận Check-in
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
