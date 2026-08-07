package dao;

import database.DatabaseConnection;
import entity.NhanVien;
import entity.TaiKhoan;
import entity.enums.VaiTro;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

public class NhanVienDAO {

    private static final Logger LOGGER = Logger.getLogger(NhanVienDAO.class.getName());

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public NhanVien getByMaNV(String maNV) {
        String sql = "SELECT nv.*, tk.tenDangNhap, tk.vaiTro, tk.trangThai " +
                "FROM NhanVien nv " +
                "LEFT JOIN TaiKhoan tk ON nv.maNhanVien = tk.maNhanVien " +
                "WHERE nv.maNhanVien=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maNV);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in NhanVienDAO", e);
        }
        return null;
    }

    public NhanVien getById(String id) {
        return getByMaNV(id);
    }

    public List<NhanVien> getAll() {
        List<NhanVien> list = new ArrayList<>();
        String sql = "SELECT nv.*, tk.tenDangNhap, tk.vaiTro, tk.trangThai " +
                "FROM NhanVien nv " +
                "LEFT JOIN TaiKhoan tk ON nv.maNhanVien = tk.maNhanVien " +
                "WHERE nv.dangLamViec = 1 " +
                "ORDER BY nv.hoTen";
        try (Statement st = getConn().createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in NhanVienDAO", e);
        }
        return list;
    }

    public List<NhanVien> search(String keyword, String filterRole) {
        List<NhanVien> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder(
                "SELECT nv.*, tk.tenDangNhap, tk.vaiTro, tk.trangThai " +
                        "FROM NhanVien nv " +
                        "LEFT JOIN TaiKhoan tk ON nv.maNhanVien = tk.maNhanVien " +
                        "WHERE nv.dangLamViec = 1");
        if (keyword != null && !keyword.isBlank())
            sb.append(" AND (nv.hoTen LIKE ? OR nv.sdt LIKE ? OR nv.maNhanVien LIKE ?)");
        if (filterRole != null && !filterRole.equals("Tất cả"))
            sb.append(" AND tk.vaiTro = ?");

        sb.append(" ORDER BY nv.hoTen");
        try (PreparedStatement ps = getConn().prepareStatement(sb.toString())) {
            int idx = 1;
            if (keyword != null && !keyword.isBlank()) {
                String kw = "%" + keyword + "%";
                ps.setString(idx++, kw);
                ps.setString(idx++, kw);
                ps.setString(idx++, kw);
            }
            if (filterRole != null && !filterRole.equals("Tất cả"))
                ps.setString(idx, filterRole);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in NhanVienDAO", e);
        }
        return list;
    }

    public boolean insert(NhanVien nv) {
        String sqlNV = "INSERT INTO NhanVien(maNhanVien, hoTen, sdt, chucVu, email, cccd, ngaySinh, gioiTinh, diaChi, ngayVaoLam, dangLamViec) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        String sqlTK = "INSERT INTO TaiKhoan(tenDangNhap, matKhau, vaiTro, trangThai, maNhanVien) VALUES(?,?,?,1,?)";
        try {
            Connection conn = getConn();
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sqlNV)) {
                    ps.setString(1, nv.getMaNhanVien());
                    ps.setString(2, nv.getHoTen());
                    ps.setString(3, nv.getSdt());
                    ps.setString(4, nv.getChucVu());
                    ps.setString(5, nv.getEmail());
                    ps.setString(6, nv.getCccd());
                    ps.setDate(7, nv.getNgaySinh() != null ? java.sql.Date.valueOf(nv.getNgaySinh()) : null);
                    ps.setString(8, nv.getGioiTinh());
                    ps.setString(9, nv.getDiaChi());
                    ps.setDate(10, nv.getNgayVaoLam() != null ? java.sql.Date.valueOf(nv.getNgayVaoLam()) : null);
                    ps.setBoolean(11, nv.isDangLamViec());
                    ps.executeUpdate();
                }
                TaiKhoan tk = nv.getTaiKhoan();
                if (tk != null) {
                    try (PreparedStatement ps = conn.prepareStatement(sqlTK)) {
                        ps.setString(1, tk.getTenDangNhap() != null ? tk.getTenDangNhap() : nv.getMaNhanVien());
                        ps.setString(2, tk.getMatKhau() != null ? tk.getMatKhau() : "123456");
                        String role = tk.getVaiTro() != null ? tk.getVaiTro().name() : "RECEPTIONIST";
                        ps.setString(3, role);
                        ps.setString(4, nv.getMaNhanVien());
                        ps.executeUpdate();
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Error inserting NhanVien", ex);
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in NhanVienDAO insert", e);
            return false;
        }
    }

    public boolean update(NhanVien nv) {
        String sqlNV = "UPDATE NhanVien SET hoTen=?, sdt=?, chucVu=?, email=?, cccd=?, ngaySinh=?, gioiTinh=?, diaChi=?, ngayVaoLam=?, dangLamViec=? WHERE maNhanVien=?";
        try {
            Connection conn = getConn();
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sqlNV)) {
                    ps.setString(1, nv.getHoTen());
                    ps.setString(2, nv.getSdt());
                    ps.setString(3, nv.getChucVu());
                    ps.setString(4, nv.getEmail());
                    ps.setString(5, nv.getCccd());
                    ps.setDate(6, nv.getNgaySinh() != null ? java.sql.Date.valueOf(nv.getNgaySinh()) : null);
                    ps.setString(7, nv.getGioiTinh());
                    ps.setString(8, nv.getDiaChi());
                    ps.setDate(9, nv.getNgayVaoLam() != null ? java.sql.Date.valueOf(nv.getNgayVaoLam()) : null);
                    ps.setBoolean(10, nv.isDangLamViec());
                    ps.setString(11, nv.getMaNhanVien());
                    ps.executeUpdate();
                }
                TaiKhoan tk = nv.getTaiKhoan();
                if (tk != null) {
                    boolean hasPw = tk.getMatKhau() != null && !tk.getMatKhau().isBlank();
                    String sqlTK = "UPDATE TaiKhoan SET vaiTro=?, trangThai=?" + (hasPw ? ", matKhau=?" : "") + " WHERE maNhanVien=?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlTK)) {
                        int idx = 1;
                        ps.setString(idx++, tk.getVaiTro() != null ? tk.getVaiTro().name() : "RECEPTIONIST");
                        ps.setBoolean(idx++, nv.isDangLamViec());
                        if (hasPw)
                            ps.setString(idx++, tk.getMatKhau());
                        ps.setString(idx, nv.getMaNhanVien());
                        ps.executeUpdate();
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Error updating NhanVien", ex);
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in NhanVienDAO update", e);
            return false;
        }
    }

    public boolean delete(String maNhanVien) {
        // Soft delete: set dangLamViec = 0 and deactivate account
        try {
            Connection conn = getConn();
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE TaiKhoan SET trangThai=0 WHERE maNhanVien=?")) {
                    ps.setString(1, maNhanVien);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE NhanVien SET dangLamViec=0 WHERE maNhanVien=?")) {
                    ps.setString(1, maNhanVien);
                    int res = ps.executeUpdate();
                    conn.commit();
                    return res > 0;
                }
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in NhanVienDAO soft delete", e);
            return false;
        }
    }

    public String generateMaNV() {
        try (Statement st = getConn().createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT MAX(CAST(SUBSTRING(maNhanVien,3,10) AS INT)) FROM NhanVien WHERE maNhanVien LIKE 'NV%'")) {
            if (rs.next())
                return String.format("NV%03d", rs.getInt(1) + 1);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in NhanVienDAO", e);
        }
        return "NV001";
    }

    public boolean isSdtExists(String sdt, String excludeMaNV) {
        String sql = "SELECT COUNT(*) FROM NhanVien WHERE sdt = ?" + (excludeMaNV != null ? " AND maNhanVien <> ?" : "");
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, sdt);
            if (excludeMaNV != null)
                ps.setString(2, excludeMaNV);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt(1) > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking SDT exists", e);
        }
        return false;
    }

    public boolean isCccdExists(String cccd, String excludeMaNV) {
        String sql = "SELECT COUNT(*) FROM NhanVien WHERE cccd = ?" + (excludeMaNV != null ? " AND maNhanVien <> ?" : "");
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, cccd);
            if (excludeMaNV != null)
                ps.setString(2, excludeMaNV);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt(1) > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking CCCD exists", e);
        }
        return false;
    }

    private NhanVien mapRow(ResultSet rs) throws SQLException {
        NhanVien nv = new NhanVien();
        nv.setMaNhanVien(rs.getString("maNhanVien"));
        nv.setHoTen(rs.getString("hoTen"));
        nv.setSdt(rs.getString("sdt"));
        nv.setChucVu(rs.getString("chucVu"));
        nv.setEmail(rs.getString("email"));
        nv.setCccd(rs.getString("cccd"));
        Date birth = rs.getDate("ngaySinh");
        if (birth != null) nv.setNgaySinh(birth.toLocalDate());
        nv.setGioiTinh(rs.getString("gioiTinh"));
        nv.setDiaChi(rs.getString("diaChi"));
        Date join = rs.getDate("ngayVaoLam");
        if (join != null) nv.setNgayVaoLam(join.toLocalDate());
        nv.setDangLamViec(rs.getBoolean("dangLamViec"));

        // Map account if exists
        String tenDN = rs.getString("tenDangNhap");
        if (tenDN != null) {
            TaiKhoan tk = new TaiKhoan();
            tk.setTenDangNhap(tenDN);
            String roleStr = rs.getString("vaiTro");
            if (roleStr != null) {
                try {
                    tk.setVaiTro(VaiTro.valueOf(roleStr));
                } catch (Exception e) {
                    tk.setVaiTro(VaiTro.RECEPTIONIST);
                }
            }
            tk.setTrangThai(rs.getBoolean("trangThai"));
            tk.setNhanVien(nv);
            nv.setTaiKhoan(tk);
        }

        return nv;
    }
}


