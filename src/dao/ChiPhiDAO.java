package dao;

import database.DatabaseConnection;
import entity.ChiPhi;
import entity.NhanVien;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChiPhiDAO {
    private static final Logger LOGGER = Logger.getLogger(ChiPhiDAO.class.getName());

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public boolean insert(ChiPhi cp) {
        String sql = "INSERT INTO ChiPhi (maNhanVien, soTien, lyDo, thoiGian, maGiaoCa) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, cp.getNhanVien().getMaNhanVien());
            ps.setDouble(2, cp.getSoTien());
            ps.setNString(3, cp.getLyDo());
            ps.setTimestamp(4, Timestamp.valueOf(cp.getThoiGian() != null ? cp.getThoiGian() : LocalDateTime.now()));
            ps.setString(5, cp.getMaGiaoCa());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi thêm ChiPhi", e);
            return false;
        }
    }

    public List<ChiPhi> getByShift(String maGiaoCa) {
        List<ChiPhi> list = new ArrayList<>();
        String sql = "SELECT cp.*, nv.hoTen FROM ChiPhi cp JOIN NhanVien nv ON cp.maNhanVien = nv.maNhanVien WHERE maGiaoCa = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maGiaoCa);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ChiPhi cp = new ChiPhi();
                cp.setMaChiPhi(rs.getInt("maChiPhi"));
                cp.setSoTien(rs.getDouble("soTien"));
                cp.setLyDo(rs.getNString("lyDo"));
                cp.setThoiGian(rs.getTimestamp("thoiGian").toLocalDateTime());
                cp.setMaGiaoCa(rs.getString("maGiaoCa"));
                
                NhanVien nv = new NhanVien();
                nv.setMaNhanVien(rs.getString("maNhanVien"));
                nv.setHoTen(rs.getNString("hoTen"));
                cp.setNhanVien(nv);
                
                list.add(cp);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy ChiPhi theo ca", e);
        }
        return list;
    }

    public double sumByShift(String maGiaoCa) {
        String sql = "SELECT SUM(soTien) FROM ChiPhi WHERE maGiaoCa = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maGiaoCa);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tính tổng ChiPhi theo ca", e);
        }
        return 0;
    }
}
