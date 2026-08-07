package com.ohno.hotel.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "SuDungDichVu")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SuDungDichVu {
    @Id
    @Column(name = "maSuDung", length = 20)
    private String maSuDung;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "maChiTiet")
    private ChiTietDatPhong chiTietDatPhong;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "maDichVu")
    private DichVu dichVu;

    @Column(name = "soLuong")
    private int soLuong;

    @Column(name = "donGiaLucDung")
    private double donGiaLucDung;

    @Column(name = "thoiGianDung")
    private LocalDateTime thoiGianDung;
}
