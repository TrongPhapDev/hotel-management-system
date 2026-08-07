package entity;

/**
 * Hướng nhìn phòng (Room View) – bảng danh mục.
 * Ví dụ: Hướng phố (×1.0), Hướng biển (×1.3), VIP Panorama (×1.5)
 * 
 * Công thức giá:  Giá phòng = LoaiPhong.giaTheoNgay × HuongNhin.heSoGia
 */
public class HuongNhin {
    private String maHuongNhin;
    private String tenHuongNhin;
    private String moTa;
    private double heSoGia = 1.0;  // multiplier: 1.0 = giá gốc, 1.3 = +30%
    private int thuTu;

    public HuongNhin() {}

    public HuongNhin(String maHuongNhin, String tenHuongNhin) {
        this.maHuongNhin = maHuongNhin;
        this.tenHuongNhin = tenHuongNhin;
    }

    public HuongNhin(String maHuongNhin, String tenHuongNhin, double heSoGia) {
        this.maHuongNhin = maHuongNhin;
        this.tenHuongNhin = tenHuongNhin;
        this.heSoGia = heSoGia;
    }

    public String getMaHuongNhin() { return maHuongNhin; }
    public void setMaHuongNhin(String maHuongNhin) { this.maHuongNhin = maHuongNhin; }

    public String getTenHuongNhin() { return tenHuongNhin; }
    public void setTenHuongNhin(String tenHuongNhin) { this.tenHuongNhin = tenHuongNhin; }

    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }

    public double getHeSoGia() { return heSoGia; }
    public void setHeSoGia(double heSoGia) { this.heSoGia = heSoGia; }

    public int getThuTu() { return thuTu; }
    public void setThuTu(int thuTu) { this.thuTu = thuTu; }

    /** Hiển thị % phụ thu, ví dụ: "+30%" hoặc "Giá gốc" */
    public String getHeSoGiaDisplay() {
        if (heSoGia == 1.0) return "Giá gốc";
        int percent = (int) Math.round((heSoGia - 1.0) * 100);
        return (percent > 0 ? "+" : "") + percent + "%";
    }

    /** Cho UI dropdown: "Hướng biển (+30%)" */
    public String getDisplayWithPrice() {
        if (heSoGia == 1.0) return tenHuongNhin;
        return tenHuongNhin + " (" + getHeSoGiaDisplay() + ")";
    }

    @Override
    public String toString() {
        return tenHuongNhin;
    }
}
