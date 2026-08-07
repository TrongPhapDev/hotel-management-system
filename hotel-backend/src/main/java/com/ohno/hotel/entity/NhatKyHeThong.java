package com.ohno.hotel.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "NhatKyHeThong")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class NhatKyHeThong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maLog")
    private int maLog;

    @Column(name = "thoiGian")
    private LocalDateTime thoiGian;

    @Column(name = "tenDangNhap", length = 150)
    private String tenDangNhap;

    @Column(name = "hanhDong", columnDefinition = "NVARCHAR(250)")
    private String hanhDong;

    @Column(name = "doiTuong", columnDefinition = "NVARCHAR(250)")
    private String doiTuong;

    @Column(name = "chiTiet", columnDefinition = "NVARCHAR(MAX)")
    private String chiTiet;
}
