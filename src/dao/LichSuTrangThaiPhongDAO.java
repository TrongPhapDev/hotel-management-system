package dao;

import database.DatabaseConnection;
import entity.LichSuTrangThaiPhong;
import entity.Phong;
import entity.enums.TrangThaiPhong;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LichSuTrangThaiPhongDAO {

    private static final Logger LOGGER = Logger.getLogger(LichSuTrangThaiPhongDAO.class.getName());

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<LichSuTrangThaiPhong> getByPhong(String maPhong) {
        List<LichSuTrangThaiPhong> list = new ArrayList<>();
        String sql = "SELECT * FROM LICH_SU_TRANG_THAI_PHONG WHERE maPhong=? ORDER BY thoiGianChuyen DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maPhong);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in LichSuTrangThaiPhongDAO", e); }
        return list;
    }

    public boolean insert(LichSuTrangThaiPhong ls) {
        String sql = "INSERT INTO LICH_SU_TRANG_THAI_PHONG(maPhong, trangThaiCu, trangThaiMoi, thoiGianChuyen, lyDo) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, ls.getPhong().getMaPhong());
            ps.setString(2, ls.getTrangThaiCu() != null ? ls.getTrangThaiCu().name() : null);
            ps.setString(3, ls.getTrangThaiMoi() != null ? ls.getTrangThaiMoi().name() : null);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(5, ls.getLyDo());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in LichSuTrangThaiPhongDAO", e); return false; }
    }

    private LichSuTrangThaiPhong mapRow(ResultSet rs) throws SQLException {
        LichSuTrangThaiPhong ls = new LichSuTrangThaiPhong();
        
        Phong p = new Phong();
        p.setMaPhong(rs.getString("maPhong"));
        ls.setPhong(p);
        
        String oldStatus = rs.getString("trangThaiCu");
        if (oldStatus != null) {
            try {
                ls.setTrangThaiCu(TrangThaiPhong.valueOf(oldStatus.toUpperCase()));
            } catch (IllegalArgumentException e) {
                ls.setTrangThaiCu(TrangThaiPhong.AVAILABLE);
            }
        }
        
        String newStatus = rs.getString("trangThaiMoi");
        if (newStatus != null) {
            try {
                ls.setTrangThaiMoi(TrangThaiPhong.valueOf(newStatus.toUpperCase()));
            } catch (IllegalArgumentException e) {
                ls.setTrangThaiMoi(TrangThaiPhong.AVAILABLE);
            }
        }
        
        Timestamp time = rs.getTimestamp("thoiGianChuyen");
        if (time != null) ls.setThoiGianChuyen(time.toLocalDateTime());
        
        ls.setLyDo(rs.getString("lyDo"));
        
        return ls;
    }
}


