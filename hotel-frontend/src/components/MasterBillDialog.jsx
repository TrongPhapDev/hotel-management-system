import React, { useEffect, useState, useCallback } from 'react';
import { chiTietDatPhongAPI, thuePhongAPI, datPhongAPI } from '../api/api';
import { X, ShieldCheck, FileText, Calendar, CreditCard, QrCode, DollarSign } from 'lucide-react';

const fmt = (n) => (n || 0).toLocaleString('vi-VN');

export const MasterBillDialog = ({ isOpen, onClose, maDatPhong, onCheckoutSuccess }) => {
  const [booking, setBooking] = useState(null);
  const [roomPreviews, setRoomPreviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [voucherCode, setVoucherCode] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('CASH');

  const loadMasterBillData = useCallback(async () => {
    if (!maDatPhong) return;
    setLoading(true);
    try {
      // 1. Get booking details
      const dpRes = await datPhongAPI.getById(maDatPhong);
      setBooking(dpRes.data);

      // 2. Get all stays (chi tiet) for this booking
      const staysRes = await chiTietDatPhongAPI.getAll(maDatPhong);
      const unpaidStays = staysRes.data.filter(s => !s.daThanhToan);

      // 3. Load previews for each segment in parallel
      const previewPromises = unpaidStays.map(async (stay) => {
        try {
          const res = await thuePhongAPI.previewCheckout(stay.maChiTiet, {
            voucherCode: '',
            customDeposit: 0 // Do not apply deposit at room level
          });
          return res.data;
        } catch (err) {
          console.error(`Error loading preview for stay ${stay.maChiTiet}`, err);
          return null;
        }
      });

      const previews = await Promise.all(previewPromises);
      setRoomPreviews(previews.filter(p => p !== null));
    } catch (e) {
      console.error(e);
    }
    setLoading(false);
  }, [maDatPhong]);

  useEffect(() => {
    if (isOpen) {
      loadMasterBillData();
    }
  }, [isOpen, loadMasterBillData]);

  if (!isOpen) return null;

  // Aggregate totals
  const totalRoomPrice = roomPreviews.reduce((acc, curr) => acc + curr.tienPhong, 0);
  const totalSurcharges = roomPreviews.reduce((acc, curr) => acc + curr.phuPhiCheckInEarly + curr.phuPhiCheckOutLate + curr.phuPhiKhac, 0);
  const totalServices = roomPreviews.reduce((acc, curr) => acc + curr.tongDichVu, 0);
  
  const subTotal = totalRoomPrice + totalSurcharges + totalServices;
  const deposit = booking?.tienDatCoc || 0;
  
  // 10% voucher code discount matching checkOut master logic
  const discount = voucherCode ? subTotal * 0.1 : 0;
  const grandTotal = Math.max(0, subTotal - discount - deposit);

  const handleCheckoutMaster = async () => {
    try {
      await thuePhongAPI.checkOutMaster({
        maDatPhong: maDatPhong,
        maNhanVien: "NV001", // fallback
        voucherCode: voucherCode
      });
      alert("Thanh toán đoàn thành công!");
      onCheckoutSuccess();
      onClose();
    } catch (e) {
      alert(e.response?.data || "Lỗi thanh toán đoàn");
    }
  };

  return (
    <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="modal-panel flex flex-col" style={{ maxWidth: 896, maxHeight: '90vh' }}>
        
        {/* Header */}
        <div className="px-5 py-4 border-b border-[var(--border-color)] flex items-center justify-between shrink-0">
          <div>
            <h3 className="font-bold text-white text-base">Thanh toán đoàn (Master Bill)</h3>
            <p className="text-xs text-[var(--text-secondary)] mt-0.5">
              Đoàn: <strong className="text-blue-400">{booking?.tenDoan || 'Khách Đoàn'}</strong> · Mã đặt: <strong className="text-blue-400">{maDatPhong}</strong>
            </p>
          </div>
          <button onClick={onClose} className="text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition-colors">
            <X size={18}/>
          </button>
        </div>

        {loading ? (
          <div className="p-20 text-center text-[var(--text-secondary)] font-semibold flex-1">
            <div className="animate-spin w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full mx-auto mb-3" />
            Đang tải dữ liệu hóa đơn đoàn...
          </div>
        ) : roomPreviews.length === 0 ? (
          <div className="p-20 text-center text-[var(--text-secondary)] font-semibold flex-1">
            Không có phòng nào chưa thanh toán trong đoàn này.
          </div>
        ) : (
          <div className="p-5 overflow-y-auto space-y-4 flex-1">
            
            {/* Rooms breakdown */}
            <div className="space-y-3">
              <h4 className="text-xs font-bold text-[var(--text-secondary)] uppercase tracking-wider flex items-center gap-1.5">
                <FileText size={14}/> Danh sách phòng & dịch vụ
              </h4>
              
              <div className="space-y-3">
                {roomPreviews.map((preview) => (
                  <div key={preview.maChiTiet} className="bg-[var(--bg-main)]/40 p-4 rounded-xl border border-[var(--border-color)] space-y-2.5">
                    <div className="flex justify-between items-center pb-2 border-b border-[var(--border-color)]/50">
                      <span className="font-bold text-white text-sm">Phòng {preview.maPhong}</span>
                      <span className="text-xs text-[var(--text-secondary)]">Khách: <strong>{preview.tenKhachHang}</strong></span>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs font-semibold text-[var(--text-secondary)]">
                      <div className="space-y-1">
                        <div className="flex justify-between">
                          <span>Tiền phòng ({preview.soNgay} đêm x {fmt(preview.donGiaPhong)}đ):</span>
                          <span className="text-[var(--text-primary)]">{fmt(preview.tienPhong)}đ</span>
                        </div>
                        {preview.phuPhiCheckInEarly > 0 && (
                          <div className="flex justify-between text-orange-400">
                            <span>Surcharge Check-in sớm:</span>
                            <span>+{fmt(preview.phuPhiCheckInEarly)}đ</span>
                          </div>
                        )}
                        {preview.phuPhiCheckOutLate > 0 && (
                          <div className="flex justify-between text-orange-400">
                            <span>Surcharge Check-out trễ:</span>
                            <span>+{fmt(preview.phuPhiCheckOutLate)}đ</span>
                          </div>
                        )}
                        {preview.phuPhiKhac > 0 && (
                          <div className="flex justify-between text-orange-400">
                            <span>Phí phát sinh khác:</span>
                            <span>+{fmt(preview.phuPhiKhac)}đ</span>
                          </div>
                        )}
                      </div>

                      <div className="space-y-1 border-t md:border-t-0 md:border-l border-[var(--border-color)]/50 pt-2 md:pt-0 md:pl-4">
                        <div className="text-[10px] text-[var(--text-secondary)] uppercase font-bold mb-1">Dịch vụ phòng</div>
                        {preview.dsDichVu && preview.dsDichVu.length > 0 ? (
                          preview.dsDichVu.map((dv, idx) => (
                            <div key={idx} className="flex justify-between font-normal">
                              <span>{dv.tenDichVu} (x{dv.soLuong}):</span>
                              <span className="text-[var(--text-primary)]">{fmt(dv.thanhTien)}đ</span>
                            </div>
                          ))
                        ) : (
                          <span className="text-[var(--text-secondary)] font-normal italic">Không dùng dịch vụ</span>
                        )}
                        {preview.tongDichVu > 0 && (
                          <div className="flex justify-between border-t border-[var(--border-color)]/30 pt-1 font-bold text-blue-400 mt-1">
                            <span>Tổng dịch vụ:</span>
                            <span>{fmt(preview.tongDichVu)}đ</span>
                          </div>
                        )}
                      </div>
                    </div>

                    <div className="flex justify-end font-bold text-xs pt-1.5 border-t border-[var(--border-color)]/30 text-[var(--text-primary)]">
                      <span>Cộng phòng {preview.maPhong}: {fmt(preview.tienPhong + preview.phuPhiCheckInEarly + preview.phuPhiCheckOutLate + preview.phuPhiKhac + preview.tongDichVu)}đ</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Billing Summary */}
            <div className="bg-[var(--bg-sidebar)] rounded-xl border border-[var(--border-color)] p-4 space-y-3">
              <h4 className="text-xs font-bold text-[var(--text-secondary)] uppercase tracking-wider flex items-center gap-1.5">
                <Calendar size={14}/> Chi tiết tổng hợp đoàn
              </h4>
              <div className="space-y-2 text-xs font-semibold text-[var(--text-secondary)]">
                <div className="flex justify-between">
                  <span>Tổng tiền phòng tất cả các phòng:</span>
                  <span>{fmt(totalRoomPrice)}đ</span>
                </div>
                {totalSurcharges > 0 && (
                  <div className="flex justify-between text-orange-400">
                    <span>Tổng phụ thu early check-in / late check-out:</span>
                    <span>+{fmt(totalSurcharges)}đ</span>
                  </div>
                )}
                {totalServices > 0 && (
                  <div className="flex justify-between">
                    <span>Tổng dịch vụ & phát sinh:</span>
                    <span>{fmt(totalServices)}đ</span>
                  </div>
                )}
                <div className="flex justify-between border-t border-[var(--border-color)]/50 pt-2">
                  <span>Tiền tạm tính (Subtotal):</span>
                  <span>{fmt(subTotal)}đ</span>
                </div>
                {discount > 0 && (
                  <div className="flex justify-between text-red-400">
                    <span>Giảm giá khuyến mãi (Voucher):</span>
                    <span>-{fmt(discount)}đ</span>
                  </div>
                )}
                <div className="flex justify-between border-b border-[var(--border-color)] pb-2 text-[var(--text-secondary)]">
                  <span>Tiền đặt cọc của đoàn (Khấu trừ):</span>
                  <span>-{fmt(deposit)}đ</span>
                </div>
                <div className="flex justify-between text-base font-extrabold text-white pt-2">
                  <span>TỔNG THU ĐOÀN CẦN THANH TOÁN:</span>
                  <span className="text-emerald-400">{fmt(grandTotal)}đ</span>
                </div>
              </div>
            </div>

            {/* Payment Method Selector */}
            <div className="bg-[var(--bg-sidebar)] p-4 rounded-xl border border-[var(--border-color)] grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="text-[10px] text-[var(--text-secondary)] font-bold uppercase">Phương thức thanh toán đoàn</label>
                <select value={paymentMethod} onChange={e=>setPaymentMethod(e.target.value)}
                  className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2 text-xs text-[var(--text-primary)] mt-1.5 focus:border-blue-500 outline-none">
                  <option value="CASH">Tiền mặt</option>
                  <option value="CARD">Thẻ ngân hàng</option>
                  <option value="TRANSFER">Chuyển khoản</option>
                </select>
              </div>

              {paymentMethod === 'TRANSFER' && (
                <div className="flex flex-col items-center space-y-2 bg-[var(--bg-main)]/20 p-3 rounded-xl border border-[var(--border-color)]">
                  <div className="text-[10px] font-bold text-white uppercase flex items-center gap-1">
                    <QrCode size={12}/> Mã QR chuyển khoản tổng đoàn
                  </div>
                  <img 
                    src={`https://img.vietqr.io/image/mbbank-0971234567-compact2.png?amount=${grandTotal}&addInfo=ThanhToanDoan${maDatPhong}`} 
                    alt="VietQR Group Payment" 
                    className="w-40 h-40 rounded-lg border border-[var(--border-color)]"
                  />
                  <span className="text-[10px] text-[var(--text-secondary)]">
                    Số tiền: <strong className="text-emerald-400">{fmt(grandTotal)}đ</strong> · Nội dung: <strong className="text-blue-400">ThanhToanDoan{maDatPhong}</strong>
                  </span>
                </div>
              )}
            </div>

            {/* Bottom Actions */}
            <div className="flex justify-between items-center border-t border-[var(--border-color)] pt-4 shrink-0">
              <div className="flex gap-2">
                <input type="text" placeholder="Mã giảm giá..." value={voucherCode} onChange={e=>setVoucherCode(e.target.value)}
                  className="bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-1.5 text-xs text-[var(--text-primary)] focus:border-blue-500 outline-none w-32"/>
              </div>
              <div className="flex gap-2.5">
                <button type="button" onClick={onClose}
                  className="px-4 py-2 bg-[var(--bg-main)] hover:bg-[var(--border-color)] text-[var(--text-secondary)] rounded-xl font-bold transition-all text-xs border border-[var(--border-color)]">
                  Quay lại
                </button>
                <button type="button" onClick={handleCheckoutMaster}
                  className="px-5 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl font-extrabold transition-all text-xs shadow-lg shadow-emerald-600/25 flex items-center gap-1.5">
                  <ShieldCheck size={14}/> Xác nhận Thanh toán đoàn
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
