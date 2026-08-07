package com.ohno.hotel.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TaiKhoan")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TaiKhoan {
    @Id
    @Column(name = "tenDangNhap", length = 50)
    private String tenDangNhap;

    @Column(name = "matKhau", nullable = false, length = 100)
    private String matKhau;

    @Column(name = "vaiTro", length = 20)
    private String vaiTro; // ADMIN, MANAGER, RECEPTIONIST

    @Column(name = "trangThai")
    private boolean trangThai = true;

    @Column(name = "lanDangNhapCuoi")
    private LocalDateTime lanDangNhapCuoi;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "maNhanVien", referencedColumnName = "maNhanVien")
    private NhanVien nhanVien;
}
