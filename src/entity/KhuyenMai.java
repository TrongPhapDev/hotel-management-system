package entity;

import entity.enums.LoaiGiam;
import java.time.LocalDateTime;

public class KhuyenMai {
    private String maKhuyenMai;
    private String tenKhuyenMai;
    private LoaiGiam loaiGiam;
    private double giaTriGiam;
    private LocalDateTime ngayBatDau;
    private LocalDateTime ngayKetThuc;
    
    // New fields
    private boolean trangThai = true; 
    private int soLuong = 999;
    private int daDung = 0;
    private double dieuKienToiThieu = 0;
    private double giaTriGiamToiDa = 0; // Cap for PERCENT discount type

    public KhuyenMai() {}

    public KhuyenMai(String maKhuyenMai, String tenKhuyenMai, LoaiGiam loaiGiam, double giaTriGiam, LocalDateTime ngayBatDau, LocalDateTime ngayKetThuc) {
        this.maKhuyenMai = maKhuyenMai;
        this.tenKhuyenMai = tenKhuyenMai;
        this.loaiGiam = loaiGiam;
        this.giaTriGiam = giaTriGiam;
        this.ngayBatDau = ngayBatDau;
        this.ngayKetThuc = ngayKetThuc;
    }

    public boolean kiemTraHopLe(double tongBill) {
        return kiemTraHopLe(tongBill, LocalDateTime.now());
    }

    /**
     * Overload dùng thời điểm đặt phòng để validate (Trường phái 1).
     * @param tongBill Tổng tiền bill
     * @param thoiDiemDat Thời điểm ghi nhận booking
     * @return true nếu hợp lệ tại thời điểm đó
     */
    public boolean kiemTraHopLe(double tongBill, LocalDateTime thoiDiemDat) {
        if (!trangThai) return false;
        if (thoiDiemDat == null) thoiDiemDat = LocalDateTime.now();
        
        if (thoiDiemDat.isBefore(ngayBatDau) || thoiDiemDat.isAfter(ngayKetThuc)) return false;
        if (daDung >= soLuong) return false;
        if (tongBill < dieuKienToiThieu) return false;
        return true;
    }

    public double tinhSoTienGiam(double tongTien) {
        double soTienGiam = 0;
        if (loaiGiam == LoaiGiam.PERCENT) {
            soTienGiam = tongTien * (giaTriGiam / 100.0);
            if (giaTriGiamToiDa > 0) {
                soTienGiam = Math.min(soTienGiam, giaTriGiamToiDa);
            }
        } else {
            soTienGiam = Math.min(tongTien, giaTriGiam);
        }
        return soTienGiam;
    }

    // Getters and Setters
    public String getMaKhuyenMai() { return maKhuyenMai; }
    public void setMaKhuyenMai(String maKhuyenMai) { this.maKhuyenMai = maKhuyenMai; }
    public String getMaKM() { return maKhuyenMai; } 
    public void setMaKM(String maKM) { this.maKhuyenMai = maKM; }

    public String getTenKhuyenMai() { return tenKhuyenMai; }
    public void setTenKhuyenMai(String tenKhuyenMai) { this.tenKhuyenMai = tenKhuyenMai; }
    public String getTenKM() { return tenKhuyenMai; }
    public void setTenKM(String tenKM) { this.tenKhuyenMai = tenKM; }

    public LoaiGiam getLoaiGiam() { return loaiGiam; }
    public void setLoaiGiam(LoaiGiam loaiGiam) { this.loaiGiam = loaiGiam; }

    public double getGiaTriGiam() { return giaTriGiam; }
    public void setGiaTriGiam(double giaTriGiam) { this.giaTriGiam = giaTriGiam; }

    public LocalDateTime getNgayBatDau() { return ngayBatDau; }
    public void setNgayBatDau(LocalDateTime ngayBatDau) { this.ngayBatDau = ngayBatDau; }

    public LocalDateTime getNgayKetThuc() { return ngayKetThuc; }
    public void setNgayKetThuc(LocalDateTime ngayKetThuc) { this.ngayKetThuc = ngayKetThuc; }

    public boolean isTrangThai() { return trangThai; }
    public void setTrangThai(boolean trangThai) { this.trangThai = trangThai; }

    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }

    public int getDaDung() { return daDung; }
    public void setDaDung(int daDung) { this.daDung = daDung; }

    public double getDieuKienToiThieu() { return dieuKienToiThieu; }
    public void setDieuKienToiThieu(double dieuKienToiThieu) { this.dieuKienToiThieu = dieuKienToiThieu; }

    public double getGiaTriGiamToiDa() { return giaTriGiamToiDa; }
    public void setGiaTriGiamToiDa(double giaTriGiamToiDa) { this.giaTriGiamToiDa = giaTriGiamToiDa; }
}
