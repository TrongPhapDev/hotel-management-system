package com.ohno.hotel.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "KhuyenMai")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class KhuyenMai {
    @Id
    @Column(name = "maKM", length = 20)
    private String maKhuyenMai;

    @Column(name = "tenKM", columnDefinition = "NVARCHAR(200)")
    private String tenKhuyenMai;

    @Column(name = "loaiGiam", length = 10)
    private String loaiGiam; // PERCENT, FIXED

    @Column(name = "giaTriGiam")
    private Double giaTriGiam;

    @Column(name = "ngayBatDau")
    private LocalDateTime ngayBatDau;

    @Column(name = "ngayKetThuc")
    private LocalDateTime ngayKetThuc;

    @Column(name = "dieuKienApDung", columnDefinition = "NVARCHAR(MAX)")
    private String dieuKienApDung;

    @Column(name = "trangThai")
    @Builder.Default
    private Boolean trangThai = true;

    @Column(name = "soLuong")
    @Builder.Default
    private Integer soLuong = 999;

    @Column(name = "daDung")
    @Builder.Default
    private Integer daDung = 0;

    @Column(name = "dieuKienToiThieu")
    @Builder.Default
    private Double dieuKienToiThieu = 0.0;

    @Column(name = "giaTriGiamToiDa")
    @Builder.Default
    private Double giaTriGiamToiDa = 0.0;
}
