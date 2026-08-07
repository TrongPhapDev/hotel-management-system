package com.ohno.hotel.controller;

import com.ohno.hotel.entity.NhanVien;
import com.ohno.hotel.service.NhanVienService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/nhan-vien")
public class NhanVienController {
    private final NhanVienService service;
    public NhanVienController(NhanVienService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<NhanVien>> getAll() { return ResponseEntity.ok(service.getAll()); }

    @GetMapping("/dang-lam-viec")
    public ResponseEntity<List<NhanVien>> getDangLamViec() { return ResponseEntity.ok(service.getDangLamViec()); }

    @GetMapping("/{id}")
    public ResponseEntity<NhanVien> getById(@PathVariable String id) {
        NhanVien nv = service.getById(id);
        return nv != null ? ResponseEntity.ok(nv) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody NhanVien nv) {
        String err = service.them(nv);
        return err == null ? ResponseEntity.ok(nv) : ResponseEntity.badRequest().body(err);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody NhanVien nv) {
        nv.setMaNhanVien(id);
        String err = service.sua(nv);
        return err == null ? ResponseEntity.ok(nv) : ResponseEntity.badRequest().body(err);
    }

    @PatchMapping("/{id}/nghi")
    public ResponseEntity<String> nghi(@PathVariable String id) {
        String err = service.nghi(id);
        return err == null ? ResponseEntity.ok("Đã cập nhật trạng thái nghỉ việc") : ResponseEntity.badRequest().body(err);
    }
}
