package service;

import dao.NhanVienDAO;
import entity.NhanVien;
import entity.TaiKhoan;
import entity.enums.VaiTro;
import java.util.List;

public class NhanVienService {
    private final NhanVienDAO dao = new NhanVienDAO();

    public List<NhanVien> getAll() {
        return dao.getAll();
    }

    public List<NhanVien> search(String kw, String chucVu) {
        return dao.search(kw, chucVu);
    }

    public NhanVien getById(String maNhanVien) {
        return dao.getById(maNhanVien);
    }

    public String them(NhanVien nv) {
        if (nv.getHoTen() == null || nv.getHoTen().isBlank())
            return "Họ tên không được trống!";
        if (nv.getSdt() == null || nv.getSdt().isBlank())
            return "Số điện thoại không được trống!";
        if (dao.isSdtExists(nv.getSdt().trim(), null))
            return "Số điện thoại này đã được sử dụng bởi nhân viên khác!";
        
        if (nv.getCccd() != null && !nv.getCccd().isBlank()) {
            if (dao.isCccdExists(nv.getCccd().trim(), null))
                return "Số CCCD này đã được sử dụng bởi nhân viên khác!";
        }

        if (nv.getMaNhanVien() == null || nv.getMaNhanVien().isBlank()) {
            nv.setMaNhanVien(dao.generateMaNV());
        }

        if (nv.getTaiKhoan() == null) {
            TaiKhoan tk = new TaiKhoan();
            tk.setTenDangNhap(nv.getMaNhanVien());
            tk.setMatKhau("123456");
            tk.setVaiTro(VaiTro.RECEPTIONIST);
            nv.setTaiKhoan(tk);
            tk.setNhanVien(nv);
        } else {
            // Đảm bảo tenDangNhap luôn được set
            if (nv.getTaiKhoan().getTenDangNhap() == null || nv.getTaiKhoan().getTenDangNhap().isBlank()) {
                nv.getTaiKhoan().setTenDangNhap(nv.getMaNhanVien());
            }
            nv.getTaiKhoan().setNhanVien(nv);
        }

        return dao.insert(nv) ? null : "Lỗi thêm nhân viên!";
    }

    public String sua(NhanVien nv) {
        if (nv.getHoTen() == null || nv.getHoTen().isBlank())
            return "Họ tên không được trống!";
        
        if (nv.getSdt() != null && !nv.getSdt().isBlank()) {
            if (dao.isSdtExists(nv.getSdt().trim(), nv.getMaNhanVien()))
                return "Số điện thoại này đã được sử dụng bởi nhân viên khác!";
        }
        
        if (nv.getCccd() != null && !nv.getCccd().isBlank()) {
            if (dao.isCccdExists(nv.getCccd().trim(), nv.getMaNhanVien()))
                return "Số CCCD này đã được sử dụng bởi nhân viên khác!";
        }

        return dao.update(nv) ? null : "Lỗi cập nhật!";
    }

    public String xoa(String maNhanVien) {
        return dao.delete(maNhanVien) ? null : "Không thể xóa nhân viên này!";
    }
}
