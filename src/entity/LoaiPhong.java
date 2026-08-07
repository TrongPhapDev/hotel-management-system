package entity;

import java.util.ArrayList;
import java.util.List;

public class LoaiPhong {
    private String maLoaiPhong;
    private String tenLoaiPhong;
    private int sucChua;
    private String moTa;
    private double giaTheoNgay;
    private boolean dangKinhDoanh = true; // true = Hoạt động, false = Ngừng
    private List<TienNghi> dsTienNghi = new ArrayList<>(); // Many-to-many relationship

    public LoaiPhong() {}

    public LoaiPhong(String maLoaiPhong, String tenLoaiPhong, int sucChua, String moTa) {
        this.maLoaiPhong = maLoaiPhong;
        this.tenLoaiPhong = tenLoaiPhong;
        this.sucChua = sucChua;
        this.moTa = moTa;
    }

    // Primary getters/setters
    public String getMaLoaiPhong() { return maLoaiPhong; }
    public void setMaLoaiPhong(String maLoaiPhong) { this.maLoaiPhong = maLoaiPhong; }

    public String getTenLoaiPhong() { return tenLoaiPhong; }
    public void setTenLoaiPhong(String tenLoaiPhong) { this.tenLoaiPhong = tenLoaiPhong; }

    public int getSucChua() { return sucChua; }
    public void setSucChua(int sucChua) { this.sucChua = sucChua; }

    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }

    public double getGiaTheoNgay() { return giaTheoNgay; }
    public void setGiaTheoNgay(double giaTheoNgay) { this.giaTheoNgay = giaTheoNgay; }
    public void setGiaTheoNgay(float gia) { this.giaTheoNgay = gia; }

    public boolean isDangKinhDoanh() { return dangKinhDoanh; }
    public void setDangKinhDoanh(boolean dangKinhDoanh) { this.dangKinhDoanh = dangKinhDoanh; }

    // Tiện nghi (Many-to-Many)
    public List<TienNghi> getDsTienNghi() { return dsTienNghi; }
    public void setDsTienNghi(List<TienNghi> dsTienNghi) { this.dsTienNghi = dsTienNghi; }

    /** Lấy chuỗi tiện nghi để hiển thị: "Wifi, Điều hòa, Tivi..." */
    public String getTienNghiDisplay() {
        if (dsTienNghi == null || dsTienNghi.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dsTienNghi.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(dsTienNghi.get(i).getTenTienNghi());
        }
        return sb.toString();
    }

    // ---- Backward compat aliases ----
    public String getMaLoai() { return maLoaiPhong; }
    public void setMaLoai(String s) { this.maLoaiPhong = s; }
    public String getTenLoai() { return tenLoaiPhong; }
    public void setTenLoai(String s) { this.tenLoaiPhong = s; }
    public int getSoNguoiToiDa() { return sucChua; }
    public void setSoNguoiToiDa(int n) { this.sucChua = n; }

    /** Backward compat: trả về chuỗi tiện nghi từ M:N relationship */
    public String getTienNghi() { return getTienNghiDisplay(); }
    public void setTienNghi(String tienNghi) { /* no-op, dùng setDsTienNghi thay */ }
    public String getTiNghi() { return getTienNghiDisplay(); }
    public void setTiNghi(String s) { /* no-op */ }

    public String getDanhMuc() { return "Phòng"; }
    public void setDanhMuc(String s) {}

    // Giá cơ sở = giaTheoNgay (rack rate)
    public double getGiaThapNhat() { return giaTheoNgay; }
    public void setGiaThapNhat(double d) { this.giaTheoNgay = d; }
    public void setGiaThapNhat(long d) { this.giaTheoNgay = d; }
    public double getGiaCaoNhat() { return giaTheoNgay; }
    public void setGiaCaoNhat(double d) { this.giaTheoNgay = d; }
    public void setGiaCaoNhat(long d) { this.giaTheoNgay = d; }

    // Trạng thái thực sự dựa trên field dangKinhDoanh
    public String getTrangThai() { return dangKinhDoanh ? "Hoạt động" : "Ngừng"; }
    public void setTrangThai(String s) {
        this.dangKinhDoanh = !"Ngừng".equals(s) && !"INACTIVE".equalsIgnoreCase(s);
    }

    public java.util.List<Phong> layDanhSachPhongTrong() {
        return new java.util.ArrayList<>();
    }

    @Override
    public String toString() {
        return tenLoaiPhong;
    }
}
