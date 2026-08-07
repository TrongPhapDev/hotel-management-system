package entity;

import entity.LoaiPhong;
import entity.HuongNhin;
import entity.LichSuTrangThaiPhong;
import entity.enums.TrangThaiPhong;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Phong {
    private String maPhong;
    private int tang;
    private String viTri;           // backward compat field
    private double giaTheoNgay;
    private TrangThaiPhong trangThai;
    private String tenKhachHienTai;
    private String sdtKhachHienTai;
    private LocalDateTime checkInTime;
    private LocalDateTime expectedCheckOutTime;
    
    private LoaiPhong loaiPhong;
    private HuongNhin huongNhin;    // FK → HuongNhin table
    private List<LichSuTrangThaiPhong> dsLichSu = new ArrayList<>();

    public Phong() {}

    public Phong(String maPhong, String viTri, TrangThaiPhong trangThai) {
        this.maPhong = maPhong;
        this.viTri = viTri;
        this.trangThai = trangThai;
    }

    public void capNhatTrangThai(TrangThaiPhong tt) {
        this.trangThai = tt;
    }

    public boolean kiemTraSanSang() {
        return this.trangThai == TrangThaiPhong.AVAILABLE;
    }

    public boolean kiemTraSanSang(LocalDateTime tuNgay, LocalDateTime denNgay) {
        return kiemTraSanSang();
    }

    public String getMaPhong() { return maPhong; }
    public void setMaPhong(String maPhong) { this.maPhong = maPhong; }

    public int getTang() { return tang; }
    public void setTang(int tang) { this.tang = tang; }

    public String getViTri() { return viTri; }
    public void setViTri(String viTri) { this.viTri = viTri; }

    public TrangThaiPhong getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiPhong trangThai) { this.trangThai = trangThai; }

    public String getTrangThaiString() {
        if (trangThai == null) return "Có sẵn";
        switch (trangThai) {
            case AVAILABLE: return "Có sẵn";
            case OCCUPIED: return "Đang thuê";
            case MAINTENANCE: return "Bảo trì";
            case CLEANING: return "Vệ sinh";
            default: return "Có sẵn";
        }
    }

    public void setTrangThai(String s) {
        if (s == null) return;
        if (s.equalsIgnoreCase("Có sẵn") || s.equalsIgnoreCase("AVAILABLE")) this.trangThai = TrangThaiPhong.AVAILABLE;
        else if (s.equalsIgnoreCase("Đang thuê") || s.equalsIgnoreCase("OCCUPIED")) this.trangThai = TrangThaiPhong.OCCUPIED;
        else if (s.equalsIgnoreCase("Bảo trì") || s.equalsIgnoreCase("MAINTENANCE")) this.trangThai = TrangThaiPhong.MAINTENANCE;
        else if (s.equalsIgnoreCase("Vệ sinh") || s.equalsIgnoreCase("CLEANING")) this.trangThai = TrangThaiPhong.CLEANING;
    }

    public LoaiPhong getLoaiPhong() { return loaiPhong; }
    public void setLoaiPhong(LoaiPhong loaiPhong) { this.loaiPhong = loaiPhong; }

    public HuongNhin getHuongNhin() { return huongNhin; }
    public void setHuongNhin(HuongNhin huongNhin) {
        this.huongNhin = huongNhin;
        // Sync viTri for backward compat
        if (huongNhin != null) this.viTri = huongNhin.getTenHuongNhin();
    }

    public List<LichSuTrangThaiPhong> getDsLichSu() { return dsLichSu; }
    public void setDsLichSu(List<LichSuTrangThaiPhong> dsLichSu) { this.dsLichSu = dsLichSu; }

    public String getTenKhachHienTai() { return tenKhachHienTai; }
    public void setTenKhachHienTai(String tenKhachHienTai) { this.tenKhachHienTai = tenKhachHienTai; }

    public String getSdtKhachHienTai() { return sdtKhachHienTai; }
    public void setSdtKhachHienTai(String sdtKhachHienTai) { this.sdtKhachHienTai = sdtKhachHienTai; }

    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }

    public LocalDateTime getExpectedCheckOutTime() { return expectedCheckOutTime; }
    public void setExpectedCheckOutTime(LocalDateTime expectedCheckOutTime) { this.expectedCheckOutTime = expectedCheckOutTime; }

    public String getSoPhong() { return maPhong; }
    public void setSoPhong(String soPhong) { this.maPhong = soPhong; }

    public String getTenLoaiPhong() { return loaiPhong != null ? loaiPhong.getTenLoaiPhong() : ""; }
    public int getSucChua() { return loaiPhong != null ? loaiPhong.getSucChua() : 0; }

    /**
     * Giá cơ sở (rack rate) CHƯA nhân hệ số hướng nhìn.
     * Dùng khi cần so sánh hoặc hiển thị giá gốc.
     */
    public double getGiaCoSo() {
        if (loaiPhong != null && loaiPhong.getGiaTheoNgay() > 0) return loaiPhong.getGiaTheoNgay();
        return giaTheoNgay;
    }

    /**
     * Giá thực tế = Giá cơ sở × Hệ số hướng nhìn.
     * Ví dụ: Standard 400,000đ × 1.3 (biển) = 520,000đ
     * Tất cả UI gọi method này → tự động đúng giá.
     */
    public double getGiaTheoNgay() {
        double base = getGiaCoSo();
        double heSo = (huongNhin != null) ? huongNhin.getHeSoGia() : 1.0;
        if (heSo <= 0) heSo = 1.0; // safety
        return base * heSo;
    }

    public void setGiaTheoNgay(double giaTheoNgay) {
        this.giaTheoNgay = giaTheoNgay;
        if (this.loaiPhong != null) this.loaiPhong.setGiaTheoNgay(giaTheoNgay);
    }

    public void setGiaTheoNgay(long giaTheoNgay) {
        setGiaTheoNgay((double) giaTheoNgay);
    }

    /** View từ HuongNhin object, fallback sang viTri string */
    public String getView() {
        if (huongNhin != null) return huongNhin.getTenHuongNhin();
        return viTri;
    }
    public void setView(String view) { this.viTri = view; }

    /** Mã hướng nhìn (for DB operations) */
    public String getMaHuongNhin() {
        return huongNhin != null ? huongNhin.getMaHuongNhin() : null;
    }

    public String getLoaiView() {
        if (huongNhin != null) return huongNhin.getTenHuongNhin();
        return viTri;
    }

    public String getMoTa() { return loaiPhong != null ? loaiPhong.getMoTa() : ""; }
    public void setMoTa(String moTa) {
        if (this.loaiPhong == null) this.loaiPhong = new LoaiPhong();
        this.loaiPhong.setMoTa(moTa);
    }

    public void setSucChua(int sucChua) {
        if (this.loaiPhong == null) this.loaiPhong = new LoaiPhong();
        this.loaiPhong.setSucChua(sucChua);
    }

    public void setMaLoai(String maLoai) {
        if (this.loaiPhong == null) this.loaiPhong = new LoaiPhong();
        this.loaiPhong.setMaLoaiPhong(maLoai);
    }

    public String getMaLoai() { return loaiPhong != null ? loaiPhong.getMaLoaiPhong() : ""; }

    @Override
    public String toString() {
        return "P." + maPhong;
    }
}
