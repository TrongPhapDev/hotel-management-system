package entity;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChiTietDatPhong {
    private String maChiTiet;
    private LocalDateTime ngayNhanThucTe;
    private LocalDateTime ngayTraThucTe;
    private double giaThucTeChot;
    private double phuPhiPhatSinh;
    private boolean daThanhToan; // 0 = Chưa/Đang treo, 1 = Đã thanh toán (Master Bill Routing)

    private Phong phong;
    private DatPhong datPhong;
    private KhachHang khachHang; // Người lưu trú thực tế của phòng này
    private NhanVien nhanVien;
    private List<SuDungDichVu> dsSuDungDichVu = new ArrayList<>();

    public ChiTietDatPhong() {}

    public int tinhSoNgayO() {
        if (ngayNhanThucTe != null && ngayTraThucTe != null) {
            return (int) Duration.between(ngayNhanThucTe, ngayTraThucTe).toDays();
        }
        return 0;
    }

    public double tinhSoGioO() {
        if (ngayNhanThucTe != null && ngayTraThucTe != null) {
            return Duration.between(ngayNhanThucTe, ngayTraThucTe).toMinutes() / 60.0;
        }
        return 0.0;
    }

    // Getters and Setters
    public String getMaChiTiet() { return maChiTiet; }
    public void setMaChiTiet(String maChiTiet) { this.maChiTiet = maChiTiet; }

    public LocalDateTime getNgayNhanThucTe() { return ngayNhanThucTe; }
    public void setNgayNhanThucTe(LocalDateTime ngayNhanThucTe) { this.ngayNhanThucTe = ngayNhanThucTe; }

    public LocalDateTime getNgayTraThucTe() { return ngayTraThucTe; }
    public void setNgayTraThucTe(LocalDateTime ngayTraThucTe) { this.ngayTraThucTe = ngayTraThucTe; }

    public double getGiaThucTeChot() { return giaThucTeChot; }
    public void setGiaThucTeChot(double giaThucTeChot) { this.giaThucTeChot = giaThucTeChot; }

    public double getPhuPhiPhatSinh() { return phuPhiPhatSinh; }
    public void setPhuPhiPhatSinh(double phuPhiPhatSinh) { this.phuPhiPhatSinh = phuPhiPhatSinh; }

    public boolean isDaThanhToan() { return daThanhToan; }
    public void setDaThanhToan(boolean daThanhToan) { this.daThanhToan = daThanhToan; }

    public Phong getPhong() { return phong; }
    public void setPhong(Phong phong) { this.phong = phong; }

    public DatPhong getDatPhong() { return datPhong; }
    public void setDatPhong(DatPhong datPhong) { this.datPhong = datPhong; }

    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }

    public List<SuDungDichVu> getDsSuDungDichVu() { return dsSuDungDichVu; }
    public void setDsSuDungDichVu(List<SuDungDichVu> dsSuDungDichVu) { this.dsSuDungDichVu = dsSuDungDichVu; }

    public KhachHang getKhachHang() { return khachHang; }
    public void setKhachHang(KhachHang khachHang) { this.khachHang = khachHang; }

    /**
     * Lấy người lưu trú thực tế: ưu tiên khách riêng của phòng,
     * fallback về khách của đơn đặt nếu chưa gán.
     */
    public KhachHang resolveKhachHang() {
        if (khachHang != null) return khachHang;
        if (datPhong != null) return datPhong.getKhachHang();
        return null;
    }

    // Compatibility aliases for legacy UI
    public String  getMaThue()      { return maChiTiet; }
    public String  getSoPhong()     { return phong != null ? phong.getMaPhong() : ""; }
    public String  getMaKH()        { return (datPhong != null && datPhong.getKhachHang() != null) ? datPhong.getKhachHang().getMaKhachHang() : ""; }
    public Date    getNgayNhan()    { return ngayNhanThucTe != null ? Date.from(ngayNhanThucTe.atZone(ZoneId.systemDefault()).toInstant()) : null; }
    public String  getMaDatPhong()  { return datPhong != null ? datPhong.getMaDatPhong() : ""; }
    public String  getTenLoaiPhong(){ return phong != null ? phong.getTenLoaiPhong() : ""; }

    public Date getNgayNhanThucTe_Date() { return getNgayNhan(); }
    public Date getNgayTraThucTe_Date() { 
        return ngayTraThucTe != null ? Date.from(ngayTraThucTe.atZone(ZoneId.systemDefault()).toInstant()) : null; 
    }
}
