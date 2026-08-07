package service;

import entity.BangGia;
import entity.ChiTietDatPhong;

import java.util.List;

public interface ITinhTienPhong {
    double tinhTienPhong(ChiTietDatPhong ctdp, List<BangGia> dsBangGia);
    double tinhPhuPhi(ChiTietDatPhong ctdp, java.time.LocalDateTime gioNhan, java.time.LocalDateTime gioTra);
}
