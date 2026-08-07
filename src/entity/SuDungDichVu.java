package entity;

import entity.*;

public class SuDungDichVu {
    private ChiTietDatPhong ctdp;
    private DichVu dichVu;
    private int soLuong;
    private double donGiaLuu;
    private java.time.LocalDateTime thoiDiem;

    public SuDungDichVu() {}

    public double tinhThanhTien() {
        return soLuong * donGiaLuu;
    }

    // Getters and Setters
    public ChiTietDatPhong getCtdp() { return ctdp; }
    public void setCtdp(ChiTietDatPhong ctdp) { this.ctdp = ctdp; }

    public DichVu getDichVu() { return dichVu; }
    public void setDichVu(DichVu dichVu) { this.dichVu = dichVu; }

    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }

    public double getDonGiaLuu() { return donGiaLuu; }
    public void setDonGiaLuu(double donGiaLuu) { this.donGiaLuu = donGiaLuu; }

    public java.time.LocalDateTime getThoiDiem() { return thoiDiem; }
    public void setThoiDiem(java.time.LocalDateTime thoiDiem) { this.thoiDiem = thoiDiem; }

    // Compatibility aliases for legacy UI
    public String getTenDichVu() { return dichVu != null ? dichVu.getTenDichVu() : ""; }
    public double getDonGia() { return donGiaLuu; }
    public double getThanhTien() { return tinhThanhTien(); }
}
