package dao;

import database.DatabaseConnection;
import entity.NhatKyHeThong;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LogDAO {
    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public boolean insert(NhatKyHeThong log) {
        String sql = "INSERT INTO NhatKyHeThong (tenDangNhap, hanhDong, doiTuong, chiTiet) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, log.getTenDangNhap());
            ps.setString(2, log.getHanhDong());
            ps.setString(3, log.getDoiTuong());
            ps.setString(4, log.getChiTiet());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<NhatKyHeThong> getAll() {
        return search(null, null);
    }

    public List<NhatKyHeThong> search(String keyword, String actionType) {
        List<NhatKyHeThong> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM NhatKyHeThong WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (tenDangNhap LIKE ? OR doiTuong LIKE ? OR chiTiet LIKE ?)");
            String k = "%" + keyword.trim() + "%";
            params.add(k); params.add(k); params.add(k);
        }

        if (actionType != null && !actionType.equals("Tất cả")) {
            sql.append(" AND hanhDong = ?");
            params.add(actionType);
        }

        sql.append(" ORDER BY thoiGian DESC");

        try (PreparedStatement ps = getConn().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                NhatKyHeThong log = new NhatKyHeThong();
                log.setMaLog(rs.getInt("maLog"));
                log.setThoiGian(rs.getTimestamp("thoiGian").toLocalDateTime());
                log.setTenDangNhap(rs.getString("tenDangNhap"));
                log.setHanhDong(rs.getString("hanhDong"));
                log.setDoiTuong(rs.getString("doiTuong"));
                log.setChiTiet(rs.getString("chiTiet"));
                list.add(log);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<String> getDistinctActions() {
        List<String> list = new ArrayList<>();
        list.add("Tất cả");
        String sql = "SELECT DISTINCT hanhDong FROM NhatKyHeThong ORDER BY hanhDong";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(rs.getString(1));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
}
