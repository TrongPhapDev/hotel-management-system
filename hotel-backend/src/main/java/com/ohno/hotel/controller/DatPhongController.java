package com.ohno.hotel.controller;

import com.ohno.hotel.entity.DatPhong;
import com.ohno.hotel.service.DatPhongService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dat-phong")
public class DatPhongController {
    private final DatPhongService service;
    public DatPhongController(DatPhongService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<DatPhong>> getAll(@RequestParam(required = false) String kw,
                                                  @RequestParam(required = false) String trangThai) {
        if (kw != null && !kw.isEmpty()) return ResponseEntity.ok(service.search(kw));
        if (trangThai != null && !trangThai.isEmpty()) return ResponseEntity.ok(service.getByTrangThai(trangThai));
        return ResponseEntity.ok(service.getActive());
    }

    @GetMapping("/all")
    public ResponseEntity<List<DatPhong>> getAllFull() { return ResponseEntity.ok(service.getAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<DatPhong> getById(@PathVariable String id) {
        DatPhong dp = service.getById(id);
        return dp != null ? ResponseEntity.ok(dp) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody DatPhong dp) {
        String err = service.them(dp);
        return err == null ? ResponseEntity.ok(dp) : ResponseEntity.badRequest().body(err);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody DatPhong dp) {
        dp.setMaDatPhong(id);
        String err = service.sua(dp);
        return err == null ? ResponseEntity.ok(dp) : ResponseEntity.badRequest().body(err);
    }

    @PatchMapping("/{id}/check-in")
    public ResponseEntity<String> checkIn(@PathVariable String id) {
        String err = service.checkIn(id);
        return err == null ? ResponseEntity.ok("Check-in thành công!") : ResponseEntity.badRequest().body(err);
    }

    @PatchMapping("/{id}/check-out")
    public ResponseEntity<String> checkOut(@PathVariable String id) {
        String err = service.checkOut(id);
        return err == null ? ResponseEntity.ok("Check-out thành công!") : ResponseEntity.badRequest().body(err);
    }

    @PatchMapping("/{id}/huy")
    public ResponseEntity<String> huy(@PathVariable String id, @RequestBody Map<String, String> body) {
        String err = service.huy(id, body.getOrDefault("lyDo", "Không có lý do"));
        return err == null ? ResponseEntity.ok("Đã hủy đặt phòng!") : ResponseEntity.badRequest().body(err);
    }
}
