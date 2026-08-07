package com.ohno.hotel.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "KhachHang")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class KhachHang {
    @Id
    @Column(name = "maKhachHang", length = 20)
    private String maKhachHang;

    @Column(name = "hoTen", nullable = false, length = 100)
    private String hoTen;

    @Column(name = "sdt", length = 20)
    private String sdt;

    @Column(name = "cccd", length = 20)
    private String cccd;

    @Column(name = "ngaySinh")
    private LocalDate ngaySinh;

    @Column(name = "gioiTinh", length = 10)
    private String gioiTinh;

    @Column(name = "quocTich", length = 50)
    private String quocTich = "Việt Nam";

    @Column(name = "loaiGiayTo", length = 20)
    private String loaiGiayTo = "CCCD"; // CCCD, CMND, PASSPORT

    @Column(name = "soHoChieu", length = 30)
    private String soHoChieu;

    @Column(name = "soVisa", length = 30)
    private String soVisa;

    @Column(name = "ngayHetHanVisa")
    private LocalDate ngayHetHanVisa;

    @Column(name = "noiCapHoChieu", length = 100)
    private String noiCapHoChieu;

    @Column(name = "ngayNhapCanh")
    private LocalDate ngayNhapCanh;
}
