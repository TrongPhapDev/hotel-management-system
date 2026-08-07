package dao;

import database.DatabaseConnection;
import entity.HuongNhin;
import entity.LoaiPhong;
import entity.Phong;
import entity.DatPhong;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhongDAO {

    private static final Logger LOGGER = Logger.getLogger(PhongDAO.class.getName());

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<Phong> getAll() {
        List<Phong> list = new ArrayList<>();
        String sql =
            "SELECT p.*, lp.tenLoaiPhong, lp.giaTheoNgay, lp.soNguoiToiDa, " +
            "hn.tenHuongNhin, hn.heSoGia, hn.moTa AS moTaHuongNhin, " +
            "(SELECT TOP 1 kh.hoTen " +
            " FROM ChiTietDatPhong ct " +
            " JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong " +
            " JOIN KhachHang kh ON ISNULL(ct.maKhachHang, dp.maKhachHang) = kh.maKhachHang " +
            " WHERE ct.maPhong = p.maPhong " +
            "   AND ct.ngayNhanThucTe IS NOT NULL " +
            "   AND ct.ngayTraThucTe IS NULL " +
            "   AND dp.trangThai NOT IN ('CHECKED_OUT', 'CANCELLED') " +
            " ORDER BY ct.ngayNhanThucTe DESC) AS tenKhachHienTai, " +
            "(SELECT TOP 1 kh.sdt " +
            " FROM ChiTietDatPhong ct " +
            " JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong " +
            " JOIN KhachHang kh ON ISNULL(ct.maKhachHang, dp.maKhachHang) = kh.maKhachHang " +
            " WHERE ct.maPhong = p.maPhong " +
            "   AND ct.ngayNhanThucTe IS NOT NULL " +
            "   AND ct.ngayTraThucTe IS NULL " +
            "   AND dp.trangThai NOT IN ('CHECKED_OUT', 'CANCELLED') " +
            " ORDER BY ct.ngayNhanThucTe DESC) AS sdtKhachHienTai, " +
            "(SELECT TOP 1 ct.ngayNhanThucTe FROM ChiTietDatPhong ct JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong WHERE ct.maPhong = p.maPhong AND ct.ngayNhanThucTe IS NOT NULL AND ct.ngayTraThucTe IS NULL AND dp.trangThai NOT IN ('CHECKED_OUT', 'CANCELLED') ORDER BY ct.ngayNhanThucTe DESC) AS thoiGianCheckIn, " +
            "(SELECT TOP 1 dp.ngayTraDuKien FROM ChiTietDatPhong ct JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong WHERE ct.maPhong = p.maPhong AND ct.ngayNhanThucTe IS NOT NULL AND ct.ngayTraThucTe IS NULL AND dp.trangThai NOT IN ('CHECKED_OUT', 'CANCELLED') ORDER BY ct.ngayNhanThucTe DESC) AS thoiGianTraDuKien " +
            "FROM Phong p " +
            "JOIN LoaiPhong lp ON p.maLoaiPhong = lp.maLoaiPhong " +
            "LEFT JOIN HuongNhin hn ON p.maHuongNhin = hn.maHuongNhin " +
            "WHERE p.trangThai <> 'DELETED'";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Phong p = mapRow(rs);
                try { 
                    String guest = rs.getString("tenKhachHienTai");
                    String phone = rs.getString("sdtKhachHienTai");
                    if (p.getTrangThai() == entity.enums.TrangThaiPhong.OCCUPIED) {
                        p.setTenKhachHienTai(guest);
                        p.setSdtKhachHienTai(phone);
                    } else {
                        p.setTenKhachHienTai(null);
                        p.setSdtKhachHienTai(null);
                    }
                    Timestamp tsIn = rs.getTimestamp("thoiGianCheckIn");
                    if (tsIn != null) p.setCheckInTime(tsIn.toLocalDateTime());
                    Timestamp tsOut = rs.getTimestamp("thoiGianTraDuKien");
                    if (tsOut != null) p.setExpectedCheckOutTime(tsOut.toLocalDateTime());
                } catch (Exception ignored) {}
                list.add(p);
            }
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in PhongDAO", e); }
        return list;
    }

    public Phong getById(String maPhong) {
        String sql = "SELECT p.*, lp.tenLoaiPhong, lp.giaTheoNgay, lp.soNguoiToiDa, " +
                     "hn.tenHuongNhin, hn.heSoGia, hn.moTa AS moTaHuongNhin, " +
                     "(SELECT TOP 1 kh.hoTen " +
                     " FROM ChiTietDatPhong ct " +
                     " JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong " +
                     " JOIN KhachHang kh ON ISNULL(ct.maKhachHang, dp.maKhachHang) = kh.maKhachHang " +
                     " WHERE ct.maPhong = p.maPhong " +
                     "   AND ct.ngayNhanThucTe IS NOT NULL " +
                     "   AND ct.ngayTraThucTe IS NULL " +
                     "   AND dp.trangThai NOT IN ('CHECKED_OUT', 'CANCELLED') " +
                     " ORDER BY ct.ngayNhanThucTe DESC) AS tenKhachHienTai, " +
                     "(SELECT TOP 1 kh.sdt " +
                     " FROM ChiTietDatPhong ct " +
                     " JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong " +
                     " JOIN KhachHang kh ON ISNULL(ct.maKhachHang, dp.maKhachHang) = kh.maKhachHang " +
                     " WHERE ct.maPhong = p.maPhong " +
                     "   AND ct.ngayNhanThucTe IS NOT NULL " +
                     "   AND ct.ngayTraThucTe IS NULL " +
                     "   AND dp.trangThai NOT IN ('CHECKED_OUT', 'CANCELLED') " +
                     " ORDER BY ct.ngayNhanThucTe DESC) AS sdtKhachHienTai, " +
                     "(SELECT TOP 1 ct.ngayNhanThucTe FROM ChiTietDatPhong ct JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong WHERE ct.maPhong = p.maPhong AND ct.ngayNhanThucTe IS NOT NULL AND ct.ngayTraThucTe IS NULL AND dp.trangThai NOT IN ('CHECKED_OUT', 'CANCELLED') ORDER BY ct.ngayNhanThucTe DESC) AS thoiGianCheckIn, " +
                     "(SELECT TOP 1 dp.ngayTraDuKien FROM ChiTietDatPhong ct JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong WHERE ct.maPhong = p.maPhong AND ct.ngayNhanThucTe IS NOT NULL AND ct.ngayTraThucTe IS NULL AND dp.trangThai NOT IN ('CHECKED_OUT', 'CANCELLED') ORDER BY ct.ngayNhanThucTe DESC) AS thoiGianTraDuKien " +
                     "FROM Phong p " +
                     "JOIN LoaiPhong lp ON p.maLoaiPhong = lp.maLoaiPhong " +
                     "LEFT JOIN HuongNhin hn ON p.maHuongNhin = hn.maHuongNhin " +
                     "WHERE p.maPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maPhong);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Phong p = mapRow(rs);
                try { 
                    String guest = rs.getString("tenKhachHienTai");
                    String phone = rs.getString("sdtKhachHienTai");
                    if (p.getTrangThai() == entity.enums.TrangThaiPhong.OCCUPIED) {
                        p.setTenKhachHienTai(guest);
                        p.setSdtKhachHienTai(phone);
                    } else {
                        p.setTenKhachHienTai(null);
                        p.setSdtKhachHienTai(null);
                    }
                    Timestamp tsIn = rs.getTimestamp("thoiGianCheckIn");
                    if (tsIn != null) p.setCheckInTime(tsIn.toLocalDateTime());
                    Timestamp tsOut = rs.getTimestamp("thoiGianTraDuKien");
                    if (tsOut != null) p.setExpectedCheckOutTime(tsOut.toLocalDateTime());
                } catch (Exception ignored) {}
                return p;
            }
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in PhongDAO", e); }
        return null;
    }

    public boolean insert(Phong p) {
        String sql = "INSERT INTO Phong(maPhong, maLoaiPhong, tang, maHuongNhin, trangThai) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, p.getMaPhong());
            ps.setString(2, p.getLoaiPhong() != null ? p.getLoaiPhong().getMaLoaiPhong() : null);
            ps.setInt(3, p.getTang());
            ps.setString(4, p.getMaHuongNhin());
            ps.setString(5, p.getTrangThai() != null ? p.getTrangThai().name() : "AVAILABLE");
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                service.LogService.addLog("Thêm phòng", "Phòng " + p.getMaPhong(), "Thêm phòng mới vào hệ thống");
            }
            return ok;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in PhongDAO", e); return false; }
    }

    public boolean update(Phong p) {
        String sql = "UPDATE Phong SET maLoaiPhong=?, tang=?, maHuongNhin=?, trangThai=? WHERE maPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, p.getLoaiPhong() != null ? p.getLoaiPhong().getMaLoaiPhong() : null);
            ps.setInt(2, p.getTang());
            ps.setString(3, p.getMaHuongNhin());
            ps.setString(4, p.getTrangThai() != null ? p.getTrangThai().name() : "AVAILABLE");
            ps.setString(5, p.getMaPhong());
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                service.LogService.addLog("Sửa phòng", "Phòng " + p.getMaPhong(), "Cập nhật thông tin phòng");
            }
            return ok;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in PhongDAO", e); return false; }
    }

    public List<Phong> findAvailableRooms(java.time.LocalDateTime start, java.time.LocalDateTime end, int sucChua) {
        List<Phong> list = new ArrayList<>();
        String sql =
            "SELECT p.*, lp.tenLoaiPhong, lp.giaTheoNgay, lp.soNguoiToiDa, " +
            "hn.tenHuongNhin, hn.heSoGia, hn.moTa AS moTaHuongNhin, " +
            "(SELECT TOP 1 kh.hoTen " +
            " FROM ChiTietDatPhong ct " +
            " JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong " +
            " JOIN KhachHang kh ON ISNULL(ct.maKhachHang, dp.maKhachHang) = kh.maKhachHang " +
            " WHERE ct.maPhong = p.maPhong " +
            "   AND ct.ngayNhanThucTe IS NOT NULL " +
            "   AND ct.ngayTraThucTe IS NULL " +
            " ORDER BY ct.ngayNhanThucTe DESC) AS tenKhachHienTai, " +
            "(SELECT TOP 1 kh.sdt " +
            " FROM ChiTietDatPhong ct " +
            " JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong " +
            " JOIN KhachHang kh ON ISNULL(ct.maKhachHang, dp.maKhachHang) = kh.maKhachHang " +
            " WHERE ct.maPhong = p.maPhong " +
            "   AND ct.ngayNhanThucTe IS NOT NULL " +
            "   AND ct.ngayTraThucTe IS NULL " +
            " ORDER BY ct.ngayNhanThucTe DESC) AS sdtKhachHienTai " +
            "FROM Phong p " +
            "JOIN LoaiPhong lp ON p.maLoaiPhong = lp.maLoaiPhong " +
            "LEFT JOIN HuongNhin hn ON p.maHuongNhin = hn.maHuongNhin " +
            "WHERE lp.soNguoiToiDa >= ? " +
            "AND p.trangThai NOT IN ('DELETED', 'MAINTENANCE') " +
            "AND p.maPhong NOT IN (" +
            "  SELECT ctdp.maPhong FROM ChiTietDatPhong ctdp " +
            "  JOIN DatPhong dp ON ctdp.maDatPhong = dp.maDatPhong " +
            "  WHERE dp.trangThai NOT IN ('CANCELLED','CHECKED_OUT') " +
            "  AND dp.ngayNhanDuKien < ? " +
            "  AND dp.ngayTraDuKien > ? " +
            ")";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, sucChua);
            ps.setTimestamp(2, Timestamp.valueOf(end));
            ps.setTimestamp(3, Timestamp.valueOf(start));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Phong p = mapRow(rs);
                try { 
                    p.setTenKhachHienTai(rs.getString("tenKhachHienTai")); 
                    p.setSdtKhachHienTai(rs.getString("sdtKhachHienTai"));
                } catch (Exception ignored) {}
                list.add(p);
            }
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in PhongDAO", e); }
        return list;
    }

    public boolean delete(String maPhong) {
        String sql = "UPDATE Phong SET trangThai='DELETED' WHERE maPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maPhong);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                service.LogService.addLog("Xóa phòng", "Phòng " + maPhong, "Chuyển trạng thái phòng sang DELETED");
            }
            return ok;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in PhongDAO", e); return false; }
    }

    public int countBookingReferences(String maPhong) {
        String sql = "SELECT COUNT(*) FROM ChiTietDatPhong WHERE maPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maPhong);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in PhongDAO", e); }
        return 0;
    }

    public List<Phong> search(String kw, String tang, String huongNhin) {
        List<Phong> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder(
            "SELECT p.*, lp.tenLoaiPhong, lp.giaTheoNgay, lp.soNguoiToiDa, hn.tenHuongNhin, hn.heSoGia, hn.moTa AS moTaHuongNhin " +
            "FROM Phong p " +
            "JOIN LoaiPhong lp ON p.maLoaiPhong = lp.maLoaiPhong " +
            "LEFT JOIN HuongNhin hn ON p.maHuongNhin = hn.maHuongNhin " +
            "WHERE p.trangThai <> 'DELETED' ");
        if (kw != null && !kw.isEmpty()) sb.append("AND (p.maPhong LIKE ?) ");
        if (tang != null && !tang.isEmpty()) sb.append("AND p.tang = ? ");
        if (huongNhin != null && !huongNhin.isEmpty()) sb.append("AND hn.tenHuongNhin = ? ");

        try (PreparedStatement ps = getConn().prepareStatement(sb.toString())) {
            int idx = 1;
            if (kw != null && !kw.isEmpty()) ps.setString(idx++, "%" + kw + "%");
            if (tang != null && !tang.isEmpty()) ps.setInt(idx++, Integer.parseInt(tang));
            if (huongNhin != null && !huongNhin.isEmpty()) ps.setString(idx++, huongNhin);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in PhongDAO", e); }
        return list;
    }

    public java.util.Map<String, Integer> getThongKeTrangThai() {
        java.util.Map<String, Integer> map = new java.util.HashMap<>();
        String sql = "SELECT trangThai, COUNT(*) FROM Phong WHERE trangThai <> 'DELETED' GROUP BY trangThai";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String key = rs.getString(1);
                int count = rs.getInt(2);
                if ("AVAILABLE".equalsIgnoreCase(key)) key = "Có sẵn";
                else if ("OCCUPIED".equalsIgnoreCase(key)) key = "Đang thuê";
                else if ("MAINTENANCE".equalsIgnoreCase(key)) key = "Bảo trì";
                else if ("CLEANING".equalsIgnoreCase(key)) key = "Vệ sinh";
                map.put(key, count);
            }
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in PhongDAO", e); }
        return map;
    }

    public Phong getPhongById(String id) { return getById(id); }

    /**
     * Tìm các phòng có đặt trước (CONFIRMED) trong khoảng thời gian.
     * Trả về Map: maPhong -> maDatPhong (để hiện badge "Đã đặt" trên sơ đồ phòng).
     */
    public Map<String, String[]> getReservedRoomsForDate(LocalDateTime date) {
        Map<String, String[]> map = new HashMap<>();
        String sql =
            "SELECT ct.maPhong, dp.maDatPhong, kh.hoTen, dp.ngayNhanDuKien, dp.ngayTraDuKien " +
            "FROM ChiTietDatPhong ct " +
            "JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong " +
            "LEFT JOIN KhachHang kh ON dp.maKhachHang = kh.maKhachHang " +
            "WHERE dp.trangThai IN ('CONFIRMED', 'PARTIALLY_CHECKED_IN') " +
            "AND ct.ngayNhanThucTe IS NULL " +
            "AND dp.ngayNhanDuKien <= DATEADD(DAY, 1, ?) " +
            "AND dp.ngayTraDuKien > ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            Timestamp ts = Timestamp.valueOf(date);
            ps.setTimestamp(1, ts);
            ps.setTimestamp(2, ts);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String maPhong = rs.getString("maPhong");
                String maDat   = rs.getString("maDatPhong");
                String tenKH   = rs.getString("hoTen");
                Timestamp tsNhan = rs.getTimestamp("ngayNhanDuKien");
                String ngayNhan = tsNhan != null ? new java.text.SimpleDateFormat("dd/MM HH:mm").format(tsNhan) : "";
                map.put(maPhong, new String[]{maDat, tenKH != null ? tenKH : "—", ngayNhan});
            }
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in PhongDAO", e); }
        return map;
    }

    /**
     * Đếm số phòng đã có reservation (CONFIRMED/CHECKED_IN) chồng lấn khoảng thời gian.
     * Dùng để validate trước khi đặt phòng.
     */
    public boolean isRoomAvailable(String maPhong, LocalDateTime from, LocalDateTime to) {
        return isRoomAvailable(maPhong, from, to, null);
    }

    /**
     * Kiểm tra sẵn sàng của phòng, có thể loại trừ 1 mã đặt phòng (để gia hạn/đổi phòng chính nó).
     */
    public boolean isRoomAvailable(String maPhong, LocalDateTime from, LocalDateTime to, String excludeMaDatPhong) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM ChiTietDatPhong ct " +
            "JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong " +
            "WHERE ct.maPhong = ? " +
            "AND dp.trangThai IN ('CONFIRMED', 'CHECKED_IN', 'PARTIALLY_CHECKED_IN') " +
            "AND dp.ngayNhanDuKien < ? " +
            "AND dp.ngayTraDuKien > ?"
        );
        
        if (excludeMaDatPhong != null && !excludeMaDatPhong.trim().isEmpty()) {
            sql.append(" AND dp.maDatPhong <> ?");
        }

        try (PreparedStatement ps = getConn().prepareStatement(sql.toString())) {
            ps.setString(1, maPhong);
            ps.setTimestamp(2, Timestamp.valueOf(to));
            ps.setTimestamp(3, Timestamp.valueOf(from));
            if (excludeMaDatPhong != null && !excludeMaDatPhong.trim().isEmpty()) {
                ps.setString(4, excludeMaDatPhong);
            }
            
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) == 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in PhongDAO", e); }
        return true;
    }

    private Phong mapRow(ResultSet rs) throws SQLException {
        Phong p = new Phong();
        p.setMaPhong(rs.getString("maPhong"));
        p.setTang(rs.getInt("tang"));

        // Map HuongNhin object (includes heSoGia for price adjustment)
        String maHN = null;
        try { maHN = rs.getString("maHuongNhin"); } catch (Exception e) {}
        if (maHN != null) {
            HuongNhin hn = new HuongNhin();
            hn.setMaHuongNhin(maHN);
            try { hn.setTenHuongNhin(rs.getString("tenHuongNhin")); } catch (Exception e) {}
            try { hn.setHeSoGia(rs.getDouble("heSoGia")); } catch (Exception e) { hn.setHeSoGia(1.0); }
            p.setHuongNhin(hn);
        }

        LoaiPhong lp = new LoaiPhong();
        lp.setMaLoaiPhong(rs.getString("maLoaiPhong"));
        try {
            lp.setTenLoaiPhong(rs.getString("tenLoaiPhong"));
            lp.setGiaTheoNgay(rs.getDouble("giaTheoNgay"));
            lp.setSucChua(rs.getInt("soNguoiToiDa"));
        } catch (Exception e) {}
        p.setLoaiPhong(lp);
        p.setGiaTheoNgay(lp.getGiaTheoNgay());

        String statusStr = rs.getString("trangThai");
        if (statusStr != null) {
            try {
                p.setTrangThai(entity.enums.TrangThaiPhong.valueOf(statusStr.toUpperCase()));
            } catch (Exception e) {
                p.setTrangThai(entity.enums.TrangThaiPhong.AVAILABLE);
            }
        }
        return p;
    }
}

