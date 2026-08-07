package service;

import dao.TaiKhoanDAO;
import entity.NhanVien;
import entity.TaiKhoan;
import entity.enums.VaiTro;

/**
 * Quản lý phiên đăng nhập (Session).
 * Dùng Singleton để giữ account đang đăng nhập trong suốt phiên.
 */
public class AuthService {

    private static AuthService instance;
    private TaiKhoan currentAccount;
    private entity.GiaoCa currentShift;
    private final dao.TaiKhoanDAO taiKhoanDAO = new TaiKhoanDAO();
    private final dao.GiaoCaDAO giaoCaDAO = new dao.GiaoCaDAO();

    private AuthService() {}

    public static AuthService getInstance() {
        if (instance == null) instance = new AuthService();
        return instance;
    }

    public NhanVien dangNhap(String tenDN, String matKhau) {
        TaiKhoan tk = taiKhoanDAO.dangNhap(tenDN, matKhau);
        if (tk != null) {
            currentAccount = tk;
            // Ghi nhật ký đăng nhập
            LogService.addLog("Đăng nhập", "Hệ thống", "Nhân viên " + tk.getNhanVien().getHoTen() + " (" + tenDN + ") đăng nhập thành công");
            
            // Khôi phục ca làm việc cũ (nếu có)
            this.currentShift = giaoCaDAO.findCurrentShift(tk.getNhanVien().getMaNhanVien());

            // Cập nhật lần đăng nhập cuối
            try {
                taiKhoanDAO.updateLanDangNhapCuoi(tenDN);
            } catch (Exception ignored) { /* không ảnh hưởng login */ }
            return tk.getNhanVien();
        }
        if (tk == null) {
            LogService.addLog("Đăng nhập thất bại", "Hệ thống", "Thử đăng nhập với tài khoản: " + tenDN);
        }
        return null;
    }

    /** Kiểm tra tài khoản bị khóa (credentials đúng nhưng trangThai = 0) */
    public boolean isAccountDisabled(String tenDN, String matKhau) {
        return taiKhoanDAO.isAccountDisabled(tenDN, matKhau);
    }

    public void dangXuat() { 
        if (currentAccount != null) {
            LogService.addLog("Đăng xuất", "Hệ thống", "Tài khoản " + currentAccount.getTenDangNhap() + " đăng xuất");
        }
        currentAccount = null; 
        currentShift = null;
    }

    public void setCurrentShift(entity.GiaoCa shift) { this.currentShift = shift; }
    public entity.GiaoCa getCurrentShift() { return currentShift; }

    public TaiKhoan getCurrentAccount() { return currentAccount; }

    public NhanVien getCurrentUser() { 
        return currentAccount != null ? currentAccount.getNhanVien() : null; 
    }

    public boolean isLoggedIn() { return currentAccount != null; }

    public boolean isAdmin() {
        return currentAccount != null && currentAccount.getVaiTro() == VaiTro.ADMIN;
    }

    public boolean isManager() {
        if (currentAccount == null) return false;
        VaiTro role = currentAccount.getVaiTro();
        return role == VaiTro.ADMIN || role == VaiTro.MANAGER;
    }

    public String getCurrentMaNV() {
        NhanVien nv = getCurrentUser();
        return nv != null ? nv.getMaNhanVien() : null;
    }

    /** Lấy vai trò hiện tại (null nếu chưa đăng nhập) */
    public VaiTro getCurrentRole() {
        return currentAccount != null ? currentAccount.getVaiTro() : null;
    }

    /**
     * Kiểm tra vai trò hiện tại có quyền truy cập (xem) module không.
     * ADMIN & MANAGER: xem tất cả.
     * RECEPTIONIST: chỉ nghiệp vụ cơ bản (tổng quan, đặt phòng, thuê phòng, hóa đơn, khách hàng).
     */
    public boolean hasAccess(String moduleKey) {
        if (currentAccount == null) return false;
        VaiTro role = currentAccount.getVaiTro();

        // ADMIN & MANAGER: full access
        if (role == VaiTro.ADMIN || role == VaiTro.MANAGER) return true;

        // RECEPTIONIST: chỉ 4 nghiệp vụ chính
        switch (moduleKey) {
            case "tongquan":
            case "kehoach":
            case "datphong":
            case "thuephong":
            case "hoadon":
                return true;
            default:
                return false; // khachhang, nhanvien, qlhethong, thongke → ẩn
        }
    }

    /**
     * Kiểm tra có quyền chỉnh sửa (Thêm/Sửa/Xóa) trong module.
     * - "nhanvien": chỉ ADMIN
     * - "qlhethong": ADMIN + MANAGER
     * - "thongke": không ai sửa (chỉ xem)
     * - Các module còn lại: ai truy cập được thì sửa được
     */
    public boolean canEdit(String moduleKey) {
        if (currentAccount == null) return false;
        VaiTro role = currentAccount.getVaiTro();

        if (role == VaiTro.ADMIN) return true;

        switch (moduleKey) {
            case "nhanvien":
                return false; // Chỉ ADMIN mới sửa nhân viên/tài khoản
            case "thongke":
                return false; // Không ai sửa báo cáo
            case "qlhethong":
                return role == VaiTro.MANAGER;
            default:
                return hasAccess(moduleKey);
        }
    }
}
