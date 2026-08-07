package com.ohno.hotel.repository;

import com.ohno.hotel.entity.GiaoCa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GiaoCaRepository extends JpaRepository<GiaoCa, String> {
    List<GiaoCa> findByTrangThai(String trangThai);
    List<GiaoCa> findByNhanVien_MaNhanVienAndTrangThai(String maNhanVien, String trangThai);

    @Query(value = "SELECT MAX(CAST(SUBSTRING(maGiaoCa,3,10) AS INT)) FROM GiaoCa WHERE maGiaoCa LIKE 'GC%'", nativeQuery = true)
    Integer getMaxId();

    @Query("SELECT gc FROM GiaoCa gc WHERE " +
           "(:kw IS NULL OR :kw = '' OR gc.maGiaoCa LIKE %:kw% OR gc.nhanVien.hoTen LIKE %:kw%) " +
           "AND (:status IS NULL OR :status = '' OR gc.trangThai = :status) " +
           "ORDER BY gc.thoiGianBatDau DESC")
    List<GiaoCa> searchShifts(@Param("kw") String kw, @Param("status") String status);
}
