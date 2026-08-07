package com.ohno.hotel.controller;

import com.ohno.hotel.entity.KhachHang;
import com.ohno.hotel.service.KhachHangService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/khach-hang")
public class KhachHangController {
    private final KhachHangService service;
    public KhachHangController(KhachHangService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<KhachHang>> getAll(@RequestParam(required = false) String kw) {
        if (kw != null && !kw.trim().isEmpty()) return ResponseEntity.ok(service.search(kw));
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<KhachHang> getById(@PathVariable String id) {
        KhachHang kh = service.getById(id);
        return kh != null ? ResponseEntity.ok(kh) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody KhachHang kh) {
        String err = service.them(kh);
        return err == null ? ResponseEntity.ok(kh) : ResponseEntity.badRequest().body(err);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody KhachHang kh) {
        kh.setMaKhachHang(id);
        String err = service.sua(kh);
        return err == null ? ResponseEntity.ok(kh) : ResponseEntity.badRequest().body(err);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable String id) {
        String err = service.xoa(id);
        return err == null ? ResponseEntity.ok("Đã xóa khách hàng " + id) : ResponseEntity.badRequest().body(err);
    }
}
