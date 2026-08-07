package com.ohno.hotel.repository;
import com.ohno.hotel.entity.LoaiPhong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LoaiPhongRepository extends JpaRepository<LoaiPhong, String> {
    List<LoaiPhong> findByTenLoaiPhongContainingIgnoreCase(String ten);
}
