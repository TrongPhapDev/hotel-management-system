package dao;

import database.DatabaseConnection;
import entity.LoaiPhong;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

public class LoaiPhongDAO {

    private static final Logger LOGGER = Logger.getLogger(LoaiPhongDAO.class.getName());

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<LoaiPhong> getAll() {
        List<LoaiPhong> list = new ArrayList<>();
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM LoaiPhong WHERE trangThai=1 ORDER BY tenLoaiPhong")) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in LoaiPhongDAO", e); }
        return list;
    }

    public List<LoaiPhong> getActive() {
        return getAll(); // getAll already filters by trangThai=1
    }

    public LoaiPhong getById(String maLoaiPhong) {
        try (PreparedStatement ps = getConn().prepareStatement("SELECT * FROM LoaiPhong WHERE maLoaiPhong=?")) {
            ps.setString(1, maLoaiPhong);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in LoaiPhongDAO", e); }
        return null;
    }

    public boolean insert(LoaiPhong lp) {
        String sql = "INSERT INTO LoaiPhong(maLoaiPhong, tenLoaiPhong, soNguoiToiDa, moTa, giaTheoNgay, trangThai) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, lp.getMaLoaiPhong());
            ps.setString(2, lp.getTenLoaiPhong());
            ps.setInt(3, lp.getSucChua());
            ps.setString(4, lp.getMoTa());
            ps.setDouble(5, lp.getGiaTheoNgay());
            ps.setBoolean(6, lp.isDangKinhDoanh());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in LoaiPhongDAO", e); return false; }
    }

    public boolean update(LoaiPhong lp) {
        String sql = "UPDATE LoaiPhong SET tenLoaiPhong=?, soNguoiToiDa=?, moTa=?, giaTheoNgay=?, trangThai=? WHERE maLoaiPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, lp.getTenLoaiPhong());
            ps.setInt(2, lp.getSucChua());
            ps.setString(3, lp.getMoTa());
            ps.setDouble(4, lp.getGiaTheoNgay());
            ps.setBoolean(5, lp.isDangKinhDoanh());
            ps.setString(6, lp.getMaLoaiPhong());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in LoaiPhongDAO", e); return false; }
    }

    public boolean delete(String maLoaiPhong) {
        try (PreparedStatement ps = getConn().prepareStatement("UPDATE LoaiPhong SET trangThai=0 WHERE maLoaiPhong=?")) {
            ps.setString(1, maLoaiPhong);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in LoaiPhongDAO", e); return false; }
    }

    /** Check if any phong references this loaiPhong */
    public int countPhongByLoai(String maLoaiPhong) {
        String sql = "SELECT COUNT(*) FROM Phong WHERE maLoaiPhong=? AND trangThai <> 'DELETED'";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maLoaiPhong);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in LoaiPhongDAO", e); }
        return 0;
    }

    /** Generate next maLoaiPhong like LP006, LP007... */
    public String generateMa() {
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT MAX(CAST(SUBSTRING(maLoaiPhong,3,10) AS INT)) FROM LoaiPhong WHERE maLoaiPhong LIKE 'LP%'")) {
            if (rs.next()) return String.format("LP%03d", rs.getInt(1) + 1);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in LoaiPhongDAO", e); }
        return "LP001";
    }

    private LoaiPhong mapRow(ResultSet rs) throws SQLException {
        LoaiPhong lp = new LoaiPhong();
        lp.setMaLoaiPhong(rs.getString("maLoaiPhong"));
        lp.setTenLoaiPhong(rs.getString("tenLoaiPhong"));
        lp.setSucChua(rs.getInt("soNguoiToiDa"));
        lp.setMoTa(rs.getString("moTa"));
        try { lp.setGiaTheoNgay(rs.getDouble("giaTheoNgay")); } catch (Exception e) {}
        try { lp.setDangKinhDoanh(rs.getBoolean("trangThai")); } catch (Exception e) { lp.setDangKinhDoanh(true); }
        return lp;
    }
}


