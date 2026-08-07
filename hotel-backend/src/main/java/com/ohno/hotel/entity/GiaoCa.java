package com.ohno.hotel.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "GiaoCa")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GiaoCa {
    @Id
    @Column(name = "maGiaoCa", length = 20)
    private String maGiaoCa;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "maNhanVien")
    private NhanVien nhanVien;

    @Column(name = "thoiGianBatDau")
    private LocalDateTime thoiGianBatDau;

    @Column(name = "thoiGianKetThuc")
    private LocalDateTime thoiGianKetThuc;

    @Column(name = "tienMatDauCa")
    private double tienMatDauCa;

    @Column(name = "tienMatThuTrongCa")
    private double tienMatThuTrongCa;

    @Column(name = "tienMatBanGiao")
    private double tienMatBanGiao;

    @Column(name = "maNhanVienNhan", length = 20)
    private String maNhanVienNhan;

    @Column(name = "ghiChu", columnDefinition = "NVARCHAR(MAX)")
    private String ghiChu;

    @Column(name = "tienMatChenhLech")
    private double tienMatChenhLech;

    @Column(name = "trangThai", length = 20)
    private String trangThai; // OPEN, CLOSED
}
