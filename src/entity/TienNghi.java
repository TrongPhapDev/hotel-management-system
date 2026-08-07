package entity;

/**
 * Tiện nghi phòng (Amenity) – bảng danh mục.
 * Ví dụ: Wifi, Điều hòa, Tivi, Bồn tắm...
 * Nhóm: Cơ bản, Giải trí, Phòng tắm, Nội thất, Tiện ích
 */
public class TienNghi {
    private String maTienNghi;
    private String tenTienNghi;
    private String nhomTienNghi;
    private String icon;
    private int thuTu;

    public TienNghi() {}

    public TienNghi(String maTienNghi, String tenTienNghi, String nhomTienNghi) {
        this.maTienNghi = maTienNghi;
        this.tenTienNghi = tenTienNghi;
        this.nhomTienNghi = nhomTienNghi;
    }

    public String getMaTienNghi() { return maTienNghi; }
    public void setMaTienNghi(String maTienNghi) { this.maTienNghi = maTienNghi; }

    public String getTenTienNghi() { return tenTienNghi; }
    public void setTenTienNghi(String tenTienNghi) { this.tenTienNghi = tenTienNghi; }

    public String getNhomTienNghi() { return nhomTienNghi; }
    public void setNhomTienNghi(String nhomTienNghi) { this.nhomTienNghi = nhomTienNghi; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public int getThuTu() { return thuTu; }
    public void setThuTu(int thuTu) { this.thuTu = thuTu; }

    /** Hiển thị trong checkbox: "📶 Wifi miễn phí" */
    public String getDisplayText() {
        return (icon != null ? icon + " " : "") + tenTienNghi;
    }

    @Override
    public String toString() {
        return tenTienNghi;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TienNghi)) return false;
        return maTienNghi != null && maTienNghi.equals(((TienNghi) o).maTienNghi);
    }

    @Override
    public int hashCode() {
        return maTienNghi != null ? maTienNghi.hashCode() : 0;
    }
}
