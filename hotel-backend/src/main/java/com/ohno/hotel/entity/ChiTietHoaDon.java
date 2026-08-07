package com.ohno.hotel.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ChiTietHoaDon")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ChiTietHoaDon {
    @Id
    @Column(name = "maChiTietHoaDon", length = 20)
    private String maChiTietHoaDon;

    @Column(name = "loaiChiTiet", length = 20)
    private String loaiChiTiet; // PHONG, DICH_VU, PHU_PHI

    @Column(name = "noiDung", columnDefinition = "NVARCHAR(500)")
    private String noiDung;

    @Column(name = "donViTinh", columnDefinition = "NVARCHAR(50)")
    private String donViTinh;

    @Column(name = "soLuong")
    private int soLuong;

    @Column(name = "donGia")
    private double donGia;

    @Column(name = "thanhTien")
    private double thanhTien;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "maHoaDon")
    private HoaDon hoaDon;
}
