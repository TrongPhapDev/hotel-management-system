package com.ohno.hotel.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "DatPhong")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DatPhong {
    @Id
    @Column(name = "maDatPhong", length = 20)
    private String maDatPhong;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "maKhachHang")
    private KhachHang khachHang;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "maNhanVien")
    private NhanVien nhanVien;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "maKenh")
    private KenhDatPhong kenhDatPhong;

    @Column(name = "ngayDat")
    private LocalDateTime ngayDat;

    @Column(name = "ngayNhanDuKien")
    private LocalDateTime ngayNhanDuKien;

    @Column(name = "ngayTraDuKien")
    private LocalDateTime ngayTraDuKien;

    @Column(name = "soNguoi")
    private Integer soNguoi;

    @Column(name = "tienDatCoc")
    private Double tienDatCoc = 0.0;

    @Column(name = "tongTienTamTinh")
    private Double tongTienTamTinh = 0.0;

    @Column(name = "trangThai", length = 20)
    private String trangThai = "PENDING";
    // PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED, NO_SHOW, WAITLIST

    @Column(name = "ghiChu", columnDefinition = "NVARCHAR(MAX)")
    private String ghiChu;

    @Column(name = "loaiKhach", length = 10)
    private String loaiKhach = "CA_NHAN"; // CA_NHAN, DOAN

    @Column(name = "tenDoan", length = 200)
    private String tenDoan;

    @Column(name = "maXacNhanKenh", length = 100)
    private String maXacNhanKenh;

    @Column(name = "phiNoShow")
    private Double phiNoShow = 0.0;

    @Column(name = "hanCheckIn")
    private LocalDateTime hanCheckIn;

    @Column(name = "thuTuWaitlist")
    private Integer thuTuWaitlist = 0;

    @Transient
    private java.util.List<ChiTietDatPhong> dsChiTiet;
}
