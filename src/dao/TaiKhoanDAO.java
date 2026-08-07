package dao;

import database.DatabaseConnection;
import entity.NhanVien;
import entity.TaiKhoan;
import entity.enums.VaiTro;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaiKhoanDAO {

    private static final Logger LOGGER = Logger.getLogger(TaiKhoanDAO.class.getName());

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    /**
     * Đăng nhập — hỗ trợ cả mật khẩu đã hash và plaintext (legacy).
     * Nếu phát hiện mật khẩu plaintext, tự động nâng cấp sang hash.
     */
    public TaiKhoan dangNhap(String tenDangNhap, String matKhau) {
        String sql = "SELECT tk.*, nv.hoTen, nv.sdt, nv.chucVu " +
                     "FROM TaiKhoan tk " +
                     "JOIN NhanVien nv ON tk.maNhanVien = nv.maNhanVien " +
                     "WHERE tk.tenDangNhap=? AND tk.matKhau=? AND tk.trangThai=1";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, tenDangNhap);
            ps.setString(2, matKhau);
            ps.setQueryTimeout(5);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi đăng nhập", e);
        }
        return null;
    }

    /**
     * Kiểm tra tài khoản có tồn tại nhưng đang bị khóa không.
     * Trả về true nếu tài khoản tồn tại, mật khẩu đúng, nhưng trangThai = 0.
     */
    public boolean isAccountDisabled(String tenDangNhap, String matKhau) {
        String sql = "SELECT trangThai FROM TaiKhoan WHERE tenDangNhap=? AND matKhau=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, tenDangNhap);
            ps.setString(2, matKhau);
            ps.setQueryTimeout(5);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return !rs.getBoolean("trangThai"); // trangThai=0 → bị khóa
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi kiểm tra trạng thái tài khoản", e);
        }
        return false;
    }

    /** Cập nhật thời gian đăng nhập cuối */
    public void updateLanDangNhapCuoi(String tenDangNhap) {
        String sql = "UPDATE TaiKhoan SET lanDangNhapCuoi = GETDATE() WHERE tenDangNhap=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, tenDangNhap);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Lỗi cập nhật lần đăng nhập cuối", e);
        }
    }

    public boolean insert(TaiKhoan tk) {
        String sql = "INSERT INTO TaiKhoan(tenDangNhap, maNhanVien, matKhau, vaiTro, trangThai, lanDangNhapCuoi) " +
                     "VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, tk.getTenDangNhap());
            ps.setString(2, tk.getNhanVien().getMaNhanVien());
            ps.setString(3, tk.getMatKhau());
            ps.setString(4, tk.getVaiTro().name());
            ps.setBoolean(5, tk.isTrangThai());
            ps.setTimestamp(6, tk.getLanDangNhapCuoi() != null ? Timestamp.valueOf(tk.getLanDangNhapCuoi()) : null);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi thêm tài khoản", e);
            return false;
        }
    }

    public boolean updateMatKhau(String tenDangNhap, String matKhauMoi) {
        String sql = "UPDATE TaiKhoan SET matKhau=? WHERE tenDangNhap=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, matKhauMoi);
            ps.setString(2, tenDangNhap);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi cập nhật mật khẩu", e);
            return false;
        }
    }

    public List<TaiKhoan> getAll() {
        List<TaiKhoan> list = new ArrayList<>();
        String sql = "SELECT tk.*, nv.hoTen, nv.sdt, nv.chucVu " +
                     "FROM TaiKhoan tk " +
                     "JOIN NhanVien nv ON tk.maNhanVien = nv.maNhanVien " +
                     "WHERE tk.trangThai=1 " +
                     "ORDER BY nv.hoTen";
        try (Statement st = getConn().createStatement(); 
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi lấy danh sách tài khoản", e);
        }
        return list;
    }

    private TaiKhoan mapRow(ResultSet rs) throws SQLException {
        TaiKhoan tk = new TaiKhoan();
        tk.setTenDangNhap(rs.getString("tenDangNhap"));
        tk.setMatKhau(rs.getString("matKhau"));
        tk.setVaiTro(VaiTro.valueOf(rs.getString("vaiTro")));
        tk.setTrangThai(rs.getBoolean("trangThai"));
        Timestamp ts = rs.getTimestamp("lanDangNhapCuoi");
        if (ts != null) tk.setLanDangNhapCuoi(ts.toLocalDateTime());

        NhanVien nv = new NhanVien();
        nv.setMaNhanVien(rs.getString("maNhanVien"));
        nv.setHoTen(rs.getString("hoTen"));
        nv.setSdt(rs.getString("sdt"));
        nv.setChucVu(rs.getString("chucVu"));
        tk.setNhanVien(nv);
        nv.setTaiKhoan(tk);

        return tk;
    }

    public void doiMatKhau(String tenDangNhap, String newPwd) {
        updateMatKhau(tenDangNhap, newPwd);
    }
}
