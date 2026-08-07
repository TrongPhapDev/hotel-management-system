package com.ohno.hotel.controller;

import com.ohno.hotel.entity.ChiTietDatPhong;
import com.ohno.hotel.repository.ChiTietDatPhongRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/chi-tiet-dat-phong")
public class ChiTietDatPhongController {
    private final ChiTietDatPhongRepository repo;

    public ChiTietDatPhongController(ChiTietDatPhongRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public ResponseEntity<List<ChiTietDatPhong>> getAll(@RequestParam(required = false) String maDatPhong) {
        if (maDatPhong != null && !maDatPhong.trim().isEmpty()) {
            return ResponseEntity.ok(repo.findByDatPhong_MaDatPhong(maDatPhong));
        }
        return ResponseEntity.ok(repo.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<ChiTietDatPhong>> getActiveStays() {
        return ResponseEntity.ok(repo.findByDaThanhToanFalse());
    }
}
