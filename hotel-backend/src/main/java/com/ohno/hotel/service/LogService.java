package com.ohno.hotel.service;

import com.ohno.hotel.entity.NhatKyHeThong;
import com.ohno.hotel.repository.NhatKyHeThongRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LogService {
    private final NhatKyHeThongRepository repo;

    public LogService(NhatKyHeThongRepository repo) {
        this.repo = repo;
    }

    public void addLog(String user, String hanhDong, String doiTuong, String chiTiet) {
        NhatKyHeThong log = NhatKyHeThong.builder()
                .thoiGian(LocalDateTime.now())
                .tenDangNhap(user != null ? user : "system")
                .hanhDong(hanhDong)
                .doiTuong(doiTuong)
                .chiTiet(chiTiet)
                .build();
        repo.save(log);
    }

    public List<NhatKyHeThong> getAllLogs() {
        return repo.findTop100ByOrderByThoiGianDesc();
    }
}
