package entity;

import entity.enums.TrangThaiPhong;
import java.time.LocalDateTime;

public class LichSuTrangThaiPhong {
    private String maLichSu;
    private TrangThaiPhong trangThaiCu;
    private TrangThaiPhong trangThaiMoi;
    private LocalDateTime thoiGianChuyen;
    private String lyDo;

    private Phong phong;

    public LichSuTrangThaiPhong() {}

    public LichSuTrangThaiPhong(String maLichSu, TrangThaiPhong trangThaiCu, TrangThaiPhong trangThaiMoi, LocalDateTime thoiGianChuyen, String lyDo) {
        this.maLichSu = maLichSu;
        this.trangThaiCu = trangThaiCu;
        this.trangThaiMoi = trangThaiMoi;
        this.thoiGianChuyen = thoiGianChuyen;
        this.lyDo = lyDo;
    }

    // Getters and Setters
    public String getMaLichSu() { return maLichSu; }
    public void setMaLichSu(String maLichSu) { this.maLichSu = maLichSu; }

    public TrangThaiPhong getTrangThaiCu() { return trangThaiCu; }
    public void setTrangThaiCu(TrangThaiPhong trangThaiCu) { this.trangThaiCu = trangThaiCu; }

    public TrangThaiPhong getTrangThaiMoi() { return trangThaiMoi; }
    public void setTrangThaiMoi(TrangThaiPhong trangThaiMoi) { this.trangThaiMoi = trangThaiMoi; }

    public LocalDateTime getThoiGianChuyen() { return thoiGianChuyen; }
    public void setThoiGianChuyen(LocalDateTime thoiGianChuyen) { this.thoiGianChuyen = thoiGianChuyen; }

    public String getLyDo() { return lyDo; }
    public void setLyDo(String lyDo) { this.lyDo = lyDo; }

    public Phong getPhong() { return phong; }
    public void setPhong(Phong phong) { this.phong = phong; }
}
