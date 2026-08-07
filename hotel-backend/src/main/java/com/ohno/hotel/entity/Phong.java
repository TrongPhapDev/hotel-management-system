package com.ohno.hotel.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Phong")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Phong {
    @Id
    @Column(name = "maPhong", length = 20)
    private String maPhong;

    @Column(name = "tang")
    private int tang;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "maLoaiPhong")
    private LoaiPhong loaiPhong;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "maHuongNhin")
    private HuongNhin huongNhin;

    @Column(name = "trangThai", length = 20)
    private String trangThai = "AVAILABLE"; // AVAILABLE, OCCUPIED, MAINTENANCE, CLEANING

    @Transient
    private String tenKhachHienTai;

    public double getGiaTheoNgay() {
        double base = (loaiPhong != null) ? loaiPhong.getGiaTheoNgay() : 0.0;
        double heSo = (huongNhin != null) ? huongNhin.getHeSoGia() : 1.0;
        if (heSo <= 0) heSo = 1.0;
        return base * heSo;
    }
}
