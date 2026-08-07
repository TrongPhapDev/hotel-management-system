package com.ohno.hotel.controller;

import com.ohno.hotel.entity.GiaoCa;
import com.ohno.hotel.service.GiaoCaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/giao-ca")
public class GiaoCaController {
    private final GiaoCaService service;

    public GiaoCaController(GiaoCaService service) {
        this.service = service;
    }

    @PostMapping("/mo-ca")
    public ResponseEntity<?> moCa(@RequestBody Map<String, Object> body) {
        String maNhanVien = (String) body.get("maNhanVien");
        double tienDauCa = body.get("tienDauCa") != null ? ((Number) body.get("tienDauCa")).doubleValue() : 0.0;
        GiaoCa gc = service.moCa(maNhanVien, tienDauCa);
        return gc != null ? ResponseEntity.ok(gc) : ResponseEntity.badRequest().body("Không thể mở ca trực!");
    }

    @PostMapping("/chot-ca")
    public ResponseEntity<String> chotCa(@RequestBody Map<String, Object> body) {
        String maGiaoCa = (String) body.get("maGiaoCa");
        double tienMatBanGiao = body.get("tienMatBanGiao") != null ? ((Number) body.get("tienMatBanGiao")).doubleValue() : 0.0;
        String maNhanVienNhan = (String) body.get("maNhanVienNhan");
        String ghiChu = (String) body.get("ghiChu");

        String err = service.chotCa(maGiaoCa, tienMatBanGiao, maNhanVienNhan, ghiChu);
        return err == null ? ResponseEntity.ok("Chốt ca thành công!") : ResponseEntity.badRequest().body(err);
    }

    @GetMapping("/current")
    public ResponseEntity<GiaoCa> getCaHienTai(@RequestParam String maNhanVien) {
        GiaoCa gc = service.getCaHienTai(maNhanVien);
        return gc != null ? ResponseEntity.ok(gc) : ResponseEntity.noContent().build();
    }

    @GetMapping("/expected-cash")
    public ResponseEntity<Double> getExpectedCash(@RequestParam String maGiaoCa) {
        return ResponseEntity.ok(service.getExpectedCash(maGiaoCa));
    }

    @GetMapping("/history")
    public ResponseEntity<List<GiaoCa>> getHistory(
            @RequestParam(required = false) String kw,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(service.searchHistory(kw, status));
    }
}
