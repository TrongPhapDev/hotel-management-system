package entity;

import java.time.LocalDateTime;

public class NhatKyHeThong {
    private int maLog;
    private LocalDateTime thoiGian;
    private String tenDangNhap;
    private String hanhDong;
    private String doiTuong;
    private String chiTiet;

    public NhatKyHeThong() {}

    public NhatKyHeThong(String tenDangNhap, String hanhDong, String doiTuong, String chiTiet) {
        this.thoiGian = LocalDateTime.now();
        this.tenDangNhap = tenDangNhap;
        this.hanhDong = hanhDong;
        this.doiTuong = doiTuong;
        this.chiTiet = chiTiet;
    }

    // Getters and Setters
    public int getMaLog() { return maLog; }
    public void setMaLog(int maLog) { this.maLog = maLog; }

    public LocalDateTime getThoiGian() { return thoiGian; }
    public void setThoiGian(LocalDateTime thoiGian) { this.thoiGian = thoiGian; }

    public String getTenDangNhap() { return tenDangNhap; }
    public void setTenDangNhap(String tenDangNhap) { this.tenDangNhap = tenDangNhap; }

    public String getHanhDong() { return hanhDong; }
    public void setHanhDong(String hanhDong) { this.hanhDong = hanhDong; }

    public String getDoiTuong() { return doiTuong; }
    public void setDoiTuong(String doiTuong) { this.doiTuong = doiTuong; }

    public String getChiTiet() { return chiTiet; }
    public void setChiTiet(String chiTiet) { this.chiTiet = chiTiet; }
}
