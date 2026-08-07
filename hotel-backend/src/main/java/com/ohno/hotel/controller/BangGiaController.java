package com.ohno.hotel.controller;

import com.ohno.hotel.entity.BangGia;
import com.ohno.hotel.entity.ChiTietBangGia;
import com.ohno.hotel.service.BangGiaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/bang-gia")
public class BangGiaController {
    private final BangGiaService service;

    public BangGiaController(BangGiaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<BangGia>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BangGia> getById(@PathVariable String id) {
        BangGia bg = service.getById(id);
        return bg != null ? ResponseEntity.ok(bg) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody BangGia bg) {
        String err = service.them(bg);
        return err == null ? ResponseEntity.ok(bg) : ResponseEntity.badRequest().body(err);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody BangGia bg) {
        bg.setMaBangGia(id);
        String err = service.sua(bg);
        return err == null ? ResponseEntity.ok(bg) : ResponseEntity.badRequest().body(err);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable String id) {
        String err = service.xoa(id);
        return err == null ? ResponseEntity.ok("Xóa thành công!") : ResponseEntity.badRequest().body(err);
    }

    @GetMapping("/{id}/chi-tiet")
    public ResponseEntity<List<ChiTietBangGia>> getChiTiet(@PathVariable String id) {
        return ResponseEntity.ok(service.getChiTiet(id));
    }

    @PostMapping("/{id}/chi-tiet")
    public ResponseEntity<?> saveChiTiet(@PathVariable String id, @RequestBody List<ChiTietBangGia> dsCT) {
        String err = service.saveChiTiet(id, dsCT);
        return err == null ? ResponseEntity.ok("Lưu chi tiết bảng giá thành công!") : ResponseEntity.badRequest().body(err);
    }
}
