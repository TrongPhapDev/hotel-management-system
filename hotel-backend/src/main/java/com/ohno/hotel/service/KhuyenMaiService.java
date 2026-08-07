package com.ohno.hotel.service;

import com.ohno.hotel.entity.KhuyenMai;
import com.ohno.hotel.repository.KhuyenMaiRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class KhuyenMaiService {
    private final KhuyenMaiRepository repo;
    public KhuyenMaiService(KhuyenMaiRepository repo) { this.repo = repo; }

    public List<KhuyenMai> getAll() { return repo.findAll(); }
    public KhuyenMai getById(String id) { return repo.findById(id).orElse(null); }
    public List<KhuyenMai> getActive() { return repo.findActive(LocalDateTime.now()); }

    @Transactional
    public String them(KhuyenMai km) {
        if (km.getMaKhuyenMai() == null || km.getMaKhuyenMai().trim().isEmpty())
            return "Mã khuyến mãi không được trống!";
        if (repo.existsById(km.getMaKhuyenMai())) return "Mã khuyến mãi đã tồn tại!";
        if (km.getGiaTriGiam() <= 0) return "Giá trị giảm phải lớn hơn 0!";
        repo.save(km);
        return null;
    }

    @Transactional
    public String sua(KhuyenMai km) {
        if (!repo.existsById(km.getMaKhuyenMai())) return "Khuyến mãi không tồn tại!";
        repo.save(km);
        return null;
    }

    @Transactional
    public String xoa(String id) {
        if (!repo.existsById(id)) return "Không tìm thấy khuyến mãi!";
        repo.deleteById(id);
        return null;
    }
}
