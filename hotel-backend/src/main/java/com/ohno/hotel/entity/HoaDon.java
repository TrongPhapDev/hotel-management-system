package com.ohno.hotel.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "HoaDon")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class HoaDon {
    @Id
    @Column(name = "maHoaDon", length = 20)
    private String maHoaDon;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "maDatPhong")
    private DatPhong datPhong;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "maNhanVien")
    private NhanVien nhanVien;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "maKhuyenMai")
    private KhuyenMai khuyenMai;

    @Column(name = "ngayLap")
    private LocalDateTime ngayLap;

    @Column(name = "tongTienPhong")
    private Double tongTienPhong = 0.0;

    @Column(name = "tongTienDichVu")
    private Double tongTienDichVu = 0.0;

    @Column(name = "tienDatCoc")
    private Double tienDatCoc = 0.0;

    @Column(name = "tienGiamKhuyenMai")
    private Double tienGiamKhuyenMai = 0.0;

    @Column(name = "tongThanhToan")
    private Double tongThanhToan = 0.0;

    @Column(name = "trangThai", length = 20)
    private String trangThai = "UNPAID"; // UNPAID, PARTIALLY_PAID, PAID, REFUNDED

    @Column(name = "phuongThucThanhToan", length = 20)
    private String phuongThucThanhToan = "CASH"; // CASH, CARD, TRANSFER

    @Column(name = "tenCongTy", length = 200)
    private String tenCongTy;

    @Column(name = "maSoThue", length = 20)
    private String maSoThue;

    @Column(name = "diaChiCongTy", length = 500)
    private String diaChiCongTy;
}
