package dao;

import database.DatabaseConnection;
import entity.HoaDon;
import entity.DatPhong;
import entity.NhanVien;
import entity.enums.TrangThaiThanhToan;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HoaDonDAO {

    private static final Logger LOGGER = Logger.getLogger(HoaDonDAO.class.getName());


    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<HoaDon> getAll() {
        List<HoaDon> list = new ArrayList<>();
        String sql = "SELECT * FROM HoaDon WHERE trangThai <> 'CANCELLED' ORDER BY ngayLap DESC";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Lỗi thao tác HoaDon", e); }
        return list;
    }

    public HoaDon getById(String maHoaDon) {
        String sql = "SELECT * FROM HoaDon WHERE maHoaDon=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maHoaDon);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Lỗi thao tác HoaDon", e); }
        return null;
    }

    public boolean insert(HoaDon hd) {
        String sql = "INSERT INTO HoaDon(maHoaDon, maDatPhong, maNhanVien, maKhuyenMai, ngayLap, tongTienPhong, tongTienDichVu, tienGiamKhuyenMai, tongThanhToan, trangThai, phuongThucThanhToan) " +
                     "VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, hd.getMaHoaDon());
            ps.setString(2, hd.getDatPhong() != null ? hd.getDatPhong().getMaDatPhong() : null);
            ps.setString(3, hd.getNhanVien() != null ? hd.getNhanVien().getMaNhanVien() : null);
            ps.setNull(4, Types.VARCHAR); // maKhuyenMai
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.setDouble(6, hd.getTongTienPhong());
            ps.setDouble(7, hd.getTongTienDichVu());
            ps.setDouble(8, hd.getTienGiamKhuyenMai());
            ps.setDouble(9, hd.getTongThanhToan());
            ps.setString(10, hd.getTrangThai() != null ? hd.getTrangThai().name() : "UNPAID");
            ps.setString(11, hd.getPhuongThucThanhToan() != null ? hd.getPhuongThucThanhToan() : "CASH");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Lỗi thao tác HoaDon", e); return false; }
    }

    public boolean update(HoaDon hd) {
        String sql = "UPDATE HoaDon SET trangThai=? WHERE maHoaDon=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, hd.getTrangThai().name());
            ps.setString(2, hd.getMaHoaDon());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Lỗi thao tác HoaDon", e); return false; }
    }

    public int countByDatPhong(String maDatPhong) {
        String sql = "SELECT COUNT(*) FROM HoaDon WHERE maDatPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maDatPhong);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Lỗi đếm hóa đơn", e); }
        return 0;
    }

    public boolean delete(String maHoaDon) {
        // Kiểm tra trạng thái hóa đơn trước khi hủy
        HoaDon current = getById(maHoaDon);
        if (current != null && current.getTrangThai() == TrangThaiThanhToan.PAID) {
            LOGGER.log(Level.WARNING, "Không được phép hủy hóa đơn đã thanh toán: {0}", maHoaDon);
            return false;
        }

        String sql = "UPDATE HoaDon SET trangThai='CANCELLED' WHERE maHoaDon=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maHoaDon);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi hủy hóa đơn: " + maHoaDon, e);
            return false;
        }
    }

    public String generateMaHD() {
        java.time.LocalDate today = java.time.LocalDate.now();
        String prefix = String.format("HD-%d%02d%02d-", today.getYear(), today.getMonthValue(), today.getDayOfMonth());
        String sql = "SELECT MAX(CAST(SUBSTRING(maHoaDon, LEN(?)+1, 10) AS INT)) FROM HoaDon WHERE maHoaDon LIKE ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, prefix); ps.setString(2, prefix + "%");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return prefix + String.format("%03d", rs.getInt(1) + 1);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Lỗi thao tác HoaDon", e); }
        return prefix + "001";
    }

    private HoaDon mapRow(ResultSet rs) throws SQLException {
        HoaDon hd = new HoaDon();
        hd.setMaHoaDon(rs.getString("maHoaDon"));
        
        DatPhong dp = new DatPhong();
        dp.setMaDatPhong(rs.getString("maDatPhong"));
        hd.setDatPhong(dp);

        NhanVien nv = new NhanVien();
        nv.setMaNhanVien(rs.getString("maNhanVien"));
        hd.setNhanVien(nv);

        Timestamp ts = rs.getTimestamp("ngayLap");
        if (ts != null) hd.setNgayLap(ts.toLocalDateTime());

        hd.setTongTienPhong(rs.getDouble("tongTienPhong"));
        hd.setTongTienDichVu(rs.getDouble("tongTienDichVu"));
        hd.setTienGiamKhuyenMai(rs.getDouble("tienGiamKhuyenMai"));
        hd.setTongThanhToan(rs.getDouble("tongThanhToan"));
        
        String statusStr = rs.getString("trangThai");
        if (statusStr != null) {
            try {
                if (statusStr.equals("Đã thanh toán")) statusStr = "PAID";
                hd.setTrangThai(TrangThaiThanhToan.valueOf(statusStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                hd.setTrangThai(TrangThaiThanhToan.UNPAID);
            }
        }
        hd.setPhuongThucThanhToan(rs.getString("phuongThucThanhToan"));

        return hd;
    }
}
