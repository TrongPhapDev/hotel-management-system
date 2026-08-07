package entity;

import java.time.LocalDate;

public class NhanVien {
    private String maNhanVien;
    private String hoTen;
    private String sdt;
    private String chucVu;
    private String email;
    private String cccd;
    private LocalDate ngaySinh;
    private String gioiTinh;
    private String diaChi;
    private LocalDate ngayVaoLam;
    private boolean dangLamViec = true; // true = đang làm, false = đã nghỉ

    private TaiKhoan taiKhoan;

    public NhanVien() {}

    public NhanVien(String maNhanVien, String hoTen, String sdt, String chucVu) {
        this.maNhanVien = maNhanVien;
        this.hoTen = hoTen;
        this.sdt = sdt;
        this.chucVu = chucVu;
    }

    // Getters and Setters
    public String getMaNhanVien() { return maNhanVien; }
    public void setMaNhanVien(String maNhanVien) { this.maNhanVien = maNhanVien; }

    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }

    public String getSdt() { return sdt; }
    public void setSdt(String sdt) { this.sdt = sdt; }

    public String getChucVu() { return chucVu; }
    public void setChucVu(String chucVu) { this.chucVu = chucVu; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCccd() { return cccd; }
    public void setCccd(String cccd) { this.cccd = cccd; }

    public LocalDate getNgaySinh() { return ngaySinh; }
    public void setNgaySinh(LocalDate ngaySinh) { this.ngaySinh = ngaySinh; }

    public String getGioiTinh() { return gioiTinh; }
    public void setGioiTinh(String gioiTinh) { this.gioiTinh = gioiTinh; }

    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }

    public LocalDate getNgayVaoLam() { return ngayVaoLam; }
    public void setNgayVaoLam(LocalDate ngayVaoLam) { this.ngayVaoLam = ngayVaoLam; }

    public boolean isDangLamViec() { return dangLamViec; }
    public void setDangLamViec(boolean dangLamViec) { this.dangLamViec = dangLamViec; }


    public TaiKhoan getTaiKhoan() { return taiKhoan; }
    public void setTaiKhoan(TaiKhoan taiKhoan) { this.taiKhoan = taiKhoan; }

    // Backward compat aliases
    public String getSoDienThoai() { return sdt; }

    @Override
    public String toString() {
        return hoTen + " (" + maNhanVien + ")";
    }
}
