package com.ohno.hotel.controller;

import com.ohno.hotel.service.ThongKeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/thong-ke")
public class ThongKeController {
    private final ThongKeService service;
    public ThongKeController(ThongKeService service) { this.service = service; }

    /** Tổng quan dashboard: phongTrong, dangO, daDat, tongPhong, checkinHomNay, checkoutHomNay, doanhThuHomNay */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(service.getDashboardStats());
    }

    /** Doanh thu 7 ngày gần nhất [[daysAgo, revenue]...] */
    @GetMapping("/doanh-thu-7-ngay")
    public ResponseEntity<List<long[]>> doanhThu7Ngay() {
        return ResponseEntity.ok(service.getDoanhThu7Ngay());
    }

    /** Thống kê theo kỳ: 7ngay | thang | quy | nam */
    @GetMapping("/ky")
    public ResponseEntity<Map<String, Object>> thongKeKy(@RequestParam(defaultValue = "thang") String type) {
        return ResponseEntity.ok(service.getThongKeKy(type));
    }

    /** Doanh thu theo ngày trong kỳ */
    @GetMapping("/doanh-thu-theo-ngay")
    public ResponseEntity<List<long[]>> doanhThuTheoNgay(@RequestParam(defaultValue = "thang") String ky) {
        return ResponseEntity.ok(service.getDoanhThuTheoNgay(ky));
    }

    /** Top N phòng doanh thu cao nhất trong kỳ */
    @GetMapping("/top-phong")
    public ResponseEntity<List<Map<String, Object>>> topPhong(
            @RequestParam(defaultValue = "5") int n,
            @RequestParam(defaultValue = "thang") String ky) {
        return ResponseEntity.ok(service.getTopPhong(n, ky));
    }

    /** Top N dịch vụ bán chạy nhất */
    @GetMapping("/top-dich-vu")
    public ResponseEntity<List<Map<String, Object>>> topDichVu(
            @RequestParam(defaultValue = "5") int n,
            @RequestParam(defaultValue = "thang") String ky) {
        return ResponseEntity.ok(service.getTopDichVu(n, ky));
    }

    /** Danh sách check-in hôm nay */
    @GetMapping("/checkin-hom-nay")
    public ResponseEntity<List<Map<String, Object>>> checkinHomNay() {
        return ResponseEntity.ok(service.getCheckinHomNay());
    }

    /** Danh sách check-out hôm nay */
    @GetMapping("/checkout-hom-nay")
    public ResponseEntity<List<Map<String, Object>>> checkoutHomNay() {
        return ResponseEntity.ok(service.getCheckoutHomNay());
    }

    /** Hoạt động gần đây */
    @GetMapping("/hoat-dong")
    public ResponseEntity<List<Map<String, Object>>> hoatDong(@RequestParam(defaultValue = "10") int n) {
        return ResponseEntity.ok(service.getHoatDongGanDay(n));
    }

    /** Cảnh báo hệ thống */
    @GetMapping("/alerts")
    public ResponseEntity<List<Map<String, Object>>> alerts() {
        return ResponseEntity.ok(service.getAlerts());
    }
}
