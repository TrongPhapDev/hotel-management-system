package com.ohno.hotel.repository;

import com.ohno.hotel.entity.NhatKyHeThong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NhatKyHeThongRepository extends JpaRepository<NhatKyHeThong, Integer> {
    List<NhatKyHeThong> findTop100ByOrderByThoiGianDesc();
}
