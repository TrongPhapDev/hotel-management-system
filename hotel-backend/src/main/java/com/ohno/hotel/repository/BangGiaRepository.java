package com.ohno.hotel.repository;

import com.ohno.hotel.entity.BangGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BangGiaRepository extends JpaRepository<BangGia, String> {
    
    @Query("SELECT b FROM BangGia b WHERE b.trangThai = true AND :time BETWEEN b.ngayBatDau AND b.ngayKetThuc ORDER BY b.mucUuTien ASC")
    List<BangGia> findActiveAtTime(@Param("time") LocalDateTime time);

    @Query("SELECT b FROM BangGia b WHERE b.trangThai = true AND :time BETWEEN b.ngayBatDau AND b.ngayKetThuc AND b.doiTuongApDung = :doiTuong ORDER BY b.mucUuTien ASC")
    List<BangGia> findActiveAtTimeAndDoiTuong(@Param("time") LocalDateTime time, @Param("doiTuong") String doiTuong);

    @Query(value = "SELECT MAX(CAST(SUBSTRING(maBangGia,3,10) AS INT)) FROM BangGia WHERE maBangGia LIKE 'BG%'", nativeQuery = true)
    Integer getMaxId();
}
