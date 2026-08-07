package com.ohno.hotel.service;

import com.ohno.hotel.entity.HoaDon;
import com.ohno.hotel.repository.HoaDonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class HoaDonService {
    private final HoaDonRepository repo;
    public HoaDonService(HoaDonRepository repo) { this.repo = repo; }

    public List<HoaDon> getAll() { return repo.findAll(); }
    public HoaDon getById(String id) { return repo.findById(id).orElse(null); }
    public HoaDon getByDatPhong(String maDatPhong) {
        return repo.findByDatPhong_MaDatPhong(maDatPhong).orElse(null);
    }
    public double getTongDoanhThu() { return repo.getTongDoanhThu(); }
    public double getDoanhThuThang(int month, int year) { return repo.getDoanhThuTheoThang(month, year); }

    public String generateMa() {
        Integer max = repo.getMaxId();
        return String.format("HD%05d", (max != null ? max : 0) + 1);
    }

    @Transactional
    public String taoHoaDon(HoaDon hd) {
        if (hd.getDatPhong() == null) return "Vui lòng chỉ định đặt phòng!";
        if (hd.getMaHoaDon() == null || hd.getMaHoaDon().trim().isEmpty())
            hd.setMaHoaDon(generateMa());
        if (hd.getNgayLap() == null) hd.setNgayLap(LocalDateTime.now());
        repo.save(hd);
        return null;
    }

    @Transactional
    public String thanhToan(String maHoaDon, String phuongThuc) {
        HoaDon hd = getById(maHoaDon);
        if (hd == null) return "Không tìm thấy hóa đơn!";
        hd.setTrangThai("PAID");
        hd.setPhuongThucThanhToan(phuongThuc);
        repo.save(hd);
        return null;
    }

    @Transactional
    public String capNhat(HoaDon hd) {
        if (!repo.existsById(hd.getMaHoaDon())) return "Hóa đơn không tồn tại!";
        repo.save(hd);
        return null;
    }
}
