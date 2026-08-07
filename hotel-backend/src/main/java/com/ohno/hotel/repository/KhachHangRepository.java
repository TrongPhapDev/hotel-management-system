package com.ohno.hotel.repository;
import com.ohno.hotel.entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, String> {
    List<KhachHang> findByHoTenContainingIgnoreCase(String hoTen);
    List<KhachHang> findBySdt(String sdt);
    List<KhachHang> findByCccd(String cccd);

    @Query("SELECT k FROM KhachHang k WHERE (:kw IS NULL OR LOWER(k.hoTen) LIKE LOWER(CONCAT('%',:kw,'%')) OR k.sdt LIKE CONCAT('%',:kw,'%') OR k.cccd LIKE CONCAT('%',:kw,'%'))")
    List<KhachHang> search(@Param("kw") String kw);

    @Query(value = "SELECT MAX(CAST(SUBSTRING(maKhachHang,3,10) AS INT)) FROM KhachHang WHERE maKhachHang LIKE 'KH%'", nativeQuery = true)
    Integer getMaxId();

    /** Khách mới trong khoảng thời gian (nếu có cột ngayTao, nếu không dùng maDatPhong fallback) */
    @Query("SELECT COUNT(k) FROM KhachHang k WHERE k.maKhachHang IS NOT NULL")
    long countByNgayTaoBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
