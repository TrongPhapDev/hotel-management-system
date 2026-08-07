package com.ohno.hotel.controller;

import com.ohno.hotel.entity.DichVu;
import com.ohno.hotel.service.DichVuService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dich-vu")
public class DichVuController {

    private final DichVuService service;

    public DichVuController(DichVuService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<DichVu>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DichVu> getById(@PathVariable String id) {
        DichVu dv = service.getById(id);
        return dv != null ? ResponseEntity.ok(dv) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody DichVu dv) {
        String error = service.them(dv);
        if (error != null) {
            return ResponseEntity.badRequest().body(error);
        }
        return ResponseEntity.ok(dv);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody DichVu dv) {
        dv.setMaDichVu(id);
        String error = service.sua(dv);
        if (error != null) {
            return ResponseEntity.badRequest().body(error);
        }
        return ResponseEntity.ok(dv);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable String id) {
        String error = service.xoa(id);
        if (error != null) {
            return ResponseEntity.badRequest().body(error);
        }
        return ResponseEntity.ok("Xóa thành công dịch vụ " + id);
    }

    @GetMapping("/search")
    public ResponseEntity<List<DichVu>> search(
            @RequestParam(required = false) String kw,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(service.search(kw, type));
    }

    @GetMapping("/stats/gia-trung-binh")
    public ResponseEntity<Double> getGiaTrungBinh() {
        return ResponseEntity.ok(service.getGiaTrungBinh());
    }
}
