package service;

import dao.LogDAO;
import entity.NhatKyHeThong;
import java.util.List;

public class LogService {
    private static final LogDAO dao = new LogDAO();

    /**
     * Ghi nhật ký hệ thống.
     * @param hanhDong Ví dụ: LOGIN, LOGOUT, DELETE_ROOM, ...
     * @param doiTuong Ví dụ: "Phòng P101", "Hóa đơn HD001", ...
     * @param chiTiet Mô tả chi tiết hành động
     */
    public static void addLog(String hanhDong, String doiTuong, String chiTiet) {
        String user = "system";
        try {
            if (AuthService.getInstance().isLoggedIn()) {
                user = AuthService.getInstance().getCurrentAccount().getTenDangNhap();
            }
        } catch (Exception ignored) {}

        NhatKyHeThong log = new NhatKyHeThong(user, hanhDong, doiTuong, chiTiet);
        dao.insert(log);
    }

    public List<NhatKyHeThong> getAllLogs() {
        return dao.getAll();
    }

    public List<NhatKyHeThong> searchLogs(String keyword, String actionType) {
        return dao.search(keyword, actionType);
    }

    public List<String> getActionTypes() {
        return dao.getDistinctActions();
    }
}
