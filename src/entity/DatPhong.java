package entity;

import entity.KhachHang;
import entity.NhanVien;
import entity.HoaDon;
import entity.ChiTietDatPhong;
import entity.SuDungDichVu;
import entity.KenhDatPhong;
import entity.enums.TrangThaiDatPhong;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.time.ZoneId;

public class DatPhong {
    private String maDatPhong;
    private LocalDateTime ngayDat;
    private LocalDateTime ngayNhanDuKien;
    private LocalDateTime ngayTraDuKien;
    private int soLuongKhach;
    private double tienDatCoc;
    private double tongTienTamTinh;
    private TrangThaiDatPhong trangThai;
    private String ghiChu;
    private String loaiKhach = "CA_NHAN"; // "CA_NHAN" hoặc "DOAN"
    private String tenDoan;               // Tên đoàn / công ty (null nếu khách lẻ)

    // === Kênh đặt phòng (Booking Channel) ===
    private String maKenh = "DIRECT";       // Mã kênh: DIRECT, BOOKING, AGODA, TRAVELOKA...
    private String maXacNhanKenh;           // OTA Confirmation Number (VD: Booking.com #12345)
    private KenhDatPhong kenhDatPhong;      // Đối tượng kênh đầy đủ

    // === No-show handling ===
    private double phiNoShow;               // Phí phạt no-show
    private LocalDateTime hanCheckIn;       // Deadline check-in (quá giờ → no-show)

    // === Waitlist / Overbooking ===
    private int thuTuWaitlist;              // Thứ tự trong waitlist (0 = không phải waitlist)
    private LocalDateTime hanNopCoc;        // Hạn nộp cọc (G-Hold)
    private double phiHuyPhong;             // Phí phạt khi hủy đơn

    private KhachHang khachHang;
    private NhanVien nhanVien;
    private HoaDon hoaDon;
    private List<ChiTietDatPhong> dsChiTiet = new ArrayList<>();
    private List<SuDungDichVu> dsDichVu = new ArrayList<>();
    private String maKhuyenMai; // Voucher code entered during check-in or booking

    public DatPhong() {}

    // Getters and Setters
    public String getMaDatPhong() { return maDatPhong; }
    public void setMaDatPhong(String maDatPhong) { this.maDatPhong = maDatPhong; }

    public LocalDateTime getNgayDat() { return ngayDat; }
    public void setNgayDat(LocalDateTime ngayDat) { this.ngayDat = ngayDat; }

    public LocalDateTime getNgayNhanDuKien() { return ngayNhanDuKien; }
    public void setNgayNhanDuKien(LocalDateTime ngayNhanDuKien) { this.ngayNhanDuKien = ngayNhanDuKien; }

    public LocalDateTime getNgayTraDuKien() { return ngayTraDuKien; }
    public void setNgayTraDuKien(LocalDateTime ngayTraDuKien) { this.ngayTraDuKien = ngayTraDuKien; }

    public int getSoLuongKhach() { return soLuongKhach; }
    public void setSoLuongKhach(int soLuongKhach) { this.soLuongKhach = soLuongKhach; }

    public double getTienDatCoc() { return tienDatCoc; }
    public void setTienDatCoc(double tienDatCoc) { this.tienDatCoc = tienDatCoc; }

    public double getTongTienTamTinh() { return tongTienTamTinh; }
    public void setTongTienTamTinh(double tongTienTamTinh) { this.tongTienTamTinh = tongTienTamTinh; }

    public TrangThaiDatPhong getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiDatPhong trangThai) { this.trangThai = trangThai; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    public String getLoaiKhach() { return loaiKhach; }
    public void setLoaiKhach(String loaiKhach) { this.loaiKhach = loaiKhach; }

    public String getTenDoan() { return tenDoan; }
    public void setTenDoan(String tenDoan) { this.tenDoan = tenDoan; }

    public boolean isDoan() { return "DOAN".equalsIgnoreCase(loaiKhach); }
    public String getLoaiKhachLabel() { return isDoan() ? "Khách đoàn" : "Khách lẻ"; }

    // === Booking Channel ===
    public String getMaKenh() { return maKenh; }
    public void setMaKenh(String maKenh) { this.maKenh = maKenh; }

    public String getMaXacNhanKenh() { return maXacNhanKenh; }
    public void setMaXacNhanKenh(String maXacNhanKenh) { this.maXacNhanKenh = maXacNhanKenh; }

    public KenhDatPhong getKenhDatPhong() { return kenhDatPhong; }
    public void setKenhDatPhong(KenhDatPhong kenhDatPhong) { this.kenhDatPhong = kenhDatPhong; }

    /** Tên kênh đặt phòng để hiển thị */
    public String getTenKenh() {
        if (kenhDatPhong != null) return kenhDatPhong.getTenKenh();
        if (maKenh == null || maKenh.isEmpty()) return "Trực tiếp";
        switch (maKenh) {
            case "DIRECT": return "Trực tiếp";
            case "WEBSITE": return "Website";
            case "BOOKING": return "Booking.com";
            case "AGODA": return "Agoda";
            case "TRAVELOKA": return "Traveloka";
            case "EXPEDIA": return "Expedia";
            case "CORPORATE": return "Doanh nghiệp";
            case "TRAVEL_AGT": return "Đại lý du lịch";
            default: return maKenh;
        }
    }

    // === No-show ===
    public double getPhiNoShow() { return phiNoShow; }
    public void setPhiNoShow(double phiNoShow) { this.phiNoShow = phiNoShow; }

    public LocalDateTime getHanCheckIn() { return hanCheckIn; }
    public void setHanCheckIn(LocalDateTime hanCheckIn) { this.hanCheckIn = hanCheckIn; }

    /** Kiểm tra đã quá hạn check-in chưa */
    public boolean isQuaHanCheckIn() {
        if (hanCheckIn == null) return false;
        return LocalDateTime.now().isAfter(hanCheckIn);
    }

    /** Đánh dấu no-show */
    public void markNoShow(double phiPhat) {
        this.trangThai = TrangThaiDatPhong.NO_SHOW;
        this.phiNoShow = phiPhat;
        this.ghiChu = (this.ghiChu != null ? this.ghiChu + " | " : "") 
                    + "Khách không đến (No-show). Phí phạt: " + String.format("%,.0f đ", phiPhat);
    }

    // === Waitlist ===
    public int getThuTuWaitlist() { return thuTuWaitlist; }
    public void setThuTuWaitlist(int thuTuWaitlist) { this.thuTuWaitlist = thuTuWaitlist; }

    public boolean isWaitlist() { return trangThai == TrangThaiDatPhong.WAITLIST; }

    /** Chuyển từ waitlist sang confirmed khi có phòng trống */
    public void confirmFromWaitlist() {
        if (trangThai == TrangThaiDatPhong.WAITLIST) {
            this.trangThai = TrangThaiDatPhong.CONFIRMED;
            this.thuTuWaitlist = 0;
            this.ghiChu = (this.ghiChu != null ? this.ghiChu + " | " : "") + "Đã xác nhận từ waitlist";
        }
    }

    public LocalDateTime getHanNopCoc() { return hanNopCoc; }
    public void setHanNopCoc(LocalDateTime hanNopCoc) { this.hanNopCoc = hanNopCoc; }

    public double getPhiHuyPhong() { return phiHuyPhong; }
    public void setPhiHuyPhong(double phiHuyPhong) { this.phiHuyPhong = phiHuyPhong; }

    public KhachHang getKhachHang() { return khachHang; }
    public void setKhachHang(KhachHang khachHang) { this.khachHang = khachHang; }

    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }

    public HoaDon getHoaDon() { return hoaDon; }
    public void setHoaDon(HoaDon hoaDon) { this.hoaDon = hoaDon; }

    public List<ChiTietDatPhong> getDsChiTiet() { return dsChiTiet; }
    public void setDsChiTiet(List<ChiTietDatPhong> dsChiTiet) { this.dsChiTiet = dsChiTiet; }

    public List<SuDungDichVu> getDsDichVu() { return dsDichVu; }
    public void setDsDichVu(List<SuDungDichVu> dsDichVu) { this.dsDichVu = dsDichVu; }

    // Compatibility aliases for Legacy UI
    public LocalDateTime getNgayNhan() { return ngayNhanDuKien; }
    public void setNgayNhan(LocalDateTime ngayNhan) { this.ngayNhanDuKien = ngayNhan; }
    public LocalDateTime getNgayTra() { return ngayTraDuKien; }
    public void setNgayTra(LocalDateTime ngayTra) { this.ngayTraDuKien = ngayTra; }
    public int getSoKhach() { return soLuongKhach; }
    public void setSoKhach(int soKhach) { this.soLuongKhach = soKhach; }
    
    public LocalDateTime getNgayNhanDK() { return ngayNhanDuKien; }
    public void setNgayNhanDK(LocalDateTime d) { this.ngayNhanDuKien = d; }
    public LocalDateTime getNgayTraDK() { return ngayTraDuKien; }
    public void setNgayTraDK(LocalDateTime d) { this.ngayTraDuKien = d; }

    public Date getNgayNhanDK_Date() { return ngayNhanDuKien != null ? Date.from(ngayNhanDuKien.atZone(ZoneId.systemDefault()).toInstant()) : null; }
    public Date getNgayTraDK_Date() { return ngayTraDuKien != null ? Date.from(ngayTraDuKien.atZone(ZoneId.systemDefault()).toInstant()) : null; }

    public void setNgayNhanDK(Date d) { if (d != null) this.ngayNhanDuKien = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(); }
    public void setNgayTraDK(Date d) { if (d != null) this.ngayTraDuKien = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(); }

    // Legacy String setters/getters
    public void setTrangThai(String s) {
        if (s == null) return;
        try {
            this.trangThai = TrangThaiDatPhong.valueOf(s.toUpperCase());
        } catch (Exception e) {
            if (s.contains("xác nhận")) this.trangThai = TrangThaiDatPhong.CONFIRMED;
            else if (s.contains("Hủy")) this.trangThai = TrangThaiDatPhong.CANCELLED;
            else if (s.contains("no-show") || s.contains("NO_SHOW")) this.trangThai = TrangThaiDatPhong.NO_SHOW;
            else if (s.contains("waitlist") || s.contains("WAITLIST")) this.trangThai = TrangThaiDatPhong.WAITLIST;
        }
    }
    
    public String getSoPhong() { return (dsChiTiet != null && !dsChiTiet.isEmpty()) ? dsChiTiet.get(0).getPhong().getMaPhong() : "—"; }
    public String getDanhSachTenPhong() {
        if (dsChiTiet == null || dsChiTiet.isEmpty()) return "—";
        StringBuilder sb = new StringBuilder();
        for (ChiTietDatPhong ct : dsChiTiet) {
            sb.append(ct.getPhong().getMaPhong()).append(", ");
        }
        if (sb.length() > 2) sb.setLength(sb.length() - 2);
        return sb.toString();
    }
    public void setSoPhong(String p) { /* Ignored, set via dsChiTiet */ }
    public void setMaKH(String maKH) { /* Ignored, set via khachHang */ }
    public String getMaKH() { return khachHang != null ? khachHang.getMaKhachHang() : null; }
    public String getTenKhachHang() { return khachHang != null ? khachHang.getHoTen() : "—"; }
    public String getTenLoaiPhong() { return (dsChiTiet != null && !dsChiTiet.isEmpty()) ? dsChiTiet.get(0).getTenLoaiPhong() : ""; }
    public void setMaNV(String maNV) {}

    public void xacNhan() {
        this.trangThai = TrangThaiDatPhong.CONFIRMED;
    }

    public void huy(String lyDo) {
        this.trangThai = TrangThaiDatPhong.CANCELLED;
        this.ghiChu = (this.ghiChu != null ? this.ghiChu + " | " : "") + "Hủy: " + lyDo;
    }

    public String getMaKhuyenMai() { return maKhuyenMai; }
    public void setMaKhuyenMai(String maKhuyenMai) { this.maKhuyenMai = maKhuyenMai; }

    public double tinhTienTamTinh() {
        double total = 0;
        for (ChiTietDatPhong ct : dsChiTiet) {
            total += ct.getGiaThucTeChot();
        }
        this.tongTienTamTinh = total;
        return total;
    }
}
