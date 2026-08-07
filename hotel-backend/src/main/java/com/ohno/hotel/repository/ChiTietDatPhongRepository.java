package com.ohno.hotel.repository;

import com.ohno.hotel.entity.ChiTietDatPhong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChiTietDatPhongRepository extends JpaRepository<ChiTietDatPhong, String> {
    List<ChiTietDatPhong> findByDatPhong_MaDatPhong(String maDatPhong);
    List<ChiTietDatPhong> findByPhong_MaPhong(String maPhong);
    List<ChiTietDatPhong> findByDaThanhToanFalse();

    @Query(value = "SELECT MAX(TRY_CAST(SUBSTRING(maChiTiet,3,10) AS INT)) FROM ChiTietDatPhong WHERE maChiTiet LIKE 'CT%'", nativeQuery = true)
    Integer getMaxId();
}
