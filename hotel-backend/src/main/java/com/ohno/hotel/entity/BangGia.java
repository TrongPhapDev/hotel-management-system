package com.ohno.hotel.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "BangGia")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BangGia {
    @Id
    @Column(name = "maBangGia", length = 20)
    private String maBangGia;

    @Column(name = "tenBangGia", columnDefinition = "NVARCHAR(200)")
    private String tenBangGia;

    @Column(name = "ngayBatDau")
    private LocalDateTime ngayBatDau;

    @Column(name = "ngayKetThuc")
    private LocalDateTime ngayKetThuc;

    @Column(name = "isKichHoat")
    private boolean trangThai;

    @Column(name = "loaiBangGia", length = 50)
    private String loaiBangGia = "RACK"; // RACK, SEASONAL, CORPORATE, OTA, PROMOTION

    @Column(name = "doiTuongApDung", length = 50)
    private String doiTuongApDung = "ALL"; // ALL, CA_NHAN, DOAN, CORPORATE, VIP

    @Column(name = "mucUuTien")
    private Integer mucUuTien = 100;

    @Column(name = "moTa", columnDefinition = "NVARCHAR(500)")
    private String moTa;
}
