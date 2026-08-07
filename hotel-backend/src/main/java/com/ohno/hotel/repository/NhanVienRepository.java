package com.ohno.hotel.repository;
import com.ohno.hotel.entity.NhanVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NhanVienRepository extends JpaRepository<NhanVien, String> {
    List<NhanVien> findByDangLamViecTrue();
    List<NhanVien> findByHoTenContainingIgnoreCase(String hoTen);
    List<NhanVien> findByChucVu(String chucVu);
}
