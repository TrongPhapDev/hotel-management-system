package com.ohno.hotel.service;

import com.ohno.hotel.entity.GiaoCa;
import com.ohno.hotel.entity.NhanVien;
import com.ohno.hotel.repository.GiaoCaRepository;
import com.ohno.hotel.repository.HoaDonRepository;
import com.ohno.hotel.repository.NhanVienRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class GiaoCaService {
    private final GiaoCaRepository repo;
    private final NhanVienRepository nhanVienRepo;
    private final HoaDonRepository hoaDonRepo;
    private final LogService logService;

    public GiaoCaService(GiaoCaRepository repo,
                         NhanVienRepository nhanVienRepo,
                         HoaDonRepository hoaDonRepo,
                         LogService logService) {
        this.repo = repo;
        this.nhanVienRepo = nhanVienRepo;
        this.hoaDonRepo = hoaDonRepo;
        this.logService = logService;
    }

    public String generateMaGiaoCa() {
        Integer max = repo.getMaxId();
        return String.format("GC%05d", (max != null ? max : 0) + 1);
    }

    @Transactional
    public GiaoCa moCa(String maNhanVien, double tienDauCa) {
        NhanVien nv = nhanVienRepo.findById(maNhanVien).orElse(null);
        if (nv == null) return null;

        // Check if there is already an open shift for this staff member
        List<GiaoCa> active = repo.findByNhanVien_MaNhanVienAndTrangThai(maNhanVien, "OPEN");
        if (!active.isEmpty()) {
            return active.get(0); // return existing
        }

        GiaoCa gc = GiaoCa.builder()
                .maGiaoCa(generateMaGiaoCa())
                .nhanVien(nv)
                .thoiGianBatDau(LocalDateTime.now())
                .tienMatDauCa(tienDauCa)
                .trangThai("OPEN")
                .build();

        GiaoCa saved = repo.save(gc);
        logService.addLog(nv.getMaNhanVien(), "Mở ca", "Ca làm " + saved.getMaGiaoCa(), "Nhân viên " + nv.getHoTen() + " bắt đầu ca trực");
        return saved;
    }

    @Transactional
    public String chotCa(String maGiaoCa, double tienMatBanGiao, String maNhanVienNhan, String ghiChu) {
        GiaoCa gc = repo.findById(maGiaoCa).orElse(null);
        if (gc == null) return "Ca làm việc không tồn tại!";
        if ("CLOSED".equals(gc.getTrangThai())) return "Ca làm việc đã chốt trước đó!";

        LocalDateTime now = LocalDateTime.now();
        double cashRevenue = hoaDonRepo.sumCashThanhToanByNgayLap(gc.getThoiGianBatDau(), now);

        gc.setThoiGianKetThuc(now);
        gc.setTienMatThuTrongCa(cashRevenue);
        gc.setTienMatBanGiao(tienMatBanGiao);
        gc.setMaNhanVienNhan(maNhanVienNhan);
        gc.setGhiChu(ghiChu);
        
        double expectedCash = gc.getTienMatDauCa() + cashRevenue;
        gc.setTienMatChenhLech(tienMatBanGiao - expectedCash);
        gc.setTrangThai("CLOSED");

        repo.save(gc);

        logService.addLog(gc.getNhanVien().getMaNhanVien(), "Chốt ca", "Ca làm " + gc.getMaGiaoCa(),
                String.format("Doanh thu ca: %,.0f, Tiền bàn giao: %,.0f, Chênh lệch: %,.0f",
                        cashRevenue, tienMatBanGiao, gc.getTienMatChenhLech()));
        return null;
    }

    public GiaoCa getCaHienTai(String maNhanVien) {
        List<GiaoCa> list = repo.findByNhanVien_MaNhanVienAndTrangThai(maNhanVien, "OPEN");
        return list.isEmpty() ? null : list.get(0);
    }

    public double getExpectedCash(String maGiaoCa) {
        GiaoCa gc = repo.findById(maGiaoCa).orElse(null);
        if (gc == null) return 0;
        double cashRevenue = hoaDonRepo.sumCashThanhToanByNgayLap(gc.getThoiGianBatDau(), LocalDateTime.now());
        return gc.getTienMatDauCa() + cashRevenue;
    }

    public List<GiaoCa> searchHistory(String kw, String status) {
        return repo.searchShifts(kw, status);
    }
}
