package com.ohno.hotel.service;

import com.ohno.hotel.entity.NhanVien;
import com.ohno.hotel.entity.TaiKhoan;
import com.ohno.hotel.repository.NhanVienRepository;
import com.ohno.hotel.repository.TaiKhoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class NhanVienService {
    private final NhanVienRepository repo;
    private final TaiKhoanRepository taiKhoanRepo;

    public NhanVienService(NhanVienRepository repo, TaiKhoanRepository taiKhoanRepo) {
        this.repo = repo;
        this.taiKhoanRepo = taiKhoanRepo;
    }

    public List<NhanVien> getAll() { return repo.findAll(); }
    public List<NhanVien> getDangLamViec() { return repo.findByDangLamViecTrue(); }
    public NhanVien getById(String id) { return repo.findById(id).orElse(null); }

    public String generateMa() {
        try {
            Integer max = repo.findAll().stream()
                .filter(n -> n.getMaNhanVien().matches("NV\\d+"))
                .mapToInt(n -> Integer.parseInt(n.getMaNhanVien().substring(2)))
                .max().orElse(0);
            return String.format("NV%03d", max + 1);
        } catch (Exception e) { return "NV001"; }
    }

    @Transactional
    public String them(NhanVien nv) {
        if (nv.getHoTen() == null || nv.getHoTen().trim().isEmpty())
            return "Họ tên không được trống!";
        if (nv.getMaNhanVien() == null || nv.getMaNhanVien().trim().isEmpty())
            nv.setMaNhanVien(generateMa());
        else if (repo.existsById(nv.getMaNhanVien()))
            return "Mã nhân viên đã tồn tại!";
        repo.save(nv);

        // Tự động tạo tài khoản khi thêm nhân viên
        TaiKhoan tk = new TaiKhoan();
        tk.setTenDangNhap(nv.getMaNhanVien());
        String matKhauDefault = (nv.getMatKhau() != null && !nv.getMatKhau().trim().isEmpty()) ? nv.getMatKhau() : "123456";
        tk.setMatKhau(matKhauDefault);
        tk.setNhanVien(nv);
        tk.setTrangThai(nv.isDangLamViec());

        String vaiTro = "RECEPTIONIST";
        if (nv.getChucVu() != null) {
            String cv = nv.getChucVu().toUpperCase();
            if (cv.equals("ADMIN") || cv.equals("MANAGER") || cv.equals("RECEPTIONIST")) {
                vaiTro = cv;
            }
        }
        tk.setVaiTro(vaiTro);
        taiKhoanRepo.save(tk);

        return null;
    }

    @Transactional
    public String sua(NhanVien nv) {
        if (nv.getHoTen() == null || nv.getHoTen().trim().isEmpty())
            return "Họ tên không được trống!";
        if (!repo.existsById(nv.getMaNhanVien()))
            return "Nhân viên không tồn tại!";
        repo.save(nv);

        // Đồng bộ tài khoản
        TaiKhoan tk = taiKhoanRepo.findById(nv.getMaNhanVien()).orElse(null);
        if (tk == null) {
            tk = new TaiKhoan();
            tk.setTenDangNhap(nv.getMaNhanVien());
            tk.setNhanVien(nv);
        }
        if (nv.getMatKhau() != null && !nv.getMatKhau().trim().isEmpty()) {
            tk.setMatKhau(nv.getMatKhau());
        } else if (tk.getMatKhau() == null) {
            tk.setMatKhau("123456");
        }
        tk.setTrangThai(nv.isDangLamViec());

        String vaiTro = "RECEPTIONIST";
        if (nv.getChucVu() != null) {
            String cv = nv.getChucVu().toUpperCase();
            if (cv.equals("ADMIN") || cv.equals("MANAGER") || cv.equals("RECEPTIONIST")) {
                vaiTro = cv;
            }
        }
        tk.setVaiTro(vaiTro);
        taiKhoanRepo.save(tk);

        return null;
    }

    @Transactional
    public String nghi(String maNV) {
        NhanVien nv = getById(maNV);
        if (nv == null) return "Không tìm thấy nhân viên!";
        nv.setDangLamViec(false);
        repo.save(nv);

        // Khóa tài khoản tương ứng
        taiKhoanRepo.findById(maNV).ifPresent(tk -> {
            tk.setTrangThai(false);
            taiKhoanRepo.save(tk);
        });

        return null;
    }
}
