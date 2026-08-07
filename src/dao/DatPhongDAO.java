package dao;

import database.DatabaseConnection;
import entity.DatPhong;
import entity.KhachHang;
import entity.NhanVien;
import entity.KenhDatPhong;
import entity.enums.TrangThaiDatPhong;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatPhongDAO {

    private static final Logger LOGGER = Logger.getLogger(DatPhongDAO.class.getName());

    private final KhachHangDAO khachHangDAO = new KhachHangDAO();
    private final KenhDatPhongDAO kenhDAO = new KenhDatPhongDAO();

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<DatPhong> getAll() {
        List<DatPhong> list = new ArrayList<>();
        String sql = "SELECT * FROM DatPhong WHERE trangThai <> 'CANCELLED' ORDER BY ngayDat DESC";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi lấy danh sách đặt phòng", e);
        }
        return list;
    }

    public DatPhong getById(String maDatPhong) {
        String sql = "SELECT * FROM DatPhong WHERE maDatPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maDatPhong);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi lấy đặt phòng theo mã", e);
        }
        return null;
    }

    public DatPhong getByIdBasic(String maDatPhong) {
        String sql = "SELECT * FROM DatPhong WHERE maDatPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maDatPhong);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapRow(rs, false);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi lấy đặt phòng (basic)", e);
        }
        return null;
    }

    public boolean insert(DatPhong dp) {
        String sql = "INSERT INTO DatPhong(maDatPhong, maKhachHang, maNhanVien, ngayDat, ngayNhanDuKien, ngayTraDuKien, soNguoi, tienDatCoc, tongTienTamTinh, trangThai, ghiChu, loaiKhach, tenDoan, maKenh, maXacNhanKenh, phiNoShow, hanCheckIn, thuTuWaitlist, hanNopCoc, phiHuyPhong, maKM) "
                +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, dp.getMaDatPhong());
            ps.setString(2, dp.getKhachHang() != null ? dp.getKhachHang().getMaKhachHang() : null);
            ps.setString(3, dp.getNhanVien() != null ? dp.getNhanVien().getMaNhanVien() : null);
            ps.setTimestamp(4, dp.getNgayDat() != null ? Timestamp.valueOf(dp.getNgayDat())
                    : Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(5, dp.getNgayNhanDuKien() != null ? Timestamp.valueOf(dp.getNgayNhanDuKien()) : null);
            ps.setTimestamp(6, dp.getNgayTraDuKien() != null ? Timestamp.valueOf(dp.getNgayTraDuKien()) : null);
            ps.setInt(7, dp.getSoLuongKhach());
            ps.setDouble(8, dp.getTienDatCoc());
            ps.setDouble(9, dp.getTongTienTamTinh());
            ps.setString(10, dp.getTrangThai() != null ? dp.getTrangThai().name() : "PENDING");
            ps.setString(11, dp.getGhiChu());
            ps.setString(12, dp.getLoaiKhach() != null ? dp.getLoaiKhach() : "CA_NHAN");
            ps.setString(13, dp.getTenDoan());
            ps.setString(14, dp.getMaKenh() != null ? dp.getMaKenh() : "DIRECT");
            ps.setString(15, dp.getMaXacNhanKenh());
            ps.setDouble(16, dp.getPhiNoShow());
            ps.setTimestamp(17, dp.getHanCheckIn() != null ? Timestamp.valueOf(dp.getHanCheckIn()) : null);
            ps.setInt(18, dp.getThuTuWaitlist());
            ps.setTimestamp(19, dp.getHanNopCoc() != null ? Timestamp.valueOf(dp.getHanNopCoc()) : null);
            ps.setDouble(20, dp.getPhiHuyPhong());
            ps.setString(21, dp.getMaKhuyenMai());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Fallback for old schema without new columns
            LOGGER.log(Level.WARNING, "Thử insert với schema cũ", e);
            return insertLegacy(dp);
        }
    }

    /** Fallback insert cho schema cũ chưa có các cột mới */
    private boolean insertLegacy(DatPhong dp) {
        String sql = "INSERT INTO DatPhong(maDatPhong, maKhachHang, maNhanVien, ngayDat, ngayNhanDuKien, ngayTraDuKien, soNguoi, tienDatCoc, tongTienTamTinh, trangThai, ghiChu, loaiKhach, tenDoan) "
                +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, dp.getMaDatPhong());
            ps.setString(2, dp.getKhachHang() != null ? dp.getKhachHang().getMaKhachHang() : null);
            ps.setString(3, dp.getNhanVien() != null ? dp.getNhanVien().getMaNhanVien() : null);
            ps.setTimestamp(4, dp.getNgayDat() != null ? Timestamp.valueOf(dp.getNgayDat())
                    : Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(5, dp.getNgayNhanDuKien() != null ? Timestamp.valueOf(dp.getNgayNhanDuKien()) : null);
            ps.setTimestamp(6, dp.getNgayTraDuKien() != null ? Timestamp.valueOf(dp.getNgayTraDuKien()) : null);
            ps.setInt(7, dp.getSoLuongKhach());
            ps.setDouble(8, dp.getTienDatCoc());
            ps.setDouble(9, dp.getTongTienTamTinh());
            ps.setString(10, dp.getTrangThai() != null ? dp.getTrangThai().name() : "PENDING");
            ps.setString(11, dp.getGhiChu());
            ps.setString(12, dp.getLoaiKhach() != null ? dp.getLoaiKhach() : "CA_NHAN");
            ps.setString(13, dp.getTenDoan());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi thêm đặt phòng", e);
            return false;
        }
    }

    public boolean update(DatPhong dp) {
        String sql = "UPDATE DatPhong SET maKhachHang=?, maNhanVien=?, ngayNhanDuKien=?, ngayTraDuKien=?, soNguoi=?, tienDatCoc=?, tongTienTamTinh=?, trangThai=?, ghiChu=?, loaiKhach=?, tenDoan=?, maKenh=?, maXacNhanKenh=?, phiNoShow=?, hanCheckIn=?, thuTuWaitlist=?, hanNopCoc=?, phiHuyPhong=?, maKM=? WHERE maDatPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, dp.getKhachHang() != null ? dp.getKhachHang().getMaKhachHang() : null);
            ps.setString(2, dp.getNhanVien() != null ? dp.getNhanVien().getMaNhanVien() : null);
            ps.setTimestamp(3, dp.getNgayNhanDuKien() != null ? Timestamp.valueOf(dp.getNgayNhanDuKien()) : null);
            ps.setTimestamp(4, dp.getNgayTraDuKien() != null ? Timestamp.valueOf(dp.getNgayTraDuKien()) : null);
            ps.setInt(5, dp.getSoLuongKhach());
            ps.setDouble(6, dp.getTienDatCoc());
            ps.setDouble(7, dp.getTongTienTamTinh());
            ps.setString(8, dp.getTrangThai() != null ? dp.getTrangThai().name() : "PENDING");
            ps.setString(9, dp.getGhiChu());
            ps.setString(10, dp.getLoaiKhach() != null ? dp.getLoaiKhach() : "CA_NHAN");
            ps.setString(11, dp.getTenDoan());
            ps.setString(12, dp.getMaKenh() != null ? dp.getMaKenh() : "DIRECT");
            ps.setString(13, dp.getMaXacNhanKenh());
            ps.setDouble(14, dp.getPhiNoShow());
            ps.setTimestamp(15, dp.getHanCheckIn() != null ? Timestamp.valueOf(dp.getHanCheckIn()) : null);
            ps.setInt(16, dp.getThuTuWaitlist());
            ps.setTimestamp(17, dp.getHanNopCoc() != null ? Timestamp.valueOf(dp.getHanNopCoc()) : null);
            ps.setDouble(18, dp.getPhiHuyPhong());
            ps.setString(19, dp.getMaKhuyenMai());
            ps.setString(20, dp.getMaDatPhong());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Fallback for old schema
            LOGGER.log(Level.WARNING, "Thử update với schema cũ", e);
            return updateLegacy(dp);
        }
    }

    /** Fallback update cho schema cũ */
    private boolean updateLegacy(DatPhong dp) {
        String sql = "UPDATE DatPhong SET maKhachHang=?, maNhanVien=?, ngayNhanDuKien=?, ngayTraDuKien=?, soNguoi=?, tienDatCoc=?, tongTienTamTinh=?, trangThai=?, ghiChu=?, loaiKhach=?, tenDoan=? WHERE maDatPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, dp.getKhachHang() != null ? dp.getKhachHang().getMaKhachHang() : null);
            ps.setString(2, dp.getNhanVien() != null ? dp.getNhanVien().getMaNhanVien() : null);
            ps.setTimestamp(3, dp.getNgayNhanDuKien() != null ? Timestamp.valueOf(dp.getNgayNhanDuKien()) : null);
            ps.setTimestamp(4, dp.getNgayTraDuKien() != null ? Timestamp.valueOf(dp.getNgayTraDuKien()) : null);
            ps.setInt(5, dp.getSoLuongKhach());
            ps.setDouble(6, dp.getTienDatCoc());
            ps.setDouble(7, dp.getTongTienTamTinh());
            ps.setString(8, dp.getTrangThai() != null ? dp.getTrangThai().name() : "PENDING");
            ps.setString(9, dp.getGhiChu());
            ps.setString(10, dp.getLoaiKhach() != null ? dp.getLoaiKhach() : "CA_NHAN");
            ps.setString(11, dp.getTenDoan());
            ps.setString(12, dp.getMaDatPhong());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi cập nhật đặt phòng", e);
            return false;
        }
    }

    /** Đổi trạng thái sang CANCELLED (Soft delete) */
    public boolean softDelete(String maDatPhong) {
        String sql = "UPDATE DatPhong SET trangThai='CANCELLED' WHERE maDatPhong=? AND trangThai NOT IN ('CHECKED_IN', 'CHECKED_OUT')";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maDatPhong);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi hủy đặt phòng: " + maDatPhong, e);
            return false;
        }
    }

    /** Xóa hoàn toàn khỏi CSDL (Hard delete) */
    public boolean deleteHard(String maDatPhong) {
        String sql = "DELETE FROM DatPhong WHERE maDatPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maDatPhong);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa cứng đặt phòng: " + maDatPhong, e);
            return false;
        }
    }

    public String generateMaDatPhong() {
        java.time.LocalDate today = java.time.LocalDate.now();
        String prefix = String.format("DP-%d%02d%02d-", today.getYear(), today.getMonthValue(), today.getDayOfMonth());
        String sql = "SELECT MAX(CAST(SUBSTRING(maDatPhong, LEN(?)+1, 10) AS INT)) FROM DatPhong WHERE maDatPhong LIKE ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, prefix);
            ps.setString(2, prefix + "%");
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return prefix + String.format("%03d", rs.getInt(1) + 1);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Lỗi sinh mã đặt phòng", e);
        }
        return prefix + "001";
    }

    public List<DatPhong> search(String kw, String status, String date) {
        List<DatPhong> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT * FROM DatPhong WHERE 1=1 ");
        if (kw != null && !kw.isEmpty())
            sb.append("AND (maDatPhong LIKE ? OR maKhachHang LIKE ? OR maXacNhanKenh LIKE ?) ");
        if (status != null && !status.isEmpty())
            sb.append("AND trangThai = ? ");
        if (date != null && !date.isEmpty())
            sb.append("AND CAST(ngayDat AS DATE) = ? ");
        sb.append("ORDER BY ngayDat DESC");

        try (PreparedStatement ps = getConn().prepareStatement(sb.toString())) {
            int idx = 1;
            if (kw != null && !kw.isEmpty()) {
                String k = "%" + kw + "%";
                ps.setString(idx++, k);
                ps.setString(idx++, k);
                ps.setString(idx++, k);
            }
            if (status != null && !status.isEmpty())
                ps.setString(idx++, status);
            if (date != null && !date.isEmpty())
                ps.setString(idx++, date);

            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi tìm kiếm đặt phòng", e);
        }
        return list;
    }

    /** 
     * Lấy danh sách đặt phòng đã quá hạn check-in (No-show).
     * @param hoursThreshold Số giờ quá hạn tối thiểu (ví dụ: quá 2 tiếng so với giờ dự kiến)
     */
    public List<DatPhong> getPotentialNoShows(int hoursThreshold) {
        List<DatPhong> list = new ArrayList<>();
        String sql = "SELECT * FROM DatPhong WHERE trangThai = 'CONFIRMED' " +
                "AND ((hanCheckIn IS NOT NULL AND hanCheckIn < DATEADD(HOUR, -?, GETDATE())) " +
                "  OR (hanCheckIn IS NULL AND ngayNhanDuKien < DATEADD(HOUR, -?, GETDATE()))) " +
                "ORDER BY ngayNhanDuKien ASC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, hoursThreshold);
            ps.setInt(2, hoursThreshold);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi lấy danh sách potential no-show", e);
        }
        return list;
    }

    /** Lấy danh sách waitlist, sắp xếp theo thứ tự ưu tiên */
    public List<DatPhong> getWaitlist() {
        List<DatPhong> list = new ArrayList<>();
        String sql = "SELECT * FROM DatPhong WHERE trangThai = 'WAITLIST' ORDER BY thuTuWaitlist ASC, ngayDat ASC";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi lấy waitlist", e);
        }
        return list;
    }

    /** Đánh dấu no-show cho một đặt phòng */
    public boolean markNoShow(String maDatPhong, double phiPhat) {
        String sql = "UPDATE DatPhong SET trangThai='NO_SHOW', phiNoShow=?, ghiChu=ISNULL(ghiChu,'') + ' | No-show: Khách không đến' WHERE maDatPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setDouble(1, phiPhat);
            ps.setString(2, maDatPhong);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi đánh dấu no-show", e);
            return false;
        }
    }

    /**
     * Lấy danh sách đặt phòng CONFIRMED sắp đến (dùng SQL, không load toàn bảng).
     * @param hoursAhead Cửa sổ tương lai (tiếng)
     */
    public List<DatPhong> getUpcomingArrivals(int hoursAhead) {
        List<DatPhong> list = new ArrayList<>();
        String sql = "SELECT * FROM DatPhong WHERE trangThai = 'CONFIRMED' " +
                     "AND ngayNhanDuKien BETWEEN DATEADD(HOUR, -2, GETDATE()) " +
                     "                      AND DATEADD(HOUR, ?, GETDATE()) " +
                     "ORDER BY ngayNhanDuKien ASC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, hoursAhead);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi getUpcomingArrivals", e);
        }
        return list;
    }

    /**
     * Lấy danh sách đặt phòng PENDING lâu hơn hoursOld tiếng (dùng SQL).
     */
    public List<DatPhong> getLongPending(int hoursOld) {
        List<DatPhong> list = new ArrayList<>();
        String sql = "SELECT * FROM DatPhong WHERE trangThai = 'PENDING' " +
                     "AND ngayDat < DATEADD(HOUR, -?, GETDATE()) " +
                     "ORDER BY ngayDat ASC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, hoursOld);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi getLongPending", e);
        }
        return list;
    }

    /** Chuyển booking từ waitlist lên confirmed */
    public boolean confirmFromWaitlist(String maDatPhong) {
        String sql = "UPDATE DatPhong SET trangThai='CONFIRMED', thuTuWaitlist=0 WHERE maDatPhong=? AND trangThai='WAITLIST'";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maDatPhong);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi confirm từ waitlist", e);
            return false;
        }
    }

    /** Lấy thứ tự waitlist tiếp theo */
    public int getNextWaitlistOrder() {
        String sql = "SELECT ISNULL(MAX(thuTuWaitlist), 0) + 1 FROM DatPhong WHERE trangThai='WAITLIST'";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Lỗi lấy thứ tự waitlist", e);
        }
        return 1;
    }

    /** Tìm kiếm theo kênh đặt phòng */
    public List<DatPhong> searchByChannel(String maKenh) {
        List<DatPhong> list = new ArrayList<>();
        String sql = "SELECT * FROM DatPhong WHERE maKenh=? ORDER BY ngayDat DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maKenh);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi tìm theo kênh", e);
        }
        return list;
    }

    private DatPhong mapRow(ResultSet rs) throws SQLException {
        return mapRow(rs, true);
    }

    private DatPhong mapRow(ResultSet rs, boolean includeDetails) throws SQLException {
        DatPhong dp = new DatPhong();
        dp.setMaDatPhong(rs.getString("maDatPhong"));

        String maKH = rs.getString("maKhachHang");
        KhachHang kh = maKH != null ? khachHangDAO.getById(maKH) : null;
        if (kh == null) {
            kh = new KhachHang();
            kh.setMaKhachHang(maKH);
        }
        dp.setKhachHang(kh);

        NhanVien nv = new NhanVien();
        nv.setMaNhanVien(rs.getString("maNhanVien"));
        dp.setNhanVien(nv);

        Timestamp tsNgayDat = rs.getTimestamp("ngayDat");
        if (tsNgayDat != null)
            dp.setNgayDat(tsNgayDat.toLocalDateTime());

        Timestamp tsNgayNhan = rs.getTimestamp("ngayNhanDuKien");
        if (tsNgayNhan != null)
            dp.setNgayNhanDuKien(tsNgayNhan.toLocalDateTime());

        Timestamp tsNgayTra = rs.getTimestamp("ngayTraDuKien");
        if (tsNgayTra != null)
            dp.setNgayTraDuKien(tsNgayTra.toLocalDateTime());

        dp.setSoLuongKhach(rs.getInt("soNguoi"));
        dp.setTienDatCoc(rs.getDouble("tienDatCoc"));
        dp.setTongTienTamTinh(rs.getDouble("tongTienTamTinh"));
        dp.setGhiChu(rs.getString("ghiChu"));

        // Đọc loại khách (safe: tương thích ngược với DB chưa có cột này)
        try {
            String lk = rs.getString("loaiKhach");
            dp.setLoaiKhach(lk != null ? lk : "CA_NHAN");
        } catch (SQLException ignored) {
            dp.setLoaiKhach("CA_NHAN");
        }
        try {
            dp.setTenDoan(rs.getString("tenDoan"));
        } catch (SQLException ignored) {
        }

        // Đọc kênh đặt phòng (safe: tương thích ngược)
        try {
            String mk = rs.getString("maKenh");
            dp.setMaKenh(mk != null ? mk : "DIRECT");
            // Tải thông tin kênh đầy đủ
            if (mk != null && !mk.isEmpty()) {
                try {
                    KenhDatPhong kenh = kenhDAO.getById(mk);
                    dp.setKenhDatPhong(kenh);
                } catch (Exception ignored) {
                }
            }
        } catch (SQLException ignored) {
            dp.setMaKenh("DIRECT");
        }

        try {
            dp.setMaXacNhanKenh(rs.getString("maXacNhanKenh"));
        } catch (SQLException ignored) {
        }

        // Đọc no-show fields
        try {
            dp.setPhiNoShow(rs.getDouble("phiNoShow"));
        } catch (SQLException ignored) {
        }
        try {
            Timestamp tsHan = rs.getTimestamp("hanCheckIn");
            if (tsHan != null)
                dp.setHanCheckIn(tsHan.toLocalDateTime());
        } catch (SQLException ignored) {
        }

        // Đọc waitlist fields
        try {
            dp.setThuTuWaitlist(rs.getInt("thuTuWaitlist"));
        } catch (SQLException ignored) {
        }
        try {
            Timestamp tsHanCoc = rs.getTimestamp("hanNopCoc");
            if (tsHanCoc != null) dp.setHanNopCoc(tsHanCoc.toLocalDateTime());
        } catch (SQLException ignored) {}
        
        try {
            dp.setPhiHuyPhong(rs.getDouble("phiHuyPhong"));
        } catch (SQLException ignored) {}
        
        try {
            dp.setMaKhuyenMai(rs.getString("maKM"));
        } catch (SQLException ignored) {}

        String statusStr = rs.getString("trangThai");
        dp.setTrangThai(parseTrangThai(statusStr));

        // Load ChiTietDatPhong for this booking (optional to avoid recursive loops)
        if (includeDetails) {
            String maDatPhong = rs.getString("maDatPhong");
            if (maDatPhong != null) {
                ChiTietDatPhongDAO ctDAO = new ChiTietDatPhongDAO();
                dp.setDsChiTiet(ctDAO.getByDatPhong(maDatPhong));
            }
        }
        // Trạng thái PARTIALLY_CHECKED_IN được đọc thẳng từ DB (không cần suy luận).

        return dp;
    }

    private TrangThaiDatPhong parseTrangThai(String raw) {
        if (raw == null)
            return TrangThaiDatPhong.PENDING;
        String s = raw.trim();
        if (s.isEmpty())
            return TrangThaiDatPhong.PENDING;

        // 1) Enum style values from DB
        try {
            return TrangThaiDatPhong.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            // Continue with localized mapping.
        }

        // 2) Localized labels (Vietnamese)
        String n = s.toLowerCase();
        if (n.contains("xác nhận"))
            return TrangThaiDatPhong.CONFIRMED;
        if (n.contains("đang check-in") || n.contains("partially"))
            return TrangThaiDatPhong.PARTIALLY_CHECKED_IN;
        if (n.contains("check-in") || n.contains("đã checkin") || n.contains("checked in"))
            return TrangThaiDatPhong.CHECKED_IN;
        if (n.contains("trả phòng") || n.contains("checkout") || n.contains("checked out"))
            return TrangThaiDatPhong.CHECKED_OUT;
        if (n.contains("hủy") || n.contains("cancel"))
            return TrangThaiDatPhong.CANCELLED;
        if (n.contains("no-show") || n.contains("no show") || n.contains("noshow"))
            return TrangThaiDatPhong.NO_SHOW;
        if (n.contains("waitlist") || n.contains("chờ xếp"))
            return TrangThaiDatPhong.WAITLIST;
        if (n.contains("chờ") || n.contains("pending"))
            return TrangThaiDatPhong.PENDING;

        return TrangThaiDatPhong.PENDING;
    }
}
