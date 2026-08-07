package dao;

import database.DatabaseConnection;
import entity.KenhDatPhong;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO cho bảng KenhDatPhong (Booking Channel).
 */
public class KenhDatPhongDAO {

    private static final Logger LOGGER = Logger.getLogger(KenhDatPhongDAO.class.getName());

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<KenhDatPhong> getAll() {
        List<KenhDatPhong> list = new ArrayList<>();
        String sql = "SELECT * FROM KenhDatPhong WHERE trangThai = 1 ORDER BY loaiKenh, tenKenh";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Lỗi lấy danh sách kênh đặt phòng", e); 
        }
        return list;
    }

    public List<KenhDatPhong> getAllIncludeInactive() {
        List<KenhDatPhong> list = new ArrayList<>();
        String sql = "SELECT * FROM KenhDatPhong ORDER BY loaiKenh, tenKenh";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Lỗi lấy danh sách kênh đặt phòng", e); 
        }
        return list;
    }

    public KenhDatPhong getById(String maKenh) {
        if (maKenh == null || maKenh.isEmpty()) return null;
        String sql = "SELECT * FROM KenhDatPhong WHERE maKenh=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maKenh);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Lỗi lấy kênh đặt phòng theo mã", e); 
        }
        return null;
    }

    public boolean insert(KenhDatPhong kenh) {
        String sql = "INSERT INTO KenhDatPhong(maKenh, tenKenh, loaiKenh, heSoHoaHong, trangThai, moTa) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, kenh.getMaKenh());
            ps.setString(2, kenh.getTenKenh());
            ps.setString(3, kenh.getLoaiKenh());
            ps.setDouble(4, kenh.getHeSoHoaHong());
            ps.setBoolean(5, kenh.isTrangThai());
            ps.setString(6, kenh.getMoTa());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Lỗi thêm kênh đặt phòng", e); 
            return false; 
        }
    }

    public boolean update(KenhDatPhong kenh) {
        String sql = "UPDATE KenhDatPhong SET tenKenh=?, loaiKenh=?, heSoHoaHong=?, trangThai=?, moTa=? WHERE maKenh=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, kenh.getTenKenh());
            ps.setString(2, kenh.getLoaiKenh());
            ps.setDouble(3, kenh.getHeSoHoaHong());
            ps.setBoolean(4, kenh.isTrangThai());
            ps.setString(5, kenh.getMoTa());
            ps.setString(6, kenh.getMaKenh());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Lỗi cập nhật kênh đặt phòng", e); 
            return false; 
        }
    }

    /** Lấy danh sách kênh theo loại (DIRECT, OTA, CORPORATE...) */
    public List<KenhDatPhong> getByLoai(String loaiKenh) {
        List<KenhDatPhong> list = new ArrayList<>();
        String sql = "SELECT * FROM KenhDatPhong WHERE loaiKenh=? AND trangThai=1 ORDER BY tenKenh";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, loaiKenh);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Lỗi lấy kênh theo loại", e); 
        }
        return list;
    }

    private KenhDatPhong mapRow(ResultSet rs) throws SQLException {
        KenhDatPhong k = new KenhDatPhong();
        k.setMaKenh(rs.getString("maKenh"));
        k.setTenKenh(rs.getString("tenKenh"));
        k.setLoaiKenh(rs.getString("loaiKenh"));
        k.setHeSoHoaHong(rs.getDouble("heSoHoaHong"));
        k.setTrangThai(rs.getBoolean("trangThai"));
        k.setMoTa(rs.getString("moTa"));
        return k;
    }
}
