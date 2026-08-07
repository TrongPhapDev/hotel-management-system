package dao;

import database.DatabaseConnection;
import entity.TienNghi;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho bảng TienNghi + bảng trung gian TienNghi_LoaiPhong.
 */
public class TienNghiDAO {

    private static final Logger LOGGER = Logger.getLogger(TienNghiDAO.class.getName());

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ======================== CRUD TienNghi ========================

    public List<TienNghi> getAll() {
        List<TienNghi> list = new ArrayList<>();
        String sql = "SELECT * FROM TienNghi ORDER BY thuTu, tenTienNghi";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in TienNghiDAO", e); }
        return list;
    }

    public List<TienNghi> getByNhom(String nhom) {
        List<TienNghi> list = new ArrayList<>();
        String sql = "SELECT * FROM TienNghi WHERE nhomTienNghi=? ORDER BY thuTu";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, nhom);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in TienNghiDAO", e); }
        return list;
    }

    /** Lấy danh sách nhóm tiện nghi (distinct) */
    public List<String> getAllNhom() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT nhomTienNghi FROM TienNghi WHERE nhomTienNghi IS NOT NULL ORDER BY nhomTienNghi";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(rs.getString(1));
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in TienNghiDAO", e); }
        return list;
    }

    public TienNghi getById(String maTienNghi) {
        String sql = "SELECT * FROM TienNghi WHERE maTienNghi=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maTienNghi);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in TienNghiDAO", e); }
        return null;
    }

    public boolean insert(TienNghi tn) {
        String sql = "INSERT INTO TienNghi(maTienNghi, tenTienNghi, nhomTienNghi, icon, thuTu) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, tn.getMaTienNghi());
            ps.setString(2, tn.getTenTienNghi());
            ps.setString(3, tn.getNhomTienNghi());
            ps.setString(4, tn.getIcon());
            ps.setInt(5, tn.getThuTu());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in TienNghiDAO", e); return false; }
    }

    public boolean update(TienNghi tn) {
        String sql = "UPDATE TienNghi SET tenTienNghi=?, nhomTienNghi=?, icon=?, thuTu=? WHERE maTienNghi=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, tn.getTenTienNghi());
            ps.setString(2, tn.getNhomTienNghi());
            ps.setString(3, tn.getIcon());
            ps.setInt(4, tn.getThuTu());
            ps.setString(5, tn.getMaTienNghi());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in TienNghiDAO", e); return false; }
    }

    public boolean delete(String maTienNghi) {
        // Xóa khỏi bảng trung gian trước
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM TienNghi_LoaiPhong WHERE maTienNghi=?")) {
            ps.setString(1, maTienNghi);
            ps.executeUpdate();
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in TienNghiDAO", e); }
        // Rồi xóa tiện nghi
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM TienNghi WHERE maTienNghi=?")) {
            ps.setString(1, maTienNghi);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in TienNghiDAO", e); return false; }
    }

    public String generateMa() {
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT MAX(CAST(SUBSTRING(maTienNghi,3,10) AS INT)) FROM TienNghi WHERE maTienNghi LIKE 'TN%'")) {
            if (rs.next()) return String.format("TN%02d", rs.getInt(1) + 1);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in TienNghiDAO", e); }
        return "TN01";
    }

    // ======================== Many-to-Many: TienNghi_LoaiPhong ========================

    /** Lấy tiện nghi của 1 loại phòng */
    public List<TienNghi> getByLoaiPhong(String maLoaiPhong) {
        List<TienNghi> list = new ArrayList<>();
        String sql = "SELECT tn.* FROM TienNghi tn " +
                     "JOIN TienNghi_LoaiPhong tnlp ON tn.maTienNghi = tnlp.maTienNghi " +
                     "WHERE tnlp.maLoaiPhong=? ORDER BY tn.thuTu";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maLoaiPhong);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in TienNghiDAO", e); }
        return list;
    }

    /** Cập nhật toàn bộ tiện nghi cho 1 loại phòng (xóa cũ, thêm mới) */
    public void updateTienNghiForLoaiPhong(String maLoaiPhong, List<String> maTienNghiList) {
        // Xóa hết mapping cũ
        try (PreparedStatement ps = getConn().prepareStatement(
                "DELETE FROM TienNghi_LoaiPhong WHERE maLoaiPhong=?")) {
            ps.setString(1, maLoaiPhong);
            ps.executeUpdate();
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in TienNghiDAO", e); }

        // Thêm mapping mới
        if (maTienNghiList != null && !maTienNghiList.isEmpty()) {
            String sql = "INSERT INTO TienNghi_LoaiPhong(maLoaiPhong, maTienNghi) VALUES(?,?)";
            try (PreparedStatement ps = getConn().prepareStatement(sql)) {
                for (String maTN : maTienNghiList) {
                    ps.setString(1, maLoaiPhong);
                    ps.setString(2, maTN);
                    ps.addBatch();
                }
                ps.executeBatch();
            } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in TienNghiDAO", e); }
        }
    }

    /** Lấy chuỗi tiện nghi gộp lại cho hiển thị: "Wifi, Điều hòa, Tivi..." */
    public String getTienNghiString(String maLoaiPhong) {
        List<TienNghi> list = getByLoaiPhong(maLoaiPhong);
        if (list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(list.get(i).getTenTienNghi());
        }
        return sb.toString();
    }

    private TienNghi mapRow(ResultSet rs) throws SQLException {
        TienNghi tn = new TienNghi();
        tn.setMaTienNghi(rs.getString("maTienNghi"));
        tn.setTenTienNghi(rs.getString("tenTienNghi"));
        tn.setNhomTienNghi(rs.getString("nhomTienNghi"));
        try { tn.setIcon(rs.getString("icon")); } catch (Exception e) {}
        try { tn.setThuTu(rs.getInt("thuTu")); } catch (Exception e) {}
        return tn;
    }
}


