package service;

import dao.DichVuDAO;
import entity.*;
import java.util.*;

public class DichVuService {
    private final DichVuDAO dao = new DichVuDAO();

    public List<DichVu> getAll() { return dao.getAll(); }
    public List<DichVu> getActive() { return dao.getActive(); }
    public List<DichVu> search(String kw, String type, String status) { return dao.search(kw, type, status); }

    public DichVu getById(String maDichVu) { return dao.getById(maDichVu); }

    public int countAll() { return dao.countAll(); }
    public int countActive() { return dao.countActive(); }
    public int countSuspended() { return dao.countSuspended(); }
    public double getGiaTrungBinh() { return dao.getGiaTrungBinh(); }

    public String them(DichVu dv) {
        if (dv.getTenDichVu() == null || dv.getTenDichVu().isBlank()) return "Tên dịch vụ không được trống!";
        if (dv.getDonGia() <= 0) return "Giá phải lớn hơn 0!";
        
        // Auto-generate ID if not provided
        if (dv.getMaDichVu() == null || dv.getMaDichVu().isBlank()) {
            dv.setMaDichVu(dao.generateMa());
        }
        
        return dao.insert(dv) ? null : "Lỗi thêm dịch vụ!";
    }

    public String sua(DichVu dv) {
        if (dv.getTenDichVu() == null || dv.getTenDichVu().isBlank()) return "Tên dịch vụ không được trống!";
        return dao.update(dv) ? null : "Lỗi cập nhật!";
    }

    public String xoa(String maDichVu) {
        return dao.delete(maDichVu) ? null : "Không thể xóa!";
    }
}
