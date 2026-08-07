package dao;

import database.DatabaseConnection;
import entity.*;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

public class DichVuDAO {

    private static final Logger LOGGER = Logger.getLogger(DichVuDAO.class.getName());

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<DichVu> getAll() {
        List<DichVu> list = new ArrayList<>();
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM DichVu WHERE trangThai >= 0 ORDER BY tenDichVu")) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in DichVuDAO", e); }
        return list;
    }

    public DichVu getById(String maDichVu) {
        try (PreparedStatement ps = getConn().prepareStatement("SELECT * FROM DichVu WHERE maDichVu=?")) {
            ps.setString(1, maDichVu);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in DichVuDAO", e); }
        return null;
    }

    public boolean insert(DichVu dv) {
        String sql = "INSERT INTO DichVu(maDichVu, tenDichVu, loai, donGiaHienTai, donViTinh, soLuongMin, moTa, trangThai) VALUES(?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, dv.getMaDichVu());
            ps.setString(2, dv.getTenDichVu());
            ps.setString(3, dv.getLoai());
            ps.setDouble(4, dv.getDonGia());
            ps.setString(5, dv.getDonViTinh());
            ps.setInt(6, dv.getSoLuongMin());
            ps.setString(7, dv.getMoTa());
            ps.setInt(8, dv.getTrangThaiInt());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in DichVuDAO", e); return false; }
    }

    public boolean update(DichVu dv) {
        String sql = "UPDATE DichVu SET tenDichVu=?, loai=?, donGiaHienTai=?, donViTinh=?, soLuongMin=?, moTa=?, trangThai=? WHERE maDichVu=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, dv.getTenDichVu());
            ps.setString(2, dv.getLoai());
            ps.setDouble(3, dv.getDonGia());
            ps.setString(4, dv.getDonViTinh());
            ps.setInt(5, dv.getSoLuongMin());
            ps.setString(6, dv.getMoTa());
            ps.setInt(7, dv.getTrangThaiInt());
            ps.setString(8, dv.getMaDichVu());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in DichVuDAO", e); return false; }
    }

    public boolean delete(String maDichVu) {
        try (PreparedStatement ps = getConn().prepareStatement("UPDATE DichVu SET trangThai = -1 WHERE maDichVu=?")) {
            ps.setString(1, maDichVu);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in DichVuDAO", e); return false; }
    }

    private DichVu mapRow(ResultSet rs) throws SQLException {
        DichVu dv = new DichVu();
        dv.setMaDichVu(rs.getString("maDichVu"));
        dv.setTenDichVu(rs.getString("tenDichVu"));
        dv.setLoai(rs.getString("loai"));
        dv.setDonGia(rs.getDouble("donGiaHienTai"));
        dv.setDonViTinh(rs.getString("donViTinh"));
        dv.setSoLuongMin(rs.getInt("soLuongMin"));
        dv.setMoTa(rs.getString("moTa"));
        dv.setTrangThaiInt(rs.getInt("trangThai"));
        return dv;
    }

    public List<DichVu> getActive() { return getAll(); } // Placeholder if no active flag

    public List<DichVu> search(String kw, String type, String status) {
        List<DichVu> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT * FROM DichVu WHERE trangThai >= 0 ");
        if (kw != null && !kw.isEmpty()) sb.append("AND tenDichVu LIKE ? ");
        if (type != null && !type.equals("Tất cả loại")) sb.append("AND loai = ? ");
        if (status != null && !status.equals("Tất cả")) {
            sb.append("AND (CASE WHEN trangThai=1 THEN N'Hoạt động' ELSE N'Tạm ngừng' END) = ? ");
        }

        try (PreparedStatement ps = getConn().prepareStatement(sb.toString())) {
            int idx = 1;
            if (kw != null && !kw.isEmpty()) ps.setString(idx++, "%" + kw + "%");
            if (type != null && !type.equals("Tất cả loại")) ps.setString(idx++, type);
            if (status != null && !status.equals("Tất cả")) {
                ps.setString(idx++, status);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in DichVuDAO", e); }
        return list;
    }

    public int countAll() {
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM DichVu WHERE trangThai >= 0")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in DichVuDAO", e); }
        return 0;
    }

    public String generateMa() {
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT MAX(CAST(SUBSTRING(maDichVu,3,10) AS INT)) FROM DichVu WHERE maDichVu LIKE 'DV%'")) {
            if (rs.next()) return String.format("DV%03d", rs.getInt(1) + 1);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in DichVuDAO", e); }
        return "DV001";
    }

    public int countActive() { 
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM DichVu WHERE trangThai = 1")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in DichVuDAO", e); }
        return 0;
    }

    public int countSuspended() { 
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM DichVu WHERE trangThai = 0")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in DichVuDAO", e); }
        return 0;
    }

    public double getGiaTrungBinh() {
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT AVG(donGiaHienTai) FROM DichVu WHERE trangThai >= 0")) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in DichVuDAO", e); }
        return 0;
    }
}


