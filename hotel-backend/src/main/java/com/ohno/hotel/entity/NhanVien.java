package com.ohno.hotel.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "NhanVien")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class NhanVien {
    @Id
    @Column(name = "maNhanVien", length = 20)
    private String maNhanVien;

    @Column(name = "hoTen", nullable = false, length = 100)
    private String hoTen;

    @Column(name = "sdt", length = 20)
    private String sdt;

    @Column(name = "chucVu", length = 50)
    private String chucVu;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "cccd", length = 20)
    private String cccd;

    @Column(name = "ngaySinh")
    private LocalDate ngaySinh;

    @Column(name = "gioiTinh", length = 10)
    private String gioiTinh;

    @Column(name = "diaChi", length = 255)
    private String diaChi;

    @Column(name = "ngayVaoLam")
    private LocalDate ngayVaoLam;

    @Column(name = "dangLamViec")
    private boolean dangLamViec = true;

    @Column(name = "luongCoBan")
    private double luongCoBan;

    @OneToOne(mappedBy = "nhanVien", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private TaiKhoan taiKhoan;

    @Transient
    private String matKhau;
}
