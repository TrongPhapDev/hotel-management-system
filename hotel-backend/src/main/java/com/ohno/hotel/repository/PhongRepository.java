package com.ohno.hotel.repository;
import com.ohno.hotel.entity.Phong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

@Repository
public interface PhongRepository extends JpaRepository<Phong, String> {
    List<Phong> findByTrangThai(String trangThai);
    List<Phong> findByLoaiPhong_MaLoaiPhong(String maLoaiPhong);
    List<Phong> findByTang(int tang);

    @Query("SELECT COUNT(p) FROM Phong p WHERE p.trangThai = :tt")
    long countByTrangThai(@Param("tt") String tt);

    @Query(value = "SELECT MAX(CAST(SUBSTRING(maPhong,2,10) AS INT)) FROM Phong WHERE maPhong LIKE 'P%'", nativeQuery = true)
    Integer getMaxId();

    /** Thống kê số lượng phòng theo từng trạng thái */
    @Query("SELECT p.trangThai AS trangThai, COUNT(p) AS soLuong FROM Phong p GROUP BY p.trangThai")
    List<Object[]> countGroupByTrangThai();

    /** Phòng trống (AVAILABLE) */
    @Query("SELECT p FROM Phong p WHERE p.trangThai = 'AVAILABLE' ORDER BY p.maPhong ASC")
    List<Phong> findAvailable();

    @Query("SELECT p FROM Phong p WHERE p.trangThai <> 'MAINTENANCE' AND p.maPhong NOT IN (" +
           "  SELECT ctdp.phong.maPhong FROM ChiTietDatPhong ctdp " +
           "  JOIN ctdp.datPhong dp " +
           "  WHERE dp.trangThai IN ('CONFIRMED', 'CHECKED_IN') " +
           "    AND dp.ngayNhanDuKien < :ngayTra " +
           "    AND dp.ngayTraDuKien > :ngayNhan" +
           ") ORDER BY p.maPhong ASC")
    List<Phong> findAvailableRoomsInRange(@Param("ngayNhan") java.time.LocalDateTime ngayNhan,
                                          @Param("ngayTra") java.time.LocalDateTime ngayTra);
}
