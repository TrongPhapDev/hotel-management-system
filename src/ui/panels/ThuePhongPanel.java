package ui.panels;

import ui.MainFrame;
import ui.components.UIConstants;
import ui.components.RoundedComponents;
import static ui.components.RoundedComponents.*;
import ui.components.*;
import ui.dialogs.*;
import dao.*;
import entity.*;
import entity.enums.TrangThaiPhong;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.util.*;
import javax.swing.*;

import service.*;

public class ThuePhongPanel extends JPanel implements ResettableFilter {

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger
            .getLogger(ThuePhongPanel.class.getName());

    private final MainFrame mainFrame;
    private final PhongService phongService = new PhongService();
    private final ThuePhongService thuePhongService = new ThuePhongService();
    private final BangGiaService bangGiaService = new BangGiaService();

    private JPanel roomGrid;
    private String currentFilter = "ALL";
    private int currentFloor = 0; // 0 = tat ca
    private String currentType = "ALL"; // ALL = tat ca
    private String currentView = "ALL"; // ALL = tat ca
    private String searchRoom = ""; // tim so phong
    private RoundedPanel filterTabsPanel;
    // ★ Map phòng đã đặt trước: maPhong -> [maDatPhong, tenKH, ngayNhan]
    private Map<String, String[]> reservedRooms = new HashMap<>();
    // ★ Map phòng đang thuê thuộc đoàn: maPhong -> maDatPhong
    private Map<String, String> activeGroupRooms = new HashMap<>();
    private javax.swing.Timer countdownTimer;

    public ThuePhongPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();
        startTimer();
    }

    private void startTimer() {
        if (countdownTimer != null && countdownTimer.isRunning()) return;
        countdownTimer = new javax.swing.Timer(60000, e -> refreshGrid());
        countdownTimer.start();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (countdownTimer != null) countdownTimer.stop();
    }

    public void resetFilters() {
        currentFilter = "ALL";
        currentFloor = 0;
        currentType = "ALL";
        currentView = "ALL";
        searchRoom = "";
        rebuildFilter();
        refreshGrid();
    }

    public void applyOccupiedFilter() {
        currentFilter = "OCCUPIED";
        currentFloor = 0;
        currentType = "ALL";
        currentView = "ALL";
        searchRoom = "";
        rebuildFilter();
        refreshGrid();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(24, 28, 16, 28));

        main.add(buildHeader(), BorderLayout.NORTH);

        // Filter + Scroll container
        JPanel filterAndScroll = new JPanel(new BorderLayout(0, 12));
        filterAndScroll.setOpaque(false);
        filterAndScroll.add(buildFilterTabs(), BorderLayout.NORTH);

        // Room grid inside scroll pane.
        // IMPORTANT: implement Scrollable so the JScrollPane viewport constrains
        // the panel's width — without this, WrapLayout sees an unbounded width
        // and never wraps cards to the next row.
        roomGrid = new ScrollablePanel();
        roomGrid.setOpaque(false);
        refreshGrid();

        JScrollPane scroll = new JScrollPane(roomGrid);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        // Hide horizontal scrollbar — wrapping handles overflow
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        filterAndScroll.add(scroll, BorderLayout.CENTER);
        main.add(filterAndScroll, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(main, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        
        JLabel title = new JLabel("Sơ đồ phòng");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel sub = new JLabel("Click vào phòng để nhận/trả phòng, chuột phải để xem thêm");
        sub.setFont(UIConstants.FONT_BODY);
        sub.setForeground(UIConstants.TEXT_SECONDARY);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(sub);

        RoundedButton btnCheckin = new RoundedButton("+ Nhận phòng", UIConstants.PRIMARY, Color.WHITE);
        btnCheckin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCheckin.setPreferredSize(new Dimension(160, 42));
        btnCheckin.addActionListener(e -> showCheckinDialog(null));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(btnCheckin);

        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    // ---- Filter bar (Modern Grid Design) ----
    private JPanel buildFilterTabs() {
        filterTabsPanel = new RoundedPanel(16);
        filterTabsPanel.setBackground(Color.WHITE);
        filterTabsPanel.setShadow(true);
        filterTabsPanel.setLayout(new GridBagLayout());
        filterTabsPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        java.util.List<Phong> allPhong = phongService.getAllPhong();
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 0;

        // --- ROW 1: Status Chips + Search ---
        g.gridy = 0; g.insets = new Insets(0, 0, 10, 0); 
        
        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        statusPanel.setOpaque(false);
        int total = allPhong.size();
        int available = (int) allPhong.stream().filter(p -> p.getTrangThai() == TrangThaiPhong.AVAILABLE).count();
        int occupied = (int) allPhong.stream().filter(p -> p.getTrangThai() == TrangThaiPhong.OCCUPIED).count();
        int cleaning = (int) allPhong.stream().filter(p -> p.getTrangThai() == TrangThaiPhong.CLEANING).count();
        int maint = (int) allPhong.stream().filter(p -> p.getTrangThai() == TrangThaiPhong.MAINTENANCE).count();

        statusPanel.add(statusChip("Tất cả", "ALL", total, new Color(0x64748B)));
        statusPanel.add(statusChip("Có sẵn", "AVAILABLE", available, new Color(0x10B981)));
        statusPanel.add(statusChip("Đang thuê", "OCCUPIED", occupied, new Color(0x3B82F6)));
        statusPanel.add(statusChip("Vệ sinh", "CLEANING", cleaning, new Color(0xF59E0B)));
        statusPanel.add(statusChip("Bảo trì", "MAINTENANCE", maint, new Color(0xEF4444)));

        g.gridx = 0; g.weightx = 1.0; g.gridwidth = 2;
        filterTabsPanel.add(statusPanel, g);

        // Search & Clear
        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightTools.setOpaque(false);

        RoundedButton btnClear = new RoundedButton("Xóa lọc", new Color(0xF1F5F9), UIConstants.TEXT_PRIMARY);
        btnClear.setPreferredSize(new Dimension(90, 36));
        btnClear.setFont(UIConstants.FONT_SMALL_BOLD);
        btnClear.addActionListener(e -> resetFilters());

        ModernTextField txtSearch = new ModernTextField("Tìm số phòng, tên khách, SĐT...");
        txtSearch.setPreferredSize(new Dimension(180, 36));
        txtSearch.setText(searchRoom);
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                searchRoom = txtSearch.getText().trim();
                refreshGrid();
            }
        });

        rightTools.add(btnClear);
        rightTools.add(txtSearch);

        g.gridx = 2; g.weightx = 0; g.gridwidth = 1;
        g.fill = GridBagConstraints.VERTICAL;
        g.anchor = GridBagConstraints.EAST;
        filterTabsPanel.add(rightTools, g);

        // --- ROW 2: Floors + Dropdowns (Consolidated) ---
        g.fill = GridBagConstraints.BOTH;
        g.anchor = GridBagConstraints.CENTER;
        g.gridy = 1; g.insets = new Insets(0, 0, 0, 0);
        
        // Collect data
        Set<Integer> floors = new TreeSet<>();
        Set<String> types = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> views = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Phong p : allPhong) {
            floors.add(p.getTang());
            if (p.getTenLoaiPhong() != null && !p.getTenLoaiPhong().isBlank()) types.add(p.getTenLoaiPhong());
            if (p.getView() != null && !p.getView().isBlank()) views.add(p.getView());
        }

        // Left Container for Floor + Dropdowns
        JPanel subFilterPanel = new JPanel(new BorderLayout());
        subFilterPanel.setOpaque(false);

        JPanel floorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        floorPanel.setOpaque(false);
        JLabel lblFloor = new JLabel("Tầng:");
        lblFloor.setFont(UIConstants.FONT_SMALL_BOLD);
        lblFloor.setForeground(UIConstants.TEXT_SECONDARY);
        floorPanel.add(lblFloor);
        floorPanel.add(floorChip("Tất cả", 0, currentFloor == 0));
        for (int f : floors) floorPanel.add(floorChip("T" + f, f, currentFloor == f));
        
        subFilterPanel.add(floorPanel, BorderLayout.WEST);

        // Dropdowns on the same row, pushed to the right but close to each other
        JPanel dropPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        dropPanel.setOpaque(false);

        // Type
        java.util.List<String> typeItems = new ArrayList<>();
        typeItems.add("Tất cả loại phòng");
        typeItems.addAll(types);
        ModernComboBox<String> cboType = new ModernComboBox<>(typeItems.toArray(new String[0]));
        cboType.setPreferredSize(new Dimension(150, 38));
        cboType.setSelectedItem("ALL".equals(currentType) ? "Tất cả loại phòng" : currentType);
        cboType.addActionListener(e -> {
            String selected = (String) cboType.getSelectedItem();
            currentType = "Tất cả loại phòng".equals(selected) ? "ALL" : selected;
            refreshGrid();
        });
        dropPanel.add(labeledFilter("Loại phòng", cboType));

        // View
        java.util.List<String> viewItems = new ArrayList<>();
        viewItems.add("Tất cả hướng");
        viewItems.addAll(views);
        ModernComboBox<String> cboView = new ModernComboBox<>(viewItems.toArray(new String[0]));
        cboView.setPreferredSize(new Dimension(140, 38));
        cboView.setSelectedItem("ALL".equals(currentView) ? "Tất cả hướng" : currentView);
        cboView.addActionListener(e -> {
            String selected = (String) cboView.getSelectedItem();
            currentView = "Tất cả hướng".equals(selected) ? "ALL" : selected;
            refreshGrid();
        });
        dropPanel.add(labeledFilter("Hướng", cboView));

        subFilterPanel.add(dropPanel, BorderLayout.EAST);

        g.gridx = 0; g.gridwidth = 3; g.weightx = 1.0;
        filterTabsPanel.add(subFilterPanel, g);

        return filterTabsPanel;
    }

    private JPanel labeledFilter(String label, JComponent comp) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_SECONDARY);
        p.add(lbl);
        p.add(comp);
        return p;
    }

    // Status chip with high-end badge
    private JPanel statusChip(String label, String filterKey, int count, Color accent) {
        boolean active = filterKey.equals(currentFilter);
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (active) {
                    g2.setColor(accent);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                } else {
                    g2.setColor(new Color(0xF1F5F9));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                }
                g2.dispose();
            }
        };
        chip.setOpaque(false);
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel lblText = new JLabel(label);
        lblText.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 13));
        lblText.setForeground(active ? Color.WHITE : UIConstants.TEXT_SECONDARY);
        chip.add(lblText);

        if (count >= 0) {
            JLabel lblCount = new JLabel(String.valueOf(count));
            lblCount.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lblCount.setForeground(active ? accent : Color.WHITE);
            JPanel badge = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0)) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(active ? Color.WHITE : accent);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                }
            };
            badge.setOpaque(false);
            badge.add(lblCount);
            chip.add(badge);
        }

        chip.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                currentFilter = filterKey;
                rebuildFilter();
                refreshGrid();
            }
        });
        return chip;
    }

    // Floor toggle chip (modern pill style)
    private JLabel floorChip(String text, int floor, boolean active) {
        JLabel lbl = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (active) {
                    g2.setColor(UIConstants.PRIMARY);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                } else {
                    g2.setColor(new Color(0xF1F5F9));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 12));
        lbl.setForeground(active ? Color.WHITE : UIConstants.TEXT_SECONDARY);
        lbl.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                currentFloor = floor;
                rebuildFilter();
                refreshGrid();
            }
        });
        return lbl;
    }

    public void rebuildFilter() {
        if (filterTabsPanel != null && filterTabsPanel.getParent() != null) {
            java.awt.Container parent = filterTabsPanel.getParent();
            parent.remove(filterTabsPanel);
            parent.add(buildFilterTabs(), BorderLayout.NORTH);
            parent.revalidate();
            parent.repaint();
        }
    }

    // ---- Room grid ----
    // Card width = 190px, hgap = 10px → 5 cards × (190+10) = 1000px ≈ fits typical
    // 1280px window
    private static final int CARD_W = 190;
    private static final int CARD_H = 145; // Tăng nhẹ để chứa đủ tên + countdown
    private static final int CARD_HGAP = 10;
    private static final int CARD_VGAP = 10;

    public void refreshGrid() {
        roomGrid.removeAll();
        roomGrid.setLayout(new BoxLayout(roomGrid, BoxLayout.Y_AXIS));

        java.util.List<Phong> allPhong = phongService.getAllPhong();

        // ★ Load reservation data for today
        try {
            reservedRooms = new PhongDAO().getReservedRoomsForDate(LocalDateTime.now());
            activeGroupRooms = thuePhongService.getActiveGroupRoomsMap();
        } catch (Exception e) {
            reservedRooms = new HashMap<>();
            activeGroupRooms = new HashMap<>();
        }

        // Group by floor, apply filter
        Map<Integer, java.util.List<Phong>> byFloor = new TreeMap<>();
        for (Phong p : allPhong) {
            boolean showStatus = false;
            TrangThaiPhong tt = p.getTrangThai();
            if ("ALL".equals(currentFilter))
                showStatus = true;
            else {
                try {
                    TrangThaiPhong filterEnum = TrangThaiPhong.valueOf(currentFilter);
                    showStatus = (tt == filterEnum);
                } catch (Exception e) {
                    showStatus = true;
                }
            }
            boolean showFloor = (currentFloor == 0) || (p.getTang() == currentFloor);
            boolean showType = "ALL".equals(currentType)
                    || (p.getTenLoaiPhong() != null && p.getTenLoaiPhong().equalsIgnoreCase(currentType));
            boolean showSearch = searchRoom.isEmpty()
                    || (p.getSoPhong() != null && p.getSoPhong().toLowerCase().contains(searchRoom.toLowerCase()))
                    || (p.getTenKhachHienTai() != null && p.getTenKhachHienTai().toLowerCase().contains(searchRoom.toLowerCase()))
                    || (p.getSdtKhachHienTai() != null && p.getSdtKhachHienTai().contains(searchRoom));
            boolean showView = "ALL".equals(currentView)
                    || (p.getView() != null && p.getView().equalsIgnoreCase(currentView));

            if (showStatus && showFloor && showType && showSearch && showView)
                byFloor.computeIfAbsent(p.getTang(), k -> new ArrayList<>()).add(p);
        }

        if (byFloor.isEmpty()) {
            JLabel lbl = new JLabel("Không có phòng nào phù hợp", SwingConstants.CENTER);
            lbl.setFont(UIConstants.FONT_BODY);
            lbl.setForeground(UIConstants.TEXT_MUTED);
            lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            roomGrid.add(Box.createVerticalStrut(40));
            roomGrid.add(lbl);
        } else {
            for (Map.Entry<Integer, java.util.List<Phong>> entry : byFloor.entrySet()) {
                roomGrid.add(buildFloorSection(entry.getKey(), entry.getValue()));
                roomGrid.add(Box.createVerticalStrut(12));
            }
        }

        roomGrid.revalidate();
        roomGrid.repaint();
    }

    // ---- Floor section ----
    // ---- Floor section ----
    private JPanel buildFloorSection(int floor, java.util.List<Phong> phongs) {
        JPanel section = new JPanel();
        section.setOpaque(false);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 9999));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Floor header
        JPanel floorHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        floorHeader.setOpaque(false);
        floorHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblFloor = new JLabel("Tầng " + floor);
        lblFloor.setFont(UIConstants.FONT_HEADER);
        lblFloor.setForeground(UIConstants.TEXT_PRIMARY);
        JLabel lblCount = new JLabel("(" + phongs.size() + " phòng)");
        lblCount.setFont(UIConstants.FONT_SMALL);
        lblCount.setForeground(UIConstants.TEXT_MUTED);
        floorHeader.add(lblFloor);
        floorHeader.add(lblCount);

        // Separator
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setForeground(new Color(200, 210, 245));
        separator.setBackground(new Color(235, 240, 255));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);

        // WrapLayout panel - cards auto-wrap based on container width
        // Each card = CARD_W px, so at ~1000px width = 5 cards per row
        JPanel cardsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, CARD_HGAP, CARD_VGAP));
        cardsPanel.setOpaque(false);
        cardsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Phong p : phongs)
            cardsPanel.add(buildRoomCard(p));

        section.add(floorHeader);
        section.add(Box.createVerticalStrut(6));
        section.add(separator);
        section.add(Box.createVerticalStrut(8));
        section.add(cardsPanel);
        return section;
    }

    // ---- Room card (fixed CARD_W × CARD_H so WrapLayout wraps at 5/row) ----
    private RoundedPanel buildRoomCard(Phong p) {
        TrangThaiPhong tt = p.getTrangThai();
        Color borderClr = UIConstants.getTrangThaiPhongColor(tt);
        Color bgHover = UIConstants.getTrangThaiPhongBg(tt);
        String txtStatus = UIConstants.getTrangThaiPhongLabel(tt);

        // ★ Reservation Logic Tweak (Professional PMS Style)
        boolean hasReservation = reservedRooms.containsKey(p.getSoPhong());
        String reservedInfo = "";
        if (hasReservation && tt == TrangThaiPhong.AVAILABLE) {
            borderClr = new Color(0xF59E0B); // Amber / Warning
            bgHover = new Color(0xFEF3C7);
            txtStatus = "Sắp check-in";
            String[] info = reservedRooms.get(p.getSoPhong());
            if (info.length > 2)
                reservedInfo = info[2]; // Time
        }

        RoundedPanel card = new RoundedPanel(10);
        card.setBackground(Color.WHITE);
        card.setLayout(new BorderLayout(0, 4));
        // Fixed size → WrapLayout knows exactly when to move to next row
        card.setPreferredSize(new Dimension(CARD_W, CARD_H));
        card.setMinimumSize(new Dimension(CARD_W, CARD_H));
        card.setMaximumSize(new Dimension(CARD_W, CARD_H));
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(10, borderClr),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Top: số phòng + trạng thái
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel lblNum = new JLabel(p.getSoPhong());
        lblNum.setFont(new Font("Segoe UI", Font.BOLD, 15));

        JLabel lblTT = new JLabel(txtStatus);
        lblTT.setFont(UIConstants.FONT_TINY);
        lblTT.setForeground(borderClr);
        if (hasReservation && tt == TrangThaiPhong.AVAILABLE) {
            lblTT.setOpaque(true);
            lblTT.setBackground(bgHover);
            lblTT.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        }
        topRow.add(lblNum, BorderLayout.WEST);
        topRow.add(lblTT, BorderLayout.EAST);

        // Middle: loại + view + khách
        JLabel lblType = new JLabel(p.getTenLoaiPhong() != null ? p.getTenLoaiPhong() : "");
        lblType.setFont(UIConstants.FONT_SMALL);
        lblType.setForeground(UIConstants.TEXT_SECONDARY);

        JLabel lblView = new JLabel(p.getView() != null ? p.getView() : "");
        lblView.setFont(UIConstants.FONT_TINY);
        lblView.setForeground(UIConstants.TEXT_MUTED);

        // Group indicator for occupied rooms
        boolean isGroup = tt == TrangThaiPhong.OCCUPIED && activeGroupRooms.containsKey(p.getSoPhong());
        if (isGroup) {
            String groupCode = activeGroupRooms.get(p.getSoPhong());
            String shortCode = groupCode.substring(Math.max(0, groupCode.length() - 4));
            JLabel lblGroup = new JLabel("🔗 ĐOÀN-" + shortCode);
            lblGroup.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lblGroup.setForeground(new Color(0x6366F1)); // Indigo
            lblGroup.setOpaque(true);
            lblGroup.setBackground(new Color(0xE0E7FF));
            lblGroup.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            lblGroup.setToolTipText("Thuộc đoàn " + groupCode);
            lblType.setText(""); // Hide type text to save space for group badge
            lblType.setIcon(null);
            card.putClientProperty("groupBadge", lblGroup);
        }

        JLabel lblGuest = new JLabel();
        lblGuest.setFont(UIConstants.FONT_SMALL);

        // Hiển thị tên khách: CHỈ khi phòng đang ở trạng thái ĐANG THUÊ (Defensive UI)
        if (tt == TrangThaiPhong.OCCUPIED && p.getTenKhachHienTai() != null) {
            lblGuest.setText(truncate(p.getTenKhachHienTai(), 14));
            lblGuest.setForeground(UIConstants.PRIMARY);

            try {
                // Sử dụng icon người cho khách đang ở
                ImageIcon originalIcon = new ImageIcon("icon/canhan.png");
                Image scaled = originalIcon.getImage().getScaledInstance(14, 14, Image.SCALE_SMOOTH);
                lblGuest.setIcon(new ImageIcon(scaled));
            } catch (Exception ex) {}

            // --- THÊM: Countdown trả phòng ---
            if (p.getExpectedCheckOutTime() != null) {
                java.time.Duration duration = java.time.Duration.between(LocalDateTime.now(), p.getExpectedCheckOutTime());
                long minutes = duration.toMinutes();
                JLabel lblCount = new JLabel();
                lblCount.setFont(new Font("Segoe UI", Font.BOLD, 10));
                
                if (minutes > 0) {
                    long h = minutes / 60;
                    long m = minutes % 60;
                    lblCount.setText(String.format("Còn %dh %02dp", h, m));
                    lblCount.setForeground(minutes < 30 ? new Color(0xD97706) : new Color(0x059669));
                } else {
                    long late = Math.abs(minutes);
                    lblCount.setText(String.format("Quá giờ %dh %02dp", late / 60, late % 60));
                    lblCount.setForeground(new Color(0xDC2626));
                }
                lblGuest.setText(truncate(p.getTenKhachHienTai(), 10) + " • ");
                lblGuest.setToolTipText(p.getTenKhachHienTai());
                
                // Cấu trúc lại lblGuest để chứa countdown bằng Box để kiểm soát layout tốt hơn
                Box pnlGuest = Box.createHorizontalBox();
                pnlGuest.add(lblGuest);
                pnlGuest.add(lblCount);
                pnlGuest.setAlignmentX(Component.LEFT_ALIGNMENT);
                pnlGuest.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22)); 
                
                card.putClientProperty("guestPanel", pnlGuest); 
            }
        } else {
            lblGuest.setAlignmentX(Component.LEFT_ALIGNMENT);
            // Các trạng thái khác (Có sẵn, Vệ sinh, Bảo trì) -> Hiện sức chứa tối đa
            lblGuest.setText(p.getSucChua() + " người tối đa");
            lblGuest.setForeground(UIConstants.TEXT_MUTED);
            lblGuest.setIcon(null);
        }

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        lblType.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblView.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComponent groupBadge = (JComponent) card.getClientProperty("groupBadge");
        if (groupBadge != null) {
            groupBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
            center.add(groupBadge);
            center.add(Box.createVerticalStrut(2));
        } else {
            center.add(lblType);
        }
        center.add(Box.createVerticalStrut(1));
        center.add(lblView);
        center.add(Box.createVerticalStrut(3));
        
        JComponent gp = (JComponent) card.getClientProperty("guestPanel");
        if (gp != null) center.add(gp);
        else center.add(lblGuest);

        // Bottom: giá
        double giaActive = bangGiaService
                .layGiaHienHanh(p.getLoaiPhong() != null ? p.getLoaiPhong().getMaLoaiPhong() : "");
        JLabel lblPrice = new JLabel(String.format("%,.0fđ/đêm", giaActive));
        lblPrice.setFont(UIConstants.FONT_SMALL_BOLD);
        lblPrice.setForeground(UIConstants.SUCCESS);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(lblPrice, BorderLayout.WEST);

        // Show reservation time subtly if it exists
        if (!reservedInfo.isEmpty()) {
            JLabel lblRsvTime = new JLabel("" + reservedInfo);
            lblRsvTime.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            lblRsvTime.setForeground(new Color(0xD97706));
            bottom.add(lblRsvTime, BorderLayout.EAST);
        }

        card.add(topRow, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);

        final Color finalBgHover = bgHover;
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1)
                    handleRoomClick(p);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    showContextMenu(p, e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    showContextMenu(p, e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(finalBgHover);
                card.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(Color.WHITE);
                card.repaint();
            }
        });
        return card;
    }

    private void handleRoomClick(Phong p) {
        TrangThaiPhong tt = p.getTrangThai();
        if (tt == TrangThaiPhong.AVAILABLE) {
            showCheckinDialog(p);
        } else if (tt == TrangThaiPhong.OCCUPIED) {
            showCheckoutDialog(p);
        } else if (tt == TrangThaiPhong.CLEANING) {
            showCleaningToAvailableDialog(p);
        } else {
            JOptionPane.showMessageDialog(mainFrame,
                    "Phòng " + p.getSoPhong() + " - Trạng thái: " + p.getTrangThai(),
                    "Thông tin", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void showCleaningToAvailableDialog(Phong p) {
        JDialog dlg = new JDialog(mainFrame, "Cập nhật phòng " + p.getSoPhong(), true);
        dlg.setSize(480, 230);
        dlg.setLocationRelativeTo(mainFrame);
        dlg.getContentPane().setBackground(Color.WHITE);

        JPanel main = new JPanel(new BorderLayout(15, 15));
        main.setBackground(Color.WHITE);
        main.setBorder(BorderFactory.createEmptyBorder(25, 25, 20, 25));

        // Center content
        JPanel centerPanel = new JPanel(new BorderLayout(15, 0));
        centerPanel.setOpaque(false);

        // Icon
        JLabel lblIcon = new JLabel("✨");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 42));
        lblIcon.setForeground(UIConstants.SUCCESS);
        centerPanel.add(lblIcon, BorderLayout.WEST);

        // Message
        String msg = "<html><body style='width: 320px; color: #334155; font-family: Segoe UI, sans-serif;'>"
                + "<h3 style='margin: 0 0 8px 0; color: #0F172A;'>Phòng đã dọn dẹp xong?</h3>"
                + "Bạn muốn chuyển phòng <b>" + p.getSoPhong()
                + "</b> sang trạng thái phòng trống sẵn sàng đón khách?</body></html>";
        JLabel lblMessage = new JLabel(msg);
        centerPanel.add(lblMessage, BorderLayout.CENTER);

        main.add(centerPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setBackground(Color.WHITE);
        RoundedButton btnCancel = new RoundedButton("Huỷ", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        RoundedButton btnConfirm = new RoundedButton("Xác nhận phòng trống", UIConstants.SUCCESS, Color.WHITE);

        btnCancel.addActionListener(e -> dlg.dispose());
        btnConfirm.addActionListener(e -> {
            phongService.updateTrangThai(p.getSoPhong(), TrangThaiPhong.AVAILABLE);
            NotificationManager.showSuccess("Cập nhật thành công", "Phòng " + p.getSoPhong() + " đã sẵn sàng đón khách");
            rebuildFilter();
            refreshGrid();
            dlg.dispose();
        });

        buttons.add(btnCancel);
        buttons.add(btnConfirm);
        main.add(buttons, BorderLayout.SOUTH);

        dlg.setContentPane(main);
        dlg.setVisible(true);
    }

    private void showCheckinDialog(Phong p) {
        CheckinDialog dlg = new CheckinDialog(
                (java.awt.Frame) SwingUtilities.getWindowAncestor(this), p);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            rebuildFilter();
            refreshGrid();
        }
    }

    private void showCheckoutDialog(Phong p) {
        ChiTietDatPhong tp = thuePhongService.getActiveByPhong(p.getSoPhong());
        if (tp == null) {
            JOptionPane.showMessageDialog(mainFrame, "Không tìm thấy thông tin thuê phòng!");
            return;
        }

        // Kiểm tra xem phòng này có thuộc đoàn không (nhiều hơn 1 phòng)
        boolean isGroup = false;
        long activeRoomsInGroup = 0;
        java.util.List<ChiTietDatPhong> allGroupRooms = null;
        if (tp.getDatPhong() != null) {
            allGroupRooms = thuePhongService.getChiTietByDatPhong(tp.getDatPhong().getMaDatPhong());
            activeRoomsInGroup = allGroupRooms.stream().filter(r -> r.getNgayTraThucTe() == null).count();
            if (allGroupRooms.size() > 1 || "DOAN".equalsIgnoreCase(tp.getDatPhong().getLoaiKhach())) {
                isGroup = true;
            }
        }

        if (isGroup && activeRoomsInGroup > 1) {
            // CÒn nhiều phòng chưa checkout → Thanh toán riêng hoặc Treo nợ

            int choice = -1;
            JDialog dlg = new JDialog(mainFrame, "", true);
            dlg.setUndecorated(true);
            dlg.setSize(520, 240);
            dlg.setLocationRelativeTo(mainFrame);

            JPanel root = new JPanel(new BorderLayout());
            root.setBackground(Color.WHITE);
            root.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0, 0, 0, 40), 1),
                    BorderFactory.createEmptyBorder(25, 30, 20, 30)));

            JPanel centerPanel = new JPanel(new BorderLayout(20, 0));
            centerPanel.setOpaque(false);

            // Icon
            JLabel lblIcon = new JLabel("🏢");
            lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 46));
            lblIcon.setForeground(UIConstants.PRIMARY);
            centerPanel.add(lblIcon, BorderLayout.WEST);

            // Text
            String msg = "<html><body style='width: 320px; color: #334155; font-family: Segoe UI, sans-serif;'>"
                    + "<h3 style='margin: 0 0 10px 0; color: #0F172A; font-size: 18px;'>Hình thức Trả phòng Đoàn</h3>"
                    + "Phòng <b>" + p.getSoPhong() + "</b> thuộc về một hợp đồng ĐOÀN đang có " + activeRoomsInGroup
                    + " phòng được thuê.<br><br>"
                    + "Bạn muốn xử lý hoá đơn cho phòng này như thế nào?</body></html>";
            JLabel lblMessage = new JLabel(msg);
            centerPanel.add(lblMessage, BorderLayout.CENTER);
            root.add(centerPanel, BorderLayout.CENTER);

            // Buttons
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
            buttons.setOpaque(false);
            buttons.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

            RoundedButton btnCancel = new RoundedButton("Huỷ bỏ", new Color(0xF1F5F9), new Color(0x64748B));
            RoundedButton btnRoute = new RoundedButton("Treo nợ Đoàn", new Color(0xEFF6FF), new Color(0x3B82F6));
            RoundedButton btnPay = new RoundedButton("Thanh toán riêng", UIConstants.PRIMARY, Color.WHITE);
            RoundedButton btnMaster = new RoundedButton("Thanh toán Cả đoàn", UIConstants.SUCCESS, Color.WHITE);

            // Hack to get values out of lambda without an array
            final int[] resultArr = { -1 };
            btnCancel.addActionListener(e -> {
                resultArr[0] = 2;
                dlg.dispose();
            });
            btnRoute.addActionListener(e -> {
                resultArr[0] = 1;
                dlg.dispose();
            });
            btnPay.addActionListener(e -> {
                resultArr[0] = 0;
                dlg.dispose();
            });
            btnMaster.addActionListener(e -> {
                resultArr[0] = 3;
                dlg.dispose();
            });

            buttons.add(btnCancel);
            buttons.add(btnRoute);
            buttons.add(btnPay);
            buttons.add(btnMaster);

            root.add(buttons, BorderLayout.SOUTH);
            dlg.setContentPane(root);
            dlg.setVisible(true);

            choice = resultArr[0];

            if (choice == 3) {
                // Thanh toán toàn bộ đoàn ngay lập tức
                showMasterBillDialog(tp.getDatPhong(), allGroupRooms);
                return;
            } else if (choice == 1) {
                    // Treo nợ đoàn (Master Folio Routing)
                    int[] confirmResult = { -1 };
                    JDialog confirmDlg = new JDialog(mainFrame, "", true);
                    confirmDlg.setUndecorated(true);
                    confirmDlg.setSize(440, 210);
                    confirmDlg.setLocationRelativeTo(mainFrame);

                    JPanel confirmRoot = new JPanel(new BorderLayout());
                    confirmRoot.setBackground(Color.WHITE);
                    confirmRoot.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(0, 0, 0, 50), 1),
                            BorderFactory.createEmptyBorder(25, 25, 20, 25)));

                    JPanel centerConfirm = new JPanel(new BorderLayout(15, 0));
                    centerConfirm.setOpaque(false);
                    JLabel lblQ = new JLabel("❓");
                    lblQ.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 46));
                    centerConfirm.add(lblQ, BorderLayout.WEST);

                    String msgConfirm = "<html><body style='width: 280px; color: #334155; font-family: Segoe UI, sans-serif;'>"
                            + "<h3 style='margin: 0 0 8px 0; color: #0F172A; font-size: 16px;'>Xác nhận Treo Nợ</h3>"
                            + "Hoá đơn phòng <b>" + p.getSoPhong()
                            + "</b> sẽ được chuyển toàn bộ sang Hóa Đơn Đoàn tổng. Bạn vẫn muốn tiếp tục?</body></html>";
                    centerConfirm.add(new JLabel(msgConfirm), BorderLayout.CENTER);
                    confirmRoot.add(centerConfirm, BorderLayout.CENTER);

                    JPanel btnConfirmPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
                    btnConfirmPanel.setOpaque(false);
                    btnConfirmPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

                    RoundedButton btnNo = new RoundedButton("Huỷ", new Color(0xF1F5F9), new Color(0x64748B));
                    RoundedButton btnYes = new RoundedButton("Treo nợ Đoàn", UIConstants.PRIMARY, Color.WHITE);
                    btnNo.addActionListener(e -> {
                        confirmResult[0] = 0;
                        confirmDlg.dispose();
                    });
                    btnYes.addActionListener(e -> {
                        confirmResult[0] = 1;
                        confirmDlg.dispose();
                    });
                    btnConfirmPanel.add(btnNo);
                    btnConfirmPanel.add(btnYes);

                    confirmRoot.add(btnConfirmPanel, BorderLayout.SOUTH);
                    confirmDlg.setContentPane(confirmRoot);
                    confirmDlg.setVisible(true);

                    if (confirmResult[0] == 1) {
                        String err = thuePhongService.checkOutWithoutBilling(tp.getMaChiTiet());
                        if (err == null) {
                            JDialog successDlg = new JDialog(mainFrame, "", true);
                            successDlg.setUndecorated(true);
                            successDlg.setSize(400, 180);
                            successDlg.setLocationRelativeTo(mainFrame);

                            JPanel rootS = new JPanel(new BorderLayout());
                            rootS.setBackground(Color.WHITE);
                            rootS.setBorder(BorderFactory.createCompoundBorder(
                                    BorderFactory.createLineBorder(new Color(0, 0, 0, 50), 1),
                                    BorderFactory.createEmptyBorder(25, 25, 20, 25)));

                            JPanel centerS = new JPanel(new BorderLayout(15, 0));
                            centerS.setOpaque(false);
                            JLabel lblTickS = new JLabel("✅");
                            lblTickS.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 46));
                            centerS.add(lblTickS, BorderLayout.WEST);

                            centerS.add(new JLabel(
                                    "<html><body style='width: 260px; color: #334155; font-family: Segoe UI, sans-serif;'>"
                                            + "<h3 style='margin: 0 0 8px 0; color: #0F172A; font-size: 16px;'>Thành công!</h3>"
                                            + "Đã trả phòng và chuyển nợ thành công vào bill Đoàn!</body></html>"),
                                    BorderLayout.CENTER);
                            rootS.add(centerS, BorderLayout.CENTER);

                            JPanel btnSPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
                            btnSPanel.setOpaque(false);
                            btnSPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
                            RoundedButton btnCloseS = new RoundedButton("Đóng", UIConstants.PRIMARY, Color.WHITE);
                            btnCloseS.addActionListener(e -> successDlg.dispose());
                            btnSPanel.add(btnCloseS);

                            rootS.add(btnSPanel, BorderLayout.SOUTH);
                            successDlg.setContentPane(rootS);
                            successDlg.setVisible(true);

                            rebuildFilter();
                            refreshGrid();
                            mainFrame.refreshThongKe();
                        } else {
                            JOptionPane.showMessageDialog(mainFrame, "Lỗi: " + err, "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    return;
            } else if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
                return;
            }
            // If choice == 0 -> Continue below to normal HoaDonDialog
        } else if (isGroup && activeRoomsInGroup == 1) {
            // Phòng CUỐI CÙNG của đoàn → Hỏi Gom Bill hay Thanh toán riêng
            long unpaidCount = allGroupRooms.stream().filter(r -> r.getNgayTraThucTe() != null && !r.isDaThanhToan())
                    .count();

            final int[] result = {-1}; // -1: cancel, 0: gom bill, 1: pay solo
            JDialog dlg = new JDialog(mainFrame, "", true);
            dlg.setUndecorated(true);
            dlg.setSize(520, unpaidCount > 0 ? 280 : 250);
            dlg.setLocationRelativeTo(mainFrame);

            JPanel root = new JPanel(new BorderLayout());
            root.setBackground(Color.WHITE);
            root.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0, 0, 0, 40), 1),
                    BorderFactory.createEmptyBorder(25, 30, 20, 30)));

            JPanel centerPanel = new JPanel(new BorderLayout(20, 0));
            centerPanel.setOpaque(false);

            // Icon
            JLabel lblIcon = new JLabel(unpaidCount > 0 ? "⚠️" : "📋");
            lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 46));
            centerPanel.add(lblIcon, BorderLayout.WEST);

            // Text
            StringBuilder msg = new StringBuilder("<html><body style='width: 320px; color: #334155; font-family: Segoe UI, sans-serif;'>");
            if (unpaidCount > 0) {
                msg.append("<h3 style='margin: 0 0 10px 0; color: #DC2626; font-size: 18px;'>Tiến hành Gom Bill Đoàn</h3>")
                   .append("Đây là phòng <b>CUỐI CÙNG</b> của đoàn.<br>")
                   .append("Hiện có <b style='color: #DC2626;'>").append(unpaidCount).append(" phòng</b> đã trả nhưng đang Treo nợ.<br><br>")
                   .append("<b>BẮT BUỘC</b> tiến hành GOM BILL toàn đoàn vào 1 Hóa đơn duy nhất để tất toán cho các phòng đã treo nợ.");
            } else {
                msg.append("<h3 style='margin: 0 0 10px 0; color: #0F172A; font-size: 18px;'>Gom Bill Đoàn</h3>")
                   .append("Đây là phòng <b>CUỐI CÙNG</b> của đoàn (").append(allGroupRooms.size()).append(" phòng).<br><br>")
                   .append("Bạn muốn GOM BILL toàn đoàn vào 1 hóa đơn tổng hay thanh toán riêng lẻ phòng này?");
            }
            msg.append("</body></html>");
            
            JLabel lblMessage = new JLabel(msg.toString());
            centerPanel.add(lblMessage, BorderLayout.CENTER);
            root.add(centerPanel, BorderLayout.CENTER);

            // Buttons
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
            buttons.setOpaque(false);
            buttons.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

            RoundedButton btnCancel = grayButton("Hủy bỏ");
            RoundedButton btnSolo = unpaidCount > 0 ? null : RoundedButton.outline("Thanh toán riêng", UIConstants.PRIMARY);
            RoundedButton btnGroup = primaryButton("Gom Bill toàn đoàn");

            btnCancel.addActionListener(e -> { result[0] = -1; dlg.dispose(); });
            btnGroup.addActionListener(e -> { result[0] = 0; dlg.dispose(); });
            if (btnSolo != null) {
                btnSolo.addActionListener(e -> { result[0] = 1; dlg.dispose(); });
                buttons.add(btnSolo);
            }
            
            buttons.add(btnCancel);
            buttons.add(btnGroup);
            root.add(buttons, BorderLayout.SOUTH);

            dlg.setContentPane(root);
            dlg.setVisible(true);

            if (result[0] == 0) {
                showMasterBillDialog(tp.getDatPhong(), allGroupRooms);
                return;
            } else if (result[0] == -1) {
                return;
            }
            // If result[0] == 1 -> Continue below to normal HoaDonDialog
        }

        HoaDonDialog dlg = new HoaDonDialog(
                (java.awt.Frame) SwingUtilities.getWindowAncestor(this), tp);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            rebuildFilter();
            refreshGrid();
            mainFrame.refreshThongKe(); // Also refresh statistics after checkout
        }
    }

    /**
     * Mở dialog Gom Bill Đoàn (Master Bill).
     */
    private void showMasterBillDialog(DatPhong dp, java.util.List<ChiTietDatPhong> rooms) {
        // Load full DatPhong data
        DatPhong fullDp = new service.DatPhongService().getById(dp.getMaDatPhong());
        if (fullDp == null)
            fullDp = dp;

        MasterBillDialog dlg = new MasterBillDialog(
                (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                fullDp, rooms);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            rebuildFilter();
            refreshGrid();
            mainFrame.refreshThongKe();
        }
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : s;
    }

    // ---- Context Menu Handler ----
    private void showContextMenu(Phong p, MouseEvent e) {
        ModernPopupMenu menu = new ModernPopupMenu();

        // Menu items with modern styling
        ModernMenuItem itemDetails = new ModernMenuItem("Xem chi tiết phòng");
        itemDetails.addActionListener(e2 -> showRoomDetailDialog(p));

        ModernMenuItem itemSwitch = new ModernMenuItem("Đổi phòng cho khách");
        itemSwitch.setEnabled(p.getTrangThai() == TrangThaiPhong.OCCUPIED);
        itemSwitch.addActionListener(e2 -> showSwitchRoomDialog(p));

        ModernMenuItem itemService = new ModernMenuItem("Thêm dịch vụ sử dụng");
        itemService.setEnabled(p.getTrangThai() == TrangThaiPhong.OCCUPIED);
        itemService.addActionListener(e2 -> showAddServiceDialog(p));
 
        ModernMenuItem itemRep = new ModernMenuItem("Thay đổi người đại diện");
        itemRep.setEnabled(p.getTrangThai() == TrangThaiPhong.OCCUPIED);
        itemRep.addActionListener(e2 -> showChangeRepresentativeDialog(p));

        ModernMenuItem itemPayment = new ModernMenuItem("Thanh toán & Trả phòng");
        itemPayment.setEnabled(p.getTrangThai() == TrangThaiPhong.OCCUPIED);
        itemPayment.addActionListener(e2 -> showCheckoutDialog(p));

        ModernMenuItem itemExtend = new ModernMenuItem("Gia hạn lưu trú");
        itemExtend.setEnabled(p.getTrangThai() == TrangThaiPhong.OCCUPIED);
        itemExtend.addActionListener(e2 -> showExtendStayDialog(p));

        ModernMenuItem itemMaint = new ModernMenuItem("Đánh dấu bảo trì");
        itemMaint.setEnabled(p.getTrangThai() == TrangThaiPhong.AVAILABLE || p.getTrangThai() == TrangThaiPhong.CLEANING);
        itemMaint.addActionListener(e2 -> showMaintenanceDialog(p, true));

        ModernMenuItem itemFinishMaint = new ModernMenuItem("Hoàn tất bảo trì");
        itemFinishMaint.setEnabled(p.getTrangThai() == TrangThaiPhong.MAINTENANCE);
        itemFinishMaint.addActionListener(e2 -> showMaintenanceDialog(p, false));

        menu.add(itemDetails);
        
        if (p.getTrangThai() == TrangThaiPhong.AVAILABLE || p.getTrangThai() == TrangThaiPhong.CLEANING) {
            menu.addSeparator();
            menu.add(itemMaint);
        }
        
        if (p.getTrangThai() == TrangThaiPhong.MAINTENANCE) {
            menu.addSeparator();
            menu.add(itemFinishMaint);
        }
        
        if (p.getTrangThai() == TrangThaiPhong.OCCUPIED) {
            menu.addSeparator();
            menu.add(itemSwitch);
            menu.add(itemRep);
            menu.add(itemService);
            menu.add(itemExtend);
            menu.addSeparator();
            menu.add(itemPayment);
            
            // Gom Bill Đoàn — hiển thị nếu phòng thuộc đoàn (nhiều hơn 1 phòng)
            ChiTietDatPhong tp = thuePhongService.getActiveByPhong(p.getSoPhong());
            if (tp != null && tp.getDatPhong() != null) {
                java.util.List<ChiTietDatPhong> allRooms = thuePhongService.getChiTietByDatPhong(tp.getDatPhong().getMaDatPhong());
                if (allRooms.size() > 1 || "DOAN".equalsIgnoreCase(tp.getDatPhong().getLoaiKhach())) {
                    ModernMenuItem itemMasterBill = new ModernMenuItem("Gom Bill Đoàn (Master Bill)");
                    itemMasterBill.addActionListener(e2 -> {
                        showMasterBillDialog(tp.getDatPhong(), allRooms);
                    });
                    menu.add(itemMasterBill);
                }
            }
        }

        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void showMaintenanceDialog(Phong p, boolean start) {
        String title = start ? "Đánh dấu bảo trì" : "Hoàn tất bảo trì";
        String emoji = start ? "🛠️" : "✅";
        String msg = start 
            ? "Bạn muốn chuyển phòng <b>" + p.getSoPhong() + "</b> sang trạng thái bảo trì?"
            : "Xác nhận phòng <b>" + p.getSoPhong() + "</b> đã bảo trì xong và sẵn sàng đón khách?";
        
        JDialog dlg = new JDialog(mainFrame, title, true);
        dlg.setSize(480, 230);
        dlg.setLocationRelativeTo(mainFrame);
        dlg.getContentPane().setBackground(Color.WHITE);

        JPanel main = new JPanel(new BorderLayout(15, 15));
        main.setBackground(Color.WHITE);
        main.setBorder(BorderFactory.createEmptyBorder(25, 25, 20, 25));

        JPanel centerPanel = new JPanel(new BorderLayout(15, 0));
        centerPanel.setOpaque(false);

        JLabel lblIcon = new JLabel(emoji);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 42));
        centerPanel.add(lblIcon, BorderLayout.WEST);

        JLabel lblMessage = new JLabel("<html><body style='width: 320px; color: #334155; font-family: Segoe UI;'>"
            + "<h3 style='margin: 0 0 8px 0; color: #0F172A;'>" + title + "?</h3>"
            + msg + "</body></html>");
        centerPanel.add(lblMessage, BorderLayout.CENTER);
        main.add(centerPanel, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setBackground(Color.WHITE);
        RoundedButton btnCancel = new RoundedButton("Huỷ", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        RoundedButton btnConfirm = new RoundedButton(start ? "Xác nhận bảo trì" : "Hoàn tất & Mở lại", 
            start ? UIConstants.DANGER : UIConstants.SUCCESS, Color.WHITE);

        btnCancel.addActionListener(e -> dlg.dispose());
        btnConfirm.addActionListener(e -> {
            phongService.updateTrangThai(p.getSoPhong(), start ? TrangThaiPhong.MAINTENANCE : TrangThaiPhong.AVAILABLE);
            NotificationManager.showSuccess("Cập nhật thành công", "Phòng " + p.getSoPhong() + (start ? " đã được đánh dấu bảo trì" : " đã sẵn sàng đón khách"));
            refreshGrid();
            mainFrame.refreshDashboard();
            dlg.dispose();
        });

        buttons.add(btnCancel);
        buttons.add(btnConfirm);
        main.add(buttons, BorderLayout.SOUTH);

        dlg.setContentPane(main);
        dlg.setVisible(true);
    }

    // ---- Action: Xem chi tiết ----
    private void showRoomDetailDialog(Phong pOld) {
        // Luôn fetch bản mới nhất từ database để tránh hiện thông tin khách cũ (Ghost Guest)
        Phong p = phongService.getPhongById(pOld.getSoPhong());
        if (p == null) p = pOld;

        JDialog dlg = new JDialog(mainFrame, "Thông tin phòng " + p.getSoPhong(), true);
        dlg.setSize(400, 480);
        dlg.setLocationRelativeTo(mainFrame);
        dlg.getContentPane().setBackground(Color.WHITE);
        dlg.setLayout(new BorderLayout());

        JPanel main = new JPanel(new BorderLayout(15, 15));
        main.setBackground(Color.WHITE);
        main.setBorder(BorderFactory.createEmptyBorder(25, 25, 20, 25));

        // Info Panel using BoxLayout for tight control
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        // title
        JLabel lblTitle = new JLabel("Chi tiết phòng " + p.getSoPhong());
        lblTitle.setFont(UIConstants.FONT_HEADER);
        lblTitle.setForeground(UIConstants.TEXT_PRIMARY);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(lblTitle);
        infoPanel.add(Box.createVerticalStrut(10));

        JSeparator sep1 = new JSeparator();
        sep1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        infoPanel.add(sep1);
        infoPanel.add(Box.createVerticalStrut(15));

        // helper to add rows
        java.util.function.BiConsumer<String, String> addRow = (label, value) -> {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
            JLabel lblL = new JLabel(label);
            lblL.setFont(UIConstants.FONT_BODY);
            lblL.setForeground(UIConstants.TEXT_SECONDARY);
            JLabel lblV = new JLabel(value);
            lblV.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblV.setForeground(UIConstants.TEXT_PRIMARY);
            row.add(lblL, BorderLayout.WEST);
            row.add(lblV, BorderLayout.EAST);
            infoPanel.add(row);
            infoPanel.add(Box.createVerticalStrut(8));
        };

        addRow.accept("Tầng:", "Tầng " + p.getTang());
        addRow.accept("Loại phòng:", p.getTenLoaiPhong());
        addRow.accept("Hướng view:", p.getView());
        addRow.accept("Sức chứa:", p.getSucChua() + " người");
        double giaDetail = bangGiaService
                .layGiaHienHanh(p.getLoaiPhong() != null ? p.getLoaiPhong().getMaLoaiPhong() : "");
        addRow.accept("Giá/đêm:", String.format("%,.0fđ", giaDetail));

        JPanel ttRow = new JPanel(new BorderLayout());
        ttRow.setOpaque(false);
        ttRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        JLabel lblTTTitle = new JLabel("Trạng thái:");
        lblTTTitle.setFont(UIConstants.FONT_BODY);
        lblTTTitle.setForeground(UIConstants.TEXT_SECONDARY);
        JLabel lblTTVal = new JLabel(UIConstants.getTrangThaiPhongLabel(p.getTrangThai()));
        lblTTVal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTTVal.setForeground(UIConstants.getTrangThaiPhongColor(p.getTrangThai()));
        ttRow.add(lblTTTitle, BorderLayout.WEST);
        ttRow.add(lblTTVal, BorderLayout.EAST);
        infoPanel.add(ttRow);
        infoPanel.add(Box.createVerticalStrut(20));

        if (p.getTenKhachHienTai() != null) {
            JLabel lblKhach = new JLabel("Khách đang thuê");
            lblKhach.setFont(UIConstants.FONT_SMALL_BOLD);
            lblKhach.setForeground(UIConstants.PRIMARY);
            lblKhach.setAlignmentX(Component.LEFT_ALIGNMENT);
            infoPanel.add(lblKhach);
            infoPanel.add(Box.createVerticalStrut(5));

            JSeparator sep2 = new JSeparator();
            sep2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            infoPanel.add(sep2);
            infoPanel.add(Box.createVerticalStrut(10));

            addRow.accept("Họ và tên:", p.getTenKhachHienTai());

            ChiTietDatPhong tp = thuePhongService.getActiveByPhong(p.getSoPhong());
            if (tp != null && tp.getNgayNhanThucTe() != null) {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                        .ofPattern("dd/MM/yyyy HH:mm");
                addRow.accept("Ngày nhận:", tp.getNgayNhanThucTe().format(formatter));
                if (tp.getNgayTraThucTe() != null) {
                    addRow.accept("Ngày trả:", tp.getNgayTraThucTe().format(formatter));
                }
            }
        }

        main.add(infoPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        RoundedButton btnClose = new RoundedButton("Đóng", UIConstants.PRIMARY, Color.WHITE);
        btnClose.addActionListener(e -> dlg.dispose());
        btnPanel.add(btnClose);
        main.add(btnPanel, BorderLayout.SOUTH);

        dlg.add(main);
        dlg.setVisible(true);
    }

    // ---- Action: Thay đổi người đại diện ----
    private void showChangeRepresentativeDialog(Phong p) {
        ChiTietDatPhong tp = thuePhongService.getActiveByPhong(p.getSoPhong());
        if (tp == null || tp.getDatPhong() == null) {
            JOptionPane.showMessageDialog(mainFrame, "Không tìm thấy thông tin thuê phòng!", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        ui.dialogs.ChangeRepresentativeDialog dlg = new ui.dialogs.ChangeRepresentativeDialog(mainFrame, tp.getDatPhong(), p.getSoPhong());
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            rebuildFilter();
            refreshGrid();
        }
    }

    // ---- Action: Đổi phòng (Room Move) ----
    private void showSwitchRoomDialog(Phong currentPhong) {
        ChiTietDatPhong tp = thuePhongService.getActiveByPhong(currentPhong.getSoPhong());
        if (tp == null) {
            JOptionPane.showMessageDialog(mainFrame, "Không tìm thấy thông tin thuê phòng!", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        ui.dialogs.RoomTransferDialog dlg = new ui.dialogs.RoomTransferDialog(mainFrame, tp);
        dlg.setVisible(true);

        if (dlg.isConfirmed()) {
            Phong newRoom = dlg.getSelectedRoom();
            boolean keepOldPrice = !dlg.isUseNewPrice();
            
            String error = thuePhongService.transferRoom(tp.getMaChiTiet(), newRoom.getMaPhong(), keepOldPrice);
            
            if (error == null) {
                NotificationManager.showSuccess("Đổi phòng thành công", 
                        "Từ P." + currentPhong.getSoPhong() + " sang P." + newRoom.getMaPhong());
                rebuildFilter();
                refreshGrid();
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Lỗi khi đổi phòng: " + error, "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ---- Action: Thêm dịch vụ (Phối hợp nhiều dịch vụ cùng lúc) ----
    private void showAddServiceDialog(Phong p) {
        ChiTietDatPhong tp = thuePhongService.getActiveByPhong(p.getSoPhong());
        if (tp == null) {
            JOptionPane.showMessageDialog(mainFrame, "Không tìm thấy thông tin thuê phòng!", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        ui.dialogs.ServiceOrderDialog dlg = new ui.dialogs.ServiceOrderDialog(mainFrame, tp);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            rebuildFilter();
            refreshGrid();
        }
    }

    // ---- Action: Gia hạn lưu trú (Stay Extension) ----
    private void showExtendStayDialog(Phong p) {
        ChiTietDatPhong tp = thuePhongService.getActiveByPhong(p.getSoPhong());
        if (tp == null || tp.getDatPhong() == null) {
            JOptionPane.showMessageDialog(mainFrame, "Không tìm thấy thông tin thuê phòng!", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        ui.dialogs.ExtendStayDialog dlg = new ui.dialogs.ExtendStayDialog(mainFrame, tp);
        dlg.setVisible(true);

        if (dlg.isConfirmed()) {
            LocalDateTime newTime = dlg.getNewCheckoutTime();
            String err = thuePhongService.extendStay(tp.getMaChiTiet(), newTime);
            if (err == null) {
                NotificationManager.showSuccess("Gia hạn thành công", "Đã cập nhật ngày trả cho phòng " + p.getSoPhong());
                rebuildFilter();
                refreshGrid();
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Lỗi: " + err, "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * JPanel implement Scrollable để JScrollPane có thể constraint width của panel
     * theo chiều rộng viewport — từ đó WrapLayout mới tính đúng điểm xuống hàng.
     */
    private static class ScrollablePanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 20;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 100;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() { return true; }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        } // scroll dọc bình thường
    }
}