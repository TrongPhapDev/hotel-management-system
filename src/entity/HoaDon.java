package entity;

import entity.enums.TrangThaiThanhToan;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HoaDon {
    private String maHoaDon;
    private LocalDateTime ngayLap;
    private double tongTienPhong;
    private double tongTienDichVu;
    private double tienGiamKhuyenMai;
    private double tongThanhToan;
    private TrangThaiThanhToan trangThai;
    private String phuongThucThanhToan; // CASH, CARD, TRANSFER

    private List<ChiTietHoaDon> dsChiTietHoaDon = new ArrayList<>();
    private DatPhong datPhong;
    private KhuyenMai khuyenMai;
    private NhanVien nhanVien;
    private KhachHang khachHang; // Added for legacy compatibility

    public HoaDon() {}

    // Getters and Setters
    public String getMaHoaDon() { return maHoaDon; }
    public void setMaHoaDon(String maHoaDon) { this.maHoaDon = maHoaDon; }

    public LocalDateTime getNgayLap() { return ngayLap; }
    public void setNgayLap(LocalDateTime ngayLap) { this.ngayLap = ngayLap; }

    public double getTongTienPhong() { return tongTienPhong; }
    public void setTongTienPhong(double tongTienPhong) { this.tongTienPhong = tongTienPhong; }

    public double getTongTienDichVu() { return tongTienDichVu; }
    public void setTongTienDichVu(double tongTienDichVu) { this.tongTienDichVu = tongTienDichVu; }

    public double getTienGiamKhuyenMai() { return tienGiamKhuyenMai; }
    public void setTienGiamKhuyenMai(double tienGiamKhuyenMai) { this.tienGiamKhuyenMai = tienGiamKhuyenMai; }

    public double getTongThanhToan() { return tongThanhToan; }
    public void setTongThanhToan(double tongThanhToan) { this.tongThanhToan = tongThanhToan; }

    public TrangThaiThanhToan getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiThanhToan trangThai) { this.trangThai = trangThai; }

    public String getPhuongThucThanhToan() { return phuongThucThanhToan; }
    public void setPhuongThucThanhToan(String phuongThucThanhToan) { this.phuongThucThanhToan = phuongThucThanhToan; }

    public List<ChiTietHoaDon> getDsChiTietHoaDon() { return dsChiTietHoaDon; }
    public void setDsChiTietHoaDon(List<ChiTietHoaDon> dsChiTietHoaDon) { this.dsChiTietHoaDon = dsChiTietHoaDon; }

    public DatPhong getDatPhong() { return datPhong; }
    public void setDatPhong(DatPhong datPhong) { this.datPhong = datPhong; }

    public KhuyenMai getKhuyenMai() { return khuyenMai; }
    public void setKhuyenMai(KhuyenMai khuyenMai) { this.khuyenMai = khuyenMai; }

    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }

    public KhachHang getKhachHang() { return khachHang; }
    public void setKhachHang(KhachHang khachHang) { this.khachHang = khachHang; }
    
    // Legacy alias
    public double tinhTongThanhToan() {
        double total = 0;
        for (ChiTietHoaDon ct : dsChiTietHoaDon) {
            total += ct.getThanhTien();
        }
        this.tongThanhToan = total - tienGiamKhuyenMai;
        return this.tongThanhToan;
    }

    public void inHoaDon() {
        System.out.println("--- HÓA ĐƠN: " + maHoaDon + " ---");
        System.out.println("Ngày lập: " + ngayLap);
        for (ChiTietHoaDon ct : dsChiTietHoaDon) {
            System.out.println("- " + ct.getNoiDung() + ": " + ct.getThanhTien());
        }
        System.out.println("Tổng cộng: " + tongThanhToan);
    }
}
