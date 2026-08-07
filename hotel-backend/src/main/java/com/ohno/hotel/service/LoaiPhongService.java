package com.ohno.hotel.service;

import com.ohno.hotel.entity.LoaiPhong;
import com.ohno.hotel.repository.LoaiPhongRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class LoaiPhongService {
    private final LoaiPhongRepository repo;
    public LoaiPhongService(LoaiPhongRepository repo) { this.repo = repo; }

    public List<LoaiPhong> getAll() { return repo.findAll(); }
    public LoaiPhong getById(String id) { return repo.findById(id).orElse(null); }

    @Transactional
    public String them(LoaiPhong lp) {
        if (lp.getTenLoaiPhong() == null || lp.getTenLoaiPhong().trim().isEmpty())
            return "Tên loại phòng không được trống!";
        if (lp.getGiaTheoNgay() <= 0) return "Giá phải lớn hơn 0!";
        repo.save(lp);
        return null;
    }

    @Transactional
    public String sua(LoaiPhong lp) {
        if (lp.getTenLoaiPhong() == null || lp.getTenLoaiPhong().trim().isEmpty())
            return "Tên loại phòng không được trống!";
        if (!repo.existsById(lp.getMaLoaiPhong())) return "Loại phòng không tồn tại!";
        repo.save(lp);
        return null;
    }

    @Transactional
    public String xoa(String id) {
        if (!repo.existsById(id)) return "Không tìm thấy loại phòng!";
        repo.deleteById(id);
        return null;
    }
}
