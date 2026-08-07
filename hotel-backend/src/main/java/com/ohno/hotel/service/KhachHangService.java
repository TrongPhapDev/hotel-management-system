package com.ohno.hotel.service;

import com.ohno.hotel.entity.KhachHang;
import com.ohno.hotel.repository.KhachHangRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class KhachHangService {
    private final KhachHangRepository repo;
    public KhachHangService(KhachHangRepository repo) { this.repo = repo; }

    public List<KhachHang> getAll() { return repo.findAll(); }
    public KhachHang getById(String id) { return repo.findById(id).orElse(null); }
    public List<KhachHang> search(String kw) { return repo.search(kw); }

    public String generateMa() {
        Integer max = repo.getMaxId();
        return String.format("KH%03d", (max != null ? max : 0) + 1);
    }

    @Transactional
    public String them(KhachHang kh) {
        if (kh.getHoTen() == null || kh.getHoTen().trim().isEmpty())
            return "Họ tên không được trống!";
        if (kh.getMaKhachHang() == null || kh.getMaKhachHang().trim().isEmpty())
            kh.setMaKhachHang(generateMa());
        else if (repo.existsById(kh.getMaKhachHang()))
            return "Mã khách hàng đã tồn tại!";
        repo.save(kh);
        return null;
    }

    @Transactional
    public String sua(KhachHang kh) {
        if (kh.getHoTen() == null || kh.getHoTen().trim().isEmpty())
            return "Họ tên không được trống!";
        if (!repo.existsById(kh.getMaKhachHang()))
            return "Khách hàng không tồn tại!";
        repo.save(kh);
        return null;
    }

    @Transactional
    public String xoa(String id) {
        if (!repo.existsById(id)) return "Không tìm thấy khách hàng!";
        repo.deleteById(id);
        return null;
    }
}
