package com.ohno.hotel.repository;

import com.ohno.hotel.entity.DichVu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DichVuRepository extends JpaRepository<DichVu, String> {

    // Tìm các dịch vụ chưa bị xóa (trangThai >= 0) sắp xếp theo tên
    List<DichVu> findByTrangThaiGreaterThanEqualOrderByTenDichVu(int minStatus);

    // Tính đơn giá trung bình của các dịch vụ chưa bị xóa
    @Query("SELECT AVG(d.donGia) FROM DichVu d WHERE d.trangThai >= 0")
    Double getGiaTrungBinh();

    // Sinh mã tự động giống cơ chế Swing cũ
    @Query(value = "SELECT MAX(CAST(SUBSTRING(maDichVu,3,10) AS INT)) FROM DichVu WHERE maDichVu LIKE 'DV%'", nativeQuery = true)
    Integer getMaxNumericId();

    // Tìm kiếm đa năng
    @Query("SELECT d FROM DichVu d WHERE d.trangThai >= 0 " +
           "AND (:kw IS NULL OR d.tenDichVu LIKE %:kw%) " +
           "AND (:loai IS NULL OR d.loai = :loai)")
    List<DichVu> searchDichVu(@Param("kw") String kw, @Param("loai") String loai);
}
