package service;

import dao.GiaoCaDAO;
import entity.GiaoCa;
import entity.NhanVien;
import java.time.LocalDateTime;
import java.util.List;

public class GiaoCaService {
    private final GiaoCaDAO dao = new GiaoCaDAO();
    private final dao.ChiPhiDAO cpDAO = new dao.ChiPhiDAO();

    public GiaoCa moCa(NhanVien nv, double tienDauCa) {
        GiaoCa gc = new GiaoCa();
        gc.setMaGiaoCa(dao.generateMaGiaoCa());
        gc.setNhanVien(nv);
        gc.setThoiGianBatDau(LocalDateTime.now());
        gc.setTienMatDauCa(tienDauCa);
        gc.setTrangThai("OPEN");
        
        if (dao.insert(gc)) {
            LogService.addLog("Mở ca", "Ca làm " + gc.getMaGiaoCa(), "Nhân viên " + nv.getHoTen() + " bắt đầu ca trực");
            return gc;
        }
        return null;
    }

    public boolean chotCa(GiaoCa gc) {
        gc.setThoiGianKetThuc(LocalDateTime.now());
        gc.setTrangThai("CLOSED");
        
        boolean ok = dao.closeShift(gc);
        if (ok) {
            LogService.addLog("Chốt ca", "Ca làm " + gc.getMaGiaoCa(), 
                String.format("Doanh thu ca: %,.0f, Tiền bàn giao: %,.0f", gc.getTienMatThuTrongCa(), gc.getTienMatBanGiao()));
        }
        return ok;
    }

    public GiaoCa getCaHienTai(String maNV) {
        return dao.findCurrentShift(maNV);
    }

    public double tinhDoanhThuCa(LocalDateTime start) {
        return dao.calculateRevenue(start, LocalDateTime.now());
    }

    public double tinhDoanhThuTienMat(LocalDateTime start) {
        return dao.calculateCashRevenue(start, LocalDateTime.now());
    }

    public double tinhChiPhiCa(String maGiaoCa) {
        return cpDAO.sumByShift(maGiaoCa);
    }
    
    public double getExpectedCash(GiaoCa gc) {
        double revenueCash = tinhDoanhThuTienMat(gc.getThoiGianBatDau());
        double expenses = tinhChiPhiCa(gc.getMaGiaoCa());
        return gc.getTienMatDauCa() + revenueCash - expenses;
    }

    public boolean saveKiemTien(String maGiaoCa, java.util.Map<Integer, Integer> map) {
        return dao.saveDenominations(maGiaoCa, map);
    }

    public List<GiaoCa> getHistory() {
        return dao.getAll();
    }

    public List<GiaoCa> searchHistory(String keyword, String status) {
        return dao.search(keyword, status);
    }

    public java.util.Map<Integer, Integer> getDenominations(String maGiaoCa) {
        return dao.getDenominations(maGiaoCa);
    }
}
