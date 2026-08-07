package com.ohno.hotel.service;

import com.ohno.hotel.entity.TaiKhoan;
import com.ohno.hotel.repository.TaiKhoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AuthService {
    private final TaiKhoanRepository repo;
    public AuthService(TaiKhoanRepository repo) { this.repo = repo; }

    @Transactional
    public TaiKhoan dangNhap(String tenDangNhap, String matKhau) {
        TaiKhoan tk = repo.findByTenDangNhapAndMatKhau(tenDangNhap, matKhau).orElse(null);
        if (tk != null && tk.isTrangThai()) {
            tk.setLanDangNhapCuoi(LocalDateTime.now());
            repo.save(tk);
        }
        return tk;
    }

    public TaiKhoan getById(String tenDangNhap) {
        return repo.findById(tenDangNhap).orElse(null);
    }

    @Transactional
    public String doiMatKhau(String tenDangNhap, String matKhauCu, String matKhauMoi) {
        TaiKhoan tk = repo.findByTenDangNhapAndMatKhau(tenDangNhap, matKhauCu).orElse(null);
        if (tk == null) return "Mật khẩu cũ không đúng!";
        if (matKhauMoi == null || matKhauMoi.length() < 3) return "Mật khẩu mới quá ngắn!";
        tk.setMatKhau(matKhauMoi);
        repo.save(tk);
        return null;
    }
}
