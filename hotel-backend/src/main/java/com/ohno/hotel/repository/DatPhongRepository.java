package com.ohno.hotel.repository;
import com.ohno.hotel.entity.DatPhong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface DatPhongRepository extends JpaRepository<DatPhong, String> {
    List<DatPhong> findByTrangThai(String trangThai);
    List<DatPhong> findByKhachHang_MaKhachHang(String maKhachHang);

    @Query("SELECT d FROM DatPhong d WHERE d.trangThai NOT IN ('CHECKED_OUT','CANCELLED') ORDER BY d.ngayDat DESC")
    List<DatPhong> findActive();

    @Query("SELECT d FROM DatPhong d WHERE (:kw IS NULL OR LOWER(d.khachHang.hoTen) LIKE LOWER(CONCAT('%',:kw,'%')) OR d.maDatPhong LIKE CONCAT('%',:kw,'%'))")
    List<DatPhong> search(@Param("kw") String kw);

    @Query(value = "SELECT MAX(TRY_CAST(SUBSTRING(maDatPhong,3,10) AS INT)) FROM DatPhong WHERE maDatPhong LIKE 'DP%'", nativeQuery = true)
    Integer getMaxId();

    // ── ThongKe queries ─────────────────────────────────────────────────────

    @Query("SELECT COUNT(d) FROM DatPhong d WHERE d.trangThai = :trangThai AND d.ngayNhanDuKien BETWEEN :start AND :end")
    long countByTrangThaiAndNgayNhanBetween(@Param("trangThai") String trangThai,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(d) FROM DatPhong d WHERE d.trangThai = :trangThai AND d.ngayTraDuKien BETWEEN :start AND :end")
    long countByTrangThaiAndNgayTraBetween(@Param("trangThai") String trangThai,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(d) FROM DatPhong d WHERE d.trangThai IN :statuses")
    long countByTrangThaiIn(@Param("statuses") Collection<String> statuses);

    @Query("SELECT COUNT(d) FROM DatPhong d WHERE d.ngayDat BETWEEN :start AND :end")
    long countByNgayDatBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** Danh sách check-in hôm nay: CONFIRMED với ngayNhanDuKien trong hôm nay */
    @Query("SELECT d FROM DatPhong d WHERE d.trangThai IN ('CONFIRMED','PENDING') AND d.ngayNhanDuKien BETWEEN :start AND :end ORDER BY d.ngayNhanDuKien ASC")
    List<DatPhong> findCheckinHomNay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** Danh sách check-out hôm nay: CHECKED_IN với ngayTraDuKien trong hôm nay */
    @Query("SELECT d FROM DatPhong d WHERE d.trangThai = 'CHECKED_IN' AND d.ngayTraDuKien BETWEEN :start AND :end ORDER BY d.ngayTraDuKien ASC")
    List<DatPhong> findCheckoutHomNay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** Tất cả đặt phòng bao gồm đã hủy */
    @Query("SELECT d FROM DatPhong d ORDER BY d.ngayDat DESC")
    List<DatPhong> findAll();
}
