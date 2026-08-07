package com.ohno.hotel.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "KenhDatPhong")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class KenhDatPhong {
    @Id
    @Column(name = "maKenh", length = 20)
    private String maKenh;

    @Column(name = "tenKenh", nullable = false, length = 100)
    private String tenKenh;

    @Column(name = "loaiKenh", length = 20)
    private String loaiKenh; // DIRECT, OTA, CORPORATE, TRAVEL_AGENT, OTHER

    @Column(name = "heSoHoaHong")
    private double heSoHoaHong = 0;

    @Column(name = "trangThai")
    private boolean trangThai = true;

    @Column(name = "moTa", columnDefinition = "NVARCHAR(MAX)")
    private String moTa;
}
