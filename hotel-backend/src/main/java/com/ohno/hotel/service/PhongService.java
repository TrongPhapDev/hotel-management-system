package com.ohno.hotel.service;

import com.ohno.hotel.entity.Phong;
import com.ohno.hotel.entity.ChiTietDatPhong;
import com.ohno.hotel.repository.PhongRepository;
import com.ohno.hotel.repository.ChiTietDatPhongRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PhongService {
    private final PhongRepository repo;
    private final ChiTietDatPhongRepository ctdpRepo;

    public PhongService(PhongRepository repo, ChiTietDatPhongRepository ctdpRepo) {
        this.repo = repo;
        this.ctdpRepo = ctdpRepo;
    }

    private void populateTenKhachHienTai(Phong p) {
        if (p != null && "OCCUPIED".equals(p.getTrangThai())) {
            List<ChiTietDatPhong> stays = ctdpRepo.findByPhong_MaPhong(p.getMaPhong());
            for (ChiTietDatPhong stay : stays) {
                if (stay.getNgayNhanThucTe() != null && stay.getNgayTraThucTe() == null && !Boolean.TRUE.equals(stay.getDaThanhToan())) {
                    if (stay.getKhachHang() != null) {
                        p.setTenKhachHienTai(stay.getKhachHang().getHoTen());
                    }
                    break;
                }
            }
        }
    }

    public List<Phong> getAll() {
        List<Phong> list = repo.findAll();
        list.forEach(this::populateTenKhachHienTai);
        return list;
    }

    public Phong getById(String id) {
        Phong p = repo.findById(id).orElse(null);
        populateTenKhachHienTai(p);
        return p;
    }

    public List<Phong> getByTrangThai(String tt) {
        List<Phong> list = repo.findByTrangThai(tt);
        list.forEach(this::populateTenKhachHienTai);
        return list;
    }

    public List<Phong> getAvailable() {
        List<Phong> list = repo.findByTrangThai("AVAILABLE");
        list.forEach(this::populateTenKhachHienTai);
        return list;
    }

    public Map<String, Long> thongKeTrangThai() {
        Map<String, Long> map = new HashMap<>();
        map.put("AVAILABLE", repo.countByTrangThai("AVAILABLE"));
        map.put("OCCUPIED",  repo.countByTrangThai("OCCUPIED"));
        map.put("MAINTENANCE", repo.countByTrangThai("MAINTENANCE"));
        map.put("CLEANING",  repo.countByTrangThai("CLEANING"));
        return map;
    }

    @Transactional
    public String them(Phong p) {
        if (p.getMaPhong() == null || p.getMaPhong().trim().isEmpty())
            return "Mã phòng không được trống!";
        if (repo.existsById(p.getMaPhong())) return "Mã phòng đã tồn tại!";
        repo.save(p);
        return null;
    }

    @Transactional
    public String sua(Phong p) {
        if (!repo.existsById(p.getMaPhong())) return "Phòng không tồn tại!";
        repo.save(p);
        return null;
    }

    @Transactional
    public String capNhatTrangThai(String maPhong, String trangThai) {
        Phong p = getById(maPhong);
        if (p == null) return "Không tìm thấy phòng!";
        p.setTrangThai(trangThai);
        repo.save(p);
        return null;
    }

    @Transactional
    public String xoa(String id) {
        if (!repo.existsById(id)) return "Không tìm thấy phòng!";
        repo.deleteById(id);
        return null;
    }

    public List<Phong> findAvailableRoomsInRange(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        return repo.findAvailableRoomsInRange(start, end);
    }
}
