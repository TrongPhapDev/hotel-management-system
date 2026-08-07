package service;

import dao.PhongDAO;
import dao.LoaiPhongDAO;
import dao.HuongNhinDAO;
import dao.TienNghiDAO;
import entity.Phong;
import entity.LoaiPhong;
import entity.HuongNhin;
import entity.TienNghi;
import entity.enums.TrangThaiPhong;

import java.util.*;

public class PhongService {

    private final PhongDAO phongDAO         = new PhongDAO();
    private final LoaiPhongDAO loaiPhongDAO = new LoaiPhongDAO();
    private final HuongNhinDAO huongNhinDAO = new HuongNhinDAO();
    private final TienNghiDAO tienNghiDAO   = new TienNghiDAO();

    // ============================================================
    //  Phòng
    // ============================================================
    public List<Phong> getAllPhong() { return phongDAO.getAll(); }

    public List<Phong> searchPhong(String keyword, String trangThai, String viTri) {
        return phongDAO.search(keyword, trangThai, viTri);
    }

    public Phong getPhongById(String maPhong) { return phongDAO.getById(maPhong); }

    public String themPhong(Phong p) {
        if (p.getMaPhong() == null || p.getMaPhong().isBlank()) return "Mã phòng không được trống!";
        if (phongDAO.getById(p.getMaPhong()) != null) return "Mã phòng đã tồn tại!";
        if (p.getLoaiPhong() == null || p.getLoaiPhong().getMaLoaiPhong() == null)
            return "Phải chọn loại phòng!";
        return phongDAO.insert(p) ? null : "Lỗi thêm phòng!";
    }

    public String suaPhong(Phong p) {
        if (!phongDAO.update(p)) return "Lỗi cập nhật phòng!";
        return null;
    }

    public String xoaPhong(String maPhong) {
        Phong p = phongDAO.getById(maPhong);
        if (p != null) {
            TrangThaiPhong tt = p.getTrangThai();
            if (tt == TrangThaiPhong.OCCUPIED) return "Không thể xóa phòng đang có khách!";
        }

        // Kiểm tra ràng buộc lịch sử đặt phòng
        int bookings = phongDAO.countBookingReferences(maPhong);
        if (bookings > 0) {
            return "Không thể xóa phòng đã có lịch sử giao dịch (" + bookings + " lượt lưu trú)! " +
                   "Hãy chuyển trạng thái sang Bảo trì hoặc Ngừng sử dụng để bảo toàn dữ liệu báo cáo.";
        }

        return phongDAO.delete(maPhong) ? null : "Không thể xóa phòng này!";
    }

    public boolean updateTrangThai(String maPhong, TrangThaiPhong trangThai) {
        Phong p = phongDAO.getById(maPhong);
        if (p == null) return false;
        p.setTrangThai(trangThai);
        return phongDAO.update(p);
    }

    public boolean updateTrangThai(String maPhong, String tt) {
        Phong p = phongDAO.getById(maPhong);
        if (p == null) return false;
        p.setTrangThai(tt);
        return phongDAO.update(p);
    }

    // ============================================================
    //  Loại phòng
    // ============================================================
    public List<LoaiPhong> getAllLoaiPhong() { return loaiPhongDAO.getAll(); }

    public LoaiPhong getLoaiPhongById(String maLoai) { return loaiPhongDAO.getById(maLoai); }

    public List<LoaiPhong> getActiveLoaiPhong() {
        return loaiPhongDAO.getActive();
    }

    public String themLoaiPhong(LoaiPhong lp) {
        if (lp.getTenLoaiPhong() == null || lp.getTenLoaiPhong().isBlank())
            return "Tên loại phòng không được trống!";
        if (lp.getGiaTheoNgay() <= 0)
            return "Giá cơ sở phải lớn hơn 0!";
        if (lp.getSucChua() <= 0)
            return "Sức chứa phải lớn hơn 0!";
        if (lp.getMaLoaiPhong() == null || lp.getMaLoaiPhong().isBlank()) {
            lp.setMaLoaiPhong(loaiPhongDAO.generateMa());
        }
        return loaiPhongDAO.insert(lp) ? null : "Lỗi thêm loại phòng!";
    }

    public String suaLoaiPhong(LoaiPhong lp) {
        if (lp.getTenLoaiPhong() == null || lp.getTenLoaiPhong().isBlank())
            return "Tên loại phòng không được trống!";
        if (lp.getGiaTheoNgay() <= 0)
            return "Giá cơ sở phải lớn hơn 0!";
        return loaiPhongDAO.update(lp) ? null : "Lỗi cập nhật loại phòng!";
    }

    public String xoaLoaiPhong(String maLoai) {
        int phongCount = loaiPhongDAO.countPhongByLoai(maLoai);
        if (phongCount > 0) {
            return "Không thể xóa! Còn " + phongCount + " phòng thuộc loại này.";
        }
        return loaiPhongDAO.delete(maLoai) ? null : "Không thể xóa loại phòng!";
    }

    public int countLoaiPhongNgung() {
        return (int) loaiPhongDAO.getAll().stream().filter(lp -> !lp.isDangKinhDoanh()).count();
    }

    public boolean isRoomAvailable(String maPhong, java.time.LocalDateTime from, java.time.LocalDateTime to) {
        return isRoomAvailable(maPhong, from, to, null);
    }

    public boolean isRoomAvailable(String maPhong, java.time.LocalDateTime from, java.time.LocalDateTime to, String excludeMaDatPhong) {
        return phongDAO.isRoomAvailable(maPhong, from, to, excludeMaDatPhong);
    }

    public Map<String, Integer> getThongKeTrangThai() {
        return phongDAO.getThongKeTrangThai();
    }

    public int countLoaiPhongActive() {
        return (int) loaiPhongDAO.getAll().stream().filter(LoaiPhong::isDangKinhDoanh).count();
    }

    // ============================================================
    //  Hướng nhìn (Room View)
    // ============================================================
    public List<HuongNhin> getAllHuongNhin() { return huongNhinDAO.getAll(); }

    public HuongNhin getHuongNhinById(String ma) { return huongNhinDAO.getById(ma); }

    public String themHuongNhin(HuongNhin hn) {
        if (hn.getTenHuongNhin() == null || hn.getTenHuongNhin().isBlank())
            return "Tên hướng nhìn không được trống!";
        if (hn.getMaHuongNhin() == null || hn.getMaHuongNhin().isBlank())
            hn.setMaHuongNhin(huongNhinDAO.generateMa());
        return huongNhinDAO.insert(hn) ? null : "Lỗi thêm hướng nhìn!";
    }

    public String suaHuongNhin(HuongNhin hn) {
        if (hn.getTenHuongNhin() == null || hn.getTenHuongNhin().isBlank())
            return "Tên hướng nhìn không được trống!";
        return huongNhinDAO.update(hn) ? null : "Lỗi cập nhật hướng nhìn!";
    }

    public String xoaHuongNhin(String ma) {
        int count = huongNhinDAO.countPhongByHuongNhin(ma);
        if (count > 0) return "Không thể xóa! Còn " + count + " phòng dùng hướng nhìn này.";
        return huongNhinDAO.delete(ma) ? null : "Không thể xóa hướng nhìn!";
    }

    // ============================================================
    //  Tiện nghi (Amenities)
    // ============================================================
    public List<TienNghi> getAllTienNghi() { return tienNghiDAO.getAll(); }

    public List<String> getAllNhomTienNghi() { return tienNghiDAO.getAllNhom(); }

    public List<TienNghi> getTienNghiByLoaiPhong(String maLoaiPhong) {
        return tienNghiDAO.getByLoaiPhong(maLoaiPhong);
    }

    public void updateTienNghiForLoaiPhong(String maLoaiPhong, List<String> maTienNghiList) {
        tienNghiDAO.updateTienNghiForLoaiPhong(maLoaiPhong, maTienNghiList);
    }

    /** Lấy chuỗi tiện nghi gộp cho hiển thị */
    public String getTienNghiString(String maLoaiPhong) {
        return tienNghiDAO.getTienNghiString(maLoaiPhong);
    }
}
