package com.ohno.hotel.repository;

import com.ohno.hotel.entity.ChiTietBangGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChiTietBangGiaRepository extends JpaRepository<ChiTietBangGia, Integer> {
    List<ChiTietBangGia> findByBangGia_MaBangGia(String maBangGia);
    ChiTietBangGia findByBangGia_MaBangGiaAndLoaiPhong_MaLoaiPhong(String maBangGia, String maLoaiPhong);
    void deleteByBangGia_MaBangGia(String maBangGia);
}
