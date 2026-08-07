package com.ohno.hotel.service;

import com.ohno.hotel.entity.*;
import com.ohno.hotel.dto.CheckoutPreviewDTO;
import com.ohno.hotel.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ThuePhongService {
    private final ChiTietDatPhongRepository ctdpRepo;
    private final PhongRepository phongRepo;
    private final HoaDonRepository hoaDonRepo;
    private final ChiTietHoaDonRepository cthdRepo;
    private final SuDungDichVuRepository sddvRepo;
    private final DatPhongRepository datPhongRepo;
    private final NhanVienRepository nhanVienRepo;
    private final DichVuRepository dichVuRepo;
    private final BangGiaService bangGiaService;
    private final KhachHangRepository khachHangRepo;
    private final KhuyenMaiRepository khuyenMaiRepo;
    private final StandardRoomCalculator roomCalculator;
    private final LogService logService;

    public ThuePhongService(ChiTietDatPhongRepository ctdpRepo,
                            PhongRepository phongRepo,
                            HoaDonRepository hoaDonRepo,
                            ChiTietHoaDonRepository cthdRepo,
                            SuDungDichVuRepository sddvRepo,
                            DatPhongRepository datPhongRepo,
                            NhanVienRepository nhanVienRepo,
                            DichVuRepository dichVuRepo,
                            BangGiaService bangGiaService,
                            KhachHangRepository khachHangRepo,
                            KhuyenMaiRepository khuyenMaiRepo,
                            StandardRoomCalculator roomCalculator,
                            LogService logService) {
        this.ctdpRepo = ctdpRepo;
        this.phongRepo = phongRepo;
        this.hoaDonRepo = hoaDonRepo;
        this.cthdRepo = cthdRepo;
        this.sddvRepo = sddvRepo;
        this.datPhongRepo = datPhongRepo;
        this.nhanVienRepo = nhanVienRepo;
        this.dichVuRepo = dichVuRepo;
        this.bangGiaService = bangGiaService;
        this.khachHangRepo = khachHangRepo;
        this.khuyenMaiRepo = khuyenMaiRepo;
        this.roomCalculator = roomCalculator;
        this.logService = logService;
    }

    public String generateMaChiTiet() {
        Integer max = ctdpRepo.getMaxId();
        return String.format("CT%04d", (max != null ? max : 0) + 1);
    }

    public String generateMaHD() {
        Integer max = hoaDonRepo.getMaxId();
        return String.format("HD%04d", (max != null ? max : 0) + 1);
    }

    public String generateMaCTHD() {
        Integer max = cthdRepo.getMaxId();
        return String.format("CTHD%04d", (max != null ? max : 0) + 1);
    }

    public String generateMaSuDung() {
        Integer max = sddvRepo.getMaxId();
        return String.format("SD%04d", (max != null ? max : 0) + 1);
    }

    @Transactional
    public String checkIn(ChiTietDatPhong ct) {
        if (ct == null || ct.getPhong() == null) return "Thông tin phòng không hợp lệ!";

        Phong phong = phongRepo.findById(ct.getPhong().getMaPhong()).orElse(null);
        if (phong == null) return "Phòng không tồn tại!";

        if (!"AVAILABLE".equals(phong.getTrangThai()))
            return "Phòng " + ct.getPhong().getMaPhong() + " không sẵn sàng!";

        ct.setNgayNhanThucTe(LocalDateTime.now());
        if (ct.getGiaThucTeChot() <= 0) {
            ct.setGiaThucTeChot(bangGiaService.layGiaHienHanh(phong.getLoaiPhong().getMaLoaiPhong()));
        }

        if (ct.getMaChiTiet() == null || ct.getMaChiTiet().trim().isEmpty()) {
            ct.setMaChiTiet(generateMaChiTiet());
        }

        ctdpRepo.save(ct);

        phong.setTrangThai("OCCUPIED");
        phong.setTenKhachHienTai(ct.getKhachHang() != null ? ct.getKhachHang().getHoTen() : "Khách Lẻ");
        phongRepo.save(phong);

        if (ct.getDatPhong() != null) {
            DatPhong dp = datPhongRepo.findById(ct.getDatPhong().getMaDatPhong()).orElse(null);
            if (dp != null) {
                dp.setTrangThai("CHECKED_IN");
                datPhongRepo.save(dp);
            }
        }

        logService.addLog("system", "Check-in", "Phòng " + phong.getMaPhong(), "Khách: " + phong.getTenKhachHienTai());
        return null;
    }

    @Transactional
    public String themDichVu(String maChiTiet, String maDV, int sl, double gia) {
        ChiTietDatPhong ct = ctdpRepo.findById(maChiTiet).orElse(null);
        if (ct == null) return "Lượt lưu trú không tồn tại!";
        DichVu dv = dichVuRepo.findById(maDV).orElse(null);
        if (dv == null) return "Dịch vụ không tồn tại!";

        SuDungDichVu s = SuDungDichVu.builder()
                .maSuDung(generateMaSuDung())
                .chiTietDatPhong(ct)
                .dichVu(dv)
                .soLuong(sl)
                .donGiaLucDung(gia > 0 ? gia : dv.getDonGia())
                .thoiGianDung(LocalDateTime.now())
                .build();
        sddvRepo.save(s);
        return null;
    }

    @Transactional
    public String extendStay(String maChiTiet, LocalDateTime newLDT) {
        ChiTietDatPhong ct = ctdpRepo.findById(maChiTiet).orElse(null);
        if (ct == null || ct.getDatPhong() == null) return "Không tìm thấy thông tin thuê phòng!";
        DatPhong dp = datPhongRepo.findById(ct.getDatPhong().getMaDatPhong()).orElse(null);
        if (dp == null) return "Không tìm thấy thông tin đặt phòng!";
        if (newLDT.isBefore(dp.getNgayTraDuKien())) {
            return "Ngày trả mới không thể sớm hơn ngày trả hiện tại!";
        }
        dp.setNgayTraDuKien(newLDT);
        datPhongRepo.save(dp);
        return null;
    }

    @Transactional
    public String transferRoom(String maCT, String maP, boolean keepPrice) {
        ChiTietDatPhong ctOld = ctdpRepo.findById(maCT).orElse(null);
        if (ctOld == null) return "Không tìm thấy thông tin lượt ở!";
        Phong newRoom = phongRepo.findById(maP).orElse(null);
        if (newRoom == null || !"AVAILABLE".equals(newRoom.getTrangThai())) {
            return "Phòng mới không khả dụng!";
        }

        LocalDateTime now = LocalDateTime.now();
        ctOld.setNgayTraThucTe(now);
        ctOld.setDaThanhToan(false);
        ctdpRepo.save(ctOld);

        Phong oldR = ctOld.getPhong();
        if (oldR != null) {
            oldR.setTrangThai("CLEANING");
            oldR.setTenKhachHienTai(null);
            phongRepo.save(oldR);
        }

        ChiTietDatPhong ctNew = ChiTietDatPhong.builder()
                .maChiTiet(generateMaChiTiet())
                .datPhong(ctOld.getDatPhong())
                .phong(newRoom)
                .khachHang(ctOld.getKhachHang())
                .ngayNhanThucTe(now)
                .giaThucTeChot(keepPrice ? ctOld.getGiaThucTeChot() : bangGiaService.layGiaHienHanh(newRoom.getLoaiPhong().getMaLoaiPhong()))
                .daThanhToan(false)
                .build();
        ctdpRepo.save(ctNew);

        newRoom.setTrangThai("OCCUPIED");
        newRoom.setTenKhachHienTai(ctNew.getKhachHang() != null ? ctNew.getKhachHang().getHoTen() : "Khách");
        phongRepo.save(newRoom);

        logService.addLog("system", "Room Transfer", "Từ P." + (oldR != null ? oldR.getMaPhong() : "?") + " sang P." + newRoom.getMaPhong(), "");
        return null;
    }

    @Transactional
    public String checkOut(String maChiTiet, String maNV, String status, String voucherCode, double customDeposit) {
        ChiTietDatPhong mainCt = ctdpRepo.findById(maChiTiet).orElse(null);
        if (mainCt == null) return "Không tìm thấy chi tiết thuê phòng!";

        List<ChiTietDatPhong> segments = new ArrayList<>();
        segments.add(mainCt);

        HoaDon hd = new HoaDon();
        hd.setMaHoaDon(generateMaHD());
        hd.setDatPhong(mainCt.getDatPhong());
        hd.setNhanVien(nhanVienRepo.findById(maNV).orElse(null));
        hd.setNgayLap(LocalDateTime.now());

        double tongTienPhong = 0;
        double tongDichVu = 0;
        double tongPhuPhi = 0;
        List<ChiTietHoaDon> pendingItems = new ArrayList<>();

        for (ChiTietDatPhong ct : segments) {
            if (ct.getNgayTraThucTe() == null) ct.setNgayTraThucTe(LocalDateTime.now());

            LocalDateTime ngayNhanResolved = ct.getNgayNhanThucTe();
            if (ngayNhanResolved == null && ct.getDatPhong() != null) ngayNhanResolved = ct.getDatPhong().getNgayNhanDuKien();
            if (ngayNhanResolved == null) ngayNhanResolved = ct.getNgayTraThucTe().minusDays(1);

            long soNgay = Math.max(1, Duration.between(ngayNhanResolved, ct.getNgayTraThucTe()).toDays());
            double donGiaChot = ct.getGiaThucTeChot() > 0 ? ct.getGiaThucTeChot() : 400000;
            double tienPhongChang = donGiaChot * soNgay;
            tongTienPhong += tienPhongChang;

            ChiTietHoaDon cthdPhong = ChiTietHoaDon.builder()
                    .maChiTietHoaDon(generateMaCTHD())
                    .hoaDon(hd)
                    .loaiChiTiet("PHONG")
                    .soLuong((int) soNgay)
                    .donGia(donGiaChot)
                    .noiDung("Phòng " + (ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?") + " (" + soNgay + " đêm)")
                    .thanhTien(tienPhongChang)
                    .build();
            pendingItems.add(cthdPhong);

            double dynamicSurcharge = roomCalculator.tinhPhuPhi(ct, ngayNhanResolved, ct.getNgayTraThucTe());
            if (dynamicSurcharge > 0) {
                ChiTietHoaDon cthdPhuPhi = ChiTietHoaDon.builder()
                        .maChiTietHoaDon(generateMaCTHD())
                        .hoaDon(hd)
                        .loaiChiTiet("PHU_PHI")
                        .soLuong(1)
                        .donGia(dynamicSurcharge)
                        .noiDung("Phụ phí (Nhận sớm/Trả trễ) - P." + (ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?"))
                        .thanhTien(dynamicSurcharge)
                        .build();
                pendingItems.add(cthdPhuPhi);
                tongPhuPhi += dynamicSurcharge;
            }

            tongPhuPhi += ct.getPhuPhiPhatSinh();
            if (ct.getPhuPhiPhatSinh() > 0) {
                ChiTietHoaDon cthdKhac = ChiTietHoaDon.builder()
                        .maChiTietHoaDon(generateMaCTHD())
                        .hoaDon(hd)
                        .loaiChiTiet("PHU_PHI")
                        .soLuong(1)
                        .donGia(ct.getPhuPhiPhatSinh())
                        .noiDung("Phí phát sinh khác - P." + (ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?"))
                        .thanhTien(ct.getPhuPhiPhatSinh())
                        .build();
                pendingItems.add(cthdKhac);
            }

            List<SuDungDichVu> dsDichVu = sddvRepo.findByChiTietDatPhong_MaChiTiet(ct.getMaChiTiet());
            for (SuDungDichVu sddv : dsDichVu) {
                double itemTotal = sddv.getDonGiaLucDung() * sddv.getSoLuong();
                tongDichVu += itemTotal;
                ChiTietHoaDon cthdDV = ChiTietHoaDon.builder()
                        .maChiTietHoaDon(generateMaCTHD())
                        .hoaDon(hd)
                        .loaiChiTiet("DICH_VU")
                        .soLuong(sddv.getSoLuong())
                        .donGia(sddv.getDonGiaLucDung())
                        .noiDung("[P." + (ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?") + "] " + (sddv.getDichVu() != null ? sddv.getDichVu().getTenDichVu() : "Dịch vụ"))
                        .thanhTien(itemTotal)
                        .build();
                pendingItems.add(cthdDV);
            }

            ct.setDaThanhToan(true);
            ctdpRepo.save(ct);

            if (ct.getPhong() != null) {
                Phong p = ct.getPhong();
                p.setTrangThai("CLEANING");
                p.setTenKhachHienTai(null);
                phongRepo.save(p);
            }
        }

        hd.setTongTienPhong(tongTienPhong);
        hd.setTongTienDichVu(tongDichVu + tongPhuPhi);

        double subTotal = tongTienPhong + hd.getTongTienDichVu();
        KhuyenMai chosenKM = null;
        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            chosenKM = khuyenMaiRepo.findById(voucherCode.trim()).orElse(null);
        }
        double tienGiam = 0;
        if (chosenKM != null) {
            hd.setKhuyenMai(chosenKM);
            tienGiam = subTotal * 0.1; 
        }
        hd.setTienGiamKhuyenMai(tienGiam);

        double tienCoc = 0;
        if (mainCt.getDatPhong() != null) {
            double maxCoc = mainCt.getDatPhong().getTienDatCoc();
            tienCoc = customDeposit >= 0 ? Math.min(customDeposit, maxCoc) : maxCoc;
        }
        hd.setTienDatCoc(tienCoc);
        hd.setTongThanhToan(subTotal - tienGiam - tienCoc);
        hd.setTrangThai(status != null ? status : "PAID");

        hoaDonRepo.save(hd);
        for (ChiTietHoaDon item : pendingItems) {
            item.setHoaDon(hd);
            cthdRepo.save(item);
        }

        if (mainCt.getDatPhong() != null) {
            DatPhong dp = datPhongRepo.findById(mainCt.getDatPhong().getMaDatPhong()).orElse(null);
            if (dp != null) {
                if (tienCoc > 0) {
                    dp.setTienDatCoc(Math.max(0, dp.getTienDatCoc() - tienCoc));
                }
                dp.setTrangThai("CHECKED_OUT");
                datPhongRepo.save(dp);
            }
        }

        logService.addLog("system", "Check-out", "Mã HD: " + hd.getMaHoaDon(), "Tổng: " + hd.getTongThanhToan());
        return null;
    }

    @Transactional
    public String checkOutMasterBill(String maDatPhong, String maNV, String voucherCode) {
        DatPhong dp = datPhongRepo.findById(maDatPhong).orElse(null);
        if (dp == null) return "Không tìm thấy thông tin đặt phòng!";

        List<ChiTietDatPhong> rooms = ctdpRepo.findByDatPhong_MaDatPhong(maDatPhong);
        if (rooms == null || rooms.isEmpty()) return "Không có phòng nào để thanh toán!";

        @SuppressWarnings("unused")
        HoaDon hd = new HoaDon();
        hd.setMaHoaDon(generateMaHD());
        hd.setDatPhong(dp);
        hd.setNhanVien(nhanVienRepo.findById(maNV).orElse(null));
        hd.setNgayLap(LocalDateTime.now());

        double tongTienPhong = 0;
        double tongDichVu = 0;
        double tongPhuPhi = 0;
        List<ChiTietHoaDon> pendingItems = new ArrayList<>();

        for (ChiTietDatPhong ct : rooms) {
            if (Boolean.TRUE.equals(ct.getDaThanhToan())) continue;

            LocalDateTime ngayNhan = ct.getNgayNhanThucTe() != null ? ct.getNgayNhanThucTe() : dp.getNgayNhanDuKien();
            LocalDateTime ngayTra = ct.getNgayTraThucTe() != null ? ct.getNgayTraThucTe() : LocalDateTime.now();
            long soNgay = Math.max(1, Duration.between(ngayNhan, ngayTra).toDays());

            double donGia = ct.getGiaThucTeChot() > 0 ? ct.getGiaThucTeChot() : 400000;
            double tienPhong = donGia * soNgay;
            tongTienPhong += tienPhong;

            ChiTietHoaDon ctPhong = ChiTietHoaDon.builder()
                    .maChiTietHoaDon(generateMaCTHD())
                    .hoaDon(hd)
                    .loaiChiTiet("PHONG")
                    .noiDung("Phòng " + (ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?") + " (" + soNgay + " đêm)")
                    .soLuong((int) soNgay)
                    .donGia(donGia)
                    .thanhTien(tienPhong)
                    .build();
            pendingItems.add(ctPhong);

            double phuPhi = roomCalculator.tinhPhuPhi(ct, ngayNhan, ngayTra) + ct.getPhuPhiPhatSinh();
            if (phuPhi > 0) {
                tongPhuPhi += phuPhi;
                ChiTietHoaDon ctPhu = ChiTietHoaDon.builder()
                        .maChiTietHoaDon(generateMaCTHD())
                        .hoaDon(hd)
                        .loaiChiTiet("PHU_PHI")
                        .noiDung("Phụ phí P." + (ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?"))
                        .soLuong(1)
                        .donGia(phuPhi)
                        .thanhTien(phuPhi)
                        .build();
                pendingItems.add(ctPhu);
            }

            List<SuDungDichVu> dsDV = sddvRepo.findByChiTietDatPhong_MaChiTiet(ct.getMaChiTiet());
            for (SuDungDichVu s : dsDV) {
                double t = s.getDonGiaLucDung() * s.getSoLuong();
                tongDichVu += t;
                ChiTietHoaDon ctDV = ChiTietHoaDon.builder()
                        .maChiTietHoaDon(generateMaCTHD())
                        .hoaDon(hd)
                        .loaiChiTiet("DICH_VU")
                        .noiDung("[P." + (ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?") + "] " + (s.getDichVu() != null ? s.getDichVu().getTenDichVu() : "Dịch vụ"))
                        .soLuong(s.getSoLuong())
                        .donGia(s.getDonGiaLucDung())
                        .thanhTien(t)
                        .build();
                pendingItems.add(ctDV);
            }

            ct.setDaThanhToan(true);
            ct.setNgayTraThucTe(ngayTra);
            ctdpRepo.save(ct);

            Phong p = ct.getPhong();
            if (p != null) {
                p.setTrangThai("CLEANING");
                p.setTenKhachHienTai(null);
                phongRepo.save(p);
            }
        }

        hd.setTongTienPhong(tongTienPhong);
        hd.setTongTienDichVu(tongDichVu + tongPhuPhi);

        double subTotal = tongTienPhong + hd.getTongTienDichVu();
        double tienGiam = 0;
        hd.setTienGiamKhuyenMai(tienGiam);

        double tienCoc = dp.getTienDatCoc();
        hd.setTienDatCoc(tienCoc);
        hd.setTongThanhToan(subTotal - tienGiam - tienCoc);
        hd.setTrangThai("PAID");
        hd.setPhuongThucThanhToan("CASH");

        hoaDonRepo.save(hd);
        for (ChiTietHoaDon c : pendingItems) {
            c.setHoaDon(hd);
            cthdRepo.save(c);
        }

        dp.setTrangThai("CHECKED_OUT");
        datPhongRepo.save(dp);

        logService.addLog("system", "Master Checkout", "Đặt phòng: " + dp.getMaDatPhong(), "Tổng HD: " + hd.getTongThanhToan());
        return null;
    }

    public List<ChiTietDatPhong> getChiTietByDatPhong(String ma) {
        return ctdpRepo.findByDatPhong_MaDatPhong(ma);
    }

    public CheckoutPreviewDTO previewCheckOut(String maChiTiet, String voucherCode, double customDeposit) {
        ChiTietDatPhong ct = ctdpRepo.findById(maChiTiet).orElse(null);
        if (ct == null) return null;

        LocalDateTime ngayTra = LocalDateTime.now();
        LocalDateTime ngayNhanResolved = ct.getNgayNhanThucTe();
        if (ngayNhanResolved == null && ct.getDatPhong() != null) ngayNhanResolved = ct.getDatPhong().getNgayNhanDuKien();
        if (ngayNhanResolved == null) ngayNhanResolved = ngayTra.minusDays(1);

        long soNgay = Math.max(1, Duration.between(ngayNhanResolved, ngayTra).toDays());
        double donGiaChot = ct.getGiaThucTeChot() > 0 ? ct.getGiaThucTeChot() : 
            (ct.getPhong() != null ? bangGiaService.layGiaHienHanh(ct.getPhong().getLoaiPhong().getMaLoaiPhong()) : 400000);
        if (donGiaChot <= 0 && ct.getPhong() != null) {
            donGiaChot = ct.getPhong().getGiaTheoNgay();
        }
        if (donGiaChot <= 0) donGiaChot = 400000;
        
        double tienPhong = donGiaChot * soNgay;

        // Calculate early/late surcharges using the exact logic from StandardRoomCalculator
        double phuPhiEarly = 0;
        double phuPhiLate = 0;
        java.math.BigDecimal bdDonGia = java.math.BigDecimal.valueOf(donGiaChot);
        
        // --- PHỤ PHÍ NHẬN PHÒNG SỚM (EARLY CHECK-IN) ---
        if (ngayNhanResolved.getHour() < 14) {
            if (ngayNhanResolved.getHour() < 5) {
                phuPhiEarly = bdDonGia.doubleValue();
            } else if (ngayNhanResolved.getHour() < 9) {
                phuPhiEarly = bdDonGia.multiply(java.math.BigDecimal.valueOf(0.5)).doubleValue();
            } else if (ngayNhanResolved.getHour() < 12) {
                phuPhiEarly = bdDonGia.multiply(java.math.BigDecimal.valueOf(0.3)).doubleValue();
            }
        }

        // --- PHỤ PHÍ TRẢ PHÒNG TRỄ (LATE CHECK-OUT) ---
        if (ngayTra.getHour() > 12) {
            if (ngayTra.getHour() >= 18) {
                phuPhiLate = bdDonGia.doubleValue();
            } else if (ngayTra.getHour() >= 15) {
                phuPhiLate = bdDonGia.multiply(java.math.BigDecimal.valueOf(0.5)).doubleValue();
            } else if (ngayTra.getHour() >= 13) {
                phuPhiLate = bdDonGia.multiply(java.math.BigDecimal.valueOf(0.3)).doubleValue();
            }
        }

        double phuPhiKhac = ct.getPhuPhiPhatSinh();
        
        List<SuDungDichVu> dsDichVu = sddvRepo.findByChiTietDatPhong_MaChiTiet(ct.getMaChiTiet());
        List<CheckoutPreviewDTO.DichVuSuDungDTO> dsDTO = new ArrayList<>();
        double tongDichVu = 0;
        for (SuDungDichVu sddv : dsDichVu) {
            double itemTotal = sddv.getDonGiaLucDung() * sddv.getSoLuong();
            tongDichVu += itemTotal;
            dsDTO.add(CheckoutPreviewDTO.DichVuSuDungDTO.builder()
                .maSuDung(sddv.getMaSuDung())
                .maDichVu(sddv.getDichVu() != null ? sddv.getDichVu().getMaDichVu() : null)
                .tenDichVu(sddv.getDichVu() != null ? sddv.getDichVu().getTenDichVu() : "Dịch vụ")
                .soLuong(sddv.getSoLuong())
                .donGia(sddv.getDonGiaLucDung())
                .thanhTien(itemTotal)
                .build());
        }

        double subTotal = tienPhong + phuPhiEarly + phuPhiLate + phuPhiKhac + tongDichVu;
        
        KhuyenMai chosenKM = null;
        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            chosenKM = khuyenMaiRepo.findById(voucherCode.trim()).orElse(null);
        }
        double tienGiam = 0;
        if (chosenKM != null) {
            tienGiam = subTotal * 0.1; // 10% discount logic matching checkOut
        }

        double tienCoc = 0;
        if (ct.getDatPhong() != null) {
            double maxCoc = ct.getDatPhong().getTienDatCoc();
            tienCoc = customDeposit >= 0 ? Math.min(customDeposit, maxCoc) : maxCoc;
        }

        double tongThanhToan = subTotal - tienGiam - tienCoc;

        return CheckoutPreviewDTO.builder()
            .maChiTiet(maChiTiet)
            .maPhong(ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?")
            .tenKhachHang(ct.getKhachHang() != null ? ct.getKhachHang().getHoTen() : "Khách Lẻ")
            .ngayNhan(ngayNhanResolved)
            .ngayTra(ngayTra)
            .donGiaPhong(donGiaChot)
            .soNgay(soNgay)
            .tienPhong(tienPhong)
            .phuPhiCheckInEarly(phuPhiEarly)
            .phuPhiCheckOutLate(phuPhiLate)
            .phuPhiKhac(phuPhiKhac)
            .dsDichVu(dsDTO)
            .tongDichVu(tongDichVu)
            .subTotal(subTotal)
            .voucherCode(voucherCode)
            .tienGiam(tienGiam)
            .tienCoc(tienCoc)
            .tongThanhToan(tongThanhToan)
            .build();
    }

    @Transactional
    public String xoaDichVu(String maSuDung) {
        if (!sddvRepo.existsById(maSuDung)) return "Dịch vụ sử dụng không tồn tại!";
        sddvRepo.deleteById(maSuDung);
        return null;
    }

    @Transactional
    public String suaSoLuongDichVu(String maSuDung, int soLuong) {
        SuDungDichVu s = sddvRepo.findById(maSuDung).orElse(null);
        if (s == null) return "Dịch vụ sử dụng không tồn tại!";
        if (soLuong <= 0) {
            sddvRepo.delete(s);
        } else {
            s.setSoLuong(soLuong);
            sddvRepo.save(s);
        }
        return null;
    }
}
