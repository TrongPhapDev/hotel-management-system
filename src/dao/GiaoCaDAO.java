package dao;

import database.DatabaseConnection;
import entity.GiaoCa;
import entity.NhanVien;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GiaoCaDAO {
    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public String generateMaGiaoCa() {
        String sql = "SELECT TOP 1 maGiaoCa FROM GiaoCa ORDER BY maGiaoCa DESC";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                String last = rs.getString(1);
                int num = Integer.parseInt(last.substring(2)) + 1;
                return String.format("GC%05d", num);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "GC00001";
    }

    public boolean insert(GiaoCa gc) {
        String sql = "INSERT INTO GiaoCa (maGiaoCa, maNhanVien, thoiGianBatDau, tienMatDauCa, trangThai) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, gc.getMaGiaoCa());
            ps.setString(2, gc.getNhanVien().getMaNhanVien());
            ps.setTimestamp(3, Timestamp.valueOf(gc.getThoiGianBatDau()));
            ps.setDouble(4, gc.getTienMatDauCa());
            ps.setString(5, "OPEN");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean closeShift(GiaoCa gc) {
        String sql = "UPDATE GiaoCa SET thoiGianKetThuc=?, tienMatThuTrongCa=?, tienMatBanGiao=?, tienMatChenhLech=?, maNhanVienNhan=?, ghiChu=?, trangThai='CLOSED' WHERE maGiaoCa=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(gc.getThoiGianKetThuc()));
            ps.setDouble(2, gc.getTienMatThuTrongCa());
            ps.setDouble(3, gc.getTienMatBanGiao());
            ps.setDouble(4, gc.getTienMatChenhLech());
            ps.setString(5, gc.getMaNhanVienNhan());
            ps.setString(6, gc.getGhiChu());
            ps.setString(7, gc.getMaGiaoCa());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public GiaoCa findCurrentShift(String maNV) {
        String sql = "SELECT * FROM GiaoCa WHERE maNhanVien=? AND trangThai='OPEN'";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maNV);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public double calculateRevenue(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT SUM(tongThanhToan) FROM HoaDon WHERE ngayLap >= ? AND ngayLap <= ? AND trangThai='PAID'";
        return queryAmount(start, end, sql);
    }

    public double calculateCashRevenue(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT SUM(tongThanhToan) FROM HoaDon WHERE ngayLap >= ? AND ngayLap <= ? AND trangThai='PAID' AND phuongThucThanhToan='CASH'";
        return queryAmount(start, end, sql);
    }

    private double queryAmount(LocalDateTime start, LocalDateTime end, String sql) {
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(start));
            ps.setTimestamp(2, Timestamp.valueOf(end != null ? end : LocalDateTime.now()));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<GiaoCa> getAll() {
        return search(null, null);
    }

    public List<GiaoCa> search(String staffName, String status) {
        List<GiaoCa> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT gc.*, nv.hoTen FROM GiaoCa gc " +
            "JOIN NhanVien nv ON gc.maNhanVien = nv.maNhanVien " +
            "WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (staffName != null && !staffName.trim().isEmpty()) {
            sql.append(" AND (nv.hoTen LIKE ? OR gc.maGiaoCa LIKE ?)");
            String k = "%" + staffName.trim() + "%";
            params.add(k); params.add(k);
        }

        if (status != null && !status.equals("Tất cả")) {
            sql.append(" AND gc.trangThai = ?");
            params.add(status);
        }

        sql.append(" ORDER BY gc.thoiGianBatDau DESC");

        try (PreparedStatement ps = getConn().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private GiaoCa mapRow(ResultSet rs) throws SQLException {
        GiaoCa gc = new GiaoCa();
        gc.setMaGiaoCa(rs.getString("maGiaoCa"));
        
        NhanVien nv = new NhanVien();
        nv.setMaNhanVien(rs.getString("maNhanVien"));
        try { nv.setHoTen(rs.getString("hoTen")); } catch (Exception ignored) {}
        gc.setNhanVien(nv);
        
        gc.setThoiGianBatDau(rs.getTimestamp("thoiGianBatDau").toLocalDateTime());
        Timestamp end = rs.getTimestamp("thoiGianKetThuc");
        if (end != null) gc.setThoiGianKetThuc(end.toLocalDateTime());
        
        gc.setTienMatDauCa(rs.getDouble("tienMatDauCa"));
        gc.setTienMatThuTrongCa(rs.getDouble("tienMatThuTrongCa"));
        gc.setTienMatBanGiao(rs.getDouble("tienMatBanGiao"));
        gc.setTienMatChenhLech(rs.getDouble("tienMatChenhLech"));
        gc.setMaNhanVienNhan(rs.getString("maNhanVienNhan"));
        gc.setGhiChu(rs.getString("ghiChu"));
        gc.setTrangThai(rs.getString("trangThai"));
        return gc;
    }

    public boolean saveDenominations(String maGiaoCa, java.util.Map<Integer, Integer> map) {
        String sql = "INSERT INTO ChiTietKiemTien (maGiaoCa, menhGia, soLuong) VALUES (?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            for (java.util.Map.Entry<Integer, Integer> entry : map.entrySet()) {
                ps.setString(1, maGiaoCa);
                ps.setInt(2, entry.getKey());
                ps.setInt(3, entry.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public java.util.Map<Integer, Integer> getDenominations(String maGiaoCa) {
        java.util.Map<Integer, Integer> map = new java.util.TreeMap<>(java.util.Collections.reverseOrder());
        String sql = "SELECT menhGia, soLuong FROM ChiTietKiemTien WHERE maGiaoCa = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maGiaoCa);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                map.put(rs.getInt("menhGia"), rs.getInt("soLuong"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }
}
