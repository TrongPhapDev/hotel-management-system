package com.ohno.hotel.repository;
import com.ohno.hotel.entity.TaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, String> {
    Optional<TaiKhoan> findByTenDangNhapAndMatKhau(String tenDangNhap, String matKhau);
    boolean existsByTenDangNhap(String tenDangNhap);
}
