package com.ohno.hotel.controller;

import com.ohno.hotel.entity.HoaDon;
import com.ohno.hotel.service.HoaDonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hoa-don")
public class HoaDonController {
    private final HoaDonService service;
    public HoaDonController(HoaDonService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<HoaDon>> getAll() { return ResponseEntity.ok(service.getAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<HoaDon> getById(@PathVariable String id) {
        HoaDon hd = service.getById(id);
        return hd != null ? ResponseEntity.ok(hd) : ResponseEntity.notFound().build();
    }

    @GetMapping("/dat-phong/{maDatPhong}")
    public ResponseEntity<HoaDon> getByDatPhong(@PathVariable String maDatPhong) {
        HoaDon hd = service.getByDatPhong(maDatPhong);
        return hd != null ? ResponseEntity.ok(hd) : ResponseEntity.notFound().build();
    }

    @GetMapping("/doanh-thu")
    public ResponseEntity<Double> getDoanhThu(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        if (month != null && year != null)
            return ResponseEntity.ok(service.getDoanhThuThang(month, year));
        return ResponseEntity.ok(service.getTongDoanhThu());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody HoaDon hd) {
        String err = service.taoHoaDon(hd);
        return err == null ? ResponseEntity.ok(hd) : ResponseEntity.badRequest().body(err);
    }

    @PatchMapping("/{id}/thanh-toan")
    public ResponseEntity<String> thanhToan(@PathVariable String id, @RequestBody Map<String, String> body) {
        String err = service.thanhToan(id, body.getOrDefault("phuongThuc", "CASH"));
        return err == null ? ResponseEntity.ok("Thanh toán thành công!") : ResponseEntity.badRequest().body(err);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody HoaDon hd) {
        hd.setMaHoaDon(id);
        String err = service.capNhat(hd);
        return err == null ? ResponseEntity.ok(hd) : ResponseEntity.badRequest().body(err);
    }
}
