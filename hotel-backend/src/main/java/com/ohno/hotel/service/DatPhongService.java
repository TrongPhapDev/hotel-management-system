package com.ohno.hotel.service;

import com.ohno.hotel.entity.DatPhong;
import com.ohno.hotel.entity.Phong;
import com.ohno.hotel.repository.DatPhongRepository;
import com.ohno.hotel.repository.PhongRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

import com.ohno.hotel.entity.ChiTietDatPhong;
import com.ohno.hotel.repository.ChiTietDatPhongRepository;

@Service
public class DatPhongService {
    private final DatPhongRepository repo;
    private final PhongRepository phongRepo;
    private final ChiTietDatPhongRepository ctdpRepo;

    public DatPhongService(DatPhongRepository repo, PhongRepository phongRepo, ChiTietDatPhongRepository ctdpRepo) {
        this.repo = repo;
        this.phongRepo = phongRepo;
        this.ctdpRepo = ctdpRepo;
    }

    private void populateDsChiTiet(DatPhong dp) {
        if (dp != null) {
            dp.setDsChiTiet(ctdpRepo.findByDatPhong_MaDatPhong(dp.getMaDatPhong()));
        }
    }

    public List<DatPhong> getAll() {
        List<DatPhong> list = repo.findAll();
        list.forEach(this::populateDsChiTiet);
        return list;
    }
    
    public List<DatPhong> getActive() {
        List<DatPhong> list = repo.findActive();
        list.forEach(this::populateDsChiTiet);
        return list;
    }
    
    public DatPhong getById(String id) {
        DatPhong dp = repo.findById(id).orElse(null);
        populateDsChiTiet(dp);
        return dp;
    }
    
    public List<DatPhong> getByTrangThai(String tt) {
        List<DatPhong> list = repo.findByTrangThai(tt);
        list.forEach(this::populateDsChiTiet);
        return list;
    }
    
    public List<DatPhong> search(String kw) {
        List<DatPhong> list = repo.search(kw);
        list.forEach(this::populateDsChiTiet);
        return list;
    }

    public String generateMa() {
        Integer max = repo.getMaxId();
        return String.format("DP%04d", (max != null ? max : 0) + 1);
    }

    @Transactional
    public String them(DatPhong dp) {
        if (dp.getKhachHang() == null) return "Vui lòng chọn khách hàng!";
        if (dp.getNgayNhanDuKien() == null) return "Vui lòng chọn ngày nhận phòng!";
        if (dp.getNgayTraDuKien() == null) return "Vui lòng chọn ngày trả phòng!";
        if (dp.getNgayNhanDuKien().isAfter(dp.getNgayTraDuKien()))
            return "Ngày nhận phải trước ngày trả!";
        if (dp.getMaDatPhong() == null || dp.getMaDatPhong().trim().isEmpty())
            dp.setMaDatPhong(generateMa());
        if (dp.getNgayDat() == null) dp.setNgayDat(LocalDateTime.now());
        dp.setTrangThai("CONFIRMED");
        repo.save(dp);

        // Lưu ChiTietDatPhong cho các phòng được chọn
        if (dp.getDsChiTiet() != null && !dp.getDsChiTiet().isEmpty()) {
            int maxIdVal = ctdpRepo.getMaxId() != null ? ctdpRepo.getMaxId() : 0;
            for (ChiTietDatPhong ct : dp.getDsChiTiet()) {
                maxIdVal++;
                ct.setMaChiTiet(String.format("CT%04d", maxIdVal));
                ct.setDatPhong(dp);
                ct.setKhachHang(dp.getKhachHang());
                ct.setDaThanhToan(false);
                if (ct.getGiaThucTeChot() == null || ct.getGiaThucTeChot() == 0.0) {
                    Phong p = phongRepo.findById(ct.getPhong().getMaPhong()).orElse(null);
                    if (p != null) {
                        ct.setGiaThucTeChot(p.getGiaTheoNgay());
                    } else {
                        ct.setGiaThucTeChot(0.0);
                    }
                }
                ctdpRepo.save(ct);
            }
        }
        return null;
    }

    @Transactional
    public String checkIn(String maDatPhong) {
        DatPhong dp = getById(maDatPhong);
        if (dp == null) return "Không tìm thấy đặt phòng!";
        if (!"CONFIRMED".equals(dp.getTrangThai()) && !"PENDING".equals(dp.getTrangThai()))
            return "Trạng thái không hợp lệ để check-in!";
        
        dp.setTrangThai("CHECKED_IN");
        repo.save(dp);

        // Cập nhật trạng thái phòng và ngày nhận thực tế cho các chi tiết đặt phòng
        List<ChiTietDatPhong> details = ctdpRepo.findByDatPhong_MaDatPhong(maDatPhong);
        if (details != null) {
            for (ChiTietDatPhong ct : details) {
                if (ct.getNgayNhanThucTe() == null) {
                    ct.setNgayNhanThucTe(LocalDateTime.now());
                }
                ctdpRepo.save(ct);

                if (ct.getPhong() != null) {
                    Phong phong = phongRepo.findById(ct.getPhong().getMaPhong()).orElse(null);
                    if (phong != null) {
                        phong.setTrangThai("OCCUPIED");
                        phong.setTenKhachHienTai(dp.getKhachHang() != null ? dp.getKhachHang().getHoTen() : "Khách");
                        phongRepo.save(phong);
                    }
                }
            }
        }
        return null;
    }

    @Transactional
    public String checkOut(String maDatPhong) {
        DatPhong dp = getById(maDatPhong);
        if (dp == null) return "Không tìm thấy đặt phòng!";
        if (!"CHECKED_IN".equals(dp.getTrangThai()))
            return "Khách chưa check-in!";
        dp.setTrangThai("CHECKED_OUT");
        repo.save(dp);
        return null;
    }

    @Transactional
    public String huy(String maDatPhong, String lyDo) {
        DatPhong dp = getById(maDatPhong);
        if (dp == null) return "Không tìm thấy đặt phòng!";
        dp.setTrangThai("CANCELLED");
        dp.setGhiChu((dp.getGhiChu() != null ? dp.getGhiChu() + " | " : "") + "Hủy: " + lyDo);
        repo.save(dp);
        return null;
    }

    @Transactional
    public String sua(DatPhong dp) {
        if (!repo.existsById(dp.getMaDatPhong())) return "Đặt phòng không tồn tại!";
        repo.save(dp);
        return null;
    }
}
