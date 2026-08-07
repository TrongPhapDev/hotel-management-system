package com.ohno.hotel.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "HuongNhin")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class HuongNhin {
    @Id
    @Column(name = "maHuongNhin", length = 20)
    private String maHuongNhin;

    @Column(name = "tenHuongNhin", nullable = false, length = 100)
    private String tenHuongNhin;

    @Column(name = "moTa", columnDefinition = "NVARCHAR(MAX)")
    private String moTa;

    @Column(name = "heSoGia")
    private double heSoGia = 1.0;

    @Column(name = "thuTu")
    private int thuTu = 0;
}
