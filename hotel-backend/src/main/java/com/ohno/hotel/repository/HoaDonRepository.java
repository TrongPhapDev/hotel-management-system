package com.ohno.hotel.repository;
import com.ohno.hotel.entity.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, String> {
    Optional<HoaDon> findByDatPhong_MaDatPhong(String maDatPhong);
    List<HoaDon> findByTrangThai(String trangThai);

    @Query("SELECT COALESCE(SUM(h.tongThanhToan),0) FROM HoaDon h WHERE h.trangThai = 'PAID'")
    double getTongDoanhThu();

    @Query("SELECT COALESCE(SUM(h.tongThanhToan),0) FROM HoaDon h WHERE h.trangThai = 'PAID' AND MONTH(h.ngayLap) = :month AND YEAR(h.ngayLap) = :year")
    double getDoanhThuTheoThang(@Param("month") int month, @Param("year") int year);

    @Query(value = "SELECT MAX(CAST(SUBSTRING(maHoaDon,3,10) AS INT)) FROM HoaDon WHERE maHoaDon LIKE 'HD%'", nativeQuery = true)
    Integer getMaxId();

    // ── ThongKe queries ─────────────────────────────────────────────────────

    @Query("SELECT COALESCE(SUM(h.tongThanhToan), 0) FROM HoaDon h WHERE h.trangThai = 'PAID' AND h.ngayLap BETWEEN :start AND :end")
    Double sumThanhToanByNgayLap(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(h.tongTienDichVu), 0) FROM HoaDon h WHERE h.trangThai = 'PAID' AND h.ngayLap BETWEEN :start AND :end")
    Double sumTienDichVuByNgayLap(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(h.tongThanhToan), 0) FROM HoaDon h WHERE h.trangThai = 'PAID' AND (h.phuongThucThanhToan IS NULL OR h.phuongThucThanhToan = 'CASH') AND h.ngayLap BETWEEN :start AND :end")
    Double sumCashThanhToanByNgayLap(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
