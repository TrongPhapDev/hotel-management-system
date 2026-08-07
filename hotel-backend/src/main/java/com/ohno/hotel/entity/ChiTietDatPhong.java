package com.ohno.hotel.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ChiTietDatPhong")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ChiTietDatPhong {
    @Id
    @Column(name = "maChiTiet", length = 20)
    private String maChiTiet;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "maDatPhong")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("dsChiTiet")
    private DatPhong datPhong;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "maPhong")
    private Phong phong;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "maKhachHang")
    private KhachHang khachHang;

    @Column(name = "ngayNhanThucTe")
    private LocalDateTime ngayNhanThucTe;

    @Column(name = "ngayTraThucTe")
    private LocalDateTime ngayTraThucTe;

    @Column(name = "giaThucTeChot")
    private Double giaThucTeChot;

    @Column(name = "phuPhiPhatSinh")
    private Double phuPhiPhatSinh = 0.0;

    @Column(name = "daThanhToan")
    private Boolean daThanhToan = false;
}
