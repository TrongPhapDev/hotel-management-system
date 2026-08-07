package service;

import dao.*;
import database.DatabaseConnection;
import entity.*;
import entity.enums.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ThuePhongService {

    private static final Logger LOGGER = Logger.getLogger(ThuePhongService.class.getName());

    private final ChiTietDatPhongDAO ctdpDAO  = new ChiTietDatPhongDAO();
    private final PhongDAO           phongDAO = new PhongDAO();
    private final HoaDonDAO          hoaDonDAO = new HoaDonDAO();
    private final ChiTietHoaDonDAO   cthdDAO  = new ChiTietHoaDonDAO();
    private final SuDungDichVuDAO    sddvDAO  = new SuDungDichVuDAO();
    private final DatPhongDAO        datPhongDAO = new DatPhongDAO();
    private final NhanVienDAO        nhanVienDAO = new NhanVienDAO();
    private final DichVuDAO          dichVuDAO   = new DichVuDAO();
    private final BangGiaService     bangGiaService = new BangGiaService();
    private final KhachHangDAO       khachHangDAO = new KhachHangDAO();
    private final KhuyenMaiDAO       khuyenMaiDAO = new KhuyenMaiDAO();

    // ---- Check-in ----
    public String checkIn(ChiTietDatPhong ct) {
        if (ct == null || ct.getPhong() == null) return "Thông tin phòng không hợp lệ!";
        
        Phong phong = phongDAO.getById(ct.getPhong().getMaPhong());
        if (phong == null) return "Phòng không tồn tại!";
        
        if (phong.getTrangThai() != TrangThaiPhong.AVAILABLE) 
            return "Phòng " + ct.getPhong().getMaPhong() + " không sẵn sàng (đang: " + phong.getTrangThaiString() + ")!";

        if (ctdpDAO.hasActiveStay(phong.getMaPhong())) {
            // Trường hợp hy hữu: Trạng thái phòng là AVAILABLE nhưng vẫn còn ChiTietDatPhong chưa kết thúc (Ghost Stay)
            // Tự động kết thúc lượt cũ để giải phóng phòng
            ChiTietDatPhong ghostStay = ctdpDAO.getActiveStayByPhong(phong.getMaPhong());
            if (ghostStay != null) {
                LOGGER.log(Level.WARNING, "Ghost stay detected for room {0}. Auto-finalizing stay {1} to allow new check-in.", 
                          new Object[]{phong.getMaPhong(), ghostStay.getMaChiTiet()});
                ghostStay.setNgayTraThucTe(LocalDateTime.now());
                ctdpDAO.update(ghostStay);
            } else {
                return "Phòng " + ct.getPhong().getMaPhong() + " hiện đang có một lượt lưu trú chưa được trả phòng. Vui lòng kiểm tra lại!";
            }
        }

        if (ct.getDatPhong() != null && phong.getLoaiPhong() != null) {
            // Kiểm tra xung đột lịch đặt phòng (Double booking protection)
            LocalDateTime checkStart = ct.getDatPhong().getNgayNhanDuKien() != null ? ct.getDatPhong().getNgayNhanDuKien() : LocalDateTime.now();
            LocalDateTime checkEnd = ct.getDatPhong().getNgayTraDuKien() != null ? ct.getDatPhong().getNgayTraDuKien() : checkStart.plusDays(1);
            
            ChiTietDatPhong conflict = ctdpDAO.getConflictingStay(phong.getMaPhong(), checkStart, checkEnd, ct.getDatPhong().getMaDatPhong());
            if (conflict != null) {
                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM");
                String overlapInfo = conflict.getDatPhong().getNgayNhanDuKien().format(fmt) + " - " + conflict.getDatPhong().getNgayTraDuKien().format(fmt);
                return "Phòng " + phong.getMaPhong() + " đã có khách đặt trước trong khoảng: " + overlapInfo + ". Không thể cho thuê trùng lịch!";
            }

            int sucChua = phong.getLoaiPhong().getSucChua();
            int maxPax = sucChua + 2;
            
            List<ChiTietDatPhong> ds = ct.getDatPhong().getDsChiTiet();
            boolean isGroup = (ds != null && ds.size() > 1);
            
            if (!isGroup && ct.getDatPhong().getSoLuongKhach() > maxPax && sucChua > 0) {
                return "Số khách (" + ct.getDatPhong().getSoLuongKhach() + ") vượt mức tối đa tuyệt đối cho phép (" + maxPax + ")!";
            }
        }

        ChiTietDatPhong existingCt = null;
        if (ct.getDatPhong() != null && ct.getDatPhong().getMaDatPhong() != null) {
            for (ChiTietDatPhong act : ctdpDAO.getByDatPhong(ct.getDatPhong().getMaDatPhong())) {
                if (act.getPhong() != null && act.getPhong().getMaPhong().equals(phong.getMaPhong()) && act.getNgayTraThucTe() == null) {
                    existingCt = act;
                    break;
                }
            }
        }

        if (existingCt != null) {
            existingCt.setNgayNhanThucTe(LocalDateTime.now());
            if (ct.getGiaThucTeChot() > 0) existingCt.setGiaThucTeChot(ct.getGiaThucTeChot());
            ct.setMaChiTiet(existingCt.getMaChiTiet());
            if (!ctdpDAO.update(existingCt)) return "Lỗi cập nhật chi tiết đặt phòng!";
        } else {
            ct.setMaChiTiet(ctdpDAO.generateMaChiTiet());
            ct.setNgayNhanThucTe(LocalDateTime.now());
            if (ct.getGiaThucTeChot() <= 0) {
                ct.setGiaThucTeChot(bangGiaService.layGiaHienHanh(phong.getLoaiPhong().getMaLoaiPhong()));
            }
            if (!ctdpDAO.insert(ct)) return "Lỗi tạo chi tiết thuê phòng!";
        }

        phong.setTrangThai(TrangThaiPhong.OCCUPIED);
        phongDAO.update(phong);
        return null;
    }

    public String themDichVu(SuDungDichVu sddv) {
        if (sddv == null) return "Dữ liệu dịch vụ trống!";
        if (sddv.getDichVu() == null || sddv.getSoLuong() <= 0) 
            return "Thông tin dịch vụ hoặc số lượng không hợp lệ!";
        return sddvDAO.insert(sddv) ? null : "Lỗi lưu dịch vụ vào cơ sở dữ liệu!";
    }

    public String themDichVu(String maChiTiet, String maDV, int sl, double gia) {
        ChiTietDatPhong ct = ctdpDAO.getById(maChiTiet);
        DichVu dv = dichVuDAO.getById(maDV);
        if (dv == null) return "Dịch vụ không tồn tại!";
        SuDungDichVu s = new SuDungDichVu();
        if (ct == null) {
            ct = new ChiTietDatPhong();
            ct.setMaChiTiet(maChiTiet);
        }
        s.setCtdp(ct);
        s.setDichVu(dv);
        s.setSoLuong(sl);
        s.setDonGiaLuu(gia > 0 ? gia : dv.getDonGia());
        s.setThoiDiem(LocalDateTime.now());
        return themDichVu(s);
    }

    public String extendStay(String maChiTiet, LocalDateTime newLDT) {
        ChiTietDatPhong ct = ctdpDAO.getById(maChiTiet);
        if (ct == null || ct.getDatPhong() == null) return "Không tìm thấy thông tin thuê phòng!";
        DatPhong dp = ct.getDatPhong();
        if (newLDT.isBefore(dp.getNgayTraDuKien())) {
            return "Ngày trả mới không thể sớm hơn ngày trả hiện tại!";
        }
        LocalDateTime oldEnd = dp.getNgayTraDuKien();
        if (!phongDAO.isRoomAvailable(ct.getPhong().getMaPhong(), oldEnd, newLDT, dp.getMaDatPhong())) {
            return "Không thể gia hạn! Phòng " + ct.getPhong().getMaPhong() + " đã có khách khác đặt trước trong khoảng thời gian này.";
        }
        dp.setNgayTraDuKien(newLDT);
        boolean ok = datPhongDAO.update(dp);
        if (ok) {
            LOGGER.info("Gia hạn phòng " + ct.getPhong().getMaPhong() + " đến " + newLDT);
            return null;
        }
        return "Lỗi cập nhật CSDL!";
    }

    public String checkOut(String maChiTiet, String maNV) {
        return checkOut(maChiTiet, maNV, "PAID", null);
    }
    
    public String checkOut(String maChiTiet, String maNV, String status) {
        return checkOut(maChiTiet, maNV, status, null);
    }

    /**
     * Overload for legacy logic that only passes status (UNPAID/PAID)
     */
    public String checkOut(String maChiTiet, String maNV, String status, String voucherCode) {
        return checkOut(maChiTiet, maNV, status, voucherCode, -1);
    }
    
    public String checkOut(String maChiTiet, String maNV, String status, String voucherCode, double customDeposit) {
        database.DatabaseConnection dbConn = database.DatabaseConnection.getInstance();
        try {
            dbConn.beginTransaction();

            ChiTietDatPhong mainCt = ctdpDAO.getById(maChiTiet);
            if (mainCt == null) {
                dbConn.rollbackTransaction();
                return "Không tìm thấy chi tiết thuê phòng!";
            }

            List<ChiTietDatPhong> segments = new ArrayList<>();
            segments.add(mainCt);

            HoaDon hd = new HoaDon();
            hd.setMaHoaDon(hoaDonDAO.generateMaHD());
            hd.setDatPhong(mainCt.getDatPhong());
            hd.setNhanVien(nhanVienDAO.getById(maNV));
            hd.setNgayLap(LocalDateTime.now());
            
            double tongTienPhong = 0;
            double tongDichVu = 0;
            double tongPhuPhi = 0;
            List<ChiTietHoaDon> pendingItems = new ArrayList<>();

            for (ChiTietDatPhong ct : segments) {
                if (ct.getNgayTraThucTe() == null) ct.setNgayTraThucTe(LocalDateTime.now());
                
                LocalDateTime ngayNhanResolved = ct.getNgayNhanThucTe();
                if (ngayNhanResolved == null && ct.getDatPhong() != null) ngayNhanResolved = ct.getDatPhong().getNgayNhanDuKien();
                
                long soNgay = Math.max(1, tinhSoNgay(ngayNhanResolved, ct.getNgayTraThucTe()));
                double donGiaChot = ct.getGiaThucTeChot() > 0 ? ct.getGiaThucTeChot() : 400000;
                double tienPhongChang = donGiaChot * soNgay;
                tongTienPhong += tienPhongChang;

                ChiTietHoaDon cthdPhong = new ChiTietHoaDon();
                cthdPhong.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                cthdPhong.setHoaDon(hd);
                cthdPhong.setLoaiChiTiet(LoaiChiTietHoaDon.PHONG);
                cthdPhong.setSoLuong((int) soNgay);
                cthdPhong.setDonGia(donGiaChot);
                cthdPhong.setNoiDung("Phòng " + (ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?") + " (" + soNgay + " đêm)");
                cthdPhong.setThanhTien(tienPhongChang);
                pendingItems.add(cthdPhong);

                // 2. Tính phụ phí động (Nhận sớm / Trả trễ) bằng StandardRoomCalculator
                StandardRoomCalculator calc = new StandardRoomCalculator();
                List<SuDungDichVu> dsDichVu = sddvDAO.getByChiTietDatPhong(ct.getMaChiTiet());
                boolean earlyAdded = dsDichVu.stream().anyMatch(s -> 
                    (s.getDichVu() != null && "DV_EXTRA_EARLY".equals(s.getDichVu().getMaDV()))
                    || (s.getTenDichVu() != null && s.getTenDichVu().toLowerCase().contains("nhận phòng sớm")));

                double dynamicSurcharge = calc.tinhPhuPhi(ct, ngayNhanResolved, ct.getNgayTraThucTe());
                if (earlyAdded && ngayNhanResolved != null) {
                    double onlyEarly = calc.tinhPhuPhi(ct, ngayNhanResolved, ngayNhanResolved.withHour(14).withMinute(0).withSecond(0));
                    dynamicSurcharge = Math.max(0, dynamicSurcharge - onlyEarly);
                }

                if (dynamicSurcharge > 0) {
                    ChiTietHoaDon cthdPhuPhi = new ChiTietHoaDon();
                    cthdPhuPhi.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                    cthdPhuPhi.setHoaDon(hd);
                    cthdPhuPhi.setLoaiChiTiet(LoaiChiTietHoaDon.PHU_PHI);
                    cthdPhuPhi.setSoLuong(1);
                    cthdPhuPhi.setDonGia(dynamicSurcharge);
                    cthdPhuPhi.setNoiDung("Phụ phí (Nhận sớm/Trả trễ) - P." + (ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?"));
                    cthdPhuPhi.setThanhTien(dynamicSurcharge);
                    pendingItems.add(cthdPhuPhi);
                    tongPhuPhi += dynamicSurcharge;
                }

                // 3. Phí phát sinh khác (đã lưu trong DB)
                tongPhuPhi += ct.getPhuPhiPhatSinh();
                if (ct.getPhuPhiPhatSinh() > 0) {
                    ChiTietHoaDon cthdKhac = new ChiTietHoaDon();
                    cthdKhac.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                    cthdKhac.setHoaDon(hd);
                    cthdKhac.setLoaiChiTiet(LoaiChiTietHoaDon.PHU_PHI);
                    cthdKhac.setSoLuong(1);
                    cthdKhac.setDonGia(ct.getPhuPhiPhatSinh());
                    cthdKhac.setNoiDung("Phí phát sinh khác - P." + (ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?"));
                    cthdKhac.setThanhTien(ct.getPhuPhiPhatSinh());
                    pendingItems.add(cthdKhac);
                }
                
                for (SuDungDichVu sddv : dsDichVu) {
                    double itemTotal = sddv.getDonGiaLuu() * sddv.getSoLuong();
                    tongDichVu += itemTotal;
                    ChiTietHoaDon cthdDV = new ChiTietHoaDon();
                    cthdDV.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                    cthdDV.setHoaDon(hd);
                    cthdDV.setLoaiChiTiet(LoaiChiTietHoaDon.DICH_VU);
                    cthdDV.setSoLuong(sddv.getSoLuong());
                    cthdDV.setDonGia(sddv.getDonGiaLuu());
                    cthdDV.setNoiDung("[" + (ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?") + "] " + (sddv.getDichVu() != null ? sddv.getDichVu().getTenDV() : "Dịch vụ"));
                    cthdDV.setThanhTien(itemTotal);
                    pendingItems.add(cthdDV);
                }
                ct.setDaThanhToan(true);
                if (!ctdpDAO.update(ct)) { dbConn.rollbackTransaction(); return "Lỗi cập nhật chặng!"; }

                if (ct.getPhong() != null) {
                    Phong p = phongDAO.getById(ct.getPhong().getMaPhong());
                    if (p != null) {
                        p.setTrangThai(TrangThaiPhong.CLEANING);
                        p.setTenKhachHienTai(null);
                        phongDAO.update(p);
                    }
                }
            }
            
            hd.setTongTienPhong(tongTienPhong);
            hd.setTongTienDichVu(tongDichVu + tongPhuPhi);
            
            KhuyenMai chosenKM = null;
            double subTotal = tongTienPhong + hd.getTongTienDichVu();
            
            String selectedVoucher = (voucherCode != null && !voucherCode.trim().isEmpty()) 
                ? voucherCode.trim() 
                : (mainCt.getDatPhong() != null ? mainCt.getDatPhong().getMaKhuyenMai() : null);

            LocalDateTime thoiDiemDat = (mainCt.getDatPhong() != null) 
                ? mainCt.getDatPhong().getNgayDat() 
                : LocalDateTime.now();

            if (selectedVoucher != null && !selectedVoucher.trim().isEmpty()) {
                chosenKM = khuyenMaiDAO.getByVoucherCode(selectedVoucher.trim());
                if (chosenKM != null && !chosenKM.kiemTraHopLe(subTotal, thoiDiemDat)) chosenKM = null;
            }
            
            hd.setKhuyenMai(chosenKM);
            double tienGiam = 0;
            if (chosenKM != null) {
                tienGiam = chosenKM.tinhSoTienGiam(subTotal);
            }
            hd.setTienGiamKhuyenMai(tienGiam);
            
            double tienCoc = 0;
            if (mainCt.getDatPhong() != null) {
                // Nghiệp vụ thực tế: Nếu tách bill thanh toán lẻ 1 phòng trong khách đoàn,
                // tiền cọc không tự động cấn trừ mà sẽ được giữ lại cho Master Bill (Hóa đơn tổng)
                // trừ phi có chỉ định cấn trừ thủ công (customDeposit >= 0).
                List<ChiTietDatPhong> realDs = ctdpDAO.getByDatPhong(mainCt.getDatPhong().getMaDatPhong());
                boolean isGroup = realDs != null && realDs.size() > 1;
                double maxCoc = mainCt.getDatPhong().getTienDatCoc();
                
                if (customDeposit >= 0) { // Khi thao tác Manual Deduct
                    tienCoc = Math.min(customDeposit, maxCoc);
                } else if (!isGroup) {
                    tienCoc = maxCoc;
                }
                
                if (tienCoc > 0) {
                    ChiTietHoaDon cthdCoc = new ChiTietHoaDon();
                    cthdCoc.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                    cthdCoc.setHoaDon(hd);
                    cthdCoc.setLoaiChiTiet(LoaiChiTietHoaDon.PHU_PHI);
                    cthdCoc.setSoLuong(1);
                    cthdCoc.setDonGia(-tienCoc);
                    cthdCoc.setNoiDung("Khấu trừ tiền cọc");
                    cthdCoc.setThanhTien(-tienCoc);
                    pendingItems.add(cthdCoc);
                }
            }
            hd.setTongThanhToan(subTotal - tienGiam - tienCoc);
            
            try { hd.setTrangThai(TrangThaiThanhToan.valueOf(status.toUpperCase())); } 
            catch (Exception e) { hd.setTrangThai(TrangThaiThanhToan.PAID); }
            
            if (!hoaDonDAO.insert(hd)) { dbConn.rollbackTransaction(); return "Lỗi tạo hóa đơn!"; }
            for (ChiTietHoaDon item : pendingItems) {
                if (!cthdDAO.insert(item)) { dbConn.rollbackTransaction(); return "Lỗi lưu chi tiết hóa đơn!"; }
            }

            if (mainCt.getDatPhong() != null) {
                DatPhong dp = datPhongDAO.getById(mainCt.getDatPhong().getMaDatPhong());
                if (dp != null) {
                    if (tienCoc > 0) {
                        dp.setTienDatCoc(Math.max(0, dp.getTienDatCoc() - tienCoc)); // Trừ cọc vừa dùng
                    }
                    updateDatPhongStatus(dp, ctdpDAO.getByDatPhong(dp.getMaDatPhong()));
                }
            }

            if (hd.getTrangThai() == TrangThaiThanhToan.PAID) {
                String targetMaKH = null;
                if (mainCt.getKhachHang() != null) {
                    targetMaKH = mainCt.getKhachHang().getMaKhachHang();
                } else if (mainCt.getDatPhong() != null && mainCt.getDatPhong().getKhachHang() != null) {
                    targetMaKH = mainCt.getDatPhong().getKhachHang().getMaKhachHang();
                }
                
                if (targetMaKH != null) {
                    khachHangDAO.updateCRMStats(targetMaKH, hd.getTongThanhToan(), true);
                }
            }

            dbConn.commitTransaction();
            return null;
        } catch (Exception e) {
            dbConn.rollbackTransaction();
            LOGGER.log(Level.SEVERE, "Lỗi check-out", e);
            return "Lỗi hệ thống: " + e.getMessage();
        }
    }

    public String checkOutWithoutBilling(String maChiTiet) {
        DatabaseConnection dbConn = DatabaseConnection.getInstance();
        try {
            dbConn.beginTransaction();
            ChiTietDatPhong ct = ctdpDAO.getById(maChiTiet);
            if (ct == null) { dbConn.rollbackTransaction(); return "Không tìm thấy chi tiết thuê phòng!"; }
            if (ct.getNgayTraThucTe() != null) { dbConn.rollbackTransaction(); return "Phòng này đã được trả!"; }
            ct.setNgayTraThucTe(LocalDateTime.now());
            ct.setDaThanhToan(false);
            if (!ctdpDAO.update(ct)) { dbConn.rollbackTransaction(); return "Lỗi cập nhật chi tiết đặt phòng!"; }
            if (ct.getDatPhong() != null) {
                DatPhong dp = datPhongDAO.getById(ct.getDatPhong().getMaDatPhong());
                if (dp != null) {
                    updateDatPhongStatus(dp, ctdpDAO.getByDatPhong(dp.getMaDatPhong()));
                }
            }
            Phong phong = phongDAO.getById(ct.getPhong().getMaPhong());
            if (phong != null) { 
                phong.setTrangThai(TrangThaiPhong.CLEANING); 
                phong.setTenKhachHienTai(null);
                phongDAO.update(phong); 
            }
            dbConn.commitTransaction();
            return null;
        } catch (Exception e) { dbConn.rollbackTransaction(); return "Lỗi hệ thống: " + e.getMessage(); }
    }

    public String checkOutMasterBill(String maDatPhong, String maNV, String voucherCode) {
        database.DatabaseConnection dbConn = database.DatabaseConnection.getInstance();
        try {
            dbConn.beginTransaction();
            DatPhong dp = datPhongDAO.getById(maDatPhong);
            if (dp == null) return "Không tìm thấy thông tin đặt phòng!";

            List<ChiTietDatPhong> rooms = ctdpDAO.getByDatPhong(maDatPhong);
            if (rooms == null || rooms.isEmpty()) return "Không có phòng nào để thanh toán!";

            HoaDon hd = new HoaDon();
            hd.setMaHoaDon(hoaDonDAO.generateMaHD());
            hd.setDatPhong(dp);
            hd.setNhanVien(nhanVienDAO.getById(maNV));
            hd.setNgayLap(LocalDateTime.now());

            double tongTienPhong = 0;
            double tongDichVu = 0;
            double tongPhuPhi = 0;
            List<ChiTietHoaDon> pendingItems = new ArrayList<>();

            StandardRoomCalculator calc = new StandardRoomCalculator();

            for (ChiTietDatPhong ct : rooms) {
                if (ct.isDaThanhToan()) continue;
                
                // 1. Tiền phòng
                LocalDateTime ngayNhan = ct.getNgayNhanThucTe() != null ? ct.getNgayNhanThucTe() : dp.getNgayNhanDuKien();
                LocalDateTime ngayTra = ct.getNgayTraThucTe() != null ? ct.getNgayTraThucTe() : LocalDateTime.now();
                long soNgay = Math.max(1, tinhSoNgay(ngayNhan, ngayTra));
                
                double donGia = ct.getGiaThucTeChot() > 0 ? ct.getGiaThucTeChot() : 400000;
                double tienPhong = donGia * soNgay;
                tongTienPhong += tienPhong;

                ChiTietHoaDon ctPhong = new ChiTietHoaDon();
                ctPhong.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                ctPhong.setHoaDon(hd);
                ctPhong.setLoaiChiTiet(LoaiChiTietHoaDon.PHONG);
                ctPhong.setNoiDung("Phòng " + (ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?") + " (" + soNgay + " đêm)");
                ctPhong.setSoLuong((int)soNgay);
                ctPhong.setDonGia(donGia);
                ctPhong.setThanhTien(tienPhong);
                pendingItems.add(ctPhong);

                // 2. Phụ phí
                double phuPhi = calc.tinhPhuPhi(ct, ngayNhan, ngayTra) + ct.getPhuPhiPhatSinh();
                if (phuPhi > 0) {
                    tongPhuPhi += phuPhi;
                    ChiTietHoaDon ctPhu = new ChiTietHoaDon();
                    ctPhu.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                    ctPhu.setHoaDon(hd);
                    ctPhu.setLoaiChiTiet(LoaiChiTietHoaDon.PHU_PHI);
                    ctPhu.setNoiDung("Phụ phí P." + (ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?"));
                    ctPhu.setSoLuong(1);
                    ctPhu.setDonGia(phuPhi);
                    ctPhu.setThanhTien(phuPhi);
                    pendingItems.add(ctPhu);
                }

                // 3. Dịch vụ
                List<SuDungDichVu> dsDV = sddvDAO.getByChiTietDatPhong(ct.getMaChiTiet());
                for (SuDungDichVu s : dsDV) {
                    double t = s.getDonGiaLuu() * s.getSoLuong();
                    tongDichVu += t;
                    ChiTietHoaDon ctDV = new ChiTietHoaDon();
                    ctDV.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                    ctDV.setHoaDon(hd);
                    ctDV.setLoaiChiTiet(LoaiChiTietHoaDon.DICH_VU);
                    ctDV.setNoiDung("[P." + (ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?") + "] " + (s.getDichVu() != null ? s.getDichVu().getTenDV() : "Dịch vụ"));
                    ctDV.setSoLuong(s.getSoLuong());
                    ctDV.setDonGia(s.getDonGiaLuu());
                    ctDV.setThanhTien(t);
                    pendingItems.add(ctDV);
                }

                ct.setDaThanhToan(true);
                ct.setNgayTraThucTe(ngayTra);
                ctdpDAO.update(ct);

                Phong p = ct.getPhong();
                if (p != null) {
                    p.setTrangThai(TrangThaiPhong.CLEANING);
                    p.setTenKhachHienTai(null);
                    phongDAO.update(p);
                }
            }

            hd.setTongTienPhong(tongTienPhong);
            hd.setTongTienDichVu(tongDichVu + tongPhuPhi);
            
            double subTotal = tongTienPhong + hd.getTongTienDichVu();
            
            KhuyenMai chosenKM = null;
            String vc = (voucherCode != null && !voucherCode.trim().isEmpty()) 
                ? voucherCode.trim() 
                : dp.getMaKhuyenMai();

            LocalDateTime thoiDiemDat = (dp.getNgayDat() != null) ? dp.getNgayDat() : LocalDateTime.now();

            if (vc != null && !vc.trim().isEmpty()) {
                chosenKM = khuyenMaiDAO.getByVoucherCode(vc.trim());
                if (chosenKM != null && !chosenKM.kiemTraHopLe(subTotal, thoiDiemDat)) chosenKM = null;
            }

            double tienGiam = 0;
            if (chosenKM != null) {
                hd.setKhuyenMai(chosenKM);
                tienGiam = chosenKM.tinhSoTienGiam(subTotal);
            }
            hd.setTienGiamKhuyenMai(tienGiam);

            double tienCoc = dp.getTienDatCoc();
            if (tienCoc > 0) {
                ChiTietHoaDon cthdCoc = new ChiTietHoaDon();
                cthdCoc.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                cthdCoc.setHoaDon(hd);
                cthdCoc.setLoaiChiTiet(LoaiChiTietHoaDon.PHU_PHI);
                cthdCoc.setNoiDung("Khấu trừ tiền cọc");
                cthdCoc.setSoLuong(1);
                cthdCoc.setDonGia(-tienCoc);
                cthdCoc.setThanhTien(-tienCoc);
                pendingItems.add(cthdCoc);
            }

            hd.setTongThanhToan(subTotal - tienGiam - tienCoc);
            hd.setTrangThai(TrangThaiThanhToan.PAID);
            hd.setPhuongThucThanhToan("CASH");

            if (!hoaDonDAO.insert(hd)) { dbConn.rollbackTransaction(); return "Lỗi tạo hóa đơn!"; }
            for (ChiTietHoaDon c : pendingItems) {
                if (!cthdDAO.insert(c)) { dbConn.rollbackTransaction(); return "Lỗi lưu chi tiết hóa đơn!"; }
            }

            // CRM Stats update for group representative
            if (dp.getKhachHang() != null) {
                khachHangDAO.updateCRMStats(dp.getKhachHang().getMaKhachHang(), hd.getTongThanhToan(), true);
            }

            dp.setTrangThai(TrangThaiDatPhong.CHECKED_OUT);
            datPhongDAO.update(dp);

            dbConn.commitTransaction();
            LogService.addLog("Thanh toán đoàn", "Giao dịch", "Mã HD: " + hd.getMaHoaDon() + ", Tổng: " + hd.getTongThanhToan());
            return null;
        } catch (Exception e) {
            dbConn.rollbackTransaction();
            LOGGER.log(Level.SEVERE, "Lỗi check-out đoàn", e);
            return "Lỗi hệ thống: " + e.getMessage();
        }
    }

    public List<ChiTietDatPhong> getUnpaidByDatPhong(String ma) { return ctdpDAO.getByDatPhong(ma).stream().filter(c -> !c.isDaThanhToan()).collect(java.util.stream.Collectors.toList()); }
    public ChiTietDatPhong getById(String ma) { return ctdpDAO.getById(ma); }
    public List<ChiTietDatPhong> getChiTietByDatPhong(String ma) { return ctdpDAO.getByDatPhong(ma); }
    public List<SuDungDichVu> getDichVuByChiTiet(String ma) { return sddvDAO.getByChiTietDatPhong(ma); }
    public ChiTietDatPhong getActiveByPhong(String p) { return ctdpDAO.getActiveStayByPhong(p); }
    public Map<String, String> getActiveGroupRoomsMap() { return ctdpDAO.getActiveGroupRoomsMap(); }

    public String updateRepresentative(String maDP, KhachHang newKh, String room) {
        if (newKh == null) return "Dữ liệu khách hàng không hợp lệ!";
        try {
            DatPhong dp = datPhongDAO.getById(maDP);
            if (dp == null) return "Không tìm thấy thông tin đặt phòng!";
            
            // 1. Cập nhật người đại diện chính của đơn đặt phòng (Cả đoàn)
            dp.setKhachHang(newKh);
            datPhongDAO.update(dp);
            
            // 2. Nếu có chỉ định phòng, cập nhật người lưu trú thực tế của chặng đó
            if (room != null) { 
                // Cập nhật tên hiển thị trên sơ đồ phòng
                Phong p = phongDAO.getById(room); 
                if (p != null) { 
                    p.setTenKhachHienTai(newKh.getHoTen()); 
                    phongDAO.update(p); 
                } 
                
                // Cập nhật quan trọng: Đồng bộ vào bản ghi lưu trú để Hóa đơn nhận diện đúng người mới
                ChiTietDatPhong ct = ctdpDAO.getActiveStayByPhong(room);
                if (ct != null && ct.getDatPhong() != null && ct.getDatPhong().getMaDatPhong().equals(maDP)) {
                    ct.setKhachHang(newKh);
                    ctdpDAO.update(ct);
                }
            }
            return null;
        } catch (Exception e) { 
            LOGGER.log(Level.SEVERE, "Lỗi cập nhật người đại diện", e);
            return "Lỗi hệ thống: " + e.getMessage(); 
        }
    }

    public String transferRoom(String maCT, String maP, boolean keepPrice) {
        DatabaseConnection dbConn = DatabaseConnection.getInstance();
        try {
            dbConn.beginTransaction();
            ChiTietDatPhong ctOld = ctdpDAO.getById(maCT);
            if (ctOld == null) { dbConn.rollbackTransaction(); return "Không tìm thấy!"; }
            Phong newRoom = phongDAO.getById(maP);
            if (newRoom == null || newRoom.getTrangThai() != TrangThaiPhong.AVAILABLE) { dbConn.rollbackTransaction(); return "Phòng không khả dụng!"; }

            ctOld.setNgayTraThucTe(LocalDateTime.now());
            ctOld.setDaThanhToan(false);
            ctdpDAO.update(ctOld);
            Phong oldR = ctOld.getPhong();
            if (oldR != null) { oldR.setTrangThai(TrangThaiPhong.CLEANING); phongDAO.update(oldR); }

            ChiTietDatPhong ctNew = new ChiTietDatPhong();
            ctNew.setMaChiTiet(ctdpDAO.generateMaChiTiet());
            ctNew.setDatPhong(ctOld.getDatPhong());
            ctNew.setPhong(newRoom);
            ctNew.setKhachHang(ctOld.getKhachHang());
            ctNew.setNgayNhanThucTe(LocalDateTime.now());
            double rate = keepPrice ? ctOld.getGiaThucTeChot() : bangGiaService.layGiaHienHanh(newRoom.getLoaiPhong().getMaLoaiPhong());
            ctNew.setGiaThucTeChot(rate);
            ctNew.setDaThanhToan(false);
            ctdpDAO.insert(ctNew);

            newRoom.setTrangThai(TrangThaiPhong.OCCUPIED);
            newRoom.setTenKhachHienTai(ctNew.getKhachHang() != null ? ctNew.getKhachHang().getHoTen() : "Khách");
            phongDAO.update(newRoom);
            dbConn.commitTransaction();
            return null;
        } catch (Exception e) { dbConn.rollbackTransaction(); return e.getMessage(); }
    }

    /**
     * Cập nhật trạng thái DatPhong dựa trên tình trạng thực tế của tất cả ChiTietDatPhong.
     *
     * Logic:
     *  - Tất cả đã trả (daThanhToan=true hoặc ngayTraThucTe!=null) → CHECKED_OUT
     *  - Có ít nhất 1 phòng đang ở (ngayNhanThucTe!=null, ngayTraThucTe==null) → CHECKED_IN hoặc PARTIALLY_CHECKED_IN
     *  - Tất cả chưa checkin → giữ trạng thái CONFIRMED (không thay đổi)
     *
     * Phòng chưa check-in (ngayNhanThucTe IS NULL) KHÔNG được tính là "đang ở" hay "chưa trả".
     */
    private void updateDatPhongStatus(DatPhong dp, List<ChiTietDatPhong> allRooms) {
        if (dp == null || allRooms == null || allRooms.isEmpty()) return;

        // Đếm từng loại trạng thái phòng
        int totalRooms     = allRooms.size();
        int checkedOutRooms = 0;  // daThanhToan=true hoặc ngayTraThucTe!=null
        int activeRooms    = 0;   // checkin rồi nhưng chưa trả (ngayNhanThucTe!=null, ngayTraThucTe==null)
        // Phòng chưa checkin (ngayNhanThucTe==null) không tính vào active — vẫn là "pending"

        for (ChiTietDatPhong c : allRooms) {
            boolean daTra = c.isDaThanhToan() || c.getNgayTraThucTe() != null;
            boolean dangO = c.getNgayNhanThucTe() != null && c.getNgayTraThucTe() == null;
            if (daTra)   checkedOutRooms++;
            if (dangO)   activeRooms++;
        }

        if (checkedOutRooms == totalRooms) {
            // Tất cả phòng đã trả (kể cả phòng chưa checkin — coi như không dùng)
            dp.setTrangThai(TrangThaiDatPhong.CHECKED_OUT);
        } else if (checkedOutRooms > 0 || activeRooms > 0) {
            // Có ít nhất 1 phòng đã trả HOẶC đang ở, nhưng chưa xong hết
            if (activeRooms == totalRooms - checkedOutRooms) {
                // Tất cả phòng còn lại đều đang ở → CHECKED_IN
                dp.setTrangThai(TrangThaiDatPhong.CHECKED_IN);
            } else {
                // Một số phòng đã trả, một số còn đang ở hoặc chưa checkin → PARTIALLY_CHECKED_IN
                dp.setTrangThai(TrangThaiDatPhong.PARTIALLY_CHECKED_IN);
            }
        }
        // Nếu checkedOutRooms==0 && activeRooms==0: tất cả chưa checkin → giữ nguyên

        datPhongDAO.update(dp);
    }

    public KhuyenMai getBestPromotion(double total) {
        List<KhuyenMai> all = khuyenMaiDAO.getAll();
        KhuyenMai best = null; double maxD = 0;
        for (KhuyenMai km : all) {
            if (km.kiemTraHopLe(total)) {
                double d = km.tinhSoTienGiam(total);
                if (d > maxD) { maxD = d; best = km; }
            }
        }
        return best;
    }

    public long tinhSoNgay(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0;
        java.time.Duration d = java.time.Duration.between(start, end);
        long days = d.toDays();
        // Tính làm tròn: hơn 2 tiếng tính thêm 1 ngày
        return Math.max(1, days + (d.toHoursPart() > 2 ? 1 : 0));
    }

    /**
     * Overload for legacy code using java.util.Date
     */
    public long tinhSoNgay(java.util.Date start, java.util.Date end) {
        if (start == null || end == null) return 0;
        LocalDateTime s = start.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime e = end.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        return tinhSoNgay(s, e);
    }

    public double tinhPhuPhiEarlyCheckin(Phong p, java.time.LocalDateTime t) {
        if (p == null || p.getLoaiPhong() == null || t == null) return 0;
        int hour = t.getHour();
        double baseRate = bangGiaService.layGiaHienHanh(p.getLoaiPhong().getMaLoaiPhong());

        if (hour >= 14) return 0; // Standard check-in

        if (hour < 5) return baseRate; // 100% surcharge
        if (hour < 9) return baseRate * 0.5; // 50% surcharge
        if (hour < 12) return baseRate * 0.3; // 30% surcharge
        
        // 12h - 14h: Free (Grace period)
        return 0;
    }
}
