package com.ohno.hotel.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "LoaiPhong")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LoaiPhong {
    @Id
    @Column(name = "maLoaiPhong", length = 20)
    private String maLoaiPhong;

    @Column(name = "tenLoaiPhong", nullable = false, length = 100)
    private String tenLoaiPhong;

    @Column(name = "soNguoiToiDa")
    private int soNguoiToiDa;

    @Column(name = "moTa", columnDefinition = "NVARCHAR(MAX)")
    private String moTa;

    @Column(name = "giaTheoNgay")
    private double giaTheoNgay;
}
