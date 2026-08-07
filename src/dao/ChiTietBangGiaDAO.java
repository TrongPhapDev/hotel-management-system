package dao;

import database.DatabaseConnection;
import entity.ChiTietBangGia;
import entity.LoaiPhong;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

public class ChiTietBangGiaDAO {

    private static final Logger LOGGER = Logger.getLogger(ChiTietBangGiaDAO.class.getName());

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<ChiTietBangGia> getByBangGia(String maBangGia) {
        List<ChiTietBangGia> list = new ArrayList<>();
        String sql = "SELECT ct.*, lp.tenLoaiPhong " +
                     "FROM ChiTietBangGia ct " +
                     "JOIN LoaiPhong lp ON ct.maLoaiPhong = lp.maLoaiPhong " +
                     "WHERE ct.maBangGia=? ORDER BY lp.tenLoaiPhong";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maBangGia);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in ChiTietBangGiaDAO", e); }
        return list;
    }

    /** Find rate for a specific loaiPhong in a specific bangGia */
    public ChiTietBangGia findByBangGiaAndLoaiPhong(String maBangGia, String maLoaiPhong) {
        String sql = "SELECT ct.*, lp.tenLoaiPhong " +
                     "FROM ChiTietBangGia ct " +
                     "JOIN LoaiPhong lp ON ct.maLoaiPhong = lp.maLoaiPhong " +
                     "WHERE ct.maBangGia=? AND ct.maLoaiPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maBangGia);
            ps.setString(2, maLoaiPhong);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in ChiTietBangGiaDAO", e); }
        return null;
    }

    public boolean insert(ChiTietBangGia ct) {
        String sql = "INSERT INTO ChiTietBangGia(maBangGia, maLoaiPhong, giaTheoNgay, giaTheoGio, phuPhiTraTre, giaCuoiTuan) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, ct.getBangGia().getMaBangGia());
            ps.setString(2, ct.getLoaiPhong().getMaLoaiPhong());
            ps.setDouble(3, ct.getGiaNgay());
            ps.setDouble(4, ct.getGiaGioDau());
            ps.setDouble(5, ct.getPhuPhiTraTre());
            ps.setDouble(6, ct.getGiaCuoiTuan());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Fallback without giaCuoiTuan
            String sqlFb = "INSERT INTO ChiTietBangGia(maBangGia, maLoaiPhong, giaTheoNgay, giaTheoGio, phuPhiTraTre) VALUES(?,?,?,?,?)";
            try (PreparedStatement ps2 = getConn().prepareStatement(sqlFb)) {
                ps2.setString(1, ct.getBangGia().getMaBangGia());
                ps2.setString(2, ct.getLoaiPhong().getMaLoaiPhong());
                ps2.setDouble(3, ct.getGiaNgay());
                ps2.setDouble(4, ct.getGiaGioDau());
                ps2.setDouble(5, ct.getPhuPhiTraTre());
                return ps2.executeUpdate() > 0;
            } catch (SQLException ex) { LOGGER.log(Level.SEVERE, "Database error in ChiTietBangGiaDAO", ex); return false; }
        }
    }

    public boolean update(ChiTietBangGia ct) {
        String sql = "UPDATE ChiTietBangGia SET giaTheoNgay=?, giaTheoGio=?, phuPhiTraTre=?, giaCuoiTuan=? WHERE maChiTietBangGia=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setDouble(1, ct.getGiaNgay());
            ps.setDouble(2, ct.getGiaGioDau());
            ps.setDouble(3, ct.getPhuPhiTraTre());
            ps.setDouble(4, ct.getGiaCuoiTuan());
            ps.setInt(5, ct.getMaChiTiet());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Fallback without giaCuoiTuan
            String sqlFb = "UPDATE ChiTietBangGia SET giaTheoNgay=?, giaTheoGio=?, phuPhiTraTre=? WHERE maChiTietBangGia=?";
            try (PreparedStatement ps2 = getConn().prepareStatement(sqlFb)) {
                ps2.setDouble(1, ct.getGiaNgay());
                ps2.setDouble(2, ct.getGiaGioDau());
                ps2.setDouble(3, ct.getPhuPhiTraTre());
                ps2.setInt(4, ct.getMaChiTiet());
                return ps2.executeUpdate() > 0;
            } catch (SQLException ex) { LOGGER.log(Level.SEVERE, "Database error in ChiTietBangGiaDAO", ex); return false; }
        }
    }

    public boolean deleteByBangGia(String maBangGia) {
        String sql = "DELETE FROM ChiTietBangGia WHERE maBangGia=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maBangGia);
            return ps.executeUpdate() >= 0; // 0 is ok if no rows
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in ChiTietBangGiaDAO", e); return false; }
    }

    private ChiTietBangGia mapRow(ResultSet rs) throws SQLException {
        ChiTietBangGia ct = new ChiTietBangGia();
        
        try { ct.setMaChiTiet(rs.getInt("maChiTietBangGia")); } catch (Exception e) {}
        
        LoaiPhong lp = new LoaiPhong();
        lp.setMaLoaiPhong(rs.getString("maLoaiPhong"));
        try { lp.setTenLoaiPhong(rs.getString("tenLoaiPhong")); } catch (Exception e) {}
        ct.setLoaiPhong(lp);
        
        try { ct.setGiaNgay(rs.getDouble("giaTheoNgay")); } catch (Exception e) {}
        try { ct.setGiaGioDau(rs.getDouble("giaTheoGio")); } catch (Exception e) {}
        try { ct.setPhuPhiTraTre(rs.getDouble("phuPhiTraTre")); } catch (Exception e) {}
        try { ct.setGiaCuoiTuan(rs.getDouble("giaCuoiTuan")); } catch (Exception e) {}
        
        return ct;
    }
}


