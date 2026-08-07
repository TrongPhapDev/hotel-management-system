package com.ohno.hotel.dto;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutPreviewDTO {
    private String maChiTiet;
    private String maPhong;
    private String tenKhachHang;
    private LocalDateTime ngayNhan;
    private LocalDateTime ngayTra;
    private double donGiaPhong;
    private long soNgay;
    private double tienPhong;
    private double phuPhiCheckInEarly;
    private double phuPhiCheckOutLate;
    private double phuPhiKhac;
    private List<DichVuSuDungDTO> dsDichVu;
    private double tongDichVu;
    private double subTotal;
    private String voucherCode;
    private double tienGiam;
    private double tienCoc;
    private double tongThanhToan;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DichVuSuDungDTO {
        private String maSuDung;
        private String maDichVu;
        private String tenDichVu;
        private int soLuong;
        private double donGia;
        private double thanhTien;
    }
}
