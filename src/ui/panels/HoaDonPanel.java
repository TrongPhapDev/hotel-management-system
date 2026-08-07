package ui.panels;

import dao.HoaDonDAO;
import entity.HoaDon;
import service.DatPhongService;
import service.KhachHangService;
import entity.DatPhong;
import entity.KhachHang;
import ui.MainFrame;
import ui.components.NotificationManager;
import ui.components.PaginationPanel;
import ui.components.UIConstants;
import ui.components.RoundedComponents.*;
import ui.components.DatePicker;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HoaDonPanel extends JPanel implements ResettableFilter {

    @Override
    public void resetFilters() {
        if (txtSearch != null) txtSearch.setText("");
        if (cboThoiGian != null) cboThoiGian.setSelectedIndex(0);
        if (dpTuNgay != null) dpTuNgay.setDate(null);
        if (dpDenNgay != null) dpDenNgay.setDate(null);
        if (cboTrangThai != null) cboTrangThai.setSelectedIndex(0);
        runFilter();
    }

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger
            .getLogger(HoaDonPanel.class.getName());

    private final HoaDonDAO dao = new HoaDonDAO();
    private final DatPhongService dpSvc = new DatPhongService();
    private final KhachHangService khSvc = new KhachHangService();
    private final Map<String, KhachHang> khachHangCache = new HashMap<>();
    private final Map<String, DatPhong> datPhongCache = new HashMap<>();

    private List<HoaDon> tatCaHoaDon;

    private ModernTextField txtSearch;
    private DatePicker dpTuNgay;
    private DatePicker dpDenNgay;
    private ModernComboBox<String> cboThoiGian;
    private ModernComboBox<String> cboTrangThai;
    private JLabel lblSummary;
    private JTable table;
    private DefaultTableModel tableModel;
    private PaginationPanel pagination;
    private List<HoaDon> filteredHoaDon;

    public HoaDonPanel(MainFrame mainFrame) {
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        main.add(buildHeader(), BorderLayout.NORTH);

        JPanel bot = new JPanel(new BorderLayout());
        bot.setOpaque(false);
        bot.add(Box.createVerticalStrut(14), BorderLayout.NORTH);
        bot.add(buildFilterBar(), BorderLayout.CENTER);

        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setOpaque(false);
        tableContainer.add(bot, BorderLayout.NORTH);
        tableContainer.add(buildTableArea(), BorderLayout.CENTER);

        main.add(tableContainer, BorderLayout.CENTER);

        add(main, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel t = new JLabel("Lịch sử Hóa đơn");
        t.setFont(new Font("Segoe UI", Font.BOLD, 28));
        t.setForeground(UIConstants.TEXT_PRIMARY);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel s = new JLabel("Tra cứu toàn bộ hóa đơn đã xuất với công cụ lọc thông minh");
        s.setFont(UIConstants.FONT_BODY);
        s.setForeground(UIConstants.TEXT_SECONDARY);
        s.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(t);
        left.add(Box.createVerticalStrut(4));
        left.add(s);

        RoundedButton btnExport = new RoundedButton("Xuất Excel", UIConstants.PRIMARY, Color.WHITE);
        btnExport.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnExport.setPreferredSize(new Dimension(140, 42));
        btnExport.addActionListener(e -> {
            if (filteredHoaDon == null || filteredHoaDon.isEmpty()) {
                NotificationManager.showInfo("Thông báo", "Không có dữ liệu để xuất!");
                return;
            }
            util.ExcelExporter.exportHoaDon(this, filteredHoaDon);
        });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btns.setOpaque(false);
        btns.add(btnExport);

        p.add(left, BorderLayout.WEST);
        p.add(btns, BorderLayout.EAST);
        return p;
    }

    private JPanel buildFilterBar() {
        RoundedPanel card = new RoundedPanel(16);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.VERTICAL;
        g.insets = new Insets(0, 0, 0, 12);
        g.weighty = 0;

        // Search
        txtSearch = new ModernTextField("Tìm Mã HĐ, Tên KH, SĐT...");
        txtSearch.setPreferredSize(new Dimension(170, 40));
        g.gridx = 0; g.weightx = 1.0; g.fill = GridBagConstraints.BOTH;
        card.add(txtSearch, g);

        // Time Filter
        cboThoiGian = new ModernComboBox<>();
        cboThoiGian.setPreferredSize(new Dimension(160, 40));
        cboThoiGian.addItem("Tất cả thời gian");
        cboThoiGian.addItem("Hôm nay");
        cboThoiGian.addItem("Hôm qua");
        cboThoiGian.addItem("7 ngày qua");
        cboThoiGian.addItem("30 ngày qua");
        cboThoiGian.addItem("Tháng này");
        cboThoiGian.addItem("Tháng trước");
        cboThoiGian.addItem("Tùy chỉnh...");
        g.gridx = 1; g.weightx = 0; g.fill = GridBagConstraints.VERTICAL;
        card.add(cboThoiGian, g);

        // Custom Date Range
        dpTuNgay = new DatePicker(null, d -> runFilter());
        dpTuNgay.setPreferredSize(new Dimension(150, 40));
        g.gridx = 2;
        card.add(dpTuNgay, g);

        dpDenNgay = new DatePicker(null, d -> runFilter());
        dpDenNgay.setPreferredSize(new Dimension(150, 40));
        g.gridx = 3;
        card.add(dpDenNgay, g);

        // Status Filter
        cboTrangThai = new ModernComboBox<>();
        cboTrangThai.setPreferredSize(new Dimension(160, 40));
        cboTrangThai.addItem("Tất cả trạng thái");
        cboTrangThai.addItem("Đã thanh toán");
        cboTrangThai.addItem("Chưa thanh toán");
        g.gridx = 4;
        card.add(cboTrangThai, g);

        // Clear Filter Button
        RoundedButton btnClear = new RoundedButton("Xóa lọc", new Color(0xF1F5F9), UIConstants.TEXT_PRIMARY);
        btnClear.setPreferredSize(new Dimension(90, 40));
        btnClear.setFont(UIConstants.FONT_SMALL_BOLD);
        btnClear.addActionListener(e -> {
            txtSearch.setText("");
            cboThoiGian.setSelectedIndex(0);
            dpTuNgay.setDate(null);
            dpDenNgay.setDate(null);
            cboTrangThai.setSelectedIndex(0);
            runFilter();
        });
        g.gridx = 5; g.insets = new Insets(0, 0, 0, 0);
        card.add(btnClear, g);

        // Logic Auto-Fill... (keeping existing logic)
        cboThoiGian.addActionListener(e -> {
            LocalDate today = LocalDate.now();
            int idx = cboThoiGian.getSelectedIndex();

            if (idx == 0 || idx == 7) { // Tất cả / Tùy chỉnh
                dpTuNgay.setDate(null);
                dpDenNgay.setDate(null);
            } else if (idx == 1) { // Hôm nay
                dpTuNgay.setDate(java.sql.Date.valueOf(today));
                dpDenNgay.setDate(java.sql.Date.valueOf(today));
            } else if (idx == 2) { // Hôm qua
                dpTuNgay.setDate(java.sql.Date.valueOf(today.minusDays(1)));
                dpDenNgay.setDate(java.sql.Date.valueOf(today.minusDays(1)));
            } else if (idx == 3) { // 7 ngày qua
                dpTuNgay.setDate(java.sql.Date.valueOf(today.minusDays(7)));
                dpDenNgay.setDate(java.sql.Date.valueOf(today));
            } else if (idx == 4) { // 30 ngày qua
                dpTuNgay.setDate(java.sql.Date.valueOf(today.minusDays(30)));
                dpDenNgay.setDate(java.sql.Date.valueOf(today));
            } else if (idx == 5) { // Tháng này
                dpTuNgay.setDate(java.sql.Date.valueOf(today.withDayOfMonth(1)));
                dpDenNgay.setDate(java.sql.Date.valueOf(today.withDayOfMonth(today.lengthOfMonth())));
            } else if (idx == 6) { // Tháng trước
                LocalDate prev = today.minusMonths(1);
                dpTuNgay.setDate(java.sql.Date.valueOf(prev.withDayOfMonth(1)));
                dpDenNgay.setDate(java.sql.Date.valueOf(prev.withDayOfMonth(prev.lengthOfMonth())));
            }
            if (idx != 7) runFilter();
        });

        javax.swing.event.DocumentListener dl = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { runFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { runFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { runFilter(); }
        };
        txtSearch.getDocument().addDocumentListener(dl);
        cboTrangThai.addActionListener(e -> runFilter());

        return card;
    }

    private JPanel buildTableArea() {
        String[] cols = { "Mã HĐ", "Loại", "Khách hàng", "Số ĐT", "Ngày xuất", "Tiền phòng", "Cộng thêm", "Tổng tiền",
                "Hình thức TT", "Trạng thái" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(tableModel);
        styleTable();

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row >= 0 && tatCaHoaDon != null) {
                        showDetails((String) tableModel.getValueAt(row, 0));
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r >= 0)
                    table.setRowSelectionInterval(r, r);
            }
        });

        table.setComponentPopupMenu(ctxMenu(
                () -> {
                    int row = table.getSelectedRow();
                    if (row >= 0)
                        showDetails((String) tableModel.getValueAt(row, 0));
                },
                () -> {
                    int row = table.getSelectedRow();
                    if (row >= 0) {
                        String maHD = (String) tableModel.getValueAt(row, 0);
                        HoaDon hd = tatCaHoaDon.stream().filter(h -> h.getMaHoaDon().equals(maHD)).findFirst()
                                .orElse(null);
                        if (hd != null) {
                            Frame owner = (Frame) SwingUtilities.getWindowAncestor(HoaDonPanel.this);
                            ui.dialogs.XemHoaDonDialog dlg = new ui.dialogs.XemHoaDonDialog(owner, hd);
                            dlg.printHoaDon(); // Call print module implicitly
                        }
                    }
                },
                () -> {
                    int row = table.getSelectedRow();
                    if (row >= 0) {
                        String maHD = (String) tableModel.getValueAt(row, 0);
                        int confirm = JOptionPane.showConfirmDialog(this,
                            "Bạn có chắc chắn muốn xóa hóa đơn " + maHD + "?\nHành động này không thể hoàn tác!",
                            "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        
                        if (confirm == JOptionPane.YES_OPTION) {
                            if (dao.delete(maHD)) {
                                NotificationManager.showSuccess("Thành công", "Đã hủy hóa đơn " + maHD);
                                loadData();
                            } else {
                                // Specific error message instead of generic
                                HoaDon selected = tatCaHoaDon.stream().filter(h -> h.getMaHoaDon().equals(maHD)).findFirst().orElse(null);
                                if (selected != null && selected.getTrangThai() == entity.enums.TrangThaiThanhToan.PAID) {
                                    JOptionPane.showMessageDialog(this, 
                                        "Không thể xóa hóa đơn đã thanh toán!\nĐây là chứng từ tài chính bắt buộc phải lưu trữ.", 
                                        "Ràng buộc nghiệp vụ", JOptionPane.WARNING_MESSAGE);
                                } else {
                                    JOptionPane.showMessageDialog(this, "Lỗi khi xử lý hủy hóa đơn!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        }
                    }
                }));

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        sp.setPreferredSize(new Dimension(1000, 500));

        pagination = new PaginationPanel();
        pagination.setPageChangeListener(page -> displayHoaDonPage());

        lblSummary = new JLabel("Tổng cộng: 0 hóa đơn - 0 đ");
        lblSummary.setFont(UIConstants.FONT_BODY_BOLD);
        lblSummary.setForeground(UIConstants.PRIMARY);
        lblSummary.setBorder(new EmptyBorder(0, 25, 0, 25));

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setPreferredSize(new Dimension(0, 60));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER));
        footer.add(lblSummary, BorderLayout.WEST);
        footer.add(pagination, BorderLayout.CENTER);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(sp, BorderLayout.CENTER);
        wrap.add(footer, BorderLayout.SOUTH);

        loadData();
        return wrap;
    }

    private JPopupMenu ctxMenu(Runnable onView, Runnable onPrint, Runnable onDelete) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem miView = new JMenuItem("Xem chi tiết");
        JMenuItem miPrint = new JMenuItem("In lại hóa đơn");
        JMenuItem miDelete = new JMenuItem("Xóa hóa đơn");
        
        miView.setFont(UIConstants.FONT_BODY);
        miPrint.setFont(UIConstants.FONT_BODY);
        miDelete.setFont(UIConstants.FONT_BODY);
        miDelete.setForeground(UIConstants.DANGER);
        
        miView.addActionListener(e -> onView.run());
        miPrint.addActionListener(e -> onPrint.run());
        miDelete.addActionListener(e -> onDelete.run());
        
        // Disable delete for PAID invoices
        menu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    String status = (String) tableModel.getValueAt(row, 9);
                    miDelete.setEnabled(!"Đã thanh toán".equals(status));
                }
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {}
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
        });
        
        menu.add(miView);
        menu.add(miPrint);
        menu.addSeparator();
        menu.add(miDelete);
        return menu;
    }

    private void styleTable() {
        table.setFont(UIConstants.FONT_BODY);
        table.setRowHeight(48);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(UIConstants.PRIMARY_LIGHT);
        // Căn lề cho tiêu đề
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                lbl.setBackground(UIConstants.BG_TABLE_HEADER);
                lbl.setForeground(UIConstants.TEXT_SECONDARY);
                lbl.setHorizontalAlignment(JLabel.CENTER);
                lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER));
                return lbl;
            }
        };
        table.getTableHeader().setDefaultRenderer(headerRenderer);

        DefaultTableCellRenderer rightRender = new DefaultTableCellRenderer();
        rightRender.setHorizontalAlignment(JLabel.RIGHT);
        for (int i : new int[] { 5, 6, 7 })
            table.getColumnModel().getColumn(i).setCellRenderer(rightRender);

        // Thiết lập chiều rộng cột để không bị cắt chữ
        int[] widths = { 150, 85, 170, 100, 150, 110, 110, 130, 110, 120 };
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean foc, int row,
                    int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                lbl.setBackground(sel ? UIConstants.PRIMARY_LIGHT : (row % 2 == 0 ? Color.WHITE : new Color(0xFAFAFA)));
                lbl.setForeground(UIConstants.TEXT_PRIMARY);

                if (col == 1) {
                    lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                    String v = val != null ? val.toString() : "";
                    lbl.setForeground(v.contains("Đoàn") ? new Color(0x7C3AED) : new Color(0x0EA5E9));
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                }
                if (col == 9) {
                    lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    String v = val != null ? val.toString() : "";
                    if (v.equals("Đã thanh toán")) {
                        lbl.setForeground(UIConstants.SUCCESS);
                    } else if (v.equals("Đã hủy")) {
                        lbl.setForeground(UIConstants.TEXT_MUTED);
                    } else if (!v.isEmpty()) {
                        lbl.setForeground(UIConstants.DANGER);
                    }
                }
                if (col == 8 || col == 0) {
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                }
                return lbl;
            }
        });
    }

    private void loadData() {
        SwingWorker<List<HoaDon>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<HoaDon> doInBackground() throws Exception {
                List<HoaDon> list = dao.getAll();
                // Map DatPhong and KhachHang into HoaDon object to avoid UI freeze during
                // filter
                for (HoaDon hd : list) {
                    if (hd.getDatPhong() != null && hd.getDatPhong().getMaDatPhong() != null) {
                        String maDp = hd.getDatPhong().getMaDatPhong();
                        DatPhong dp;
                        if (datPhongCache.containsKey(maDp)) {
                            dp = datPhongCache.get(maDp);
                        } else {
                            dp = dpSvc.getById(maDp);
                            datPhongCache.put(maDp, dp);
                        }
                        if (dp != null) {
                            hd.setDatPhong(dp); // Set full DatPhong with loaiKhach
                            if (dp.getKhachHang() != null) {
                                String maKh = dp.getKhachHang().getMaKhachHang();
                                if (!khachHangCache.containsKey(maKh)) {
                                    khachHangCache.put(maKh, khSvc.getById(maKh));
                                }
                                hd.setKhachHang(khachHangCache.get(maKh));
                            }
                        }
                    }
                }
                return list;
            }

            @Override
            protected void done() {
                try {
                    tatCaHoaDon = get();
                    runFilter();
                } catch (Exception e) {
                    LOGGER.log(java.util.logging.Level.WARNING, "UI error in HoaDonPanel", e);
                }
            }
        };
        worker.execute();
    }

    private void runFilter() {
        if (tatCaHoaDon == null)
            return;
        tableModel.setRowCount(0);

        String kw = txtSearch.getText().trim().toLowerCase();
        if (kw.equals("tìm mã hđ, tên kh, sđt..."))
            kw = "";

        java.util.Date tuNgay = dpTuNgay.getDate();
        java.util.Date denNgay = dpDenNgay.getDate();

        LocalDateTime fromDate = null;
        LocalDateTime toDate = null;
        if (tuNgay != null) {
            fromDate = LocalDateTime.ofInstant(tuNgay.toInstant(), java.time.ZoneId.systemDefault());
        }
        if (denNgay != null) {
            toDate = LocalDateTime.ofInstant(denNgay.toInstant(), java.time.ZoneId.systemDefault()).with(LocalTime.MAX);
        }

        int idxStatus = cboTrangThai.getSelectedIndex();
        boolean filterPaid = (idxStatus == 1);
        boolean filterUnpaid = (idxStatus == 2);

        final LocalDateTime finalFrom = fromDate;
        final LocalDateTime finalTo = toDate;
        final String finalKw = kw;
        DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        List<HoaDon> filtered = tatCaHoaDon.stream()
                .filter(hd -> {
                    boolean matchKw = finalKw.isEmpty() ||
                            (hd.getMaHoaDon() != null && hd.getMaHoaDon().toLowerCase().contains(finalKw)) ||
                            (hd.getKhachHang() != null && hd.getKhachHang().getHoTen() != null
                                    && hd.getKhachHang().getHoTen().toLowerCase().contains(finalKw))
                            ||
                            (hd.getKhachHang() != null && hd.getKhachHang().getSoDienThoai() != null
                                    && hd.getKhachHang().getSoDienThoai().contains(finalKw));

                    boolean matchDate = true;
                    if (finalFrom != null && hd.getNgayLap() != null && hd.getNgayLap().isBefore(finalFrom))
                        matchDate = false;
                    if (finalTo != null && hd.getNgayLap() != null && hd.getNgayLap().isAfter(finalTo))
                        matchDate = false;

                    boolean matchStatus = true;
                    boolean isPaid = hd.getTrangThai() != null && "PAID".equalsIgnoreCase(hd.getTrangThai().name());
                    if (filterPaid && !isPaid)
                        matchStatus = false;
                    if (filterUnpaid && isPaid)
                        matchStatus = false;

                    return matchKw && matchDate && matchStatus;
                })
                .collect(Collectors.toList());

        filteredHoaDon = filtered;
        updateSummary();
        if (pagination != null) pagination.setCurrentPage(1);
        displayHoaDonPage();
    }

    private void updateSummary() {
        if (filteredHoaDon == null || lblSummary == null) return;
        double total = 0;
        int count = filteredHoaDon.size();
        for (HoaDon hd : filteredHoaDon) {
            // Chỉ cộng các hóa đơn đã thanh toán vào tổng doanh thu
            if (hd.getTrangThai() == entity.enums.TrangThaiThanhToan.PAID) {
                total += hd.getTongThanhToan();
            }
        }
        lblSummary.setText(String.format("Tổng cộng: %d hóa đơn - Doanh thu: %,.0f đ", count, total));
    }

    private void displayHoaDonPage() {
        tableModel.setRowCount(0);
        if (filteredHoaDon == null) return;

        int pageSize = 12;
        int currentPage = pagination != null ? pagination.getCurrentPage() : 1;

        int totalPages = (int) Math.ceil((double) filteredHoaDon.size() / pageSize);
        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
            if (pagination != null) pagination.setCurrentPage(currentPage);
        }

        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, filteredHoaDon.size());

        DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (int i = start; i < end; i++) {
            HoaDon hd = filteredHoaDon.get(i);
            String tenKh = (hd.getKhachHang() != null && hd.getKhachHang().getHoTen() != null)
                    ? hd.getKhachHang().getHoTen()
                    : "Khách vãng lai";
            String sdt = (hd.getKhachHang() != null && hd.getKhachHang().getSoDienThoai() != null)
                    ? hd.getKhachHang().getSoDienThoai()
                    : "";
            double phuPhiDichVu = hd.getTongTienDichVu();

            String statusGui = "Chưa thanh toán";
            if (hd.getTrangThai() != null) {
                switch (hd.getTrangThai()) {
                    case PAID: statusGui = "Đã thanh toán"; break;
                    case CANCELLED: statusGui = "Đã hủy"; break;
                    case REFUNDED: statusGui = "Đã hoàn tiền"; break;
                    default: statusGui = "Chưa thanh toán"; break;
                }
            }

            String loaiHD = "Khách lẻ";
            if (hd.getDatPhong() != null && "DOAN".equalsIgnoreCase(hd.getDatPhong().getLoaiKhach())) {
                loaiHD = "Đoàn";
                if (hd.getDatPhong().getTenDoan() != null && !hd.getDatPhong().getTenDoan().isBlank()) {
                    tenKh = hd.getDatPhong().getTenDoan() + " (" + tenKh + ")";
                }
            }

            tableModel.addRow(new Object[] {
                    hd.getMaHoaDon(),
                    loaiHD,
                    tenKh,
                    sdt,
                    hd.getNgayLap() != null ? hd.getNgayLap().format(outputFmt) : "",
                    String.format("%,.0f đ", hd.getTongTienPhong()),
                    String.format("%,.0f đ", phuPhiDichVu),
                    String.format("%,.0f đ", hd.getTongThanhToan()),
                    "Tiền mặt/CK",
                    statusGui
            });
        }

        if (pagination != null) {
            pagination.update(filteredHoaDon.size(), pageSize, currentPage);
        }
    }

    private void showDetails(String maHD) {
        HoaDon hd = tatCaHoaDon.stream()
                .filter(h -> h.getMaHoaDon().equals(maHD))
                .findFirst().orElse(null);
        if (hd != null) {
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(HoaDonPanel.this);
            ui.dialogs.XemHoaDonDialog dlg = new ui.dialogs.XemHoaDonDialog(owner, hd);
            dlg.setVisible(true);
        }
    }

    /** 
     * Hỗ trợ tìm kiếm từ các panel khác (vd: từ KhachHangPanel)
     */
    public void prefillSearch(String keyword) {
        if (txtSearch != null) {
            txtSearch.setText(keyword);
            runFilter();
        }
    }

    public void refresh() {
        loadData();
    }
}
