package com.ohno.hotel.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ChiTietBangGia")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ChiTietBangGia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maChiTiet")
    private int maChiTiet;

    @Column(name = "giaNgay")
    private Double giaNgay;

    @Column(name = "giaGioDau")
    private Double giaGioDau;

    @Column(name = "giaGioTiepTheo")
    private Double giaGioTiepTheo;

    @Column(name = "phuPhiTraTre")
    private Double phuPhiTraTre;

    @Column(name = "giaCuoiTuan")
    private Double giaCuoiTuan; // Giá riêng cho T7, CN (0 = dùng giá ngày thường)

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "maBangGia")
    private BangGia bangGia;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "maLoaiPhong")
    private LoaiPhong loaiPhong;
}
