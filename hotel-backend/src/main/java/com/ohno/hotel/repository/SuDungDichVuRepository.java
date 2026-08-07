package com.ohno.hotel.repository;

import com.ohno.hotel.entity.SuDungDichVu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SuDungDichVuRepository extends JpaRepository<SuDungDichVu, String> {
    List<SuDungDichVu> findByChiTietDatPhong_MaChiTiet(String maChiTiet);

    @Query(value = "SELECT MAX(CAST(SUBSTRING(maSuDung,3,10) AS INT)) FROM SuDungDichVu WHERE maSuDung LIKE 'SD%'", nativeQuery = true)
    Integer getMaxId();
}
