package com.ohno.hotel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "DichVu")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DichVu {

    @Id
    @Column(name = "maDichVu", length = 20)
    private String maDichVu;

    @Column(name = "tenDichVu", nullable = false, length = 100)
    private String tenDichVu;

    @Column(name = "loai", length = 50)
    private String loai;

    @Column(name = "donGiaHienTai", nullable = false)
    private double donGia;

    @Column(name = "donViTinh", length = 20)
    private String donViTinh;

    @Column(name = "soLuongMin")
    private int soLuongMin;

    @Column(name = "moTa")
    private String moTa;

    @Column(name = "trangThai")
    private int trangThai; // 1: Hoạt động, 0: Tạm ngừng, -1: Đã xóa
}
