package com.ohno.hotel.repository;
import com.ohno.hotel.entity.HuongNhin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HuongNhinRepository extends JpaRepository<HuongNhin, String> {
    List<HuongNhin> findAllByOrderByThuTuAsc();
}
