package ui.panels;

import service.*;
import entity.*;
import ui.MainFrame;
import ui.components.NotificationManager;
import ui.components.PaginationPanel;
import ui.components.UIConstants;
import ui.components.WrapLayout;
import ui.dialogs.CheckinDialog;
import ui.dialogs.NoShowDialog;
import ui.dialogs.DatPhongDialog;
import ui.components.DateTimePicker;
import ui.components.StatusBadge;
import ui.components.RoundedComponents.*;
import entity.enums.TrangThaiDatPhong;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.text.*;
import java.util.*;
import java.awt.event.*;

public class DatPhongPanel extends JPanel implements ResettableFilter {

    @Override
    public void resetFilters() {
        if (txtSearchList != null) txtSearchList.setText("");
        if (cboFilterStatus != null) cboFilterStatus.setSelectedIndex(0);
        
        // Reset Search Card filters too
        if (txtNgayNhan != null) {
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.set(java.util.Calendar.HOUR_OF_DAY, 14);
            c.set(java.util.Calendar.MINUTE, 0);
            c.set(java.util.Calendar.SECOND, 0);
            txtNgayNhan.setDate(c.getTime());
            
            c.add(java.util.Calendar.DAY_OF_YEAR, 1);
            c.set(java.util.Calendar.HOUR_OF_DAY, 12);
            c.set(java.util.Calendar.MINUTE, 0);
            c.set(java.util.Calendar.SECOND, 0);
            txtNgayTra.setDate(c.getTime());
        }
        if (spnSoKhach != null) spnSoKhach.setValue(1);
        if (cboTang != null) cboTang.setSelectedIndex(0);
        if (cboView != null) cboView.setSelectedIndex(0);
        if (cboLoaiPhong != null) cboLoaiPhong.setSelectedIndex(0);
        
        showResultPlaceholder();
        filterListTable();
    }

    private final DatPhongService datPhongService = new DatPhongService();
    private final KhachHangService khService = new KhachHangService();
    private final PhongService phongService = new PhongService();
    private final BangGiaService bangGiaService = new BangGiaService();
    private final MainFrame mainFrame;

    private CardLayout cardLayout;
    private JPanel cardContainer;

    // ---- Card 1: Danh sách đặt phòng ----
    private DefaultTableModel modelDatPhong;
    private JTable tableDatPhong;
    private ModernTextField txtSearchList;
    private JComboBox<String> cboFilterStatus;
    private PaginationPanel pagination;
    private List<DatPhong> allDatPhong;
    private List<DatPhong> filteredDatPhong;

    // ---- Card 2: Tìm & Đặt phòng ----
    private DateTimePicker txtNgayNhan, txtNgayTra;
    private JComboBox<String> cboTang, cboView, cboLoaiPhong;
    private JSpinner spnSoKhach;
    private JPanel resultPanel;

    private Set<Phong> selectedRooms = new LinkedHashSet<>();
    private RoundedButton btnDatNhieuPhong;
    private JLabel lblSelectedCount;

    // KPI Labels
    private JLabel lblStatTotal, lblStatUpcoming, lblStatInHouse, lblStatCompleted;

    private static final String CARD_LIST = "LIST";
    private static final String CARD_SEARCH = "SEARCH";

    private KhachHang prefilledKhachHang;
    private JPanel pnlBell;

    public void prefillKhachHang(KhachHang kh) {
        this.prefilledKhachHang = kh;
        showSearchCard();
    }

    /** Trạng thái 2 là "Đã xác nhận" trong cboFilterStatus */
    public void applyConfirmedFilter() {
        cardLayout.show(cardContainer, CARD_LIST);
        if (cboFilterStatus != null) {
            cboFilterStatus.setSelectedIndex(2);
        }
        if (txtSearchList != null) {
            txtSearchList.setText("");
        }
        filterListTable();
    }

    public void focusBooking(String maDatPhong) {
        cardLayout.show(cardContainer, CARD_LIST);
        if (cboFilterStatus != null) {
            cboFilterStatus.setSelectedIndex(0);
        }
        if (txtSearchList != null) {
            txtSearchList.setText(maDatPhong);
        }
        filterListTable();
    }

    public DatPhongPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();
        startBellTimer();
    }

    private javax.swing.Timer bellTimer;
    private void startBellTimer() {
        bellTimer = new javax.swing.Timer(30000, e -> refreshNotificationBell());
        bellTimer.start();
    }

    private void buildUI() {
        cardLayout = new CardLayout();
        cardContainer = new JPanel(cardLayout);
        cardContainer.setOpaque(false);

        cardContainer.add(buildListCard(), CARD_LIST);
        cardContainer.add(buildSearchCard(), CARD_SEARCH);

        add(cardContainer, BorderLayout.CENTER);
        cardLayout.show(cardContainer, CARD_LIST);
    }

    // ================================================================
    // CARD 1 — DANH SÁCH ĐẶT PHÒNG
    // ================================================================
    private JPanel buildListCard() {
        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // ---- Header ----
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Đặt phòng trước");
        title.setFont(UIConstants.FONT_TITLE);
        JLabel sub = new JLabel("Quản lý danh sách phòng đã đặt trước");
        sub.setFont(UIConstants.FONT_BODY);
        sub.setForeground(UIConstants.TEXT_SECONDARY);
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(2));
        titleBox.add(sub);

        JPanel rightHdr = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightHdr.setOpaque(false);
        pnlBell = new JPanel(new BorderLayout());
        pnlBell.setOpaque(false);
        pnlBell.add(buildAlertBell());
        rightHdr.add(pnlBell);
        RoundedButton btnDatPhong = new RoundedButton("+ Đặt phòng", UIConstants.PRIMARY, Color.WHITE);
        btnDatPhong.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnDatPhong.setPreferredSize(new Dimension(150, 38));
        btnDatPhong.addActionListener(e -> showSearchCard());
        rightHdr.add(btnDatPhong);

        hdr.add(titleBox, BorderLayout.WEST);
        hdr.add(rightHdr, BorderLayout.EAST);

        // ---- Stats Dashboard ----
        JPanel statsPanel = buildStats();

        // ---- Card trắng ----
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // ---- Toolbar (Modern style) ----
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        // Left: search + status filter
        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftTools.setOpaque(false);

        txtSearchList = new ModernTextField("Tìm khách hàng, số phòng...");
        txtSearchList.setPreferredSize(new Dimension(280, 40));
        txtSearchList.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterListTable(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterListTable(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterListTable(); }
        });

        String[] statuses = { "Tất cả trạng thái", "Chờ xác nhận", "Đã xác nhận", "Đang check-in", "Đã check-in",
                "Đã trả phòng", "Đã hủy", "Khách không đến", "Chờ xếp phòng" };
        cboFilterStatus = new ModernComboBox<>(statuses);
        cboFilterStatus.setPreferredSize(new Dimension(200, 40));
        cboFilterStatus.addActionListener(e -> filterListTable());

        leftTools.add(txtSearchList);
        leftTools.add(cboFilterStatus);

        RoundedButton btnClearFilter = new RoundedButton("Xóa lọc", new Color(0xF1F5F9), UIConstants.TEXT_PRIMARY);
        btnClearFilter.setPreferredSize(new Dimension(90, 40));
        btnClearFilter.setFont(UIConstants.FONT_SMALL_BOLD);
        btnClearFilter.addActionListener(e -> {
            txtSearchList.setText("");
            cboFilterStatus.setSelectedIndex(0);
            filterListTable();
        });
        leftTools.add(btnClearFilter);

        toolbar.add(leftTools, BorderLayout.WEST);

        // ---- Table ----
        String[] cols = { "_ma", "Mã đặt", "Khách hàng", "Loại", "Kênh", "Phòng", "Loại phòng", "Check-in", "Check-out",
                "Số khách", "Đặt cọc", "Trạng thái" };
        modelDatPhong = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tableDatPhong = new JTable(modelDatPhong);
        styleListTable();
        pagination = new PaginationPanel();
        pagination.setPageChangeListener(page -> displayDatPhongPage());
        loadDatPhongTable();

        tableDatPhong.getColumnModel().removeColumn(tableDatPhong.getColumnModel().getColumn(0));
        tableDatPhong.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) { showPopup(e); }
            @Override
            public void mousePressed(MouseEvent e) { showPopup(e); }
            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int r = tableDatPhong.rowAtPoint(e.getPoint());
                    if (r >= 0 && r < tableDatPhong.getRowCount()) {
                        tableDatPhong.setRowSelectionInterval(r, r);
                    } else {
                        tableDatPhong.clearSelection();
                    }
                    int row = tableDatPhong.getSelectedRow();
                    if (row >= 0) {
                        int modelRow = tableDatPhong.convertRowIndexToModel(row);
                        String ma = (String) modelDatPhong.getValueAt(modelRow, 0);
                        String ten = (String) modelDatPhong.getValueAt(modelRow, 2);
                        TrangThaiDatPhong status = (TrangThaiDatPhong) modelDatPhong.getValueAt(modelRow, 11);
                        JPopupMenu menu = createDynamicMenu(ma, ten, status);
                        menu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && tableDatPhong.getSelectedRow() >= 0) {
                    int row = tableDatPhong.convertRowIndexToModel(tableDatPhong.getSelectedRow());
                    String maDat = (String) modelDatPhong.getValueAt(row, 0);
                    viewDetails(maDat);
                }
            }
        });

        JScrollPane sp = new JScrollPane(tableDatPhong);
        sp.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER));
        footer.add(pagination, BorderLayout.CENTER);

        card.add(toolbar, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);

        JPanel mainContent = new JPanel(new BorderLayout(0, 20));
        mainContent.setOpaque(false);
        mainContent.add(statsPanel, BorderLayout.NORTH);
        mainContent.add(card, BorderLayout.CENTER);

        root.add(hdr, BorderLayout.NORTH);
        root.add(mainContent, BorderLayout.CENTER);

        // Restore card settings
        card.setShadow(true);
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        return root;
    }

    private void filterListTable() {
        if (allDatPhong == null)
            return;

        String kw = txtSearchList.getText().trim().toLowerCase();
        String selectedStatus = (String) cboFilterStatus.getSelectedItem();

        filteredDatPhong = allDatPhong.stream().filter(dp -> {
            boolean matchSearch = kw.isEmpty()
                    || (dp.getMaDatPhong() != null && dp.getMaDatPhong().toLowerCase().contains(kw))
                    || (dp.getTenKhachHang() != null && dp.getTenKhachHang().toLowerCase().contains(kw))
                    || (dp.getDsChiTiet() != null && dp.getDanhSachTenPhong().toLowerCase().contains(kw));

            boolean matchStatus = "Tất cả trạng thái".equals(selectedStatus);
            if (!matchStatus) {
                String label = UIConstants.getTrangThaiDatPhongLabel(dp.getTrangThai());
                matchStatus = label != null && label.contains(selectedStatus);
            }
            return matchSearch && matchStatus;
        }).collect(java.util.stream.Collectors.toList());

        if (pagination != null)
            pagination.setCurrentPage(1);
        displayDatPhongPage();
    }

    // ================================================================
    // CARD 2 — TÌM & ĐẶT PHÒNG (redesigned)
    // ================================================================
    private JPanel buildSearchCard() {
        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // ---- Header ----
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        // ---- Breadcrumbs (Bánh mì) navigation ----
        JPanel breadcrumb = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        breadcrumb.setOpaque(false);

        JButton lblBack = new JButton("Sơ đồ phòng");
        lblBack.setFont(UIConstants.FONT_BODY);
        lblBack.setForeground(UIConstants.TEXT_SECONDARY);
        lblBack.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        lblBack.setContentAreaFilled(false);
        lblBack.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblBack.addActionListener(e -> cardLayout.show(cardContainer, CARD_LIST));

        lblBack.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                lblBack.setForeground(UIConstants.PRIMARY);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                lblBack.setForeground(UIConstants.TEXT_SECONDARY);
            }
        });

        JLabel lblSep = new JLabel(" / ");
        lblSep.setFont(UIConstants.FONT_BODY);
        lblSep.setForeground(UIConstants.TEXT_MUTED);
        lblSep.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

        JLabel lblCurrent = new JLabel("Tìm phòng trống");
        lblCurrent.setFont(UIConstants.FONT_BODY_BOLD);
        lblCurrent.setForeground(UIConstants.TEXT_PRIMARY);
        lblCurrent.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        breadcrumb.add(lblBack);
        breadcrumb.add(lblSep);
        breadcrumb.add(lblCurrent);
        breadcrumb.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.add(breadcrumb);
        titleBox.add(Box.createVerticalStrut(8));

        JLabel title = new JLabel("Tìm phòng trống");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleBox.add(title);

        titleBox.add(Box.createVerticalStrut(4));
        JLabel sub = new JLabel("Chọn ngày, số khách và bộ lọc để tìm phòng phù hợp");
        sub.setFont(UIConstants.FONT_BODY);
        sub.setForeground(UIConstants.TEXT_SECONDARY);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleBox.add(sub);

        hdr.add(titleBox, BorderLayout.WEST);

        // ---- Search filters card ----
        RoundedPanel searchCard = new RoundedPanel(UIConstants.CARD_RADIUS);
        searchCard.setBackground(Color.WHITE);
        searchCard.setShadow(true);
        searchCard.setLayout(new GridBagLayout());
        searchCard.setBorder(BorderFactory.createEmptyBorder(24, 24, 20, 24));

        // Mặc định chuẩn khách sạn: 14:00 hôm nay -> 12:00 ngày mai
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 14);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        txtNgayNhan = new DateTimePicker(cal.getTime());

        cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 12);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        txtNgayTra = new DateTimePicker(cal.getTime());

        // Combo box & Spinner initialization with fixed widths to prevent unwanted stretching
        cboTang = new ModernComboBox<>(
                new String[] { "Tất cả tầng", "Tầng 1", "Tầng 2", "Tầng 3", "Tầng 4", "Tầng 5" });
        cboTang.setPreferredSize(new Dimension(140, 40));

        java.util.Set<String> views = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        phongService.getAllPhong().stream().map(Phong::getView).filter(v -> v != null && !v.isBlank())
                .forEach(views::add);
        String[] viewArr = new String[views.size() + 1];
        viewArr[0] = "Tất cả view";
        int vi = 1;
        for (String v : views)
            viewArr[vi++] = v;
        cboView = new ModernComboBox<>(viewArr);
        cboView.setPreferredSize(new Dimension(140, 40));

        java.util.Set<String> types = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        phongService.getAllPhong().stream().map(Phong::getTenLoaiPhong).filter(t -> t != null && !t.isBlank())
                .forEach(types::add);
        String[] typeArr = new String[types.size() + 1];
        typeArr[0] = "Tất cả loại";
        int ti = 1;
        for (String t : types)
            typeArr[ti++] = t;
        cboLoaiPhong = new ModernComboBox<>(typeArr);
        cboLoaiPhong.setPreferredSize(new Dimension(150, 40));

        spnSoKhach = new ModernSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        spnSoKhach.setPreferredSize(new Dimension(80, 40));

        RoundedButton btnSearch = new RoundedButton("Tìm phòng", UIConstants.PRIMARY, Color.WHITE);
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSearch.setPreferredSize(new Dimension(85, 42));
        btnSearch.addActionListener(e -> doSearch());

        // GridBag Layout configuration
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.insets = new Insets(0, 0, 0, 10); // Horizontal spacing
        g.weighty = 0;
        g.gridy = 0;

        // Single Row Layout - Redistributed weights for better appearance
        g.gridx = 0;
        g.weightx = 0.22; // Increased for better time visibility
        searchCard.add(labeledComp("Ngày nhận phòng *", txtNgayNhan), g);

        g.gridx = 1;
        g.weightx = 0.22; // Increased for better time visibility
        searchCard.add(labeledComp("Ngày trả phòng *", txtNgayTra), g);

        g.gridx = 2;
        g.weightx = 0.08;
        searchCard.add(labeledComp("Số khách", spnSoKhach), g);

        g.gridx = 3;
        g.weightx = 0.10; // Slightly reduced
        searchCard.add(labeledComp("Tầng", cboTang), g);

        g.gridx = 4;
        g.weightx = 0.10; // Reduced
        searchCard.add(labeledComp("Hướng view", cboView), g);

        g.gridx = 5;
        g.weightx = 0.10; // Significantly reduced to match "Tất cả loại" content
        searchCard.add(labeledComp("Loại phòng", cboLoaiPhong), g);

        // Clear Filter
        RoundedButton btnClear = new RoundedButton("Xóa lọc", new Color(0xF1F5F9), UIConstants.TEXT_PRIMARY);
        btnClear.setPreferredSize(new Dimension(60, 42));
        btnClear.setFont(UIConstants.FONT_SMALL_BOLD);
        btnClear.addActionListener(e -> {
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.set(java.util.Calendar.HOUR_OF_DAY, 14);
            c.set(java.util.Calendar.MINUTE, 0);
            c.set(java.util.Calendar.SECOND, 0);
            txtNgayNhan.setDate(c.getTime());

            c.add(java.util.Calendar.DAY_OF_YEAR, 1);
            c.set(java.util.Calendar.HOUR_OF_DAY, 12);
            c.set(java.util.Calendar.MINUTE, 0);
            c.set(java.util.Calendar.SECOND, 0);
            txtNgayTra.setDate(c.getTime());
            spnSoKhach.setValue(1);
            cboTang.setSelectedIndex(0);
            cboView.setSelectedIndex(0);
            cboLoaiPhong.setSelectedIndex(0);
            showResultPlaceholder();
        });
        g.gridx = 6;
        g.weightx = 0.08;
        g.insets = new Insets(0, 5, 0, 0);
        searchCard.add(labeledComp(" ", btnClear), g);

        g.gridx = 7;
        g.weightx = 0.12;
        g.insets = new Insets(0, 5, 0, 0);
        JPanel btnWrapper = new JPanel(new BorderLayout());
        btnWrapper.setOpaque(false);
        // Add vertical padding to match labels' height
        btnWrapper.add(Box.createVerticalStrut(22), BorderLayout.NORTH);
        btnWrapper.add(btnSearch, BorderLayout.CENTER);
        searchCard.add(btnWrapper, g);

        // ---- Result panel ----
        resultPanel = new JPanel(new BorderLayout());
        resultPanel.setOpaque(false);
        showResultPlaceholder();

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.add(searchCard, BorderLayout.NORTH);
        body.add(resultPanel, BorderLayout.CENTER);

        root.add(hdr, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        return root;
    }

    private JPanel labeledComp(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_SECONDARY);
        p.add(lbl, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private void showResultPlaceholder() {
        resultPanel.removeAll();
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel icon = new JLabel("🔍", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lbl = new JLabel("Nhập ngày nhận & ngày trả rồi nhấn Tìm phòng", SwingConstants.CENTER);
        lbl.setFont(UIConstants.FONT_BODY);
        lbl.setForeground(UIConstants.TEXT_MUTED);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(Box.createVerticalGlue());
        center.add(icon);
        center.add(Box.createVerticalStrut(10));
        center.add(lbl);
        center.add(Box.createVerticalGlue());

        resultPanel.add(center, BorderLayout.CENTER);
        resultPanel.revalidate();
        resultPanel.repaint();
    }

    private void showSearchCard() {
        showResultPlaceholder();
        if (prefilledKhachHang != null) {
            // Optional: show a banner or toast message
        }
        cardLayout.show(cardContainer, CARD_SEARCH);
    }

    // ================================================================
    // TÌM PHÒNG TRỐNG
    // ================================================================
    private void doSearch() {
        Date checkIn = txtNgayNhan.getDate();
        Date checkOut = txtNgayTra.getDate();
        if (!checkOut.after(checkIn)) {
            NotificationManager.showWarning("Lỗi ngày tháng", "Ngày trả phải sau ngày nhận!");
            return;
        }

        int soKhach = (int) spnSoKhach.getValue();
        String tangSel = (String) cboTang.getSelectedItem();
        String tangFilter = (tangSel != null && !tangSel.equals("Tất cả tầng"))
                ? tangSel.replace("Tầng ", "").trim()
                : null;
        String viewSel = (String) cboView.getSelectedItem();
        String viewFilter = (viewSel != null && !viewSel.equals("Tất cả view")) ? viewSel : null;
        String typeSel = (String) cboLoaiPhong.getSelectedItem();
        String typeFilter = (typeSel != null && !typeSel.equals("Tất cả loại")) ? typeSel : null;

        java.util.List<Phong> allRooms = datPhongService.timPhongTrong(checkIn, checkOut, 1);
        java.util.List<Phong> rooms = new ArrayList<>();
        for (Phong ph : allRooms) {
            boolean matchTang = tangFilter == null || String.valueOf(ph.getTang()).equals(tangFilter);
            boolean matchView = viewFilter == null
                    || (ph.getView() != null && ph.getView().equalsIgnoreCase(viewFilter));
            boolean matchType = typeFilter == null
                    || (ph.getTenLoaiPhong() != null && ph.getTenLoaiPhong().equalsIgnoreCase(typeFilter));
            if (matchTang && matchView && matchType)
                rooms.add(ph);
        }
        showResults(rooms, checkIn, checkOut);
    }

    private void showResults(java.util.List<Phong> rooms, Date checkIn, Date checkOut) {
        resultPanel.removeAll();
        selectedRooms.clear();
        if (rooms.isEmpty()) {
            JPanel empty = new JPanel();
            empty.setOpaque(false);
            empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
            JLabel icon = new JLabel("🏨", SwingConstants.CENTER);
            icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel lbl = new JLabel("Không tìm thấy phòng trống phù hợp!", SwingConstants.CENTER);
            lbl.setFont(UIConstants.FONT_BODY_BOLD);
            lbl.setForeground(UIConstants.DANGER);
            lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            empty.add(Box.createVerticalGlue());
            empty.add(icon);
            empty.add(Box.createVerticalStrut(8));
            empty.add(lbl);
            empty.add(Box.createVerticalGlue());
            resultPanel.add(empty, BorderLayout.CENTER);
        } else {
            // Summary bar
            JPanel summary = new JPanel(new BorderLayout());
            summary.setOpaque(false);
            summary.setBorder(BorderFactory.createEmptyBorder(10, 0, 8, 0));

            JLabel cnt = new JLabel("Tìm thấy " + rooms.size() + " phòng trống phù hợp");
            cnt.setFont(UIConstants.FONT_BODY_BOLD);
            cnt.setForeground(UIConstants.SUCCESS);

            long soNgay = new ThuePhongService().tinhSoNgay(checkIn, checkOut);
            JLabel nights = new JLabel(soNgay + " đêm");
            nights.setFont(UIConstants.FONT_SMALL);
            nights.setForeground(UIConstants.TEXT_MUTED);

            summary.add(cnt, BorderLayout.WEST);
            summary.add(nights, BorderLayout.EAST);

            // Group by Floor
            Map<Integer, java.util.List<Phong>> byFloor = new TreeMap<>();
            for (Phong p : rooms) {
                byFloor.computeIfAbsent(p.getTang(), k -> new ArrayList<>()).add(p);
            }

            JPanel roomGrid = new JPanel();
            roomGrid.setOpaque(false);
            roomGrid.setLayout(new BoxLayout(roomGrid, BoxLayout.Y_AXIS));

            for (Map.Entry<Integer, java.util.List<Phong>> entry : byFloor.entrySet()) {
                roomGrid.add(buildFloorSection(entry.getKey(), entry.getValue(), soNgay, checkIn, checkOut));
                roomGrid.add(Box.createVerticalStrut(12));
            }

            JScrollPane sp = new JScrollPane(roomGrid);
            sp.setOpaque(false);
            sp.getViewport().setOpaque(false);
            sp.setBorder(null);
            sp.getVerticalScrollBar().setUnitIncrement(20);
            sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            // Selection Bar
            JPanel selectionBar = new JPanel(new BorderLayout());
            selectionBar.setOpaque(false);
            selectionBar.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            lblSelectedCount = new JLabel("Chưa chọn phòng nào");
            lblSelectedCount.setFont(UIConstants.FONT_BODY_BOLD);
            btnDatNhieuPhong = new RoundedButton("Đặt phòng đã chọn", UIConstants.PRIMARY, Color.WHITE);
            btnDatNhieuPhong.setEnabled(false);
            btnDatNhieuPhong.setPreferredSize(new Dimension(180, 40));
            btnDatNhieuPhong.addActionListener(e -> {
                if (!selectedRooms.isEmpty()) {
                    new DatPhongDialog(
                            (Frame) SwingUtilities.getWindowAncestor(this),
                            new ArrayList<>(selectedRooms),
                            checkIn, checkOut,
                            () -> {
                                refresh();
                                cardLayout.show(cardContainer, CARD_LIST);
                            }).setVisible(true);
                }
            });
            selectionBar.add(lblSelectedCount, BorderLayout.WEST);
            selectionBar.add(btnDatNhieuPhong, BorderLayout.EAST);

            resultPanel.add(summary, BorderLayout.NORTH);
            resultPanel.add(sp, BorderLayout.CENTER);
            resultPanel.add(selectionBar, BorderLayout.SOUTH);
        }
        resultPanel.revalidate();
        resultPanel.repaint();
    }

    private JPanel buildFloorSection(int floor, java.util.List<Phong> phongs, long soNgay, Date checkIn,
            Date checkOut) {
        JPanel section = new JPanel();
        section.setOpaque(false);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 9999));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Floor header
        JPanel floorHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        floorHeader.setOpaque(false);
        floorHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        floorHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JLabel lblFloor = new JLabel("Tầng " + floor);
        lblFloor.setFont(UIConstants.FONT_HEADER);
        lblFloor.setForeground(UIConstants.TEXT_PRIMARY);
        JLabel lblCount = new JLabel("(" + phongs.size() + " phòng)");
        lblCount.setFont(UIConstants.FONT_SMALL);
        lblCount.setForeground(UIConstants.TEXT_MUTED);
        floorHeader.add(lblFloor);
        floorHeader.add(lblCount);

        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setForeground(new Color(200, 210, 245));
        separator.setBackground(new Color(235, 240, 255));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel cardsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 10));
        cardsPanel.setOpaque(false);
        cardsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Phong p : phongs)
            cardsPanel.add(buildRoomCard(p, soNgay, checkIn, checkOut));

        section.add(floorHeader);
        section.add(Box.createVerticalStrut(6));
        section.add(separator);
        section.add(Box.createVerticalStrut(8));
        section.add(cardsPanel);
        return section;
    }

    private RoundedPanel buildRoomCard(Phong p, long soNgay, Date checkIn, Date checkOut) {
        Color borderClr = new Color(0x10B981); // AVAILABLE color
        Color bgHover = new Color(0xECFDF5);
        String txtStatus = "Có sẵn";

        RoundedPanel card = new RoundedPanel(10);
        card.setBackground(Color.WHITE);
        card.setLayout(new BorderLayout(0, 4));
        card.setPreferredSize(new Dimension(190, 128));
        card.setMinimumSize(new Dimension(190, 128));
        card.setMaximumSize(new Dimension(190, 128));
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(10, borderClr),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Top: số phòng + trạng thái
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel lblNum = new JLabel("P." + p.getSoPhong());
        lblNum.setFont(new Font("Segoe UI", Font.BOLD, 15));

        JLabel lblTT = new JLabel(txtStatus);
        lblTT.setFont(UIConstants.FONT_TINY);
        lblTT.setForeground(borderClr);

        topRow.add(lblNum, BorderLayout.WEST);
        topRow.add(lblTT, BorderLayout.EAST);

        // Middle: loại + view + khách
        JLabel lblType = new JLabel(p.getTenLoaiPhong() != null ? p.getTenLoaiPhong() : "");
        lblType.setFont(UIConstants.FONT_SMALL);
        lblType.setForeground(UIConstants.TEXT_SECONDARY);

        JLabel lblView = new JLabel(p.getView() != null ? "Hướng: " + p.getView() : "");
        lblView.setFont(UIConstants.FONT_TINY);
        lblView.setForeground(UIConstants.TEXT_MUTED);

        JLabel lblGuest = new JLabel(p.getSucChua() + " người tối đa");
        lblGuest.setFont(UIConstants.FONT_SMALL);
        lblGuest.setForeground(UIConstants.TEXT_MUTED);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(lblType);
        center.add(Box.createVerticalStrut(1));
        center.add(lblView);
        center.add(Box.createVerticalStrut(3));
        center.add(lblGuest);

        // Bottom: giá
        double giaActive = bangGiaService.layGiaVaoThoiDiem(
                p.getLoaiPhong() != null ? p.getLoaiPhong().getMaLoaiPhong() : "",
                checkIn.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        JLabel lblPrice = new JLabel(String.format("%,.0fđ/đêm", giaActive));
        lblPrice.setFont(UIConstants.FONT_SMALL_BOLD);
        lblPrice.setForeground(UIConstants.SUCCESS);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(lblPrice, BorderLayout.WEST);

        card.add(topRow, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);

        Color selectedBorderClr = UIConstants.PRIMARY;
        Color selectedBg = new Color(0xEFF6FF);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (selectedRooms.contains(p)) {
                        selectedRooms.remove(p);
                    } else {
                        selectedRooms.add(p);
                    }
                    updateSelectionBar();
                    card.setBackground(selectedRooms.contains(p) ? selectedBg : Color.WHITE);
                    card.setBorder(BorderFactory.createCompoundBorder(
                            new RoundedBorder(10, selectedRooms.contains(p) ? selectedBorderClr : borderClr),
                            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
                    card.repaint();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!selectedRooms.contains(p)) {
                    card.setBackground(bgHover);
                    card.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!selectedRooms.contains(p)) {
                    card.setBackground(Color.WHITE);
                    card.repaint();
                }
            }
        });

        card.setBackground(selectedRooms.contains(p) ? selectedBg : Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(10, selectedRooms.contains(p) ? selectedBorderClr : borderClr),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        return card;
    }

    private void updateSelectionBar() {
        if (lblSelectedCount != null && btnDatNhieuPhong != null) {
            if (selectedRooms.isEmpty()) {
                lblSelectedCount.setText("Chưa chọn phòng nào");
                lblSelectedCount.setForeground(UIConstants.TEXT_MUTED);
                btnDatNhieuPhong.setEnabled(false);
            } else {
                lblSelectedCount.setText("Đã chọn " + selectedRooms.size() + " phòng");
                lblSelectedCount.setForeground(UIConstants.PRIMARY);
                btnDatNhieuPhong.setEnabled(true);
            }
        }
    }

    private void showError(String msg) {
        NotificationManager.showError("Lỗi", msg);
    }

    // ================================================================
    // TABLE STYLE & LOAD
    // ================================================================
    private void styleListTable() {
        tableDatPhong.setRowHeight(48);
        tableDatPhong.setShowGrid(false);
        tableDatPhong.setIntercellSpacing(new Dimension(0, 0));
        tableDatPhong.setBackground(Color.WHITE);
        tableDatPhong.setForeground(UIConstants.TEXT_PRIMARY);
        tableDatPhong.setSelectionBackground(UIConstants.PRIMARY_LIGHT);
        tableDatPhong.setSelectionForeground(UIConstants.TEXT_PRIMARY);
        tableDatPhong.setFont(UIConstants.FONT_BODY);
        tableDatPhong.getTableHeader().setBackground(new Color(0xF8FAFC));
        tableDatPhong.getTableHeader().setFont(UIConstants.FONT_SMALL_BOLD);
        tableDatPhong.getTableHeader().setForeground(UIConstants.TEXT_PRIMARY);
        tableDatPhong.getTableHeader().setPreferredSize(new Dimension(0, 40));
        tableDatPhong.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER));

        // Renderers
        DefaultTableCellRenderer left = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return lbl;
            }
        };

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                lbl.setHorizontalAlignment(SwingConstants.RIGHT);
                return lbl;
            }
        };

        tableDatPhong.getColumnModel().getColumn(1).setCellRenderer(center); // Mã đặt
        tableDatPhong.getColumnModel().getColumn(2).setCellRenderer(left);   // Khách hàng
        tableDatPhong.getColumnModel().getColumn(4).setCellRenderer(left);   // Kênh
        tableDatPhong.getColumnModel().getColumn(5).setCellRenderer(left);   // Phòng
        tableDatPhong.getColumnModel().getColumn(6).setCellRenderer(center); // Loại phòng
        tableDatPhong.getColumnModel().getColumn(7).setCellRenderer(center); // Check-in
        tableDatPhong.getColumnModel().getColumn(8).setCellRenderer(center); // Check-out
        tableDatPhong.getColumnModel().getColumn(9).setCellRenderer(center); // Số khách
        tableDatPhong.getColumnModel().getColumn(10).setCellRenderer(right); // Đặt cọc

        // Badge Loại (Blue/Purple)
        tableDatPhong.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                String val = (v == null) ? "" : v.toString();
                Color bg = val.toLowerCase().contains("đoàn") ? new Color(0xF3E8FF) : new Color(0xDBEAFE);
                Color fg = val.toLowerCase().contains("đoàn") ? new Color(0x9333EA) : new Color(0x2563EB);
                return new ui.components.StatusBadge(val, bg, fg);
            }
        });

        // Status Badge
        tableDatPhong.getColumnModel().getColumn(11).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                if (!(v instanceof TrangThaiDatPhong)) return super.getTableCellRendererComponent(t, v, s, f, r, c);
                TrangThaiDatPhong st = (TrangThaiDatPhong) v;
                String label = UIConstants.getTrangThaiDatPhongLabel(st);
                Color bg = new Color(0xF1F5F9);
                Color fg = UIConstants.TEXT_SECONDARY;

                if (st == TrangThaiDatPhong.CONFIRMED) { bg = new Color(0xDCFCE7); fg = new Color(0x15803D); }
                else if (st == TrangThaiDatPhong.PENDING) { bg = new Color(0xFEF9C3); fg = new Color(0x854D0E); }
                else if (st == TrangThaiDatPhong.PARTIALLY_CHECKED_IN) { bg = new Color(0xDBEAFE); fg = new Color(0x1D4ED8); }
                else if (st == TrangThaiDatPhong.CHECKED_IN) { bg = new Color(0xCFFAFE); fg = new Color(0x0E7490); }
                else if (st == TrangThaiDatPhong.CHECKED_OUT) { bg = new Color(0xDCFCE7); fg = new Color(0x15803D); }
                else if (st == TrangThaiDatPhong.CANCELLED) { bg = new Color(0xFEE2E2); fg = new Color(0xB91C1C); }
                else if (st == TrangThaiDatPhong.NO_SHOW) { bg = new Color(0xFFEDD5); fg = new Color(0x9A3412); }

                return new ui.components.StatusBadge(label, bg, fg);
            }
        });
    }

    private void loadDatPhongTable() {
        allDatPhong = datPhongService.getAll();
        updateStats();
        filterListTable();
    }

    private void updateStats() {
        if (allDatPhong == null || lblStatTotal == null) return;
        int total = allDatPhong.size();
        int upcoming = 0;
        int inHouse = 0;
        int completed = 0;
        for (DatPhong d : allDatPhong) {
            TrangThaiDatPhong st = d.getTrangThai();
            if (st == TrangThaiDatPhong.PENDING || st == TrangThaiDatPhong.CONFIRMED || st == TrangThaiDatPhong.WAITLIST) upcoming++;
            else if (st == TrangThaiDatPhong.PARTIALLY_CHECKED_IN || st == TrangThaiDatPhong.CHECKED_IN) inHouse++;
            else if (st == TrangThaiDatPhong.CHECKED_OUT) completed++;
        }
        lblStatTotal.setText(String.valueOf(total));
        lblStatUpcoming.setText(String.valueOf(upcoming));
        lblStatInHouse.setText(String.valueOf(inHouse));
        lblStatCompleted.setText(String.valueOf(completed));
    }

    private void displayDatPhongPage() {
        modelDatPhong.setRowCount(0);
        if (filteredDatPhong == null)
            return;

        int pageSize = 12;
        int currentPage = pagination != null ? pagination.getCurrentPage() : 1;

        int totalPages = (int) Math.ceil((double) filteredDatPhong.size() / pageSize);
        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
            if (pagination != null)
                pagination.setCurrentPage(currentPage);
        }

        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, filteredDatPhong.size());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        for (int i = start; i < end; i++) {
            DatPhong dp = filteredDatPhong.get(i);
            String loaiLabel = dp.getLoaiKhachLabel();
            if (dp.isDoan() && dp.getTenDoan() != null && !dp.getTenDoan().isEmpty()) {
                loaiLabel += " (" + dp.getTenDoan() + ")";
            }
            modelDatPhong.addRow(new Object[] {
                    dp.getMaDatPhong(),
                    dp.getMaDatPhong(),
                    dp.getTenKhachHang() != null ? dp.getTenKhachHang() : "—",
                    loaiLabel,
                    dp.getTenKenh() != null ? dp.getTenKenh() : "Trực tiếp",
                    dp.getDanhSachTenPhong(),
                    dp.getTenLoaiPhong() != null ? dp.getTenLoaiPhong() : "",
                    dp.getNgayNhanDK() != null ? sdf.format(dp.getNgayNhanDK_Date()) : "",
                    dp.getNgayTraDK() != null ? sdf.format(dp.getNgayTraDK_Date()) : "",
                    dp.getSoLuongKhach(),
                    dp.getTienDatCoc() > 0
                            ? String.format("%,.0fđ", dp.getTienDatCoc())
                            : "Không cọc",
                    dp.getTrangThai()
            });
        }

        if (pagination != null) {
            pagination.update(filteredDatPhong.size(), pageSize, currentPage);
        }
    }

    // ================================================================
    // QUICK CHECK-IN DIALOG
    // ================================================================
    private void showQuickCheckinDialog(DatPhong dp) {
        if (dp == null)
            return;
        if (dp.getTrangThai() != TrangThaiDatPhong.CONFIRMED &&
                dp.getTrangThai() != TrangThaiDatPhong.PARTIALLY_CHECKED_IN &&
                dp.getTrangThai() != TrangThaiDatPhong.CHECKED_IN) {
            NotificationManager.showInfo("Thông báo",
                    "Chỉ đơn 'Đã xác nhận' hoặc 'Đang check-in' mới có thể tiếp tục Check-in.");
            return;
        }

        // --- GROUP CHECK-IN HANDLING ---
        if (dp.getDsChiTiet() != null && dp.getDsChiTiet().size() > 1) {
            JDialog mapDlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chọn phòng để Check-in",
                    true);
            mapDlg.setSize(400, Math.min(600, 100 + dp.getDsChiTiet().size() * 50));
            mapDlg.setLocationRelativeTo(this);
            JPanel mapPanel = new JPanel();
            mapPanel.setLayout(new BoxLayout(mapPanel, BoxLayout.Y_AXIS));
            mapPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            mapPanel.setBackground(Color.WHITE);

            JLabel lblGuide = new JLabel("Đơn đặt này có " + dp.getDsChiTiet().size() + " phòng. Chọn phòng để xử lý:");
            lblGuide.setFont(UIConstants.FONT_BODY_BOLD);
            lblGuide.setAlignmentX(Component.CENTER_ALIGNMENT);
            mapPanel.add(lblGuide);
            mapPanel.add(Box.createVerticalStrut(15));

            for (ChiTietDatPhong ct : dp.getDsChiTiet()) {
                Phong p = ct.getPhong();
                RoundedButton btnRoom = new RoundedButton("P." + p.getSoPhong() + " (" + p.getTenLoaiPhong() + ")",
                        UIConstants.PRIMARY, Color.WHITE);
                btnRoom.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                btnRoom.setAlignmentX(Component.CENTER_ALIGNMENT);

                boolean isDone = ct.isDaThanhToan() || ct.getNgayTraThucTe() != null;
                boolean isOccupied = p.getTrangThai() == entity.enums.TrangThaiPhong.OCCUPIED;

                if (isOccupied || isDone) {
                    String lockLabel = isDone 
                            ? "P." + p.getSoPhong() + " - Đã hoàn thành ✓" 
                            : "P." + p.getSoPhong() + " - Đã nhận phòng";
                    btnRoom.setText(lockLabel);
                    btnRoom.setBackground(new Color(156, 163, 175)); // Khóa nút
                } else {
                    btnRoom.addActionListener(ev -> {
                        mapDlg.dispose();
                        KhachHang kh = (dp.getMaKH() != null) ? khService.getById(dp.getMaKH()) : null;
                        ui.dialogs.CheckinDialog ck = new ui.dialogs.CheckinDialog(
                                (Frame) SwingUtilities.getWindowAncestor(this), p, kh, dp);
                        ck.setVisible(true);
                        if (ck.isConfirmed())
                            refresh();
                    });
                }
                mapPanel.add(btnRoom);
                mapPanel.add(Box.createVerticalStrut(10));
            }
            mapDlg.add(new JScrollPane(mapPanel));
            mapDlg.setVisible(true);
            return;
        }

        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Check-in — " + dp.getMaDatPhong(), true);
        dlg.setSize(440, 300);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel container = new JPanel(new BorderLayout(0, 12));
        container.setBackground(Color.WHITE);
        container.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Info section
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        addInfoRow(info, "Thời gian hiện tại:", sdf.format(new Date()), UIConstants.TEXT_PRIMARY);
        // Fallback cho khách lẻ
        String phongDisplay = dp.getSoPhong();
        if (dp.getDsChiTiet() != null && !dp.getDsChiTiet().isEmpty() && dp.getDsChiTiet().get(0).getPhong() != null) {
            phongDisplay = dp.getDsChiTiet().get(0).getPhong().getMaPhong();
        }
        addInfoRow(info, "Phòng:", phongDisplay + " — " + dp.getTenLoaiPhong(), UIConstants.TEXT_PRIMARY);
        addInfoRow(info, "Khách hàng:", dp.getTenKhachHang() != null ? dp.getTenKhachHang() : "—",
                UIConstants.TEXT_PRIMARY);
        addInfoRow(info, "Số lượng khách:", dp.getSoLuongKhach() + " người", UIConstants.TEXT_PRIMARY);
        addInfoRow(info, "Check-in dự kiến:",
                dp.getNgayNhanDK_Date() != null
                        ? new SimpleDateFormat("dd/MM/yyyy HH:mm").format(dp.getNgayNhanDK_Date())
                        : "—",
                UIConstants.WARNING);
        if (dp.getGhiChu() != null && !dp.getGhiChu().isBlank()) {
            addInfoRow(info, "Ghi chú:", dp.getGhiChu(), UIConstants.INFO);
        }

        container.add(info, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        RoundedButton btnCancel = new RoundedButton("Hủy đơn", UIConstants.DANGER, Color.WHITE);
        RoundedButton btnCheckin = new RoundedButton("Check-in ngay", UIConstants.SUCCESS, Color.WHITE);
        btnCheckin.setFont(new Font("Segoe UI", Font.BOLD, 13));

        btnCancel.addActionListener(e -> {
            ui.dialogs.HuyDatPhongDialog cancelDlg = new ui.dialogs.HuyDatPhongDialog(
                    (Frame) SwingUtilities.getWindowAncestor(this), dp.getMaDatPhong(), dp.getTenKhachHang());
            cancelDlg.setVisible(true);
            if (cancelDlg.isConfirmed()) {
                dlg.dispose();
                refresh();
            }
        });

        btnCheckin.addActionListener(e -> {
            Phong phong = null;
            if (dp.getDsChiTiet() != null && !dp.getDsChiTiet().isEmpty()) {
                phong = phongService.getPhongById(dp.getDsChiTiet().get(0).getPhong().getMaPhong());
            } else {
                phong = phongService.getPhongById(dp.getSoPhong());
            }
            if (phong == null) {
                NotificationManager.showError("Lỗi", "Không tìm thấy thông tin phòng!");
                return;
            }
            String status = phong.getTrangThaiString();
            if (!"Có sẵn".equals(status) && !"Đã đặt".equals(status)) {
                NotificationManager.showWarning("Không thể Check-in", "Phòng hiện không trống. Trạng thái: " + status);
                return;
            }
            KhachHang kh = (dp.getMaKH() != null) ? khService.getById(dp.getMaKH()) : null;
            dlg.dispose();
            ui.dialogs.CheckinDialog ck = new ui.dialogs.CheckinDialog((Frame) SwingUtilities.getWindowAncestor(this),
                    phong, kh, dp);
            ck.setVisible(true);
            if (ck.isConfirmed())
                refresh();
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnCheckin);
        container.add(btnPanel, BorderLayout.SOUTH);

        dlg.setContentPane(container);
        dlg.setVisible(true);
    }

    private void addInfoRow(JPanel panel, String key, String val, Color valColor) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        JLabel k = new JLabel(key);
        k.setFont(UIConstants.FONT_SMALL_BOLD);
        k.setPreferredSize(new Dimension(160, 22));
        JLabel v = new JLabel(val);
        v.setFont(UIConstants.FONT_BODY);
        v.setForeground(valColor);
        row.add(k, BorderLayout.WEST);
        row.add(v, BorderLayout.CENTER);
        panel.add(row);
        panel.add(Box.createVerticalStrut(4));
    }

    // ================================================================
    // ================================================================
    // ALERT BELL
    // ================================================================

    private void showAlertDialog(
            java.util.List<DatPhong> noShows,
            java.util.List<DatPhong> upcoming,
            java.util.List<DatPhong> longPending,
            java.util.List<DatPhong> overdueDeposit,
            java.util.List<DatPhong> needPrep,
            java.util.List<DatPhong> nearDeadline) {

        int total = noShows.size() + upcoming.size() + longPending.size() + overdueDeposit.size() + needPrep.size() + nearDeadline.size();
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);

        JDialog dlg = new JDialog(parent, "Trung tâm cảnh báo", true);
        dlg.setUndecorated(true);
        dlg.setSize(560, total == 0 ? 320 : Math.min(680, 180 + total * 80));
        dlg.setLocationRelativeTo(parent);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 0, 0, 50), 1),
                BorderFactory.createLineBorder(Color.WHITE, 1)));

        // === HEADER ===
        JPanel hdr = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(0xF1F5F9));
                g.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        hdr.setBackground(Color.WHITE);
        hdr.setPreferredSize(new Dimension(0, 76));
        hdr.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 16));

        JPanel hdrIcon = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(44, 44);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xFEF2F2));
                g2.fillOval(0, 0, 44, 44);
                g2.setColor(new Color(0xEF4444));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = 22, cy = 21;
                g2.drawArc(cx - 8, cy - 8, 16, 16, 0, 180);
                g2.drawLine(cx - 8, cy, cx - 8, cy + 7);
                g2.drawLine(cx + 8, cy, cx + 8, cy + 7);
                g2.drawLine(cx - 10, cy + 7, cx + 10, cy + 7);
                g2.fillOval(cx - 2, cy + 8, 4, 4);
                g2.drawLine(cx - 2, cy - 8, cx + 2, cy - 8);
                g2.dispose();
            }
        };
        hdrIcon.setOpaque(false);

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        JLabel t1 = new JLabel("Trung tâm cảnh báo");
        t1.setFont(new Font("Segoe UI", Font.BOLD, 17));
        t1.setForeground(new Color(0x0F172A));
        JLabel t2 = new JLabel(
                total > 0 ? "Bạn có " + total + " việc cần ưu tiên xử lý" : "Mọi thứ đang hoạt động ổn định");
        t2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t2.setForeground(new Color(0x64748B));
        titleBlock.add(t1);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(t2);

        JPanel hdrLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 14));
        hdrLeft.setOpaque(false);
        hdrLeft.add(hdrIcon);
        hdrLeft.add(titleBlock);

        JButton btnCloseHdr = new JButton("×");
        btnCloseHdr.setFont(new Font("Segoe UI", Font.PLAIN, 28));
        btnCloseHdr.setForeground(new Color(0x94A3B8));
        btnCloseHdr.setContentAreaFilled(false);
        btnCloseHdr.setBorderPainted(false);
        btnCloseHdr.setFocusPainted(false);
        btnCloseHdr.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCloseHdr.addActionListener(e -> dlg.dispose());

        hdr.add(hdrLeft, BorderLayout.WEST);
        hdr.add(btnCloseHdr, BorderLayout.EAST);
        root.add(hdr, BorderLayout.NORTH);

        // === BODY ===
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM");

        if (total == 0) {
            JPanel empty = new JPanel(new GridBagLayout());
            empty.setBackground(Color.WHITE);
            empty.setBorder(BorderFactory.createEmptyBorder(40, 20, 50, 20));

            JLabel okMsg = new JLabel("Tất cả đơn đặt đều ổn!");
            okMsg.setFont(new Font("Segoe UI", Font.BOLD, 18));
            okMsg.setForeground(new Color(0x1E293B));
            empty.add(okMsg);
            body.add(empty);
        } else {
            if (!noShows.isEmpty()) {
                body.add(alertGroupHeader("Quá hạn nhận phòng (Khách không đến)", noShows.size(), new Color(0xFEF2F2),
                        new Color(0xEF4444)));
                for (DatPhong dp : noShows) {
                    body.add(alertItemRow(dp, fmt, new Color(0xEF4444), "Đã quá hạn check-in", "Đánh dấu vắng mặt",
                            () -> {
                                dlg.dispose();
                                doMarkNoShow(dp.getMaDatPhong(), dp.getTenKhachHang());
                            }));
                }
            }
            if (!needPrep.isEmpty()) {
                body.add(alertGroupHeader("Phòng chưa sẵn sàng cho khách sắp đến", needPrep.size(), new Color(0xFEF2F2),
                        new Color(0xEF4444)));
                for (DatPhong dp : needPrep) {
                    body.add(alertItemRow(dp, fmt, new Color(0xEF4444), "Phòng " + dp.getDanhSachTenPhong() + " chưa sẵn sàng (Bẩn/Bảo trì)", "Chuẩn bị ngay", () -> {
                        dlg.dispose();
                        if (mainFrame != null) mainFrame.navigateTo("thuephong"); 
                    }));
                }
            }
            if (!overdueDeposit.isEmpty()) {
                body.add(alertGroupHeader("Đã quá hạn nộp cọc (Ưu tiên xử lý)", overdueDeposit.size(), new Color(0xFEF2F2),
                        new Color(0xEF4444)));
                for (DatPhong dp : overdueDeposit) {
                    body.add(alertItemRow(dp, fmt, new Color(0xEF4444),
                            "Hạn nộp cọc: " + (dp.getHanNopCoc() != null ? dp.getHanNopCoc().format(fmt) : "--"),
                            "Hủy đơn", () -> {
                                dlg.dispose();
                                doCancel(dp.getMaDatPhong(), dp.getTenKhachHang());
                            }));
                }
            }
            if (!nearDeadline.isEmpty()) {
                body.add(alertGroupHeader("Sắp đến hạn nộp cọc", nearDeadline.size(), new Color(0xFFFBEB),
                        new Color(0xF59E0B)));
                for (DatPhong dp : nearDeadline) {
                    body.add(alertItemRow(dp, fmt, new Color(0xF59E0B),
                            "Hết hạn lúc: " + (dp.getHanNopCoc() != null ? dp.getHanNopCoc().format(fmt) : "--"),
                            "Xem đơn", () -> {
                                dlg.dispose();
                                viewDetails(dp.getMaDatPhong());
                            }));
                }
            }
            if (!upcoming.isEmpty()) {
                body.add(alertGroupHeader("Sắp nhận phòng hôm nay", upcoming.size(), new Color(0xFFFBEB),
                        new Color(0xF59E0B)));
                for (DatPhong dp : upcoming) {
                    String tg = dp.getNgayNhanDuKien() != null ? dp.getNgayNhanDuKien().format(fmt) : "--";
                    body.add(alertItemRow(dp, fmt, new Color(0xF59E0B), "Dự kiến nhận lúc " + tg, "Nhận phòng", () -> {
                        dlg.dispose();
                        showQuickCheckinDialog(dp);
                    }));
                }
            }
            if (!longPending.isEmpty()) {
                body.add(alertGroupHeader("Đơn chờ phản hồi lâu", longPending.size(), new Color(0xEFF6FF),
                        new Color(0x3B82F6)));
                for (DatPhong dp : longPending) {
                    body.add(alertItemRow(dp, fmt, new Color(0x3B82F6), "Đã tạo từ lâu", "Xem chi tiết", () -> {
                        dlg.dispose();
                        viewDetails(dp.getMaDatPhong());
                    }));
                }
            }
        }

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        root.add(scroll, BorderLayout.CENTER);

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    private JPanel alertGroupHeader(String label, int count, Color bg, Color accent) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        p.setBorder(BorderFactory.createEmptyBorder(18, 24, 6, 24));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(new Color(0x64748B));

        JLabel cnt = new JLabel(String.valueOf(count), SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        cnt.setOpaque(false);
        cnt.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cnt.setForeground(accent);
        cnt.setPreferredSize(new Dimension(28, 22));

        p.add(lbl, BorderLayout.WEST);
        p.add(cnt, BorderLayout.EAST);
        return p;
    }

    private JPanel alertItemRow(DatPhong dp, java.time.format.DateTimeFormatter fmt,
            Color accent, String subText,
            String btnLabel, Runnable onAction) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(true);
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(4, 20, 6, 20),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xF1F5F9), 1),
                        BorderFactory.createEmptyBorder(14, 16, 14, 16))));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));

        String tenKH = (dp.getTenKhachHang() != null && !dp.getTenKhachHang().trim().isEmpty()) ? dp.getTenKhachHang()
                : "?";
        String init = tenKH.substring(0, 1).toUpperCase();

        JPanel av = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(40, 40);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 28));
                g2.fillOval(0, 0, 40, 40);
                g2.setColor(accent);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(init, (40 - fm.stringWidth(init)) / 2, (40 + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        av.setOpaque(false);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel l1 = new JLabel(dp.getMaDatPhong() + " • " + tenKH);
        l1.setFont(new Font("Segoe UI", Font.BOLD, 14));
        l1.setForeground(new Color(0x1E293B));
        JLabel l2 = new JLabel(subText);
        l2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l2.setForeground(new Color(0x64748B));
        info.add(l1);
        info.add(Box.createVerticalStrut(4));
        info.add(l2);

        JButton btn = new JButton(btnLabel) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(130, 34));
        btn.addActionListener(e -> onAction.run());

        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        center.setOpaque(false);
        center.add(av);
        center.add(info);

        row.add(center, BorderLayout.CENTER);

        JPanel rightFlow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        rightFlow.setOpaque(false);
        rightFlow.add(btn);
        row.add(rightFlow, BorderLayout.EAST);

        return row;
    }

    private JPopupMenu createDynamicMenu(String ma, String ten, TrangThaiDatPhong status) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));

        // 1. Xem chi tiết (Luôn có)
        JMenuItem mnuDetail = createMenuItem("Xem chi tiết", UIConstants.TEXT_PRIMARY);
        mnuDetail.addActionListener(e -> viewDetails(ma));
        menu.add(mnuDetail);
        menu.addSeparator();

        // 2. Nhận phòng
        if (status == TrangThaiDatPhong.CONFIRMED || status == TrangThaiDatPhong.PARTIALLY_CHECKED_IN) {
            JMenuItem mnuCheckin = createMenuItem("Nhận phòng", UIConstants.SUCCESS);
            mnuCheckin.setFont(UIConstants.FONT_BODY_BOLD);
            mnuCheckin.addActionListener(e -> {
                DatPhong dp = datPhongService.getById(ma);
                if (dp != null)
                    showQuickCheckinDialog(dp);
            });
            menu.add(mnuCheckin);
        }

        // 2.1 Sửa đặt phòng (Mới khôi phục)
        if (status == TrangThaiDatPhong.PENDING || status == TrangThaiDatPhong.CONFIRMED) {
            JMenuItem mnuEdit = createMenuItem("Sửa đặt phòng", UIConstants.PRIMARY);
            mnuEdit.addActionListener(e -> {
                DatPhong dp = datPhongService.getById(ma);
                if (dp != null) {
                    ui.dialogs.EditDatPhongDialog dlg = new ui.dialogs.EditDatPhongDialog(
                            (Frame) SwingUtilities.getWindowAncestor(this), dp);
                    dlg.setVisible(true);
                    if (dlg.isConfirmed())
                        refresh();
                }
            });
            menu.add(mnuEdit);
        }

        // 2.2 Thu tiền cọc (Quick action)
        if (status == TrangThaiDatPhong.PENDING || status == TrangThaiDatPhong.WAITLIST) {
            JMenuItem mnuDeposit = createMenuItem("Thu tiền cọc", new Color(0xF59E0B));
            mnuDeposit.setFont(UIConstants.FONT_BODY_BOLD);
            mnuDeposit.addActionListener(e -> doNopCoc(ma, ten));
            menu.add(mnuDeposit);
        }

        // 2.3 Chuyển sang chờ xếp phòng (Waitlist)
        if (status == TrangThaiDatPhong.PENDING || status == TrangThaiDatPhong.CONFIRMED) {
            JMenuItem mnuMoveWaitlist = createMenuItem("Chuyển sang chờ xếp phòng", new Color(0xF59E0B));
            mnuMoveWaitlist.addActionListener(e -> doMoveToWaitlist(ma, ten));
            menu.add(mnuMoveWaitlist);
        }

        // 3. Xác nhận (PENDING hoặc WAITLIST)
        if (status == TrangThaiDatPhong.PENDING || status == TrangThaiDatPhong.WAITLIST) {
            JMenuItem mnuConfirm = createMenuItem("Xác nhận đặt phòng", UIConstants.SUCCESS);
            mnuConfirm.setFont(UIConstants.FONT_BODY_BOLD);
            mnuConfirm.addActionListener(e -> {
                if (status == TrangThaiDatPhong.PENDING)
                    doConfirmPending(ma, ten);
                else
                    doConfirmWaitlist(ma, ten);
            });
            menu.add(mnuConfirm);
        }

        // 4. No-show
        if (status == TrangThaiDatPhong.CONFIRMED) {
            JMenuItem mnuNoShow = createMenuItem("Đánh dấu Khách không đến", new Color(0xB91C1C));
            mnuNoShow.addActionListener(e -> doMarkNoShow(ma, ten));
            menu.add(mnuNoShow);
        }

        // 4. Hủy (Dành cho đơn chưa check-in: PENDING, CONFIRMED, WAITLIST)
        if (status == TrangThaiDatPhong.PENDING || status == TrangThaiDatPhong.CONFIRMED
                || status == TrangThaiDatPhong.WAITLIST) {
            menu.addSeparator();
            JMenuItem mnuHuy = createMenuItem("Hủy đặt phòng", UIConstants.TEXT_SECONDARY);
            mnuHuy.addActionListener(e -> doCancel(ma, ten));
            menu.add(mnuHuy);
        }

        // 5. Xóa (Hard delete - Chỉ dành cho nghiệp vụ nháp hoặc đã hủy để dọn rác)
        if (status == TrangThaiDatPhong.PENDING || status == TrangThaiDatPhong.CANCELLED
                || status == TrangThaiDatPhong.WAITLIST) {
            // Nếu là PENDING hoặc WAITLIST, không cần separator nếu đã có ở trên
            if (status == TrangThaiDatPhong.CANCELLED)
                menu.addSeparator();

            JMenuItem mnuXoa = createMenuItem("Xóa vĩnh viễn", UIConstants.DANGER);
            mnuXoa.addActionListener(e -> doDelete(ma, ten));
            menu.add(mnuXoa);
        }

        return menu;
    }

    private JMenuItem createMenuItem(String text, Color color) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(UIConstants.FONT_BODY);
        item.setForeground(color);
        item.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        return item;
    }

    private void viewDetails(String ma) {
        DatPhong dp = datPhongService.getById(ma);
        if (dp != null) {
            try {
                new ui.dialogs.DatPhongDetailDialog((Frame) SwingUtilities.getWindowAncestor(this), dp, this)
                        .setVisible(true);
            } catch (Exception ex) {
                showError("Không thể mở hộp thoại: " + ex.getMessage());
            }
        }
    }

    private void doCancel(String ma, String ten) {
        ui.dialogs.HuyDatPhongDialog dlg = new ui.dialogs.HuyDatPhongDialog(
                (Frame) SwingUtilities.getWindowAncestor(this), ma, ten);
        dlg.setVisible(true);
        if (dlg.isConfirmed())
            refresh();
    }

    private void doDelete(String ma, String ten) {
        int ok = JOptionPane.showConfirmDialog(this,
                "<html><b>Cảnh báo:</b> Xóa hoàn toàn dữ liệu đơn đặt của \"" + ten
                        + "\".<br>Hành động này không thể hoàn tác!</html>",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            String err = datPhongService.xoaDatPhong(ma);
            if (err == null) {
                loadDatPhongTable();
                NotificationManager.showSuccess("Thành công", "Đã xóa đơn đặt phòng của " + ten);
            } else
                showError(err);
        }
    }

    private void doMarkNoShow(String ma, String ten) {
        NoShowDialog dlg = new NoShowDialog((Frame) SwingUtilities.getWindowAncestor(this), ma, ten);
        dlg.setVisible(true);

        if (dlg.isConfirmed()) {
            double phi = dlg.getPhiPhat();
            String maNV = AuthService.getInstance().getCurrentMaNV();
            if (maNV == null)
                maNV = "ADMIN";

            String err = datPhongService.markNoShow(ma, maNV, phi);
            if (err == null) {
                loadDatPhongTable();
                NotificationManager.showSuccess("Thành công",
                        "Đã đánh dấu khách không đến"
                                + (phi > 0 ? " (Phí: " + String.format("%,.0fđ", phi) + ")" : ""));
            } else
                showError(err);
        }
    }

    private void doConfirmPending(String ma, String ten) {
        int ok = JOptionPane.showConfirmDialog(this,
                "Xác nhận đặt phòng cho \"" + ten + "\"?\nĐơn sẽ chuyển sang trạng thái CONFIRMED.", "Xác nhận",
                JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            if (datPhongService.updateTrangThai(ma, TrangThaiDatPhong.CONFIRMED)) {
                loadDatPhongTable();
                NotificationManager.showSuccess("Thành công", "Đã xác nhận đặt phòng!");
            } else
                showError("Không thể cập nhật trạng thái!");
        }
    }

    private void doNopCoc(String ma, String ten) {
        DatPhong dp = datPhongService.getById(ma);
        if (dp == null) return;

        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        p.add(new JLabel("<html>Khách hàng: <b>" + ten + "</b><br>Nhập số tiền cọc thu được (VNĐ):</html>"), BorderLayout.NORTH);

        ModernTextField txtCoc = new ModernTextField("Ví dụ: 500000");
        txtCoc.setPreferredSize(new Dimension(250, 40));
        if (dp.getTienDatCoc() > 0) {
            txtCoc.setText(String.format("%.0f", dp.getTienDatCoc()));
        }
        p.add(txtCoc, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, p, "Thu tiền cọc & Xác nhận đơn", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                double amount = Double.parseDouble(txtCoc.getText().replace(",", ""));
                dp.setTienDatCoc(amount);
                dp.setTrangThai(TrangThaiDatPhong.CONFIRMED);
                if (datPhongService.suaDatPhong(dp) == null) {
                    Window w = SwingUtilities.getWindowAncestor(this);
                    if (w instanceof MainFrame) {
                        ((MainFrame) w).notifyDataChanged();
                    }
                    refresh();
                    NotificationManager.showSuccess("Thành công", "Đã thu cọc " + String.format("%,.0f đ", amount) + " và xác nhận đơn!");
                } else {
                    showError("Lỗi khi cập nhật cơ sở dữ liệu!");
                }
            } catch (NumberFormatException ex) {
                showError("Số tiền không hợp lệ!");
            }
        }
    }

    private void doConfirmWaitlist(String ma, String ten) {
        int ok = JOptionPane.showConfirmDialog(this, "Xác nhận đặt phòng cho \"" + ten + "\" từ danh sách chờ?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            if (new dao.DatPhongDAO().confirmFromWaitlist(ma)) {
                loadDatPhongTable();
                NotificationManager.showSuccess("Thành công", "Đã xác nhận từ waitlist thành công!");
            } else
                showError("Không thể cập nhật trạng thái!");
        }
    }

    private void doMoveToWaitlist(String ma, String ten) {
        int ok = JOptionPane.showConfirmDialog(this, 
                "Bạn có chắc muốn chuyển đơn đặt phòng của \"" + ten + "\" sang danh sách Chờ xếp phòng?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            String err = datPhongService.moveToWaitlist(ma);
            if (err == null) {
                refresh();
                NotificationManager.showSuccess("Thành công", "Đã chuyển đơn sang danh sách chờ xếp phòng!");
            } else {
                showError(err);
            }
        }
    }

    private void showWarning(JDialog parent, String msg) {
        NotificationManager.showWarning("Cảnh báo", msg);
    }

    private void refreshNotificationBell() {
        if (pnlBell != null) {
            pnlBell.removeAll();
            pnlBell.add(buildAlertBell());
            pnlBell.revalidate();
            pnlBell.repaint();
        }
        // Sync with global bell if possible
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w instanceof MainFrame) {
            ((MainFrame) w).refreshNotifications();
        }
    }

    private JComponent buildAlertBell() {
        List<DatPhong> noShows = datPhongService.getNoShowBookings(4);
        List<DatPhong> upcoming = datPhongService.getUpcomingArrivals(6);
        List<DatPhong> longPending = datPhongService.getLongPendingBookings(0);
        List<DatPhong> overdue = datPhongService.getOverdueDepositBookings();
        List<DatPhong> nearDeadline = datPhongService.getUpcomingDepositDeadlines(12);

        // BUSINESS LOGIC: Check for rooms needing preparation
        List<DatPhong> needPrep = new ArrayList<>();
        for (DatPhong dp : upcoming) {
            boolean hasDirty = false;
            if (dp.getDsChiTiet() != null) {
                for (ChiTietDatPhong ct : dp.getDsChiTiet()) {
                    if (ct.getPhong() != null) {
                        entity.enums.TrangThaiPhong tt = ct.getPhong().getTrangThai();
                        if (tt == entity.enums.TrangThaiPhong.CLEANING || tt == entity.enums.TrangThaiPhong.MAINTENANCE) {
                            hasDirty = true;
                            break;
                        }
                    }
                }
            }
            if (hasDirty) needPrep.add(dp);
        }

        int count = noShows.size() + upcoming.size() + longPending.size() + overdue.size() + nearDeadline.size() + needPrep.size();

        JButton btnBell = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Hover effect
                if (getModel().isRollover()) {
                    g2.setColor(new Color(0, 0, 0, 10));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }

                // Bell Icon
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
                // Use orange for generic alerts, red for danger (no-shows)
                Color bellColor = count > 0 ? (noShows.isEmpty() ? new Color(0xF59E0B) : UIConstants.DANGER) : UIConstants.TEXT_MUTED;
                g2.setColor(bellColor);
                g2.drawString("🔔", 10, 28);

                // Notification Badge
                if (count > 0) {
                    int badgeSize = 18;
                    int bx = getWidth() - badgeSize - 2;
                    int by = 2;

                    g2.setColor(bellColor);
                    g2.fillOval(bx, by, badgeSize, badgeSize);

                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                    String s = String.valueOf(count);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(s, bx + (badgeSize - fm.stringWidth(s)) / 2,
                            by + (badgeSize + fm.getAscent() - fm.getDescent()) / 2);
                }
                g2.dispose();
            }
        };
        btnBell.setToolTipText("Trung tâm cảnh báo (" + count + ")");
        btnBell.setPreferredSize(new Dimension(44, 44));
        btnBell.setFocusPainted(false);
        btnBell.setBorderPainted(false);
        btnBell.setContentAreaFilled(false);
        btnBell.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnBell.addActionListener(e -> {
            List<DatPhong> currentNoShows = datPhongService.getNoShowBookings(4);
            List<DatPhong> currentUpcoming = datPhongService.getUpcomingArrivals(6);
            List<DatPhong> currentLongPending = datPhongService.getLongPendingBookings(0);
            List<DatPhong> currentOverdue = datPhongService.getOverdueDepositBookings();
            List<DatPhong> currentNearDeadline = datPhongService.getUpcomingDepositDeadlines(12);

            List<DatPhong> currentNeedPrep = new ArrayList<>();
            for (DatPhong dp : currentUpcoming) {
                boolean hasDirty = false;
                if (dp.getDsChiTiet() != null) {
                    for (ChiTietDatPhong ct : dp.getDsChiTiet()) {
                        if (ct.getPhong() != null) {
                            entity.enums.TrangThaiPhong tt = ct.getPhong().getTrangThai();
                            if (tt == entity.enums.TrangThaiPhong.CLEANING || tt == entity.enums.TrangThaiPhong.MAINTENANCE) {
                                hasDirty = true;
                                break;
                            }
                        }
                    }
                }
                if (hasDirty) currentNeedPrep.add(dp);
            }

            showAlertDialog(currentNoShows, currentUpcoming, currentLongPending, currentOverdue, currentNeedPrep, currentNearDeadline);
        });

        return btnBell;
    }

    private JPanel buildStats() {
        JPanel p = new JPanel(new GridLayout(1, 4, 16, 0));
        p.setOpaque(false);

        p.add(buildStatCard("Tổng đơn đặt", "0", new Color(0x6366F1), 1));
        p.add(buildStatCard("Sắp đến", "0", new Color(0xF59E0B), 2));
        p.add(buildStatCard("Đang lưu trú", "0", new Color(0x3B82F6), 3));
        p.add(buildStatCard("Hoàn thành", "0", new Color(0x10B981), 4));

        return p;
    }

    private JPanel buildStatCard(String title, String value, Color accent, int type) {
        RoundedPanel card = new RoundedPanel(12);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(UIConstants.FONT_SMALL_BOLD);
        lblTitle.setForeground(UIConstants.TEXT_SECONDARY);

        JLabel lblVal = new JLabel(value);
        lblVal.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblVal.setForeground(UIConstants.TEXT_PRIMARY);

        if (type == 1) lblStatTotal = lblVal;
        else if (type == 2) lblStatUpcoming = lblVal;
        else if (type == 3) lblStatInHouse = lblVal;
        else if (type == 4) lblStatCompleted = lblVal;

        JPanel indicator = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
            }
        };
        indicator.setPreferredSize(new Dimension(4, 24));

        JPanel left = new JPanel(new BorderLayout(12, 0));
        left.setOpaque(false);
        left.add(indicator, BorderLayout.WEST);
        
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);
        text.add(lblTitle);
        text.add(lblVal);
        left.add(text, BorderLayout.CENTER);

        card.add(left, BorderLayout.WEST);
        return card;
    }

    public void refresh() {
        if (txtSearchList != null)
            txtSearchList.setText("");
        if (cboFilterStatus != null)
            cboFilterStatus.setSelectedIndex(0);
        loadDatPhongTable();
        refreshNotificationBell();
    }
}
