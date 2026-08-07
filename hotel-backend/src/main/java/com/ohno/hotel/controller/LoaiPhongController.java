package com.ohno.hotel.controller;

import com.ohno.hotel.entity.LoaiPhong;
import com.ohno.hotel.service.LoaiPhongService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/loai-phong")
public class LoaiPhongController {
    private final LoaiPhongService service;
    public LoaiPhongController(LoaiPhongService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<LoaiPhong>> getAll() { return ResponseEntity.ok(service.getAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<LoaiPhong> getById(@PathVariable String id) {
        LoaiPhong lp = service.getById(id);
        return lp != null ? ResponseEntity.ok(lp) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody LoaiPhong lp) {
        String err = service.them(lp);
        return err == null ? ResponseEntity.ok(lp) : ResponseEntity.badRequest().body(err);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody LoaiPhong lp) {
        lp.setMaLoaiPhong(id);
        String err = service.sua(lp);
        return err == null ? ResponseEntity.ok(lp) : ResponseEntity.badRequest().body(err);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable String id) {
        String err = service.xoa(id);
        return err == null ? ResponseEntity.ok("Đã xóa loại phòng " + id) : ResponseEntity.badRequest().body(err);
    }
}
