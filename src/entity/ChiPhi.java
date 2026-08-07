package entity;

import java.time.LocalDateTime;

public class ChiPhi {
    private int maChiPhi;
    private NhanVien nhanVien;
    private double soTien;
    private String lyDo;
    private LocalDateTime thoiGian;
    private String maGiaoCa;

    public ChiPhi() {}

    public ChiPhi(NhanVien nhanVien, double soTien, String lyDo, LocalDateTime thoiGian, String maGiaoCa) {
        this.nhanVien = nhanVien;
        this.soTien = soTien;
        this.lyDo = lyDo;
        this.thoiGian = thoiGian;
        this.maGiaoCa = maGiaoCa;
    }

    // Getters and Setters
    public int getMaChiPhi() { return maChiPhi; }
    public void setMaChiPhi(int maChiPhi) { this.maChiPhi = maChiPhi; }

    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }

    public double getSoTien() { return soTien; }
    public void setSoTien(double soTien) { this.soTien = soTien; }

    public String getLyDo() { return lyDo; }
    public void setLyDo(String lyDo) { this.lyDo = lyDo; }

    public LocalDateTime getThoiGian() { return thoiGian; }
    public void setThoiGian(LocalDateTime thoiGian) { this.thoiGian = thoiGian; }

    public String getMaGiaoCa() { return maGiaoCa; }
    public void setMaGiaoCa(String maGiaoCa) { this.maGiaoCa = maGiaoCa; }
}
