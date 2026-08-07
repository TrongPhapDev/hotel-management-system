package com.ohno.hotel.controller;

import com.ohno.hotel.entity.TaiKhoan;
import com.ohno.hotel.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("tenDangNhap");
        String password = body.get("matKhau");
        TaiKhoan tk = service.dangNhap(username, password);
        if (tk == null) return ResponseEntity.status(401).body("Tên đăng nhập hoặc mật khẩu không đúng!");
        if (!tk.isTrangThai()) return ResponseEntity.status(403).body("Tài khoản đã bị khóa!");
        return ResponseEntity.ok(tk);
    }

    @PostMapping("/doi-mat-khau")
    public ResponseEntity<String> doiMatKhau(@RequestBody Map<String, String> body) {
        String err = service.doiMatKhau(
            body.get("tenDangNhap"), body.get("matKhauCu"), body.get("matKhauMoi")
        );
        return err == null ? ResponseEntity.ok("Đổi mật khẩu thành công!") : ResponseEntity.badRequest().body(err);
    }
}
