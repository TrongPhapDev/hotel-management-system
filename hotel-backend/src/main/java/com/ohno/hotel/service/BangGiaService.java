package com.ohno.hotel.service;

import com.ohno.hotel.entity.BangGia;
import com.ohno.hotel.entity.ChiTietBangGia;
import com.ohno.hotel.entity.LoaiPhong;
import com.ohno.hotel.repository.BangGiaRepository;
import com.ohno.hotel.repository.ChiTietBangGiaRepository;
import com.ohno.hotel.repository.LoaiPhongRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BangGiaService {
    private final BangGiaRepository repo;
    private final ChiTietBangGiaRepository ctRepo;
    private final LoaiPhongRepository lpRepo;

    public BangGiaService(BangGiaRepository repo, ChiTietBangGiaRepository ctRepo, LoaiPhongRepository lpRepo) {
        this.repo = repo;
        this.ctRepo = ctRepo;
        this.lpRepo = lpRepo;
    }

    public List<BangGia> getAll() { return repo.findAll(); }
    public BangGia getById(String id) { return repo.findById(id).orElse(null); }

    public String generateMa() {
        Integer max = repo.getMaxId();
        return String.format("BG%04d", (max != null ? max : 0) + 1);
    }

    @Transactional
    public String them(BangGia bg) {
        if (bg.getMaBangGia() == null || bg.getMaBangGia().trim().isEmpty()) {
            bg.setMaBangGia(generateMa());
        }
        repo.save(bg);
        return null;
    }

    @Transactional
    public String sua(BangGia bg) {
        if (!repo.existsById(bg.getMaBangGia())) return "Bảng giá không tồn tại!";
        repo.save(bg);
        return null;
    }

    @Transactional
    public String xoa(String id) {
        if (!repo.existsById(id)) return "Bảng giá không tồn tại!";
        ctRepo.deleteByBangGia_MaBangGia(id);
        repo.deleteById(id);
        return null;
    }

    public List<ChiTietBangGia> getChiTiet(String maBangGia) {
        return ctRepo.findByBangGia_MaBangGia(maBangGia);
    }

    @Transactional
    public String saveChiTiet(String maBangGia, List<ChiTietBangGia> dsCT) {
        ctRepo.deleteByBangGia_MaBangGia(maBangGia);
        BangGia bg = repo.findById(maBangGia).orElse(null);
        if (bg == null) return "Bảng giá không tồn tại!";
        for (ChiTietBangGia ct : dsCT) {
            ct.setBangGia(bg);
            ctRepo.save(ct);
        }
        return null;
    }

    public double layGiaHienHanh(String maLoaiPhong) {
        return layGiaVaoThoiDiem(maLoaiPhong, LocalDateTime.now());
    }

    public double layGiaVaoThoiDiem(String maLoaiPhong, LocalDateTime thoiDiem) {
        List<BangGia> list = repo.findActiveAtTimeAndDoiTuong(thoiDiem, "ALL");
        if (!list.isEmpty()) {
            BangGia active = list.get(0); // Top mucUuTien
            ChiTietBangGia ct = ctRepo.findByBangGia_MaBangGiaAndLoaiPhong_MaLoaiPhong(active.getMaBangGia(), maLoaiPhong);
            if (ct != null) {
                java.time.DayOfWeek day = thoiDiem.getDayOfWeek();
                double gia;
                if ((day == java.time.DayOfWeek.SATURDAY || day == java.time.DayOfWeek.SUNDAY) && ct.getGiaCuoiTuan() > 0) {
                    gia = ct.getGiaCuoiTuan();
                } else {
                    gia = ct.getGiaNgay();
                }
                if (gia > 0) return gia;
            }
        }
        // Fallback to base LoaiPhong price
        LoaiPhong lp = lpRepo.findById(maLoaiPhong).orElse(null);
        return lp != null ? lp.getGiaTheoNgay() : 0;
    }

    public double layGiaTheoGio(String maLoaiPhong) {
        List<BangGia> list = repo.findActiveAtTime(LocalDateTime.now());
        if (!list.isEmpty()) {
            BangGia active = list.get(0);
            ChiTietBangGia ct = ctRepo.findByBangGia_MaBangGiaAndLoaiPhong_MaLoaiPhong(active.getMaBangGia(), maLoaiPhong);
            if (ct != null && ct.getGiaGioDau() > 0) return ct.getGiaGioDau();
        }
        return 0;
    }

    public double layPhuPhiTraTre(String maLoaiPhong) {
        List<BangGia> list = repo.findActiveAtTime(LocalDateTime.now());
        if (!list.isEmpty()) {
            BangGia active = list.get(0);
            ChiTietBangGia ct = ctRepo.findByBangGia_MaBangGiaAndLoaiPhong_MaLoaiPhong(active.getMaBangGia(), maLoaiPhong);
            if (ct != null) return ct.getPhuPhiTraTre();
        }
        return 0;
    }
}
