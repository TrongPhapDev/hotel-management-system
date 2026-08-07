package com.ohno.hotel.repository;

import com.ohno.hotel.entity.ChiTietHoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChiTietHoaDonRepository extends JpaRepository<ChiTietHoaDon, String> {
    List<ChiTietHoaDon> findByHoaDon_MaHoaDon(String maHoaDon);

    @Query(value = "SELECT MAX(CAST(SUBSTRING(maChiTietHoaDon,4,10) AS INT)) FROM ChiTietHoaDon WHERE maChiTietHoaDon LIKE 'CTHD%'", nativeQuery = true)
    Integer getMaxId();
}
