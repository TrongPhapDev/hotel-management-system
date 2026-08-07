package entity;

import entity.enums.VaiTro;
import java.time.LocalDateTime;

public class TaiKhoan {
    private String tenDangNhap;
    private String matKhau;
    private VaiTro vaiTro;
    private boolean trangThai;
    private LocalDateTime lanDangNhapCuoi;

    private NhanVien nhanVien;

    public TaiKhoan() {}

    public TaiKhoan(String tenDangNhap, String matKhau, VaiTro vaiTro, boolean trangThai, LocalDateTime lanDangNhapCuoi) {
        this.tenDangNhap = tenDangNhap;
        this.matKhau = matKhau;
        this.vaiTro = vaiTro;
        this.trangThai = trangThai;
        this.lanDangNhapCuoi = lanDangNhapCuoi;
    }

    public boolean dangNhap(String tenDN, String matKhau) {
        // Authentication logic
        return this.tenDangNhap.equals(tenDN) && this.matKhau.equals(matKhau);
    }

    public void doiMatKhau(String matKhauMoi) {
        this.matKhau = matKhauMoi;
    }

    // Getters and Setters
    public String getTenDangNhap() { return tenDangNhap; }
    public void setTenDangNhap(String tenDangNhap) { this.tenDangNhap = tenDangNhap; }

    public String getMatKhau() { return matKhau; }
    public void setMatKhau(String matKhau) { this.matKhau = matKhau; }

    public VaiTro getVaiTro() { return vaiTro; }
    public void setVaiTro(VaiTro vaiTro) { this.vaiTro = vaiTro; }

    public boolean isTrangThai() { return trangThai; }
    public void setTrangThai(boolean trangThai) { this.trangThai = trangThai; }

    public LocalDateTime getLanDangNhapCuoi() { return lanDangNhapCuoi; }
    public void setLanDangNhapCuoi(LocalDateTime lanDangNhapCuoi) { this.lanDangNhapCuoi = lanDangNhapCuoi; }

    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }
}
