package dao;

import database.DatabaseConnection;
import entity.KhachHang;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

public class KhachHangDAO {

    private static final Logger LOGGER = Logger.getLogger(KhachHangDAO.class.getName());

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<KhachHang> getAll() {
        List<KhachHang> list = new ArrayList<>();
        String sql = "SELECT * FROM KhachHang WHERE trangThai=1 ORDER BY hoTen";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in KhachHangDAO", e); }
        return list;
    }

    public List<KhachHang> timKiem(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return getAll();
        
        List<KhachHang> list = new ArrayList<>();
        String sql = "SELECT * FROM KhachHang WHERE (hoTen LIKE ? OR sdt LIKE ? OR cccd LIKE ? OR soHoChieu LIKE ?) AND trangThai=1";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            String kw = "%" + keyword.trim() + "%";
            ps.setString(1, kw);
            ps.setString(2, kw);
            ps.setString(3, kw);
            ps.setString(4, kw);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { 
            // Fallback without soHoChieu
            list.clear();
            String sqlFallback = "SELECT * FROM KhachHang WHERE (hoTen LIKE ? OR sdt LIKE ? OR cccd LIKE ?) AND trangThai=1";
            try (PreparedStatement ps = getConn().prepareStatement(sqlFallback)) {
                String kw = "%" + keyword.trim() + "%";
                ps.setString(1, kw); ps.setString(2, kw); ps.setString(3, kw);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) list.add(mapRow(rs));
            } catch (SQLException ex) { LOGGER.log(Level.SEVERE, "Database error in KhachHangDAO", ex); }
        }
        return list;
    }

    public KhachHang getById(String maKhachHang) {
        String sql = "SELECT * FROM KhachHang WHERE maKhachHang=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maKhachHang);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in KhachHangDAO", e); }
        return null;
    }

    public KhachHang getByPhone(String sdt) {
        String sql = "SELECT * FROM KhachHang WHERE sdt=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, sdt);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in KhachHangDAO", e); }
        return null;
    }

    public KhachHang getByCCCD(String cccd) {
        String sql = "SELECT * FROM KhachHang WHERE cccd=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, cccd);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in KhachHangDAO", e); }
        return null;
    }

    /** Tìm khách hàng theo số hộ chiếu */
    public KhachHang getByPassport(String soHoChieu) {
        String sql = "SELECT * FROM KhachHang WHERE soHoChieu=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, soHoChieu);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in KhachHangDAO", e); }
        return null;
    }

    public boolean insert(KhachHang kh) {
        String sql = "INSERT INTO KhachHang(maKhachHang, hoTen, sdt, cccd, ngaySinh, gioiTinh, quocTich, loaiGiayTo, soHoChieu, soVisa, ngayHetHanVisa, noiCapHoChieu, ngayNhapCanh, trangThai, anhCCCD, soLanO, tongChiTieu, diemTichLuy, hangKhachHang, isBlacklist, preferences, vipLevel) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, kh.getMaKhachHang());
            ps.setString(2, kh.getHoTen());
            ps.setString(3, kh.getSdt());
            ps.setString(4, kh.getCccd());
            ps.setDate(5, kh.getNgaySinh() != null ? java.sql.Date.valueOf(kh.getNgaySinh()) : null);
            ps.setString(6, kh.getGioiTinh());
            ps.setString(7, kh.getQuocTich());
            ps.setString(8, kh.getLoaiGiayTo() != null ? kh.getLoaiGiayTo() : "CCCD");
            ps.setString(9, kh.getSoHoChieu());
            ps.setString(10, kh.getSoVisa());
            ps.setDate(11, kh.getNgayHetHanVisa() != null ? java.sql.Date.valueOf(kh.getNgayHetHanVisa()) : null);
            ps.setString(12, kh.getNoiCapHoChieu());
            ps.setDate(13, kh.getNgayNhapCanh() != null ? java.sql.Date.valueOf(kh.getNgayNhapCanh()) : null);
            ps.setBoolean(14, !"Ngừng".equals(kh.getTrangThai()));
            ps.setString(15, kh.getAnhCCCD());
            ps.setInt(16, kh.getSoLanO());
            ps.setDouble(17, kh.getTongChiTieu());
            ps.setInt(18, kh.getDiemTichLuy());
            ps.setString(19, kh.getHangKhachHang());
            ps.setBoolean(20, kh.isBlacklist());
            ps.setString(21, kh.getPreferences());
            ps.setString(22, kh.getVipLevel());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { 
            // Attempt to update only primary columns if CRM columns aren't ready
            return insertLegacy(kh);
        }
    }

    private boolean insertLegacy(KhachHang kh) {
        String sql = "INSERT INTO KhachHang(maKhachHang, hoTen, sdt, cccd, ngaySinh, gioiTinh, quocTich, loaiGiayTo, soHoChieu, trangThai) VALUES(?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, kh.getMaKhachHang());
            ps.setString(2, kh.getHoTen());
            ps.setString(3, kh.getSdt());
            ps.setString(4, kh.getCccd());
            ps.setDate(5, kh.getNgaySinh() != null ? java.sql.Date.valueOf(kh.getNgaySinh()) : null);
            ps.setString(6, kh.getGioiTinh());
            ps.setString(7, kh.getQuocTich());
            ps.setString(8, kh.getLoaiGiayTo());
            ps.setString(9, kh.getSoHoChieu());
            ps.setBoolean(10, true);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { LOGGER.log(Level.SEVERE, "Insert failed", ex); return false; }
    }

    public boolean update(KhachHang kh) {
        String sql = "UPDATE KhachHang SET hoTen=?, sdt=?, cccd=?, ngaySinh=?, gioiTinh=?, quocTich=?, loaiGiayTo=?, soHoChieu=?, soVisa=?, ngayHetHanVisa=?, noiCapHoChieu=?, ngayNhapCanh=?, trangThai=?, anhCCCD=?, soLanO=?, tongChiTieu=?, diemTichLuy=?, hangKhachHang=?, isBlacklist=?, preferences=?, vipLevel=? WHERE maKhachHang=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, kh.getHoTen());
            ps.setString(2, kh.getSdt());
            ps.setString(3, kh.getCccd());
            ps.setDate(4, kh.getNgaySinh() != null ? java.sql.Date.valueOf(kh.getNgaySinh()) : null);
            ps.setString(5, kh.getGioiTinh());
            ps.setString(6, kh.getQuocTich());
            ps.setString(7, kh.getLoaiGiayTo() != null ? kh.getLoaiGiayTo() : "CCCD");
            ps.setString(8, kh.getSoHoChieu());
            ps.setString(9, kh.getSoVisa());
            ps.setDate(10, kh.getNgayHetHanVisa() != null ? java.sql.Date.valueOf(kh.getNgayHetHanVisa()) : null);
            ps.setString(11, kh.getNoiCapHoChieu());
            ps.setDate(12, kh.getNgayNhapCanh() != null ? java.sql.Date.valueOf(kh.getNgayNhapCanh()) : null);
            ps.setBoolean(13, !"Ngừng".equals(kh.getTrangThai()));
            ps.setString(14, kh.getAnhCCCD());
            ps.setInt(15, kh.getSoLanO());
            ps.setDouble(16, kh.getTongChiTieu());
            ps.setInt(17, kh.getDiemTichLuy());
            ps.setString(18, kh.getHangKhachHang());
            ps.setBoolean(19, kh.isBlacklist());
            ps.setString(20, kh.getPreferences());
            ps.setString(21, kh.getVipLevel());
            ps.setString(22, kh.getMaKhachHang());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { 
            // Fallback
            return updateLegacy(kh);
        }
    }

    private boolean updateLegacy(KhachHang kh) {
        String sql = "UPDATE KhachHang SET hoTen=?, sdt=?, cccd=? WHERE maKhachHang=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, kh.getHoTen());
            ps.setString(2, kh.getSdt());
            ps.setString(3, kh.getCccd());
            ps.setString(4, kh.getMaKhachHang());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { LOGGER.log(Level.SEVERE, "Update failed", ex); return false; }
    }

    public String generateMaKH() {
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT MAX(CAST(SUBSTRING(maKhachHang,3,10) AS INT)) FROM KhachHang WHERE maKhachHang LIKE 'KH%'")) {
            if (rs.next()) return String.format("KH%03d", rs.getInt(1) + 1);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in KhachHangDAO", e); }
        return "KH001";
    }

    public boolean delete(String maKhachHang) {
        String sql = "UPDATE KhachHang SET trangThai=0 WHERE maKhachHang=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maKhachHang);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in KhachHangDAO", e); return false; }
    }

    /** Lấy danh sách khách nước ngoài */
    public List<KhachHang> getKhachNuocNgoai() {
        List<KhachHang> list = new ArrayList<>();
        String sql = "SELECT * FROM KhachHang WHERE quocTich IS NOT NULL AND quocTich <> N'Việt Nam' AND quocTich <> 'VN' AND trangThai=1 ORDER BY hoTen";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in KhachHangDAO", e); }
        return list;
    }

    private KhachHang mapRow(ResultSet rs) throws SQLException {
        KhachHang kh = new KhachHang();
        kh.setMaKhachHang(rs.getString("maKhachHang"));
        kh.setHoTen(rs.getString("hoTen"));
        kh.setSdt(rs.getString("sdt"));
        kh.setCccd(rs.getString("cccd"));
        try {
            Date ns = rs.getDate("ngaySinh");
            if (ns != null) kh.setNgaySinh(ns.toLocalDate());
            kh.setGioiTinh(rs.getString("gioiTinh"));
            kh.setQuocTich(rs.getString("quocTich"));
        } catch (SQLException ignored) {
            // Ignore for backwards compatibility with old schema
        }
        // Passport / Visa fields (safe read)
        try {
            kh.setLoaiGiayTo(rs.getString("loaiGiayTo"));
        } catch (SQLException ignored) { }
        try {
            kh.setSoHoChieu(rs.getString("soHoChieu"));
        } catch (SQLException ignored) { }
        try {
            kh.setSoVisa(rs.getString("soVisa"));
        } catch (SQLException ignored) { }
        try {
            Date hhv = rs.getDate("ngayHetHanVisa");
            if (hhv != null) kh.setNgayHetHanVisa(hhv.toLocalDate());
        } catch (SQLException ignored) { }
        try {
            kh.setNoiCapHoChieu(rs.getString("noiCapHoChieu"));
        } catch (SQLException ignored) { }
        try {
            Date nc = rs.getDate("ngayNhapCanh");
            if (nc != null) kh.setNgayNhapCanh(nc.toLocalDate());
        } catch (SQLException ignored) { }
        try {
            kh.setAnhCCCD(rs.getString("anhCCCD"));
        } catch (SQLException ignored) { }
        try { kh.setTrangThai(rs.getBoolean("trangThai") ? "Hoạt động" : "Ngừng"); } catch (Exception e) { kh.setTrangThai("Hoạt động"); }
        
        // CRM Mapping
        try { kh.setSoLanO(rs.getInt("soLanO")); } catch (SQLException ignored) { }
        try { kh.setTongChiTieu(rs.getDouble("tongChiTieu")); } catch (SQLException ignored) { }
        try { kh.setDiemTichLuy(rs.getInt("diemTichLuy")); } catch (SQLException ignored) { }
        try { kh.setHangKhachHang(rs.getString("hangKhachHang")); } catch (SQLException ignored) { }
        try { kh.setBlacklist(rs.getBoolean("isBlacklist")); } catch (SQLException ignored) { }
        try { kh.setPreferences(rs.getString("preferences")); } catch (SQLException ignored) { }
        try { kh.setVipLevel(rs.getString("vipLevel")); } catch (SQLException ignored) { }
        
        return kh;
    }

    /** Cập nhật thống kê CRM (nguyên tử) */
    public boolean updateCRMStats(String maKh, double addedSpending, boolean incrementStay) {
        String sql = "UPDATE KhachHang SET " +
                     "soLanO = ISNULL(soLanO, 0) + ?, " +
                     "tongChiTieu = ISNULL(tongChiTieu, 0) + ?, " +
                     "diemTichLuy = ISNULL(diemTichLuy, 0) + ?, " +
                     "hangKhachHang = CASE " +
                     "  WHEN (ISNULL(soLanO, 0) + ?) >= 10 OR (ISNULL(tongChiTieu, 0) + ?) >= 20000000 THEN N'VIP (Gold)' " +
                     "  WHEN (ISNULL(soLanO, 0) + ?) >= 3 OR (ISNULL(tongChiTieu, 0) + ?) >= 5000000 THEN N'Thân thiết (Silver)' " +
                     "  ELSE N'Hạng Thường (Bronze)' " +
                     "END " +
                     "WHERE maKhachHang = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            int inc = incrementStay ? 1 : 0;
            int points = (int) (addedSpending / 200000);
            
            ps.setInt(1, inc);
            ps.setDouble(2, addedSpending);
            ps.setInt(3, points);
            ps.setInt(4, inc);
            ps.setDouble(5, addedSpending);
            ps.setInt(6, inc);
            ps.setDouble(7, addedSpending);
            ps.setString(8, maKh);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "CRM Update failed", e); return false; }
    }
}
