package entity;

public class DichVu {
    private String maDichVu;
    private String tenDichVu;
    private double donGia;
    private String donViTinh;
    private String loai;
    private String moTa;
    private int soLuongMin;
    private int trangThai; // 1: Hoạt động, 0: Tạm ngừng, -1: Đã xóa

    public DichVu() {}

    public boolean kiemTraSanSang() {
        return this.trangThai == 1;
    }

    // Getters and Setters
    public String getMaDichVu() { return maDichVu; }
    public void setMaDichVu(String maDichVu) { this.maDichVu = maDichVu; }
    public String getMaDV() { return maDichVu; }

    public String getTenDichVu() { return tenDichVu; }
    public void setTenDichVu(String tenDichVu) { this.tenDichVu = tenDichVu; }
    public String getTenDV() { return tenDichVu; }

    public double getDonGia() { return donGia; }
    public void setDonGia(double donGia) { this.donGia = donGia; }
    public double getGia() { return donGia; }
    public long getDonGiaHienTai() { return (long) donGia; }
    public void setDonGiaHienTai(double d) { this.donGia = d; }
    public void setDonGiaHienTai(long l) { this.donGia = (double) l; }

    public String getDonViTinh() { return donViTinh; }
    public void setDonViTinh(String donViTinh) { this.donViTinh = donViTinh; }
    public String getDonVi() { return donViTinh; }

    public String getLoai() { return loai; }
    public void setLoai(String loai) { this.loai = loai; }

    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }

    public int getSoLuongMin() { return soLuongMin; }
    public void setSoLuongMin(int soLuongMin) { this.soLuongMin = soLuongMin; }

    public int getTrangThaiInt() { return trangThai; }
    public void setTrangThaiInt(int trangThai) { this.trangThai = trangThai; }

    public boolean isTrangThai() { return trangThai == 1; }
    public void setTrangThai(boolean active) { this.trangThai = active ? 1 : 0; }
    
    public String getTrangThai() { 
        if (trangThai == 1) return "Hoạt động";
        if (trangThai == 0) return "Tạm ngừng";
        return "Đã xóa";
    }
    public void setTrangThai(String s) { 
        if ("Hoạt động".equalsIgnoreCase(s)) this.trangThai = 1;
        else if ("Tạm ngừng".equalsIgnoreCase(s)) this.trangThai = 0;
        else this.trangThai = -1;
    }

    // Compatibility aliases
    public void setTenDV(String s) { this.tenDichVu = s; }
    public void setGia(double d) { this.donGia = d; }
    public void setGia(long l) { this.donGia = (double)l; }
    public void setDonVi(String s) { this.donViTinh = s; }

    @Override
    public String toString() {
        return tenDichVu;
    }
}
