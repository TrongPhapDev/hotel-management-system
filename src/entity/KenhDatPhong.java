package entity;

/**
 * Kênh đặt phòng (Booking Channel).
 * Quản lý nguồn đặt phòng: trực tiếp, OTA (Booking.com, Agoda...), 
 * doanh nghiệp, đại lý du lịch.
 */
public class KenhDatPhong {
    private String maKenh;
    private String tenKenh;
    private String loaiKenh; // DIRECT, OTA, CORPORATE, TRAVEL_AGENT, OTHER
    private double heSoHoaHong; // % hoa hồng cho OTA
    private boolean trangThai;
    private String moTa;

    public KenhDatPhong() {}

    public KenhDatPhong(String maKenh, String tenKenh, String loaiKenh) {
        this.maKenh = maKenh;
        this.tenKenh = tenKenh;
        this.loaiKenh = loaiKenh;
    }

    // Getters and Setters
    public String getMaKenh() { return maKenh; }
    public void setMaKenh(String maKenh) { this.maKenh = maKenh; }

    public String getTenKenh() { return tenKenh; }
    public void setTenKenh(String tenKenh) { this.tenKenh = tenKenh; }

    public String getLoaiKenh() { return loaiKenh; }
    public void setLoaiKenh(String loaiKenh) { this.loaiKenh = loaiKenh; }

    public double getHeSoHoaHong() { return heSoHoaHong; }
    public void setHeSoHoaHong(double heSoHoaHong) { this.heSoHoaHong = heSoHoaHong; }

    public boolean isTrangThai() { return trangThai; }
    public void setTrangThai(boolean trangThai) { this.trangThai = trangThai; }

    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }

    /** Label hiển thị cho loại kênh */
    public String getLoaiKenhLabel() {
        if (loaiKenh == null) return "Khác";
        switch (loaiKenh) {
            case "DIRECT": return "Trực tiếp";
            case "OTA": return "OTA";
            case "CORPORATE": return "Doanh nghiệp";
            case "TRAVEL_AGENT": return "Đại lý du lịch";
            default: return "Khác";
        }
    }

    @Override
    public String toString() {
        return tenKenh != null ? tenKenh : maKenh;
    }
}
