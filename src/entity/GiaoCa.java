package entity;

import java.time.LocalDateTime;

public class GiaoCa {
    private String maGiaoCa;
    private NhanVien nhanVien; // Người bàn giao
    private LocalDateTime thoiGianBatDau;
    private LocalDateTime thoiGianKetThuc;
    private double tienMatDauCa;
    private double tienMatThuTrongCa;
    private double tienMatBanGiao;
    private String maNhanVienNhan;
    private String ghiChu;
    private double tienMatChenhLech;
    private String trangThai; // OPEN, CLOSED

    public GiaoCa() {}

    // Getters and Setters
    public String getMaGiaoCa() { return maGiaoCa; }
    public void setMaGiaoCa(String maGiaoCa) { this.maGiaoCa = maGiaoCa; }

    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }

    public LocalDateTime getThoiGianBatDau() { return thoiGianBatDau; }
    public void setThoiGianBatDau(LocalDateTime thoiGianBatDau) { this.thoiGianBatDau = thoiGianBatDau; }

    public LocalDateTime getThoiGianKetThuc() { return thoiGianKetThuc; }
    public void setThoiGianKetThuc(LocalDateTime thoiGianKetThuc) { this.thoiGianKetThuc = thoiGianKetThuc; }

    public double getTienMatDauCa() { return tienMatDauCa; }
    public void setTienMatDauCa(double tienMatDauCa) { this.tienMatDauCa = tienMatDauCa; }

    public double getTienMatThuTrongCa() { return tienMatThuTrongCa; }
    public void setTienMatThuTrongCa(double tienMatThuTrongCa) { this.tienMatThuTrongCa = tienMatThuTrongCa; }

    public double getTienMatBanGiao() { return tienMatBanGiao; }
    public void setTienMatBanGiao(double tienMatBanGiao) { this.tienMatBanGiao = tienMatBanGiao; }

    public String getMaNhanVienNhan() { return maNhanVienNhan; }
    public void setMaNhanVienNhan(String maNhanVienNhan) { this.maNhanVienNhan = maNhanVienNhan; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }

    public double getTienMatChenhLech() { return tienMatChenhLech; }
    public void setTienMatChenhLech(double tienMatChenhLech) { this.tienMatChenhLech = tienMatChenhLech; }
}
