package ui.dialogs;

import dao.ChiTietHoaDonDAO;
import entity.ChiTietHoaDon;
import entity.HoaDon;
import entity.enums.LoaiChiTietHoaDon;
import ui.components.UIConstants;
import ui.components.RoundedComponents.*;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class XemHoaDonDialog extends JDialog {

    private final HoaDon hoaDon;
    private final ChiTietHoaDonDAO cthdDAO = new ChiTietHoaDonDAO();
    private List<ChiTietHoaDon> dsChiTiet;

    public XemHoaDonDialog(Frame parent, HoaDon hd) {
        super(parent, "Chi tiết Hóa đơn - " + hd.getMaHoaDon(), true);
        this.hoaDon = hd;
        setSize(580, 680);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(Color.WHITE);

        // Fetch details
        dsChiTiet = cthdDAO.getByHoaDon(hd.getMaHoaDon());

        // Fallback cho dữ liệu cũ (chưa lưu ChiTietHoaDon vào SQL)
        if (dsChiTiet.isEmpty() && (hd.getTongTienPhong() > 0 || hd.getTongTienDichVu() > 0)) {
            if (hd.getTongTienPhong() > 0) {
                ChiTietHoaDon ctPhong = new ChiTietHoaDon();
                ctPhong.setNoiDung("Tiền phòng (Dữ liệu cũ)");
                ctPhong.setLoaiChiTiet(LoaiChiTietHoaDon.PHONG);
                ctPhong.setDonGia(hd.getTongTienPhong());
                ctPhong.setSoLuong(1);
                ctPhong.setThanhTien(hd.getTongTienPhong());
                dsChiTiet.add(ctPhong);
            }
            if (hd.getTongTienDichVu() > 0) {
                ChiTietHoaDon ctDV = new ChiTietHoaDon();
                ctDV.setNoiDung("Tiền dịch vụ (Dữ liệu cũ)");
                ctDV.setLoaiChiTiet(LoaiChiTietHoaDon.DICH_VU);
                ctDV.setDonGia(hd.getTongTienDichVu());
                ctDV.setSoLuong(1);
                ctDV.setThanhTien(hd.getTongTienDichVu());
                dsChiTiet.add(ctDV);
            }
        }

        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        add(buildBanner(), BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(buildBody());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildBanner() {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(UIConstants.PRIMARY);
        banner.setBorder(BorderFactory.createEmptyBorder(18, 28, 18, 28));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel t1 = new JLabel("CHI TIẾT HÓA ĐƠN");
        t1.setFont(new Font("Segoe UI", Font.BOLD, 20));
        t1.setForeground(Color.WHITE);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String ngay = hoaDon.getNgayLap() != null ? hoaDon.getNgayLap().format(dtf) : "—";
        JLabel t2 = new JLabel("Ngày lập: " + ngay);
        t2.setFont(UIConstants.FONT_BODY);
        t2.setForeground(new Color(255, 255, 255, 200));
        left.add(t1);
        left.add(Box.createVerticalStrut(2));
        left.add(t2);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JLabel lma = new JLabel(hoaDon.getMaHoaDon());
        lma.setFont(UIConstants.FONT_TITLE);
        lma.setForeground(Color.WHITE);
        lma.setHorizontalAlignment(SwingConstants.RIGHT);

        String tt = hoaDon.getTrangThai() != null ? hoaDon.getTrangThai().toString() : "—";
        JLabel lst = new JLabel("Trạng thái: " + tt);
        lst.setFont(UIConstants.FONT_SMALL);
        lst.setForeground(new Color(255, 255, 255, 180));
        lst.setHorizontalAlignment(SwingConstants.RIGHT);
        right.add(lma);
        right.add(Box.createVerticalStrut(2));
        right.add(lst);

        banner.add(left, BorderLayout.WEST);
        banner.add(right, BorderLayout.EAST);
        return banner;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel();
        body.setBackground(Color.WHITE);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(20, 28, 16, 28));

        body.add(buildInfoSection());
        body.add(Box.createVerticalStrut(18));
        body.add(buildBillSection());
        body.add(Box.createVerticalStrut(16));
        body.add(buildSummarySection());
        return body;
    }

    private JPanel buildInfoSection() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 24, 0));
        panel.setOpaque(false);

        String dp = hoaDon.getDatPhong() != null ? hoaDon.getDatPhong().getMaDatPhong() : "—";
        String nv = hoaDon.getNhanVien() != null ? hoaDon.getNhanVien().getMaNhanVien() : "—";

        panel.add(infoBlock("THÔNG TIN", new String[][] {
                { "Mã hợp đồng:", dp },
                { "Khách hàng:", "Xem theo mã hợp đồng" },
                { "Thu ngân:", nv }
        }));

        panel.add(infoBlock("GHI CHÚ", new String[][] {
                { "Hóa đơn này chỉ có giá trị", "xem lại." },
                { "Mọi thắc mắc xin liên hệ", "quản trị viên." }
        }));

        return panel;
    }

    private JPanel infoBlock(String heading, String[][] rows) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel lbl = new JLabel(heading);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_MUTED);
        panel.add(lbl);
        panel.add(Box.createVerticalStrut(8));
        for (String[] row : rows) {
            JPanel r = new JPanel(new BorderLayout(8, 0));
            r.setOpaque(false);
            r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
            JLabel k = new JLabel(row[0]);
            k.setFont(UIConstants.FONT_SMALL);
            k.setForeground(UIConstants.TEXT_SECONDARY);
            k.setPreferredSize(new Dimension(90, 18));
            JLabel v = new JLabel(row[1]);
            v.setFont(UIConstants.FONT_SMALL);
            r.add(k, BorderLayout.WEST);
            r.add(v, BorderLayout.CENTER);
            panel.add(r);
            panel.add(Box.createVerticalStrut(3));
        }
        return panel;
    }

    private JPanel buildBillSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        JLabel hdr = new JLabel("BẢNG CHI TIẾT");
        hdr.setFont(UIConstants.FONT_SMALL_BOLD);
        hdr.setForeground(UIConstants.TEXT_MUTED);
        hdr.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        String[] cols = { "Khoản mục", "Loại", "Đơn giá", "SL", "Thành tiền" };
        DefaultTableModel tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        table.setFont(UIConstants.FONT_BODY);
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(UIConstants.PRIMARY_LIGHT);
        table.setSelectionForeground(UIConstants.TEXT_PRIMARY); // Set selection text color
        table.getTableHeader().setFont(UIConstants.FONT_SMALL_BOLD);
        table.getTableHeader().setBackground(UIConstants.BG_TABLE_HEADER);
        table.getTableHeader().setForeground(UIConstants.TEXT_SECONDARY);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER));

        DefaultTableCellRenderer rightRender = new DefaultTableCellRenderer();
        rightRender.setHorizontalAlignment(JLabel.RIGHT);
        table.getColumnModel().getColumn(2).setCellRenderer(rightRender);
        table.getColumnModel().getColumn(3).setCellRenderer(rightRender);
        table.getColumnModel().getColumn(4).setCellRenderer(rightRender);

        for (ChiTietHoaDon ct : dsChiTiet) {
            tableModel.addRow(new Object[] {
                    ct.getNoiDung(),
                    ct.getLoaiChiTiet() != null ? ct.getLoaiChiTiet().toString() : "",
                    String.format("%,.0f đ", ct.getDonGia()),
                    ct.getSoLuong(),
                    String.format("%,.0f đ", ct.getThanhTien())
            });
        }

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        sp.setPreferredSize(new Dimension(0, 200));
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        panel.add(hdr, BorderLayout.NORTH);
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSummarySection() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));

        panel.add(Box.createVerticalStrut(10));
        panel.add(sumRow("Tổng tiền phòng", String.format("%,.0f đ", hoaDon.getTongTienPhong()), false));
        panel.add(Box.createVerticalStrut(4));
        panel.add(sumRow("Tổng tiền dịch vụ", String.format("%,.0f đ", hoaDon.getTongTienDichVu()), false));
        panel.add(Box.createVerticalStrut(4));
        panel.add(sumRow("Giảm giá / Khuyến mãi", String.format("%,.0f đ", hoaDon.getTienGiamKhuyenMai()), false));
        panel.add(Box.createVerticalStrut(6));
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(sep);
        panel.add(Box.createVerticalStrut(6));
        panel.add(sumRow("TỔNG THANH TOÁN", String.format("%,.0f đ", hoaDon.getTongThanhToan()), true));
        return panel;
    }

    private JPanel sumRow(String label, String val, boolean bold) {
        JPanel r = new JPanel(new BorderLayout());
        r.setOpaque(false);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JLabel lbl = new JLabel(label);
        lbl.setFont(bold ? new Font("Segoe UI", Font.BOLD, 15) : UIConstants.FONT_BODY);
        lbl.setForeground(bold ? UIConstants.PRIMARY : UIConstants.TEXT_SECONDARY);

        JLabel vLbl = new JLabel(val);
        vLbl.setFont(bold ? new Font("Segoe UI", Font.BOLD, 18) : UIConstants.FONT_BODY);
        vLbl.setForeground(bold ? UIConstants.PRIMARY : UIConstants.TEXT_PRIMARY);

        r.add(lbl, BorderLayout.WEST);
        r.add(vLbl, BorderLayout.EAST);
        return r;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER));

        RoundedButton btnPrint = new RoundedButton("In lại hóa đơn", UIConstants.SUCCESS, Color.WHITE);
        btnPrint.addActionListener(e -> printHoaDon());

        RoundedButton btnClose = new RoundedButton("Đóng", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        btnClose.addActionListener(e -> dispose());

        footer.add(btnPrint);
        footer.add(btnClose);
        return footer;
    }

    // ---- PRINT ----
    public void printHoaDon() {
        util.PDFExporter.exportHoaDon(this, hoaDon, dsChiTiet);
    }
}
