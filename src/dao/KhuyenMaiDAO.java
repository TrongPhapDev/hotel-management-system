package dao;

import database.DatabaseConnection;
import entity.KhuyenMai;
import entity.enums.LoaiGiam;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

public class KhuyenMaiDAO {

    private static final Logger LOGGER = Logger.getLogger(KhuyenMaiDAO.class.getName());

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<KhuyenMai> getAll() {
        return search(null, null);
    }

    public List<KhuyenMai> search(String keyword, String status) {
        List<KhuyenMai> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM KhuyenMai WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (maKM LIKE ? OR tenKM LIKE ?)");
            String k = "%" + keyword.trim() + "%";
            params.add(k); params.add(k);
        }

        if (status != null && !status.equals("Tất cả")) {
            if ("Hoạt động".equals(status)) {
                sql.append(" AND trangThai = 1 AND ngayBatDau <= GETDATE() AND ngayKetThuc >= GETDATE()");
            } else if ("Hết hạn".equals(status)) {
                sql.append(" AND ngayKetThuc < GETDATE()");
            } else if ("Tạm dừng".equals(status)) {
                sql.append(" AND trangThai = 0");
            }
        }

        sql.append(" ORDER BY ngayBatDau DESC");

        try (PreparedStatement ps = getConn().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Search error", e);
        }
        return list;
    }

    public KhuyenMai getByVoucherCode(String code) {
        String sql = "SELECT * FROM KhuyenMai WHERE maKM=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error", e); }
        return null;
    }

    public boolean insert(KhuyenMai km) {
        String sql = "INSERT INTO KhuyenMai(maKM, tenKM, loaiGiam, giaTriGiam, ngayBatDau, ngayKetThuc, trangThai, soLuong, daDung, dieuKienToiThieu, giaTriGiamToiDa) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, km.getMaKhuyenMai());
            ps.setString(2, km.getTenKhuyenMai());
            ps.setString(3, km.getLoaiGiam().name());
            ps.setDouble(4, km.getGiaTriGiam());
            ps.setTimestamp(5, Timestamp.valueOf(km.getNgayBatDau()));
            ps.setTimestamp(6, Timestamp.valueOf(km.getNgayKetThuc()));
            ps.setBoolean(7, km.isTrangThai());
            ps.setInt(8, km.getSoLuong());
            ps.setInt(9, km.getDaDung());
            ps.setDouble(10, km.getDieuKienToiThieu());
            ps.setDouble(11, km.getGiaTriGiamToiDa());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Insert error", e); return false; }
    }

    public boolean update(KhuyenMai km) {
        String sql = "UPDATE KhuyenMai SET tenKM=?, loaiGiam=?, giaTriGiam=?, ngayBatDau=?, ngayKetThuc=?, trangThai=?, soLuong=?, daDung=?, dieuKienToiThieu=?, giaTriGiamToiDa=? WHERE maKM=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, km.getTenKhuyenMai());
            ps.setString(2, km.getLoaiGiam().name());
            ps.setDouble(3, km.getGiaTriGiam());
            ps.setTimestamp(4, Timestamp.valueOf(km.getNgayBatDau()));
            ps.setTimestamp(5, Timestamp.valueOf(km.getNgayKetThuc()));
            ps.setBoolean(6, km.isTrangThai());
            ps.setInt(7, km.getSoLuong());
            ps.setInt(8, km.getDaDung());
            ps.setDouble(9, km.getDieuKienToiThieu());
            ps.setDouble(10, km.getGiaTriGiamToiDa());
            ps.setString(11, km.getMaKhuyenMai());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Update error", e); return false; }
    }

    public boolean delete(String maKM) {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM KhuyenMai WHERE maKM=?")) {
            ps.setString(1, maKM);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Delete error", e); return false; }
    }

    private KhuyenMai mapRow(ResultSet rs) throws SQLException {
        KhuyenMai km = new KhuyenMai();
        km.setMaKhuyenMai(rs.getString("maKM"));
        km.setTenKM(rs.getString("tenKM"));
        
        String loaiStr = rs.getString("loaiGiam");
        if (loaiStr != null) {
            try {
                km.setLoaiGiam(LoaiGiam.valueOf(loaiStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                km.setLoaiGiam(LoaiGiam.PERCENT);
            }
        }
        
        km.setGiaTriGiam(rs.getDouble("giaTriGiam"));
        km.setNgayBatDau(rs.getTimestamp("ngayBatDau").toLocalDateTime());
        km.setNgayKetThuc(rs.getTimestamp("ngayKetThuc").toLocalDateTime());
        
        km.setTrangThai(rs.getBoolean("trangThai"));
        km.setSoLuong(rs.getInt("soLuong"));
        km.setDaDung(rs.getInt("daDung"));
        km.setDieuKienToiThieu(rs.getDouble("dieuKienToiThieu"));
        try {
            km.setGiaTriGiamToiDa(rs.getDouble("giaTriGiamToiDa"));
        } catch (SQLException ignored) {
            km.setGiaTriGiamToiDa(0);
        }
        
        return km;
    }
}


