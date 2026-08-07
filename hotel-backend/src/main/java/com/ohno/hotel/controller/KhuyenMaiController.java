package com.ohno.hotel.controller;

import com.ohno.hotel.entity.KhuyenMai;
import com.ohno.hotel.service.KhuyenMaiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/khuyen-mai")
public class KhuyenMaiController {
    private final KhuyenMaiService service;
    public KhuyenMaiController(KhuyenMaiService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<KhuyenMai>> getAll() { return ResponseEntity.ok(service.getAll()); }

    @GetMapping("/dang-hoat-dong")
    public ResponseEntity<List<KhuyenMai>> getActive() { return ResponseEntity.ok(service.getActive()); }

    @GetMapping("/{id}")
    public ResponseEntity<KhuyenMai> getById(@PathVariable String id) {
        KhuyenMai km = service.getById(id);
        return km != null ? ResponseEntity.ok(km) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody KhuyenMai km) {
        String err = service.them(km);
        return err == null ? ResponseEntity.ok(km) : ResponseEntity.badRequest().body(err);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody KhuyenMai km) {
        km.setMaKhuyenMai(id);
        String err = service.sua(km);
        return err == null ? ResponseEntity.ok(km) : ResponseEntity.badRequest().body(err);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable String id) {
        String err = service.xoa(id);
        return err == null ? ResponseEntity.ok("Đã xóa khuyến mãi " + id) : ResponseEntity.badRequest().body(err);
    }
}
