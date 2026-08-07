package service;

import dao.ThongKeDAO;
import java.util.*;

public class ThongKeService {
    private final ThongKeDAO dao = new ThongKeDAO();

    public Map<String, Object> getDashboardStats()             { return dao.getDashboardStats(); }
    public Map<String, Object> getThongKeKy(String ky)        { return dao.getThongKeKy(ky); }
    public List<long[]>        getDoanhThuTheoNgay(String ky)           { return dao.getDoanhThuTheoNgay(ky); }
    public List<Map<String, Object>> getTopPhong(int n, String ky)        { return dao.getTopPhongDoanhThu(n, ky); }
    public List<Map<String, Object>> getTopDichVu(int n, String ky)       { return dao.getTopDichVu(n, ky); }
    public List<Map<String, Object>> getCheckinHomNay()        { return dao.getCheckinHomNay(); }
    public List<Map<String, Object>> getCheckoutHomNay()       { return dao.getCheckoutHomNay(); }
    public List<Map<String, Object>> getHoatDongGanDay(int n)  { return dao.getHoatDongGanDay(n); }
    public List<Map<String, Object>> getAlerts()               { return dao.getAlerts(); }
    public List<long[]>              getDoanhThu7Ngay()        { return dao.getDoanhThu7Ngay(); }
}
