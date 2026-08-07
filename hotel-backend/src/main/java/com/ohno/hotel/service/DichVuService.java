package com.ohno.hotel.service;

import com.ohno.hotel.entity.DichVu;
import com.ohno.hotel.repository.DichVuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DichVuService {

    private final DichVuRepository repository;

    public DichVuService(DichVuRepository repository) {
        this.repository = repository;
    }

    public List<DichVu> getAll() {
        return repository.findByTrangThaiGreaterThanEqualOrderByTenDichVu(0);
    }

    public DichVu getById(String maDichVu) {
        return repository.findById(maDichVu).orElse(null);
    }

    public double getGiaTrungBinh() {
        Double avg = repository.getGiaTrungBinh();
        return avg != null ? avg : 0.0;
    }

    public String generateMa() {
        Integer maxId = repository.getMaxNumericId();
        if (maxId != null) {
            return String.format("DV%03d", maxId + 1);
        }
        return "DV001";
    }

    @Transactional
    public String them(DichVu dv) {
        if (dv.getTenDichVu() == null || dv.getTenDichVu().trim().isEmpty()) {
            return "Tên dịch vụ không được trống!";
        }
        if (dv.getDonGia() <= 0) {
            return "Giá phải lớn hơn 0!";
        }
        if (dv.getMaDichVu() == null || dv.getMaDichVu().trim().isEmpty()) {
            dv.setMaDichVu(generateMa());
        } else if (repository.existsById(dv.getMaDichVu())) {
            return "Mã dịch vụ đã tồn tại!";
        }
        dv.setTrangThai(1); // Hoạt động mặc định khi thêm mới
        repository.save(dv);
        return null;
    }

    @Transactional
    public String sua(DichVu dv) {
        if (dv.getTenDichVu() == null || dv.getTenDichVu().trim().isEmpty()) {
            return "Tên dịch vụ không được trống!";
        }
        if (dv.getDonGia() <= 0) {
            return "Giá phải lớn hơn 0!";
        }
        if (!repository.existsById(dv.getMaDichVu())) {
            return "Dịch vụ không tồn tại để cập nhật!";
        }
        repository.save(dv);
        return null;
    }

    @Transactional
    public String xoa(String maDichVu) {
        DichVu dv = getById(maDichVu);
        if (dv == null) {
            return "Dịch vụ không tồn tại!";
        }
        dv.setTrangThai(-1); // Đánh dấu xóa logic
        repository.save(dv);
        return null;
    }

    public List<DichVu> search(String kw, String type) {
        String queryKw = (kw != null && !kw.trim().isEmpty()) ? kw.trim() : null;
        String queryLoai = (type != null && !type.equals("Tất cả loại")) ? type : null;
        return repository.searchDichVu(queryKw, queryLoai);
    }
}
