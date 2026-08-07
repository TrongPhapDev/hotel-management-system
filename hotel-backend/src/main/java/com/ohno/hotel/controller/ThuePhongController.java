package com.ohno.hotel.controller;

import com.ohno.hotel.entity.ChiTietDatPhong;
import com.ohno.hotel.entity.SuDungDichVu;
import com.ohno.hotel.service.ThuePhongService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.ohno.hotel.dto.CheckoutPreviewDTO;

@RestController
@RequestMapping("/api/thue-phong")
public class ThuePhongController {
    private final ThuePhongService service;

    public ThuePhongController(ThuePhongService service) {
        this.service = service;
    }

    @PostMapping("/check-in")
    public ResponseEntity<String> checkIn(@RequestBody ChiTietDatPhong ct) {
        String err = service.checkIn(ct);
        return err == null ? ResponseEntity.ok("Check-in thành công!") : ResponseEntity.badRequest().body(err);
    }

    @PostMapping("/them-dich-vu")
    public ResponseEntity<String> themDichVu(@RequestBody Map<String, Object> body) {
        String maChiTiet = (String) body.get("maChiTiet");
        String maDV = (String) body.get("maDV");
        int sl = (int) body.get("soLuong");
        double gia = body.get("donGia") != null ? ((Number) body.get("donGia")).doubleValue() : -1.0;
        String err = service.themDichVu(maChiTiet, maDV, sl, gia);
        return err == null ? ResponseEntity.ok("Thêm dịch vụ thành công!") : ResponseEntity.badRequest().body(err);
    }

    @PostMapping("/gia-han")
    public ResponseEntity<String> extendStay(@RequestBody Map<String, Object> body) {
        String maChiTiet = (String) body.get("maChiTiet");
        LocalDateTime userTime = LocalDateTime.parse((String) body.get("ngayTraMoi"));
        String err = service.extendStay(maChiTiet, userTime);
        return err == null ? ResponseEntity.ok("Gia hạn thành công!") : ResponseEntity.badRequest().body(err);
    }

    @PostMapping("/doi-phong")
    public ResponseEntity<String> transferRoom(@RequestBody Map<String, Object> body) {
        String maCT = (String) body.get("maChiTiet");
        String maP = (String) body.get("maPhongMoi");
        boolean keepPrice = (boolean) body.getOrDefault("giuNguyenGia", true);
        String err = service.transferRoom(maCT, maP, keepPrice);
        return err == null ? ResponseEntity.ok("Đổi phòng thành công!") : ResponseEntity.badRequest().body(err);
    }

    @GetMapping("/check-out/preview/{maChiTiet}")
    public ResponseEntity<?> previewCheckOut(
            @PathVariable String maChiTiet,
            @RequestParam(required = false) String voucherCode,
            @RequestParam(required = false, defaultValue = "-1.0") double customDeposit) {
        CheckoutPreviewDTO dto = service.previewCheckOut(maChiTiet, voucherCode, customDeposit);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/check-out")
    public ResponseEntity<String> checkOut(@RequestBody Map<String, Object> body) {
        String maChiTiet = (String) body.get("maChiTiet");
        String maNV = (String) body.get("maNhanVien");
        String status = (String) body.getOrDefault("trangThai", "PAID");
        String voucherCode = (String) body.get("voucherCode");
        double customDeposit = body.get("customDeposit") != null ? ((Number) body.get("customDeposit")).doubleValue() : -1.0;

        String err = service.checkOut(maChiTiet, maNV, status, voucherCode, customDeposit);
        return err == null ? ResponseEntity.ok("Thanh toán thành công!") : ResponseEntity.badRequest().body(err);
    }

    @PostMapping("/check-out-master")
    public ResponseEntity<String> checkOutMaster(@RequestBody Map<String, Object> body) {
        String maDatPhong = (String) body.get("maDatPhong");
        String maNV = (String) body.get("maNhanVien");
        String voucherCode = (String) body.get("voucherCode");

        String err = service.checkOutMasterBill(maDatPhong, maNV, voucherCode);
        return err == null ? ResponseEntity.ok("Thanh toán đoàn thành công!") : ResponseEntity.badRequest().body(err);
    }

    @DeleteMapping("/su-dung-dich-vu/{maSuDung}")
    public ResponseEntity<String> xoaDichVu(@PathVariable String maSuDung) {
        String err = service.xoaDichVu(maSuDung);
        return err == null ? ResponseEntity.ok("Xóa dịch vụ thành công!") : ResponseEntity.badRequest().body(err);
    }

    @PutMapping("/su-dung-dich-vu/{maSuDung}")
    public ResponseEntity<String> suaDichVu(@PathVariable String maSuDung, @RequestBody Map<String, Integer> body) {
        Integer sl = body.get("soLuong");
        if (sl == null) return ResponseEntity.badRequest().body("Thiếu số lượng!");
        String err = service.suaSoLuongDichVu(maSuDung, sl);
        return err == null ? ResponseEntity.ok("Cập nhật dịch vụ thành công!") : ResponseEntity.badRequest().body(err);
    }
}
