package com.ohno.hotel.repository;

import com.ohno.hotel.entity.KhuyenMai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KhuyenMaiRepository extends JpaRepository<KhuyenMai, String> {
    @Query("SELECT k FROM KhuyenMai k WHERE k.ngayBatDau <= :now AND k.ngayKetThuc >= :now")
    List<KhuyenMai> findActive(LocalDateTime now);
}
