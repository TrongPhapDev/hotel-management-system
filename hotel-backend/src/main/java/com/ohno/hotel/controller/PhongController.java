package com.ohno.hotel.controller;

import com.ohno.hotel.entity.Phong;
import com.ohno.hotel.service.PhongService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/phong")
public class PhongController {
    private final PhongService service;
    public PhongController(PhongService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<Phong>> getAll(@RequestParam(required = false) String trangThai) {
        if (trangThai != null && !trangThai.isEmpty())
            return ResponseEntity.ok(service.getByTrangThai(trangThai));
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/trong")
    public ResponseEntity<List<Phong>> getTrong() { return ResponseEntity.ok(service.getAvailable()); }

    @GetMapping("/thong-ke")
    public ResponseEntity<Map<String, Long>> thongKe() { return ResponseEntity.ok(service.thongKeTrangThai()); }

    @GetMapping("/{id}")
    public ResponseEntity<Phong> getById(@PathVariable String id) {
        Phong p = service.getById(id);
        return p != null ? ResponseEntity.ok(p) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Phong p) {
        String err = service.them(p);
        return err == null ? ResponseEntity.ok(p) : ResponseEntity.badRequest().body(err);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Phong p) {
        p.setMaPhong(id);
        String err = service.sua(p);
        return err == null ? ResponseEntity.ok(p) : ResponseEntity.badRequest().body(err);
    }

    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<String> capNhatTrangThai(@PathVariable String id, @RequestBody Map<String, String> body) {
        String err = service.capNhatTrangThai(id, body.get("trangThai"));
        return err == null ? ResponseEntity.ok("Cập nhật thành công") : ResponseEntity.badRequest().body(err);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable String id) {
        String err = service.xoa(id);
        return err == null ? ResponseEntity.ok("Đã xóa phòng " + id) : ResponseEntity.badRequest().body(err);
    }

    @GetMapping("/available-range")
    public ResponseEntity<List<Phong>> getAvailableRoomsInRange(
            @RequestParam String ngayNhan,
            @RequestParam String ngayTra) {
        java.time.LocalDateTime start = java.time.LocalDateTime.parse(ngayNhan);
        java.time.LocalDateTime end = java.time.LocalDateTime.parse(ngayTra);
        return ResponseEntity.ok(service.findAvailableRoomsInRange(start, end));
    }
}
