package util;

import entity.KhachHang;
import entity.NhanVien;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Xuất dữ liệu ra file Excel (.xlsx thực sự) bằng Apache POI.
 */
public class ExcelExporter {

    /**
     * Xuất JTable ra file Excel.
     */
    public static void exportTable(Component parent, DefaultTableModel model, String title, int... hiddenCols) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Lưu file Excel (.xlsx)");
        chooser.setSelectedFile(
                new File(title + "_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".xlsx"));

        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION)
            return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".xlsx")) {
            file = new File(file.getAbsolutePath() + ".xlsx");
        }

        try (Workbook workbook = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(file)) {
            Sheet sheet = workbook.createSheet(title);
            Row headerRow = sheet.createRow(0);

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            int currentCell = 0;
            // Header row
            for (int c = 0; c < model.getColumnCount(); c++) {
                if (isHidden(c, hiddenCols))
                    continue;
                Cell cell = headerRow.createCell(currentCell++);
                cell.setCellValue(model.getColumnName(c));
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            for (int r = 0; r < model.getRowCount(); r++) {
                Row row = sheet.createRow(r + 1);
                currentCell = 0;
                for (int c = 0; c < model.getColumnCount(); c++) {
                    if (isHidden(c, hiddenCols))
                        continue;
                    Object val = model.getValueAt(r, c);
                    Cell cell = row.createCell(currentCell++);
                    if (val instanceof Number) {
                        cell.setCellValue(((Number) val).doubleValue());
                    } else {
                        cell.setCellValue(val != null ? val.toString() : "");
                    }
                }
            }

            for (int i = 0; i < currentCell; i++)
                sheet.autoSizeColumn(i);

            workbook.write(fos);
            JOptionPane.showMessageDialog(parent,
                    "Xu\u1ea5t Excel th\u00e0nh c\u00f4ng!\nFile: " + file.getAbsolutePath(), "Th\u00e0nh c\u00f4ng",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent, "L\u1ed7i xu\u1ea5t file Excel: " + e.getMessage(), "L\u1ed7i",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Xuất danh sách KhachHang ra Excel
     */
    public static void exportKhachHang(Component parent, List<KhachHang> list) {
        String[] cols = { "M\u00e3 KH", "H\u1ecd t\u00ean", "S\u0110T", "CCCD / H\u1ed9 Chi\u1ebfu" };
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        for (KhachHang kh : list)
            model.addRow(new Object[] { kh.getMaKhachHang(), kh.getHoTen(),
                    kh.getSoDienThoai() != null ? kh.getSoDienThoai() : kh.getSdt(),
                    kh.getCccd() != null ? kh.getCccd() : kh.getSoHoChieu() });
        exportTable(parent, model, "DanhSachKhachHang");
    }

    public static void exportNhanVien(Component parent, List<NhanVien> list) {
        String[] cols = { "M\u00e3 NV", "H\u1ecd t\u00ean", "S\u0110T", "Ch\u1ee9c v\u1ee5", "Vai tr\u00f2" };
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        for (NhanVien nv : list) {
            String vaiTro = (nv.getTaiKhoan() != null && nv.getTaiKhoan().getVaiTro() != null)
                    ? nv.getTaiKhoan().getVaiTro().name()
                    : "N/A";
            model.addRow(new Object[] { nv.getMaNhanVien(), nv.getHoTen(), nv.getSdt(), nv.getChucVu(), vaiTro });
        }
        exportTable(parent, model, "DanhSachNhanVien");
    }

    /**
     * Xuất danh sách HoaDon ra Excel
     */
    public static void exportHoaDon(Component parent, List<entity.HoaDon> list) {
        String[] cols = { "Mã HĐ", "Ngày lập", "Khách hàng", "Tiền phòng", "Cộng thêm", "Tổng tiền", "Trạng thái" };
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        
        for (entity.HoaDon hd : list) {
            String tenKh = (hd.getKhachHang() != null) ? hd.getKhachHang().getHoTen() : "Khách vãng lai";
            String ngayLapStr = (hd.getNgayLap() != null) 
                ? hd.getNgayLap().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) 
                : "";
            
            model.addRow(new Object[] {
                hd.getMaHoaDon(),
                ngayLapStr,
                tenKh,
                hd.getTongTienPhong(),
                hd.getTongTienDichVu(),
                hd.getTongThanhToan(),
                hd.getTrangThai() != null ? hd.getTrangThai().name() : ""
            });
        }
        exportTable(parent, model, "BaoCaoHoaDon");
    }

    private static boolean isHidden(int col, int[] hidden) {
        for (int h : hidden)
            if (h == col)
                return true;
        return false;
    }
}
