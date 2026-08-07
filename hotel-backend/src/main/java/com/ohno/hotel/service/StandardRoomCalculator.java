package com.ohno.hotel.service;

import com.ohno.hotel.entity.ChiTietDatPhong;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Component
public class StandardRoomCalculator {

    private final BangGiaService bangGiaService;

    public StandardRoomCalculator(BangGiaService bangGiaService) {
        this.bangGiaService = bangGiaService;
    }

    public double tinhTienPhong(ChiTietDatPhong ctdp) {
        if (ctdp == null || ctdp.getPhong() == null) return 0;
        String maLoai = ctdp.getPhong().getLoaiPhong() != null ? ctdp.getPhong().getLoaiPhong().getMaLoaiPhong() : "";
        double policyPrice = bangGiaService.layGiaHienHanh(maLoai);

        double donGia = policyPrice > 0 ? policyPrice : ctdp.getGiaThucTeChot();
        if (donGia <= 0 && ctdp.getPhong() != null) {
            donGia = ctdp.getPhong().getGiaTheoNgay();
        }

        LocalDateTime checkOut = ctdp.getNgayTraThucTe() != null ? ctdp.getNgayTraThucTe() : LocalDateTime.now();
        LocalDateTime checkIn = ctdp.getNgayNhanThucTe();
        if (checkIn == null && ctdp.getDatPhong() != null) {
            checkIn = ctdp.getDatPhong().getNgayNhanDuKien();
        }
        if (checkIn == null) checkIn = checkOut.minusDays(1);

        long soNgay = java.time.temporal.ChronoUnit.DAYS.between(checkIn.toLocalDate(), checkOut.toLocalDate());
        if (soNgay <= 0) soNgay = 1;

        BigDecimal basePrice = BigDecimal.valueOf(donGia);
        BigDecimal days = BigDecimal.valueOf(soNgay);

        return basePrice.multiply(days)
                .setScale(0, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public double tinhPhuPhi(ChiTietDatPhong ctdp, LocalDateTime gioNhan, LocalDateTime gioTra) {
        if (gioNhan == null || gioTra == null || ctdp == null || ctdp.getPhong() == null) return 0;

        String maLoai = ctdp.getPhong().getLoaiPhong() != null ? ctdp.getPhong().getLoaiPhong().getMaLoaiPhong() : "";
        double donGia = bangGiaService.layGiaHienHanh(maLoai);
        if (donGia <= 0) {
            donGia = ctdp.getGiaThucTeChot() > 0 ? ctdp.getGiaThucTeChot() : ctdp.getPhong().getGiaTheoNgay();
        }

        BigDecimal phuPhi = BigDecimal.ZERO;
        BigDecimal bdDonGia = BigDecimal.valueOf(donGia);

        // --- PHỤ PHÍ NHẬN PHÒNG SỚM (EARLY CHECK-IN) ---
        if (gioNhan.getHour() < 14) {
            if (gioNhan.getHour() < 5) {
                phuPhi = phuPhi.add(bdDonGia);
            } else if (gioNhan.getHour() < 9) {
                phuPhi = phuPhi.add(bdDonGia.multiply(BigDecimal.valueOf(0.5)));
            } else if (gioNhan.getHour() < 12) {
                phuPhi = phuPhi.add(bdDonGia.multiply(BigDecimal.valueOf(0.3)));
            }
        }

        // --- PHỤ PHÍ TRẢ PHÒNG TRỄ (LATE CHECK-OUT) ---
        if (gioTra.getHour() > 12) {
            if (gioTra.getHour() >= 18) {
                phuPhi = phuPhi.add(bdDonGia);
            } else if (gioTra.getHour() >= 15) {
                phuPhi = phuPhi.add(bdDonGia.multiply(BigDecimal.valueOf(0.5)));
            } else if (gioTra.getHour() >= 13) {
                phuPhi = phuPhi.add(bdDonGia.multiply(BigDecimal.valueOf(0.3)));
            }
        }

        return phuPhi.setScale(0, RoundingMode.HALF_UP).doubleValue();
    }
}
