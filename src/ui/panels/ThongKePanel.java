package ui.panels;

import ui.MainFrame;
import ui.components.UIConstants;
import ui.components.NotificationManager;
import ui.components.RoundedComponents;
import static ui.components.RoundedComponents.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import javax.swing.*;
import service.ThongKeService;
import dao.ThongKeDAO;

// Apache POI Imports - Avoid wildcard to prevent 'Color' and 'Font' ambiguity
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

public class ThongKePanel extends JPanel implements ResettableFilter {

    @Override
    public void resetFilters() {
        currentKy = "thang";
        refresh();
    }

    private final ThongKeService service = new ThongKeService();
    private final ThongKeDAO thongKeDAO = new ThongKeDAO();
    private String currentKy = "thang";

    public ThongKePanel(MainFrame mainFrame) {
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();
        setupRefreshTimer();
    }

    private void setupRefreshTimer() {
        javax.swing.Timer timer = new javax.swing.Timer(30000, e -> {
            if (isShowing()) {
                refresh();
            }
        });
        timer.start();
    }

    private void buildUI() {
        JScrollPane scroll = new JScrollPane(buildContent());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        content.add(buildHeader());
        content.add(Box.createVerticalStrut(16));
        content.add(buildKPIRow());
        content.add(Box.createVerticalStrut(16));
        content.add(buildChartRow());
        content.add(Box.createVerticalStrut(16));
        content.add(buildBottomRow());

        // Final safety: ensure content doesn't exceed viewport unnecessarily
        content.setMaximumSize(new Dimension(1600, Short.MAX_VALUE));
        return content;
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Short.MAX_VALUE, 70));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Báo cáo & Thống kê");
        title.setFont(UIConstants.FONT_TITLE);
        JLabel sub = new JLabel("Tổng quan hiệu quả kinh doanh khách sạn");
        sub.setFont(UIConstants.FONT_BODY);
        sub.setForeground(UIConstants.TEXT_MUTED);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        left.add(title);
        left.add(sub);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.setOpaque(false);
        String[] kys = { "7 ngày", "Tháng này", "Quý này", "Năm nay" };
        String[] keys = { "7ngay", "thang", "quy", "nam" };
        ButtonGroup kyGroup = new ButtonGroup();
        for (int i = 0; i < kys.length; i++) {
            final String k = keys[i];
            JToggleButton btn = new JToggleButton(kys[i]) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (isSelected()) {
                        g2.setColor(UIConstants.PRIMARY);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    } else {
                        g2.setColor(new Color(0xF1F5F9));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btn.setFont(UIConstants.FONT_SMALL_BOLD);
            btn.setFocusPainted(false);
            styleKyBtn(btn, k.equals(currentKy));
            btn.addActionListener(e -> {
                currentKy = k;
                refresh();
            });
            kyGroup.add(btn);
            right.add(btn);
        }

        JToggleButton btnCustom = new JToggleButton("Tùy chỉnh") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isSelected()) {
                    g2.setColor(UIConstants.PRIMARY);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                } else {
                    g2.setColor(new Color(0xF1F5F9));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnCustom.setFont(UIConstants.FONT_SMALL_BOLD);
        btnCustom.setFocusPainted(false);
        styleKyBtn(btnCustom, currentKy.startsWith("custom:"));
        btnCustom.addActionListener(e -> showCustomDateDialog(btnCustom, right));
        kyGroup.add(btnCustom);
        right.add(btnCustom);

        RoundedButton btnExport = new RoundedButton("Xuất", UIConstants.PRIMARY, Color.WHITE);
        btnExport.setPreferredSize(new Dimension(80, 32));
        btnExport.addActionListener(e -> showExportDialog());
        right.add(btnExport);

        panel.add(left, BorderLayout.CENTER);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private void styleKyBtn(JToggleButton btn, boolean active) {
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFont(UIConstants.FONT_SMALL_BOLD);
        btn.setSelected(active);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        if (active) {
            btn.setForeground(Color.WHITE);
        } else {
            btn.setForeground(UIConstants.TEXT_SECONDARY);
        }
    }

    private JPanel buildKPIRow() {
        Map<String, Object> stats = service.getThongKeKy(currentKy);
        Map<String, Object> dashStats = thongKeDAO.getDashboardStats();

        long dt = (long) stats.getOrDefault("doanhThu", 0L);
        int luot = (int) stats.getOrDefault("luotDatPhong", 0);
        int khachMoi = (int) stats.getOrDefault("khachMoi", 0);
        long dtDV = (long) stats.getOrDefault("doanhThuDV", 0L);
        int phongDangThue = (int) stats.getOrDefault("phongDangThue", 0);
        int tongPhong = Math.max(1, (int) dashStats.getOrDefault("tongPhong", 1));
        int congSuat = (int) (100.0 * phongDangThue / tongPhong);

        // Previous period values for trend
        long dtTruoc = (long) stats.getOrDefault("doanhThuTruoc", 0L);
        int luotTruoc = (int) stats.getOrDefault("luotDatPhongTruoc", 0);
        int khachMoiTruoc = (int) stats.getOrDefault("khachMoiTruoc", 0);
        long dtDVTruoc = (long) stats.getOrDefault("doanhThuDVTruoc", 0L);

        JPanel row = new JPanel(new GridLayout(1, 6, 6, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, 110));
        row.setPreferredSize(new Dimension(800, 110)); // Force a smaller preferred width

        String tk = getTenKy();
        String displayKy = (tk.length() > 15) ? tk : "Kỳ " + tk;

        row.add(buildKpiCard("Doanh thu", String.format("%,.0fđ", (double) dt), displayKy,
                UIConstants.PRIMARY, calcTrend(dt, dtTruoc)));
        row.add(buildKpiCard("Công suất phòng", congSuat + "%", phongDangThue + "/" + tongPhong + " phòng",
                UIConstants.SUCCESS, Integer.MIN_VALUE));
        row.add(buildKpiCard("Khách mới", khachMoi + " KH", displayKy,
                UIConstants.INFO, calcTrend(khachMoi, khachMoiTruoc)));
        row.add(buildKpiCard("Lượt check-in", luot + " lượt", displayKy,
                UIConstants.WARNING, calcTrend(luot, luotTruoc)));
        row.add(buildKpiCard("Phòng đang thuê", phongDangThue + " phòng", "Hiện tại",
                new java.awt.Color(0xF97316), Integer.MIN_VALUE));
        row.add(buildKpiCard("Dịch vụ bán thêm", String.format("%,.0fđ", (double) dtDV), displayKy,
                UIConstants.DANGER, calcTrend(dtDV, dtDVTruoc)));
        return row;
    }

    private int calcTrend(long current, long previous) {
        if (previous == 0)
            return current > 0 ? 100 : 0;
        return (int) ((current - previous) * 100 / previous);
    }

    private RoundedPanel buildKpiCard(String label, String value, String subtitle, java.awt.Color accent,
            int trendPct) {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Live pulsing dot for real-time
                if (subtitle.equals("Hiện tại")) {
                    float alpha = (float) (Math.sin(System.currentTimeMillis() / 400.0) * 0.5 + 0.5);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    g2.setColor(UIConstants.SUCCESS);
                    g2.fillOval(getWidth() - 18, 12, 8, 8);
                }

                g2.dispose();
            }
        };
        card.setBackground(java.awt.Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 10, 12));
        card.setPreferredSize(new Dimension(120, 110));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Subtitle at top right
        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(UIConstants.FONT_SMALL);
        lblSub.setForeground(UIConstants.TEXT_MUTED);
        lblSub.setAlignmentX(Component.RIGHT_ALIGNMENT);

        // Push labels down to give space for icon at top left
        content.add(lblSub);
        content.add(Box.createVerticalStrut(14));

        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(UIConstants.FONT_SMALL);
        lblLabel.setForeground(UIConstants.TEXT_SECONDARY);
        lblLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblVal = new JLabel(value);
        lblVal.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblVal.setForeground(UIConstants.TEXT_PRIMARY);
        lblVal.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(lblLabel);
        content.add(Box.createVerticalStrut(2));
        content.add(lblVal);

        if (trendPct != Integer.MIN_VALUE) {
            String arrow = trendPct > 0 ? "↑" : trendPct < 0 ? "↓" : "—";
            java.awt.Color trendColor = trendPct > 0 ? UIConstants.SUCCESS
                    : trendPct < 0 ? UIConstants.DANGER : UIConstants.TEXT_MUTED;
            String trendText = trendPct == 0 ? "— vs kỳ trước"
                    : arrow + " " + Math.abs(trendPct) + "% vs kỳ trước";
            JLabel lblTrend = new JLabel(trendText);
            lblTrend.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lblTrend.setForeground(trendColor);
            lblTrend.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(Box.createVerticalStrut(2));
            content.add(lblTrend);
        }

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildChartRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, 300));
        row.add(buildBarChart());
        row.add(buildDonutChart());
        return row;
    }

    private RoundedPanel buildBarChart() {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(java.awt.Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        String unitLabel = ("nam".equals(currentKy) || "quy".equals(currentKy)) ? "tháng" : "ngày";
        String tk = getTenKy();
        JLabel title = new JLabel(
                "Doanh thu theo " + unitLabel + " — " + (tk.length() > 25 ? tk.substring(0, 22) + "..." : tk));
        title.setToolTipText("Doanh thu theo " + unitLabel + " — " + tk);
        title.setFont(UIConstants.FONT_HEADER);
        JLabel unit = new JLabel("Đơn vị: triệu đồng");
        unit.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        unit.setForeground(UIConstants.TEXT_MUTED);
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.add(title, BorderLayout.WEST);
        hdr.add(unit, BorderLayout.EAST);

        java.util.List<long[]> data = service.getDoanhThuTheoNgay(currentKy);

        card.add(hdr, BorderLayout.NORTH);
        card.add(new ModernBarChart(data), BorderLayout.CENTER);
        return card;
    }

    private RoundedPanel buildDonutChart() {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(java.awt.Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel title = new JLabel("Công suất phòng (%)");
        title.setFont(UIConstants.FONT_HEADER);
        card.add(title, BorderLayout.NORTH);

        // Lấy data thật từ DB
        Map<String, Object> dash = thongKeDAO.getDashboardStats();
        int dangO = (int) dash.getOrDefault("dangO", 0);
        int phongT = (int) dash.getOrDefault("phongTrong", 0);
        int daDat = (int) dash.getOrDefault("daDat", 0);
        int tongP = Math.max(1, (int) dash.getOrDefault("tongPhong", 1));
        int baoTri = Math.max(0, tongP - dangO - phongT - daDat);

        double[] values = { (double) dangO, (double) phongT, (double) daDat, (double) baoTri };
        java.awt.Color[] colors = { UIConstants.SUCCESS, UIConstants.PRIMARY, UIConstants.WARNING, UIConstants.DANGER };
        String[] labels = { "Đang ở", "Trống", "Đặt trước", "Bảo trì" };

        card.add(new ModernDonutChart(values, colors, labels, tongP), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildBottomRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 16, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, 280));
        row.add(buildTopPhongCard());
        row.add(buildTopDichVuCard());
        row.add(buildNguonDatPhongCard());
        return row;
    }

    private RoundedPanel buildTopPhongCard() {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(java.awt.Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel title = new JLabel("Top phòng doanh thu");
        title.setFont(UIConstants.FONT_HEADER);
        card.add(title, BorderLayout.NORTH);

        java.util.List<Map<String, Object>> tops = service.getTopPhong(5, currentKy);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        if (tops.isEmpty()) {
            JLabel empty = new JLabel("Chưa có dữ liệu trong kỳ này");
            empty.setFont(UIConstants.FONT_BODY);
            empty.setForeground(UIConstants.TEXT_MUTED);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            list.add(Box.createVerticalStrut(16));
            list.add(empty);
        } else {
            long maxDT = tops.stream()
                    .mapToLong(t -> t.containsKey("doanhThu") ? (long) t.get("doanhThu") : 0L)
                    .max().orElse(1L);

            java.awt.Color[] barColors = { UIConstants.SUCCESS, UIConstants.PRIMARY, UIConstants.INFO,
                    UIConstants.WARNING, UIConstants.TEXT_SECONDARY };

            for (int i = 0; i < tops.size(); i++) {
                final Map<String, Object> t = tops.get(i);
                final String soPhong = String.valueOf(t.getOrDefault("soPhong", ""));
                final String tenLoai = String.valueOf(t.getOrDefault("tenLoai", ""));
                final long dt = t.containsKey("doanhThu") ? (long) t.get("doanhThu") : 0L;
                final int luot = t.containsKey("luot") ? (int) t.get("luot") : 0;
                final int pct = (int) (100L * dt / maxDT);

                final java.awt.Color barColor = barColors[i % barColors.length];
                final String nm = (i + 1) + ". P." + soPhong + " – " + tenLoai;
                final String dtStr = String.format("%,.0f tr", dt / 1_000_000.0);

                JPanel item = new JPanel(new BorderLayout(6, 0));
                item.setOpaque(false);
                item.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

                JPanel nameRow = new JPanel(new BorderLayout());
                nameRow.setOpaque(false);
                JLabel lblName = new JLabel(nm);
                lblName.setFont(UIConstants.FONT_SMALL);
                JLabel lblInfo = new JLabel(luot + " lượt  " + dtStr);
                lblInfo.setFont(UIConstants.FONT_SMALL);
                lblInfo.setForeground(UIConstants.TEXT_MUTED);
                nameRow.add(lblName, BorderLayout.WEST);
                nameRow.add(lblInfo, BorderLayout.EAST);

                JPanel bar = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new java.awt.Color(0xF1F5F9));
                        g2.fillRoundRect(0, 4, getWidth(), 10, 10, 10);

                        GradientPaint gp = new GradientPaint(0, 0, barColor, getWidth(), 0,
                                new java.awt.Color(barColor.getRed(), barColor.getGreen(), barColor.getBlue(), 180));
                        g2.setPaint(gp);
                        g2.fillRoundRect(0, 4, getWidth() * pct / 100, 10, 10, 10);
                        g2.dispose();
                    }
                };
                bar.setOpaque(false);
                bar.setPreferredSize(new Dimension(0, 18));

                item.add(nameRow, BorderLayout.NORTH);
                item.add(bar, BorderLayout.CENTER);
                list.add(item);
            }
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(8);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private RoundedPanel buildTopDichVuCard() {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(java.awt.Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel title = new JLabel("Dịch vụ bán chạy");
        title.setFont(UIConstants.FONT_HEADER);
        card.add(title, BorderLayout.NORTH);

        java.util.List<Map<String, Object>> dvList = service.getTopDichVu(5, currentKy);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        if (dvList.isEmpty()) {
            JLabel empty = new JLabel("Chưa có dữ liệu dịch vụ");
            empty.setFont(UIConstants.FONT_BODY);
            empty.setForeground(UIConstants.TEXT_MUTED);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            list.add(Box.createVerticalStrut(16));
            list.add(empty);
        } else {
            java.awt.Color[] dvColors = { UIConstants.PRIMARY, UIConstants.INFO, UIConstants.WARNING,
                    UIConstants.SUCCESS, UIConstants.ORANGE };

            for (int i = 0; i < dvList.size(); i++) {
                final Map<String, Object> dv = dvList.get(i);
                final String tenDV = String.valueOf(dv.getOrDefault("tenDV", ""));
                final int soLan = dv.containsKey("soLan") ? (int) dv.get("soLan") : 0;
                final long dtDV = dv.containsKey("doanhThu") ? (long) dv.get("doanhThu") : 0L;

                JPanel item = new JPanel(new BorderLayout());
                item.setOpaque(false);
                item.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

                final java.awt.Color c = dvColors[i % dvColors.length];
                JPanel dot = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(c);
                        g2.fillOval(0, 4, 8, 8);
                        g2.dispose();
                    }
                };
                dot.setOpaque(false);
                dot.setPreferredSize(new Dimension(12, 16));

                JPanel info = new JPanel();
                info.setOpaque(false);
                info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
                JLabel l1 = new JLabel(tenDV);
                l1.setFont(UIConstants.FONT_SMALL_BOLD);
                JLabel l2 = new JLabel(soLan + " lần");
                l2.setFont(UIConstants.FONT_SMALL);
                l2.setForeground(UIConstants.TEXT_MUTED);
                info.add(l1);
                info.add(l2);

                JLabel lDt = new JLabel(String.format("%,.0fđ", (double) dtDV));
                lDt.setFont(UIConstants.FONT_SMALL_BOLD);
                lDt.setForeground(UIConstants.PRIMARY);

                JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
                left.setOpaque(false);
                left.add(dot);
                left.add(info);
                item.add(left, BorderLayout.WEST);
                item.add(lDt, BorderLayout.EAST);
                list.add(item);
            }
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(8);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private RoundedPanel buildNguonDatPhongCard() {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(java.awt.Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel title = new JLabel("Nguồn đặt phòng");
        title.setFont(UIConstants.FONT_HEADER);
        JLabel note = new JLabel("(Demo)");
        note.setFont(UIConstants.FONT_SMALL);
        note.setForeground(UIConstants.TEXT_MUTED);
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.add(title, BorderLayout.WEST);
        hdr.add(note, BorderLayout.EAST);
        card.add(hdr, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        String[][] nguons = { { "Trực tiếp", "38%" }, { "Booking.com", "28%" }, { "Agoda", "18%" }, { "Airbnb", "10%" },
                { "Khác", "6%" } };
        java.awt.Color[] cs = { UIConstants.PRIMARY, UIConstants.SUCCESS, UIConstants.WARNING, UIConstants.INFO,
                UIConstants.TEXT_MUTED };

        for (int i = 0; i < nguons.length; i++) {
            final java.awt.Color barC = cs[i];
            final int pct = Integer.parseInt(nguons[i][1].replace("%", ""));
            JPanel item = new JPanel(new BorderLayout(8, 0));
            item.setOpaque(false);
            item.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            JLabel lName = new JLabel(nguons[i][0]);
            lName.setFont(UIConstants.FONT_SMALL);
            lName.setPreferredSize(new Dimension(90, 16));
            JPanel bar = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new java.awt.Color(0xF1F5F9));
                    g2.fillRoundRect(0, 4, getWidth(), 10, 10, 10);

                    GradientPaint gp = new GradientPaint(0, 0, barC, getWidth(), 0,
                            new java.awt.Color(barC.getRed(), barC.getGreen(), barC.getBlue(), 180));
                    g2.setPaint(gp);
                    g2.fillRoundRect(0, 4, (int) (getWidth() * pct / 100.0), 10, 10, 10);
                    g2.dispose();
                }
            };
            bar.setOpaque(false);
            bar.setPreferredSize(new Dimension(0, 18));
            JLabel lPct = new JLabel(nguons[i][1]);
            lPct.setFont(UIConstants.FONT_SMALL_BOLD);
            lPct.setForeground(barC);
            lPct.setPreferredSize(new Dimension(34, 16));
            item.add(lName, BorderLayout.WEST);
            item.add(bar, BorderLayout.CENTER);
            item.add(lPct, BorderLayout.EAST);
            list.add(item);
        }
        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private void showExportDialog() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel lbTitle = new JLabel("Chọn nội dung muốn xuất:");
        lbTitle.setFont(UIConstants.FONT_BODY_BOLD);
        lbTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbTitle);
        panel.add(Box.createVerticalStrut(12));

        JCheckBox chkThongKeKy = new JCheckBox("Thống kê tổng quan (" + getTenKy() + ")");
        JCheckBox chkTheoNgay = new JCheckBox("Doanh thu theo ngày");
        JCheckBox chkTopPhong = new JCheckBox("Top phòng doanh thu");
        JCheckBox chkTopDichVu = new JCheckBox("Dịch vụ bán chạy");

        for (JCheckBox cb : new JCheckBox[] { chkThongKeKy, chkTheoNgay, chkTopPhong, chkTopDichVu }) {
            cb.setSelected(true);
            cb.setFont(UIConstants.FONT_BODY);
            cb.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(cb);
            panel.add(Box.createVerticalStrut(6));
        }

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Xuất báo cáo Excel",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION)
            return;
        if (!chkThongKeKy.isSelected() && !chkTheoNgay.isSelected()
                && !chkTopPhong.isSelected() && !chkTopDichVu.isSelected()) {
            NotificationManager.showWarning("Cảnh báo", "Vui lòng chọn ít nhất 1 mục!");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Lưu file báo cáo");
        chooser.setSelectedFile(new File("BaoCaoThongKe_" + getTenKy() + "_"
                + new java.text.SimpleDateFormat("yyyyMMdd_HHmm").format(new java.util.Date()) + ".xlsx"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".xlsx"))
            file = new File(file.getAbsolutePath() + ".xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Báo cáo " + getTenKy());

            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setFontHeightInPoints((short) 18);
            titleFont.setBold(true);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.LEFT);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.cloneStyleFrom(dataStyle);
            currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0 \"đ\""));

            CellStyle sectionStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font sFont = workbook.createFont();
            sFont.setBold(true);
            sFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            sectionStyle.setFont(sFont);

            int rowIdx = 0;
            Row r0 = sheet.createRow(rowIdx++);
            r0.createCell(0).setCellValue("HOTEL OHNO - HỆ THỐNG QUẢN LÝ KHÁCH SẠN");
            r0.getCell(0).setCellStyle(sectionStyle);

            Row r1 = sheet.createRow(rowIdx++);
            Cell cTitle = r1.createCell(0);
            cTitle.setCellValue("BÁO CÁO THỐNG KÊ - " + getTenKy().toUpperCase());
            cTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 4));

            Row r2 = sheet.createRow(rowIdx++);
            r2.createCell(0).setCellValue(
                    "Ngày xuất: " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()));
            r2.getCell(0).setCellStyle(sectionStyle);
            rowIdx++;

            if (chkThongKeKy.isSelected()) {
                Map<String, Object> stats = service.getThongKeKy(currentKy);
                Row rs = sheet.createRow(rowIdx++);
                rs.createCell(0).setCellValue("=== THỐNG KÊ TỔNG QUAN ===");
                rs.getCell(0).setCellStyle(sectionStyle);

                String[] kpiH = { "Chỉ tiêu", "Giá trị" };
                Row h = sheet.createRow(rowIdx++);
                for (int i = 0; i < 2; i++) {
                    Cell c = h.createCell(i);
                    c.setCellValue(kpiH[i]);
                    c.setCellStyle(headerStyle);
                }

                String[][] items = {
                        { "Doanh thu phòng", String.valueOf(stats.getOrDefault("doanhThu", 0)) },
                        { "Lượt đặt phòng", String.valueOf(stats.getOrDefault("luotDatPhong", 0)) },
                        { "Khách mới", String.valueOf(stats.getOrDefault("khachMoi", 0)) },
                        { "Doanh thu dịch vụ", String.valueOf(stats.getOrDefault("doanhThuDV", 0)) }
                };
                for (String[] item : items) {
                    Row r = sheet.createRow(rowIdx++);
                    r.createCell(0).setCellValue(item[0]);
                    r.getCell(0).setCellStyle(dataStyle);
                    Cell cVal = r.createCell(1);
                    cVal.setCellValue(Double.parseDouble(item[1]));
                    if (item[0].contains("Doanh thu"))
                        cVal.setCellStyle(currencyStyle);
                    else
                        cVal.setCellStyle(dataStyle);
                }
                rowIdx++;
            }

            if (chkTheoNgay.isSelected()) {
                Row rs = sheet.createRow(rowIdx++);
                rs.createCell(0).setCellValue("=== DOANH THU THEO NGÀY ===");
                rs.getCell(0).setCellStyle(sectionStyle);

                Row h = sheet.createRow(rowIdx++);
                String[] heads = { "Ngày", "Doanh thu (VNĐ)" };
                for (int i = 0; i < 2; i++) {
                    Cell c = h.createCell(i);
                    c.setCellValue(heads[i]);
                    c.setCellStyle(headerStyle);
                }

                long tong = 0;
                for (long[] d : service.getDoanhThuTheoNgay(currentKy)) {
                    Row r = sheet.createRow(rowIdx++);
                    r.createCell(0).setCellValue("Ngày " + d[0]);
                    r.getCell(0).setCellStyle(dataStyle);
                    Cell cDt = r.createCell(1);
                    cDt.setCellValue((double) d[1]);
                    cDt.setCellStyle(currencyStyle);
                    tong += d[1];
                }
                Row rTotal = sheet.createRow(rowIdx++);
                Cell cL = rTotal.createCell(0);
                cL.setCellValue("TỔNG CỘNG");
                cL.setCellStyle(headerStyle);
                Cell cT = rTotal.createCell(1);
                cT.setCellValue((double) tong);
                cT.setCellStyle(currencyStyle);
                rowIdx++;
            }

            if (chkTopPhong.isSelected()) {
                Row rs = sheet.createRow(rowIdx++);
                rs.createCell(0).setCellValue("=== TOP PHÒNG DOANH THU ===");
                rs.getCell(0).setCellStyle(sectionStyle);

                Row h = sheet.createRow(rowIdx++);
                String[] heads = { "STT", "Số phòng", "Loại phòng", "Doanh thu", "Lượt thuê" };
                for (int i = 0; i < 5; i++) {
                    Cell c = h.createCell(i);
                    c.setCellValue(heads[i]);
                    c.setCellStyle(headerStyle);
                }

                int stt = 1;
                for (Map<String, Object> m : service.getTopPhong(10, currentKy)) {
                    Row r = sheet.createRow(rowIdx++);
                    r.createCell(0).setCellValue(stt++);
                    r.getCell(0).setCellStyle(dataStyle);
                    r.createCell(1).setCellValue(String.valueOf(m.getOrDefault("soPhong", "")));
                    r.getCell(1).setCellStyle(dataStyle);
                    r.createCell(2).setCellValue(String.valueOf(m.getOrDefault("tenLoai", "")));
                    r.getCell(2).setCellStyle(dataStyle);
                    Cell cDt = r.createCell(3);
                    cDt.setCellValue(Double.parseDouble(String.valueOf(m.getOrDefault("doanhThu", 0))));
                    cDt.setCellStyle(currencyStyle);
                    r.createCell(4).setCellValue(Integer.parseInt(String.valueOf(m.getOrDefault("luot", 0))));
                    r.getCell(4).setCellStyle(dataStyle);
                }
                rowIdx++;
            }

            if (chkTopDichVu.isSelected()) {
                Row rs = sheet.createRow(rowIdx++);
                rs.createCell(0).setCellValue("=== DỊCH VỤ BÁN CHẠY ===");
                rs.getCell(0).setCellStyle(sectionStyle);

                Row h = sheet.createRow(rowIdx++);
                String[] heads = { "STT", "Tên dịch vụ", "Số lần dùng", "Doanh thu tổng" };
                for (int i = 0; i < 4; i++) {
                    Cell c = h.createCell(i);
                    c.setCellValue(heads[i]);
                    c.setCellStyle(headerStyle);
                }

                int stt = 1;
                for (Map<String, Object> m : service.getTopDichVu(10, currentKy)) {
                    Row r = sheet.createRow(rowIdx++);
                    r.createCell(0).setCellValue(stt++);
                    r.getCell(0).setCellStyle(dataStyle);
                    r.createCell(1).setCellValue(String.valueOf(m.getOrDefault("tenDV", "")));
                    r.getCell(1).setCellStyle(dataStyle);
                    r.createCell(2).setCellValue(Integer.parseInt(String.valueOf(m.getOrDefault("soLan", 0))));
                    r.getCell(2).setCellStyle(dataStyle);
                    Cell cDt = r.createCell(3);
                    cDt.setCellValue(Double.parseDouble(String.valueOf(m.getOrDefault("doanhThu", 0))));
                    cDt.setCellStyle(currencyStyle);
                }
            }

            for (int i = 0; i < 6; i++)
                sheet.autoSizeColumn(i);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            NotificationManager.showSuccess("Thành công", "✓ Xuất báo cáo Excel thành công!");

        } catch (Exception ex) {
            NotificationManager.showError("Lỗi", "Lỗi xuất file Excel: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private String getTenKy() {
        if (currentKy != null && currentKy.startsWith("custom:")) {
            try {
                String[] parts = currentKy.split(":");
                java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyy-MM-dd");
                java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("dd/MM/yyyy");
                return out.format(in.parse(parts[1])) + " - " + out.format(in.parse(parts[2]));
            } catch (Exception ex) {
            }
            return "Tùy chỉnh";
        }
        switch (currentKy != null ? currentKy : "thang") {
            case "7ngay":
                return "7 ngày";
            case "quy":
                return "Quý này";
            case "nam":
                return "Năm nay";
            default:
                return "Tháng này";
        }
    }

    private void showCustomDateDialog(JToggleButton btnCustom, JPanel right) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Tùy chỉnh thời gian",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.setResizable(false);

        JPanel pnl = new JPanel(new GridLayout(2, 2, 10, 10));
        pnl.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        pnl.setBackground(java.awt.Color.WHITE);

        JLabel lbTu = new JLabel("Từ ngày:");
        lbTu.setFont(UIConstants.FONT_BODY);
        ui.components.DatePicker dpTu = new ui.components.DatePicker(new java.util.Date());
        dpTu.setPreferredSize(new Dimension(140, 38));

        JLabel lbDen = new JLabel("Đến ngày:");
        lbDen.setFont(UIConstants.FONT_BODY);
        ui.components.DatePicker dpDen = new ui.components.DatePicker(new java.util.Date());
        dpDen.setPreferredSize(new Dimension(140, 38));

        pnl.add(lbTu);
        pnl.add(dpTu);
        pnl.add(lbDen);
        pnl.add(dpDen);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(java.awt.Color.WHITE);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 10));

        RoundedButton btnOk = new RoundedButton("Xác nhận", UIConstants.PRIMARY, java.awt.Color.WHITE);
        btnOk.setFont(UIConstants.FONT_BODY_BOLD);
        btnOk.setPreferredSize(new Dimension(100, 38));
        btnOk.addActionListener(ev -> {
            java.util.Date d1 = dpTu.getDate();
            java.util.Date d2 = dpDen.getDate();
            if (d1.after(d2)) {
                JOptionPane.showMessageDialog(dialog, "Ngày bắt đầu không được lớn hơn ngày kết thúc!", "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            currentKy = "custom:" + sdf.format(d1) + ":" + sdf.format(d2);
            dialog.dispose();

            for (Component c : right.getComponents()) {
                if (c instanceof JToggleButton) {
                    JToggleButton tb = (JToggleButton) c;
                    styleKyBtn(tb, tb == btnCustom);
                }
            }
            refresh();
        });
        btnPanel.add(btnOk);

        dialog.add(pnl, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setSize(new Dimension(Math.max(dialog.getWidth(), 350), Math.max(dialog.getHeight(), 180)));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    public void refresh() {
        removeAll();
        buildUI();
        revalidate();
        repaint();
    }

    private class ModernBarChart extends JPanel {
        private final java.util.List<long[]> data;
        private double animationProgress = 0;
        private int hoverIndex = -1;
        private Point mousePos = null;

        public ModernBarChart(java.util.List<long[]> data) {
            this.data = data;
            setOpaque(false);

            javax.swing.Timer timer = new javax.swing.Timer(15, null);
            timer.addActionListener(e -> {
                animationProgress += 0.05;
                if (animationProgress >= 1) {
                    animationProgress = 1;
                    timer.stop();
                }
                repaint();
            });
            timer.start();

            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override
                public void mouseMoved(java.awt.event.MouseEvent e) {
                    mousePos = e.getPoint();
                    int w = getWidth();
                    int padL = 36, padR = 10;
                    int chartW = w - padL - padR;
                    if (data.isEmpty())
                        return;

                    int index = (e.getX() - padL) / (chartW / data.size());
                    if (index >= 0 && index < data.size()) {
                        if (hoverIndex != index) {
                            hoverIndex = index;
                            repaint();
                        }
                    } else {
                        if (hoverIndex != -1) {
                            hoverIndex = -1;
                            repaint();
                        }
                    }
                }
            });

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    hoverIndex = -1;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int padL = 36, padB = 24, padT = 10, padR = 10;
            int chartW = w - padL - padR;
            int chartH = h - padB - padT;

            g2.setColor(new java.awt.Color(0xF1F5F9));
            long maxVal = data.stream().mapToLong(d -> d[1]).max().orElse(1L);
            long stepVal = (long) Math.ceil(maxVal / 4.0 / 1_000_000.0) * 1_000_000;
            if (stepVal == 0)
                stepVal = 10_000_000;

            for (int i = 0; i <= 4; i++) {
                int y = padT + chartH - (chartH * i / 4);
                g2.drawLine(padL, y, w - padR, y);
                g2.setFont(UIConstants.FONT_SMALL);
                g2.setColor(UIConstants.TEXT_MUTED);
                g2.drawString(String.format("%.0f", (stepVal * i) / 1_000_000.0), 2, y + 4);
                g2.setColor(new java.awt.Color(0xF1F5F9));
            }

            if (data.isEmpty()) {
                g2.dispose();
                return;
            }

            int barW = Math.max(4, chartW / data.size() - 6);

            for (int i = 0; i < data.size(); i++) {
                long val = data.get(i)[1];
                int barH = (int) (chartH * (val / (double) (stepVal * 4)) * animationProgress);
                int x = padL + i * (chartW / data.size()) + 3;
                int y = padT + chartH - barH;

                Color c1 = (i == hoverIndex) ? UIConstants.PRIMARY : new java.awt.Color(0x93C5FD);
                Color c2 = (i == hoverIndex) ? new java.awt.Color(0x1E40AF) : new java.awt.Color(0xBFDBFE);

                GradientPaint gp = new GradientPaint(x, y, c1, x, h - padB, c2);
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(x, y, barW, barH, 6, 6));

                if (data.size() < 15 || (data.size() < 40 && i % 5 == 0) || (data.size() >= 40 && i % 10 == 0)
                        || i == data.size() - 1) {
                    g2.setColor(UIConstants.TEXT_MUTED);
                    long val0 = data.get(i)[0];
                    String lbl = String.valueOf(val0);

                    if ("nam".equals(currentKy) || "quy".equals(currentKy)) {
                        lbl = "T" + val0;
                    } else if (currentKy.startsWith("custom:") && data.get(i).length > 2) {
                        long raw = data.get(i)[2];
                        if (raw > 10000 && raw < 1000000) { // YYYYMM
                            lbl = (raw % 100) + "/" + String.valueOf(raw / 100).substring(2);
                        } else if (raw > 1000000) { // YYYYMMDD
                            lbl = (raw % 100) + "/" + ((raw / 100) % 100);
                        }
                    }
                    g2.drawString(lbl, x + barW / 2 - g2.getFontMetrics().stringWidth(lbl) / 2, h - 6);
                }
            }

            if (hoverIndex != -1 && mousePos != null) {
                long[] entry = data.get(hoverIndex);
                String timeText = "";
                if (currentKy.startsWith("custom:") && entry.length > 2) {
                    long raw = entry[2];
                    if (raw > 1000000) { // YYYYMMDD
                        String s = String.valueOf(raw);
                        timeText = s.substring(6, 8) + "/" + s.substring(4, 6) + "/" + s.substring(0, 4);
                    } else if (raw > 10000) { // YYYYMM
                        timeText = "Tháng " + (raw % 100) + "/" + (raw / 100);
                    }
                } else {
                    String prefix = ("nam".equals(currentKy) || "quy".equals(currentKy)) ? "Tháng " : "Ngày ";
                    timeText = prefix + entry[0];
                }

                String amount = String.format("%,.0f VNĐ", (double) entry[1]);
                String txt = timeText + ": " + amount;
                g2.setFont(UIConstants.FONT_SMALL_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(txt) + 16;
                int th = 28;
                int tx = Math.min(w - tw - 5, Math.max(5, mousePos.x - tw / 2));
                int ty = mousePos.y - th - 10;

                g2.setColor(new java.awt.Color(0x1E293B));
                g2.fillRoundRect(tx, ty, tw, th, 8, 8);
                g2.setColor(java.awt.Color.WHITE);
                g2.drawString(txt, tx + 8, ty + 18);
            }

            g2.dispose();
        }
    }

    private class ModernDonutChart extends JPanel {
        private final double[] values;
        private final java.awt.Color[] colors;
        private final String[] labels;
        private final int total;
        private int hoverIndex = -1;
        private double animationProgress = 0;

        public ModernDonutChart(double[] values, java.awt.Color[] colors, String[] labels, int total) {
            this.values = values;
            this.colors = colors;
            this.labels = labels;
            this.total = total;
            setOpaque(false);

            javax.swing.Timer timer = new javax.swing.Timer(20, null);
            timer.addActionListener(e -> {
                animationProgress += 0.04;
                if (animationProgress >= 1) {
                    animationProgress = 1;
                    timer.stop();
                }
                repaint();
            });
            timer.start();

            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override
                public void mouseMoved(java.awt.event.MouseEvent e) {
                    int cx = getWidth() / 2 - 40, cy = getHeight() / 2;
                    double dx = e.getX() - cx;
                    double dy = e.getY() - cy;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    int r = Math.min(cx, cy) - 10;
                    int innerR = (int) (r * 0.6);

                    if (dist > innerR && dist < r) {
                        double angle = Math.toDegrees(Math.atan2(-dy, dx));
                        if (angle < 0)
                            angle += 360;
                        double currentAngle = 90;
                        int newHover = -1;
                        for (int i = 0; i < values.length; i++) {
                            double sweep = (360.0 * values[i] / total);
                            double endAngle = currentAngle - sweep;
                            double normalizedAngle = angle;
                            double start = currentAngle;
                            double end = endAngle;
                            if (isAngleBetween(normalizedAngle, end, start)) {
                                newHover = i;
                                break;
                            }
                            currentAngle -= sweep;
                        }
                        if (hoverIndex != newHover) {
                            hoverIndex = newHover;
                            repaint();
                        }
                    } else {
                        if (hoverIndex != -1) {
                            hoverIndex = -1;
                            repaint();
                        }
                    }
                }

                private boolean isAngleBetween(double a, double start, double end) {
                    a = (a % 360 + 360) % 360;
                    start = (start % 360 + 360) % 360;
                    end = (end % 360 + 360) % 360;
                    if (start <= end)
                        return a >= start && a <= end;
                    return a >= start || a <= end;
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int cx = w / 2 - 50, cy = h / 2;
            int r = Math.min(cx, cy) - 15;
            int innerR = (int) (r * 0.65);

            double currentAngle = 90;
            for (int i = 0; i < values.length; i++) {
                if (values[i] <= 0)
                    continue;
                double sweep = (360.0 * values[i] / total) * animationProgress;
                int drawR = r;
                if (i == hoverIndex)
                    drawR += 5;
                g2.setColor(colors[i]);
                g2.fillArc(cx - drawR, cy - drawR, drawR * 2, drawR * 2, (int) currentAngle, (int) -sweep);
                currentAngle -= (360.0 * values[i] / total);
            }

            g2.setColor(java.awt.Color.WHITE);
            g2.fillOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);

            if (hoverIndex != -1) {
                g2.setColor(colors[hoverIndex]);
                g2.setFont(UIConstants.FONT_SMALL_BOLD);
                String label = labels[hoverIndex];
                g2.drawString(label, cx - g2.getFontMetrics().stringWidth(label) / 2, cy - 5);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
                String val = String.format("%.0f", values[hoverIndex]);
                g2.drawString(val, cx - g2.getFontMetrics().stringWidth(val) / 2, cy + 15);
            } else {
                g2.setColor(UIConstants.TEXT_PRIMARY);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                String ct = (int) values[0] + "/" + total;
                g2.drawString(ct, cx - g2.getFontMetrics().stringWidth(ct) / 2, cy + 5);
                g2.setFont(UIConstants.FONT_SMALL);
                g2.setColor(UIConstants.TEXT_MUTED);
                g2.drawString("phòng đang ở", cx - g2.getFontMetrics().stringWidth("phòng đang ở") / 2, cy + 20);
            }

            int lx = cx + r + 30;
            int ly = cy - (labels.length * 25) / 2;
            for (int i = 0; i < labels.length; i++) {
                g2.setColor(colors[i]);
                g2.fillOval(lx, ly + i * 25 + 5, 10, 10);
                g2.setColor(i == hoverIndex ? UIConstants.TEXT_PRIMARY : UIConstants.TEXT_SECONDARY);
                g2.setFont(i == hoverIndex ? UIConstants.FONT_SMALL_BOLD : UIConstants.FONT_SMALL);
                g2.drawString(labels[i] + ": " + (int) values[i], lx + 18, ly + i * 25 + 14);
            }
            g2.dispose();
        }
    }
}
