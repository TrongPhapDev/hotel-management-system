package entity;

public class KhachHang {
    private String maKhachHang;
    private String hoTen;
    private String sdt;
    private String cccd;
    private java.time.LocalDate ngaySinh;
    private String gioiTinh;
    private String quocTich = "Việt Nam";

    // === Passport / Visa fields (pháp luật VN bắt buộc cho khách nước ngoài) ===
    private String loaiGiayTo = "CCCD"; // CCCD, CMND, PASSPORT
    private String soHoChieu;           // Passport number (riêng biệt với CCCD)
    private String soVisa;              // Visa number
    private java.time.LocalDate ngayHetHanVisa;
    private String noiCapHoChieu;       // Issuing country / authority
    private java.time.LocalDate ngayNhapCanh; // Entry date

    // === CRM & Loyalty ===
    private int soLanO;                 // Tổng số lần đã ở (stays)
    private double tongChiTieu;         // Tổng chi tiền quyết toán (VND)
    private int diemTichLuy;            // Điểm thưởng (loyalty points)
    private String hangKhachHang = "Hạng Thường"; 
    private boolean isBlacklist = false;
    private String preferences;
    private String vipLevel = "BRONZE";

    public KhachHang() {}

    public KhachHang(String maKhachHang, String hoTen, String sdt, String cccd) {
        this.maKhachHang = maKhachHang;
        this.hoTen = hoTen;
        this.sdt = sdt;
        this.cccd = cccd;
    }

    public KhachHang(String maKhachHang, String hoTen, String sdt, String cccd, java.time.LocalDate ngaySinh, String gioiTinh, String quocTich) {
        this.maKhachHang = maKhachHang;
        this.hoTen = hoTen;
        this.sdt = sdt;
        this.cccd = cccd;
        this.ngaySinh = ngaySinh;
        this.gioiTinh = gioiTinh;
        this.quocTich = quocTich;
    }

    /** Kiểm tra xem khách có phải người nước ngoài không */
    public boolean isNuocNgoai() {
        return quocTich != null && !quocTich.isEmpty() 
            && !quocTich.equalsIgnoreCase("Việt Nam") 
            && !quocTich.equalsIgnoreCase("Viet Nam")
            && !quocTich.equalsIgnoreCase("VN");
    }

    /** Kiểm tra giấy tờ hợp lệ cho khách nước ngoài theo pháp luật VN */
    public String validateGiayToNuocNgoai() {
        if (!isNuocNgoai()) return null; // Khách VN → không cần passport/visa

        StringBuilder errors = new StringBuilder();
        if (soHoChieu == null || soHoChieu.trim().isEmpty()) {
            errors.append("• Khách nước ngoài bắt buộc phải có số hộ chiếu (Passport)\n");
        }
        // Visa có thể miễn cho một số quốc gia, nên chỉ cảnh báo
        if (soVisa == null || soVisa.trim().isEmpty()) {
            // Soft warning, không block
        }
        if (ngayHetHanVisa != null && ngayHetHanVisa.isBefore(java.time.LocalDate.now())) {
            errors.append("• Visa đã hết hạn ngày " + ngayHetHanVisa + "\n");
        }
        return errors.length() > 0 ? errors.toString() : null;
    }

    /** Lấy số giấy tờ tùy thân chính (CCCD hoặc Passport) */
    public String getSoGiayToChinhDisplay() {
        if ("PASSPORT".equals(loaiGiayTo) && soHoChieu != null && !soHoChieu.isEmpty()) {
            return "HC: " + soHoChieu;
        }
        return cccd != null ? cccd : "—";
    }

    public java.time.LocalDate getNgaySinh() { return ngaySinh; }
    public void setNgaySinh(java.time.LocalDate ngaySinh) { this.ngaySinh = ngaySinh; }

    public String getGioiTinh() { return gioiTinh; }
    public void setGioiTinh(String gioiTinh) { this.gioiTinh = gioiTinh; }

    public String getQuocTich() { return quocTich; }
    public void setQuocTich(String quocTich) { this.quocTich = quocTich; }

    // Passport / Visa getters & setters
    public String getLoaiGiayTo() { return loaiGiayTo; }
    public void setLoaiGiayTo(String loaiGiayTo) { this.loaiGiayTo = loaiGiayTo; }

    public String getSoHoChieu() { return soHoChieu; }
    public void setSoHoChieu(String soHoChieu) { this.soHoChieu = soHoChieu; }

    public String getSoVisa() { return soVisa; }
    public void setSoVisa(String soVisa) { this.soVisa = soVisa; }

    public java.time.LocalDate getNgayHetHanVisa() { return ngayHetHanVisa; }
    public void setNgayHetHanVisa(java.time.LocalDate ngayHetHanVisa) { this.ngayHetHanVisa = ngayHetHanVisa; }

    public String getNoiCapHoChieu() { return noiCapHoChieu; }
    public void setNoiCapHoChieu(String noiCapHoChieu) { this.noiCapHoChieu = noiCapHoChieu; }

    public java.time.LocalDate getNgayNhapCanh() { return ngayNhapCanh; }
    public void setNgayNhapCanh(java.time.LocalDate ngayNhapCanh) { this.ngayNhapCanh = ngayNhapCanh; }

    public KhachHang timKiem(String keyword) {
        // Search logic based on keyword (hoTen, sdt, or cccd)
        if (this.hoTen.contains(keyword) || this.sdt.contains(keyword) || (this.cccd != null && this.cccd.contains(keyword))) {
            return this;
        }
        return null;
    }

    // Getters and Setters
    public String getMaKhachHang() { return maKhachHang; }
    public void setMaKhachHang(String maKhachHang) { this.maKhachHang = maKhachHang; }

    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }

    public String getSdt() { return sdt; }
    public void setSdt(String sdt) { this.sdt = sdt; }

    public String getCccd() { return cccd; }
    public void setCccd(String cccd) { this.cccd = cccd; }

    private String email;
    private String trangThai = "Hoạt động";
    private String anhCCCD; // Đường dẫn file ảnh CCCD/Passport

    public String getAnhCCCD() { return anhCCCD; }
    public void setAnhCCCD(String anhCCCD) { this.anhCCCD = anhCCCD; }

    public String getMaKH() { return maKhachHang; }
    public void setMaKH(String maKH) { this.maKhachHang = maKH; }
    public String getSoDienThoai() { return sdt; }
    public void setSoDienThoai(String sdt) { this.sdt = sdt; }
    public String getEmail() { return email != null ? email : ""; }
    public void setEmail(String email) { this.email = email; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String status) { this.trangThai = status; }

    @Override
    public String toString() {
        return hoTen + " (" + maKhachHang + ")";
    }

    // CRM Getters & Setters
    public int getSoLanO() { return soLanO; }
    public void setSoLanO(int soLanO) { this.soLanO = soLanO; }

    public double getTongChiTieu() { return tongChiTieu; }
    public void setTongChiTieu(double tongChiTieu) { this.tongChiTieu = tongChiTieu; }

    public int getDiemTichLuy() { return diemTichLuy; }
    public void setDiemTichLuy(int diemTichLuy) { this.diemTichLuy = diemTichLuy; }

    public String getHangKhachHang() {
        // Tự động tính hạng dựa trên ngưỡng trong implementation_plan
        if (soLanO >= 10 || tongChiTieu >= 20000000) return "VIP (Gold)";
        if (soLanO >= 3 || tongChiTieu >= 5000000) return "Thân thiết (Silver)";
        return "Hạng Thường (Bronze)";
    }
    public void setHangKhachHang(String hangKhachHang) { this.hangKhachHang = hangKhachHang; }

    public boolean isBlacklist() { return isBlacklist; }
    public void setBlacklist(boolean blacklist) { isBlacklist = blacklist; }

    public String getPreferences() { return preferences != null ? preferences : ""; }
    public void setPreferences(String preferences) { this.preferences = preferences; }

    public String getVipLevel() { return vipLevel; }
    public void setVipLevel(String vipLevel) { this.vipLevel = vipLevel; }
}
