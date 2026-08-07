package service;

import dao.KhachHangDAO;
import entity.KhachHang;
import java.util.*;

public class KhachHangService {
    private final KhachHangDAO dao = new KhachHangDAO();

    public List<KhachHang> getAll() { return dao.getAll(); }

    public List<KhachHang> search(String keyword) { return dao.timKiem(keyword); }

    public List<KhachHang> search(String keyword, String dummy) { return search(keyword); }

    public KhachHang getById(String maKhachHang) { return dao.getById(maKhachHang); }

    public KhachHang getByCCCD(String cccd) { return dao.getByCCCD(cccd); }

    public KhachHang getByPhone(String sdt) { return dao.getByPhone(sdt); }

    public String them(KhachHang kh) {
        if (kh.getHoTen() == null || kh.getHoTen().isBlank()) return "Họ tên không được trống!";
        if (kh.getSdt() == null || kh.getSdt().isBlank()) return "Số điện thoại không được trống!";
        if (kh.getMaKhachHang() == null || kh.getMaKhachHang().isBlank()) {
            kh.setMaKhachHang(dao.generateMaKH());
        }
        return dao.insert(kh) ? null : "Lỗi thêm khách hàng!";
    }

    public String sua(KhachHang kh) {
        if (kh.getHoTen() == null || kh.getHoTen().isBlank()) return "Họ tên không được trống!";
        return dao.update(kh) ? null : "Lỗi cập nhật!";
    }

    public String xoa(String maKhachHang) {
        return dao.delete(maKhachHang) ? null : "Không thể xóa!";
    }

    /**
     * Cập nhật số lần ở và doanh thu đóng góp của khách hàng.
     * Thường gọi sau khi thanh toán hóa đơn.
     */
    public boolean capNhatThongKe(String maKh, double chiTieuThem, boolean tangSoLanO) {
        if (maKh == null || maKh.isEmpty()) return false;
        return dao.updateCRMStats(maKh, chiTieuThem, tangSoLanO);
    }
}
