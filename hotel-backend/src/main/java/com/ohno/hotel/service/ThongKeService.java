package com.ohno.hotel.service;

import com.ohno.hotel.repository.*;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Service
public class ThongKeService {

    private final PhongRepository phongRepo;
    private final DatPhongRepository datPhongRepo;
    private final HoaDonRepository hoaDonRepo;
    private final KhachHangRepository khachHangRepo;

    public ThongKeService(PhongRepository phongRepo,
                          DatPhongRepository datPhongRepo,
                          HoaDonRepository hoaDonRepo,
                          KhachHangRepository khachHangRepo) {
        this.phongRepo = phongRepo;
        this.datPhongRepo = datPhongRepo;
        this.hoaDonRepo = hoaDonRepo;
        this.khachHangRepo = khachHangRepo;
    }

    // ── Dashboard Stats ────────────────────────────────────────────────────────
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        long tongPhong = phongRepo.count();
        long phongTrong = phongRepo.countByTrangThai("AVAILABLE");
        long dangO = phongRepo.countByTrangThai("OCCUPIED");
        long baoTri = phongRepo.countByTrangThai("MAINTENANCE");
        long dangDon = phongRepo.countByTrangThai("CLEANING");

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);

        long checkinHomNay = datPhongRepo.countByTrangThaiAndNgayNhanBetween("CHECKED_IN", startOfDay, endOfDay);
        long checkoutHomNay = datPhongRepo.countByTrangThaiAndNgayTraBetween("CHECKED_OUT", startOfDay, endOfDay);
        long daDat = datPhongRepo.countByTrangThaiIn(List.of("CONFIRMED", "PENDING"));

        Double doanhThuHomNay = hoaDonRepo.sumThanhToanByNgayLap(startOfDay, endOfDay);

        stats.put("tongPhong", (int) tongPhong);
        stats.put("phongTrong", (int) phongTrong);
        stats.put("dangO", (int) dangO);
        stats.put("baoTri", (int) baoTri);
        stats.put("dangDon", (int) dangDon);
        stats.put("daDat", (int) daDat);
        stats.put("checkinHomNay", (int) checkinHomNay);
        stats.put("checkoutHomNay", (int) checkoutHomNay);
        stats.put("doanhThuHomNay", doanhThuHomNay != null ? doanhThuHomNay.longValue() : 0L);
        return stats;
    }

    // ── Doanh thu 7 ngày ─────────────────────────────────────────────────────
    public List<long[]> getDoanhThu7Ngay() {
        List<long[]> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(23, 59, 59);
            Double rev = hoaDonRepo.sumThanhToanByNgayLap(start, end);
            result.add(new long[]{i, rev != null ? rev.longValue() : 0L});
        }
        return result;
    }

    // ── Thống kê theo kỳ ─────────────────────────────────────────────────────
    public Map<String, Object> getThongKeKy(String ky) {
        LocalDateTime[] range = getDateRange(ky);
        LocalDateTime[] prevRange = getPrevDateRange(ky);

        Double doanhThu = hoaDonRepo.sumThanhToanByNgayLap(range[0], range[1]);
        Double doanhThuTruoc = hoaDonRepo.sumThanhToanByNgayLap(prevRange[0], prevRange[1]);
        Double doanhThuDV = hoaDonRepo.sumTienDichVuByNgayLap(range[0], range[1]);
        Double doanhThuDVTruoc = hoaDonRepo.sumTienDichVuByNgayLap(prevRange[0], prevRange[1]);

        long luotDatPhong = datPhongRepo.countByNgayDatBetween(range[0], range[1]);
        long luotDatPhongTruoc = datPhongRepo.countByNgayDatBetween(prevRange[0], prevRange[1]);
        long khachMoi = khachHangRepo.countByNgayTaoBetween(range[0], range[1]);
        long khachMoiTruoc = khachHangRepo.countByNgayTaoBetween(prevRange[0], prevRange[1]);
        long phongDangThue = phongRepo.countByTrangThai("OCCUPIED");

        Map<String, Object> stats = new HashMap<>();
        stats.put("doanhThu", doanhThu != null ? doanhThu.longValue() : 0L);
        stats.put("doanhThuTruoc", doanhThuTruoc != null ? doanhThuTruoc.longValue() : 0L);
        stats.put("doanhThuDV", doanhThuDV != null ? doanhThuDV.longValue() : 0L);
        stats.put("doanhThuDVTruoc", doanhThuDVTruoc != null ? doanhThuDVTruoc.longValue() : 0L);
        stats.put("luotDatPhong", (int) luotDatPhong);
        stats.put("luotDatPhongTruoc", (int) luotDatPhongTruoc);
        stats.put("khachMoi", (int) khachMoi);
        stats.put("khachMoiTruoc", (int) khachMoiTruoc);
        stats.put("phongDangThue", (int) phongDangThue);
        return stats;
    }

    // ── Doanh thu theo ngày/tháng trong kỳ ───────────────────────────────────
    public List<long[]> getDoanhThuTheoNgay(String ky) {
        LocalDateTime[] range = getDateRange(ky);
        List<long[]> result = new ArrayList<>();
        LocalDate start = range[0].toLocalDate();
        LocalDate end = range[1].toLocalDate();
        LocalDate cur = start;
        while (!cur.isAfter(end)) {
            LocalDateTime s = cur.atStartOfDay();
            LocalDateTime e = cur.atTime(23, 59, 59);
            Double rev = hoaDonRepo.sumThanhToanByNgayLap(s, e);
            result.add(new long[]{cur.getDayOfMonth(), rev != null ? rev.longValue() : 0L});
            cur = cur.plusDays(1);
        }
        return result;
    }

    // ── Top phòng doanh thu ───────────────────────────────────────────────────
    public List<Map<String, Object>> getTopPhong(int n, String ky) {
        // Simplified: trả về empty list nếu chưa có query phức tạp
        return new ArrayList<>();
    }

    // ── Top dịch vụ ───────────────────────────────────────────────────────────
    public List<Map<String, Object>> getTopDichVu(int n, String ky) {
        return new ArrayList<>();
    }

    // ── Check-in hôm nay ─────────────────────────────────────────────────────
    public List<Map<String, Object>> getCheckinHomNay() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(23, 59, 59);
        var list = datPhongRepo.findCheckinHomNay(start, end);
        List<Map<String, Object>> result = new ArrayList<>();
        for (var dp : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("maDatPhong", dp.getMaDatPhong());
            m.put("hoTen", dp.getKhachHang() != null ? dp.getKhachHang().getHoTen() : "—");
            m.put("ngayNhan", dp.getNgayNhanDuKien());
            result.add(m);
        }
        return result;
    }

    // ── Check-out hôm nay ────────────────────────────────────────────────────
    public List<Map<String, Object>> getCheckoutHomNay() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(23, 59, 59);
        var list = datPhongRepo.findCheckoutHomNay(start, end);
        List<Map<String, Object>> result = new ArrayList<>();
        for (var dp : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("maDatPhong", dp.getMaDatPhong());
            m.put("hoTen", dp.getKhachHang() != null ? dp.getKhachHang().getHoTen() : "—");
            m.put("ngayTra", dp.getNgayTraDuKien());
            result.add(m);
        }
        return result;
    }

    // ── Hoạt động gần đây ────────────────────────────────────────────────────
    public List<Map<String, Object>> getHoatDongGanDay(int n) {
        return new ArrayList<>(); // TODO: kết nối SystemLog entity
    }

    // ── Cảnh báo ─────────────────────────────────────────────────────────────
    public List<Map<String, Object>> getAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();
        // Phòng bảo trì quá 3 ngày
        long baoTri = phongRepo.countByTrangThai("MAINTENANCE");
        if (baoTri > 0) {
            Map<String, Object> a = new HashMap<>();
            a.put("type", "WARNING");
            a.put("title", baoTri + " phòng đang bảo trì");
            alerts.add(a);
        }
        return alerts;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private LocalDateTime[] getDateRange(String ky) {
        LocalDate now = LocalDate.now();
        return switch (ky) {
            case "7ngay" -> new LocalDateTime[]{now.minusDays(6).atStartOfDay(), now.atTime(23,59,59)};
            case "quy" -> {
                int q = (now.getMonthValue() - 1) / 3;
                LocalDate start = LocalDate.of(now.getYear(), q * 3 + 1, 1);
                LocalDate end = start.plusMonths(3).minusDays(1);
                yield new LocalDateTime[]{start.atStartOfDay(), end.atTime(23,59,59)};
            }
            case "nam" -> new LocalDateTime[]{LocalDate.of(now.getYear(), 1, 1).atStartOfDay(), LocalDate.of(now.getYear(), 12, 31).atTime(23,59,59)};
            default -> new LocalDateTime[]{now.withDayOfMonth(1).atStartOfDay(), now.atTime(23,59,59)};
        };
    }

    private LocalDateTime[] getPrevDateRange(String ky) {
        LocalDate now = LocalDate.now();
        return switch (ky) {
            case "7ngay" -> new LocalDateTime[]{now.minusDays(13).atStartOfDay(), now.minusDays(7).atTime(23,59,59)};
            case "quy" -> {
                int q = (now.getMonthValue() - 1) / 3;
                LocalDate start = LocalDate.of(now.getYear(), q * 3 + 1, 1).minusMonths(3);
                LocalDate end = start.plusMonths(3).minusDays(1);
                yield new LocalDateTime[]{start.atStartOfDay(), end.atTime(23,59,59)};
            }
            case "nam" -> new LocalDateTime[]{LocalDate.of(now.getYear()-1,1,1).atStartOfDay(), LocalDate.of(now.getYear()-1,12,31).atTime(23,59,59)};
            default -> {
                LocalDate start = now.withDayOfMonth(1).minusMonths(1);
                LocalDate end = now.withDayOfMonth(1).minusDays(1);
                yield new LocalDateTime[]{start.atStartOfDay(), end.atTime(23,59,59)};
            }
        };
    }
}
