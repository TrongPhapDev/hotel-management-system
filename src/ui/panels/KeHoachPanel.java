package ui.panels;

import dao.ChiTietDatPhongDAO;
import dao.HuongNhinDAO;
import dao.LoaiPhongDAO;
import dao.PhongDAO;
import entity.ChiTietDatPhong;
import entity.HuongNhin;
import entity.LoaiPhong;
import entity.Phong;
import entity.enums.TrangThaiDatPhong;
import entity.enums.TrangThaiPhong;
import ui.MainFrame;
import ui.components.DatePicker;
import ui.components.UIConstants;
import ui.components.RoundedComponents;
import ui.components.RoundedComponents.RoundedButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Panel Kế Hoạch Sử Dụng Phòng - Gantt Chart/Timeline của hệ thống PMS.
 */
public class KeHoachPanel extends JPanel implements ResettableFilter {

    private final MainFrame parent;
    
    // Data lists
    private List<Phong> allRooms = new ArrayList<>();
    private List<ChiTietDatPhong> allStays = new ArrayList<>();
    private List<Phong> filteredRooms = new ArrayList<>();
    
    // State of timeline
    private LocalDate startDate;
    private int numDays = 7;
    
    // Components
    private JTextField txtSearch;
    private JComboBox<String> cbDays;
    private JComboBox<String> cbLoaiPhong;
    private JComboBox<String> cbHuongNhin;
    private JComboBox<String> cbTang;
    private DatePicker dpStart;
    
    private JPanel filterContainer;
    private CardLayout filterCardLayout;
    private JPanel filterPanel;
    
    private TimelineGrid timelineGrid;
    private TimelineHeader timelineHeader;
    private RoomHeaderColumn roomHeader;
    private JScrollPane scrollPane;

    public KeHoachPanel(MainFrame parent) {
        this.parent = parent;
        this.startDate = LocalDate.now();
        
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG_MAIN);
        
        // 1. Build Top Bar (Date navigator & settings)
        add(buildTopBar(), BorderLayout.NORTH);
        
        // 2. Main content container (split into left filter sidebar & right timeline)
        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setOpaque(false);
        
        // Build Left Filter Sidebar with CardLayout for smooth collapse/expand
        filterCardLayout = new CardLayout();
        filterContainer = new JPanel(filterCardLayout) {
            @Override
            public Dimension getPreferredSize() {
                for (Component c : getComponents()) {
                    if (c.isVisible()) {
                        return c.getPreferredSize();
                    }
                }
                return super.getPreferredSize();
            }
        };
        filterContainer.setOpaque(false);
        
        filterPanel = buildFilterPanel();
        JPanel collapsedPanel = buildCollapsedPanel();
        
        filterContainer.add(filterPanel, "full");
        filterContainer.add(collapsedPanel, "collapsed");
        
        mainContent.add(filterContainer, BorderLayout.WEST);
        
        // Build Right Timeline Area
        mainContent.add(buildTimelineArea(), BorderLayout.CENTER);
        
        add(mainContent, BorderLayout.CENTER);
        
        // Load initial data
        refresh();
    }
    
    // ===== DATA LOADING & FILTERING =====
    
    public void refresh() {
        // Fetch fresh data from DB
        try {
            allRooms = new PhongDAO().getAll();
            allStays = new ChiTietDatPhongDAO().getAll();
        } catch (Exception e) {
            e.printStackTrace();
            allRooms = new ArrayList<>();
            allStays = new ArrayList<>();
        }
        applyFilters();
    }
    
    @Override
    public void resetFilters() {
        txtSearch.setText("");
        cbDays.setSelectedIndex(0); // 7 days
        cbLoaiPhong.setSelectedIndex(0); // Tất cả
        cbHuongNhin.setSelectedIndex(0); // Tất cả
        cbTang.setSelectedIndex(0); // Tất cả
        startDate = LocalDate.now();
        dpStart.setDate(new Date());
        refresh();
    }
    
    private void applyFilters() {
        String kw = txtSearch.getText().trim().toLowerCase();
        String loai = cbLoaiPhong.getSelectedItem() != null ? cbLoaiPhong.getSelectedItem().toString() : "Tất cả";
        String huong = cbHuongNhin.getSelectedItem() != null ? cbHuongNhin.getSelectedItem().toString() : "Tất cả";
        String tangStr = cbTang.getSelectedItem() != null ? cbTang.getSelectedItem().toString() : "Tất cả";
        
        filteredRooms = allRooms.stream()
            .filter(p -> {
                if (!kw.isEmpty() && !p.getMaPhong().toLowerCase().contains(kw)) return false;
                if (!loai.equals("Tất cả") && p.getLoaiPhong() != null && !p.getLoaiPhong().getTenLoaiPhong().equals(loai)) return false;
                if (!huong.equals("Tất cả") && p.getHuongNhin() != null && !p.getHuongNhin().getTenHuongNhin().equals(huong)) return false;
                if (!tangStr.equals("Tất cả") && !String.valueOf(p.getTang()).equals(tangStr)) return false;
                return true;
            })
            .collect(Collectors.toList());
            
        // Get num days
        String daysStr = cbDays.getSelectedItem() != null ? cbDays.getSelectedItem().toString() : "7 ngày";
        try {
            numDays = Integer.parseInt(daysStr.split(" ")[0]);
        } catch (Exception e) {
            numDays = 7;
        }
        
        // Update components
        if (timelineGrid != null) {
            timelineGrid.updateData(filteredRooms, allStays, startDate, numDays);
        }
        if (timelineHeader != null) {
            timelineHeader.updateData(startDate, numDays);
        }
        if (roomHeader != null) {
            roomHeader.updateData(filteredRooms);
        }
        
        if (scrollPane != null) {
            scrollPane.revalidate();
            scrollPane.repaint();
        }
    }
    
    // ===== UI BUILDING =====
    
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));
        
        // Left side: Title & Subtitle
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        
        JLabel lblTitle = new JLabel("KẾ HOẠCH SỬ DỤNG PHÒNG");
        lblTitle.setFont(UIConstants.FONT_TITLE);
        lblTitle.setForeground(UIConstants.TEXT_PRIMARY);
        
        JLabel lblSubtitle = new JLabel("Kế hoạch đặt phòng, nhận/trả phòng trực quan của khách sạn");
        lblSubtitle.setFont(UIConstants.FONT_SUBTITLE);
        lblSubtitle.setForeground(UIConstants.TEXT_SECONDARY);
        
        titlePanel.add(lblTitle);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(lblSubtitle);
        bar.add(titlePanel, BorderLayout.WEST);
        
        // Right side: Navigation & Actions
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        navPanel.setOpaque(false);
        

        
        // Prev button
        JButton btnPrev = buildNavButton("‹", e -> {
            startDate = startDate.minusDays(numDays);
            dpStart.setDate(java.sql.Date.valueOf(startDate));
            applyFilters();
        });
        navPanel.add(btnPrev);
        
        // Date picker
        dpStart = new DatePicker(new Date(), d -> {
            if (d != null) {
                startDate = d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                applyFilters();
            }
        });
        dpStart.setPreferredSize(new Dimension(140, 36));
        navPanel.add(dpStart);
        
        // Next button
        JButton btnNext = buildNavButton("›", e -> {
            startDate = startDate.plusDays(numDays);
            dpStart.setDate(java.sql.Date.valueOf(startDate));
            applyFilters();
        });
        navPanel.add(btnNext);
        
        // Today button
        JButton btnToday = RoundedComponents.primaryButton("Hôm nay");
        btnToday.addActionListener(e -> {
            startDate = LocalDate.now();
            dpStart.setDate(new Date());
            applyFilters();
        });
        navPanel.add(btnToday);
        
        bar.add(navPanel, BorderLayout.EAST);
        return bar;
    }
    
    private JButton buildNavButton(String text, ActionListener l) {
        JButton btn = RoundedComponents.grayButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        btn.addActionListener(l);
        return btn;
    }
    
    private JPanel buildCollapsedPanel() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setPreferredSize(new Dimension(20, 0));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIConstants.BORDER));
        bar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        bar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                filterCardLayout.show(filterContainer, "full");
                revalidate();
                repaint();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                bar.setBackground(new Color(0xF1F5F9));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                bar.setBackground(Color.WHITE);
            }
        });
        
        JLabel label = new JLabel("›");
        label.setFont(new Font("Segoe UI", Font.BOLD, 18));
        label.setForeground(UIConstants.PRIMARY);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        bar.add(label, BorderLayout.CENTER);
        
        return bar;
    }
    
    private JPanel buildFilterPanel() {
        JPanel side = new JPanel();
        side.setPreferredSize(new Dimension(240, 0));
        side.setBackground(Color.WHITE);
        side.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIConstants.BORDER));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(20, 18, 20, 18)
        ));
        
        // Section Title: Bộ lọc + Collapse Button
        JPanel titleBox = new JPanel(new BorderLayout());
        titleBox.setOpaque(false);
        titleBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        titleBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel lblTitle = new JLabel("BỘ LỌC TÌM KIẾM");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(UIConstants.TEXT_PRIMARY);
        titleBox.add(lblTitle, BorderLayout.WEST);
        
        JButton btnCollapse = new JButton("‹") {
            @Override
            public Dimension getPreferredSize() { return new Dimension(22, 22); }
        };
        btnCollapse.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCollapse.setForeground(UIConstants.TEXT_SECONDARY);
        btnCollapse.setBackground(Color.WHITE);
        btnCollapse.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER, 1));
        btnCollapse.setFocusPainted(false);
        btnCollapse.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCollapse.setToolTipText("Thu gọn bộ lọc");
        btnCollapse.addActionListener(e -> {
            filterCardLayout.show(filterContainer, "collapsed");
            revalidate();
            repaint();
        });
        
        btnCollapse.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnCollapse.setBackground(new Color(0xF1F5F9));
                btnCollapse.setForeground(UIConstants.PRIMARY);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btnCollapse.setBackground(Color.WHITE);
                btnCollapse.setForeground(UIConstants.TEXT_SECONDARY);
            }
        });
        
        titleBox.add(btnCollapse, BorderLayout.EAST);
        side.add(titleBox);
        side.add(Box.createVerticalStrut(15));
        
        // Search Input
        JLabel lblSearch = new JLabel("Số phòng");
        lblSearch.setFont(UIConstants.FONT_SMALL_BOLD);
        lblSearch.setForeground(UIConstants.TEXT_SECONDARY);
        lblSearch.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(lblSearch);
        side.add(Box.createVerticalStrut(4));
        
        txtSearch = new JTextField();
        txtSearch.setFont(UIConstants.FONT_BODY);
        txtSearch.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        txtSearch.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        });
        side.add(txtSearch);
        side.add(Box.createVerticalStrut(12));
        
        // Display Days
        JLabel lblDays = new JLabel("Số ngày hiển thị");
        lblDays.setFont(UIConstants.FONT_SMALL_BOLD);
        lblDays.setForeground(UIConstants.TEXT_SECONDARY);
        lblDays.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(lblDays);
        side.add(Box.createVerticalStrut(4));
        
        cbDays = new JComboBox<>(new String[]{"7 ngày", "10 ngày", "14 ngày", "21 ngày", "30 ngày"});
        cbDays.setFont(UIConstants.FONT_BODY);
        cbDays.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        cbDays.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbDays.addActionListener(e -> applyFilters());
        side.add(cbDays);
        side.add(Box.createVerticalStrut(12));
        
        // Room Type (Hạng phòng)
        JLabel lblLoai = new JLabel("Hạng phòng");
        lblLoai.setFont(UIConstants.FONT_SMALL_BOLD);
        lblLoai.setForeground(UIConstants.TEXT_SECONDARY);
        lblLoai.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(lblLoai);
        side.add(Box.createVerticalStrut(4));
        
        cbLoaiPhong = new JComboBox<>();
        cbLoaiPhong.setFont(UIConstants.FONT_BODY);
        cbLoaiPhong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        cbLoaiPhong.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbLoaiPhong.addItem("Tất cả");
        try {
            new LoaiPhongDAO().getAll().forEach(lp -> cbLoaiPhong.addItem(lp.getTenLoaiPhong()));
        } catch (Exception ignored) {}
        cbLoaiPhong.addActionListener(e -> applyFilters());
        side.add(cbLoaiPhong);
        side.add(Box.createVerticalStrut(12));
        
        // Room View (Hướng phòng)
        JLabel lblHuong = new JLabel("Hướng phòng");
        lblHuong.setFont(UIConstants.FONT_SMALL_BOLD);
        lblHuong.setForeground(UIConstants.TEXT_SECONDARY);
        lblHuong.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(lblHuong);
        side.add(Box.createVerticalStrut(4));
        
        cbHuongNhin = new JComboBox<>();
        cbHuongNhin.setFont(UIConstants.FONT_BODY);
        cbHuongNhin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        cbHuongNhin.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbHuongNhin.addItem("Tất cả");
        try {
            new HuongNhinDAO().getAll().forEach(hn -> cbHuongNhin.addItem(hn.getTenHuongNhin()));
        } catch (Exception ignored) {}
        cbHuongNhin.addActionListener(e -> applyFilters());
        side.add(cbHuongNhin);
        side.add(Box.createVerticalStrut(12));
        
        // Floor (Tầng)
        JLabel lblTang = new JLabel("Tầng");
        lblTang.setFont(UIConstants.FONT_SMALL_BOLD);
        lblTang.setForeground(UIConstants.TEXT_SECONDARY);
        lblTang.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(lblTang);
        side.add(Box.createVerticalStrut(4));
        
        cbTang = new JComboBox<>();
        cbTang.setFont(UIConstants.FONT_BODY);
        cbTang.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        cbTang.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbTang.addItem("Tất cả");
        // Populating floor numbers from 1 to 10 dynamically
        for (int i = 1; i <= 10; i++) cbTang.addItem(String.valueOf(i));
        cbTang.addActionListener(e -> applyFilters());
        side.add(cbTang);
        side.add(Box.createVerticalStrut(24));
        
        // Legend Section
        JLabel lblLegend = new JLabel("CHÚ THÍCH MÀU SẮC");
        lblLegend.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblLegend.setForeground(UIConstants.TEXT_PRIMARY);
        lblLegend.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(lblLegend);
        side.add(Box.createVerticalStrut(12));
        
        side.add(buildLegendItem("Đang ở (Checked-in)", new Color(0x4361EE)));
        side.add(buildLegendItem("Đã đặt trước (Confirmed)", new Color(0x10B981)));
        side.add(buildLegendItem("Đã trả phòng (Checked-out)", new Color(0x94A3B8)));
        side.add(buildLegendItem("Chờ xếp phòng (Waitlist)", new Color(0xF59E0B)));
        side.add(buildLegendItem("Quá hạn / No-show", new Color(0x991B1B)));
        
        return side;
    }
    
    private JPanel buildLegendItem(String labelText, Color color) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        item.setOpaque(false);
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        
        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(12, 12));
        dot.setOpaque(false);
        
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UIConstants.FONT_SMALL);
        lbl.setForeground(new Color(0x475569));
        
        item.add(dot);
        item.add(lbl);
        return item;
    }
    
    private JPanel buildTimelineArea() {
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);
        container.setBorder(new EmptyBorder(12, 12, 12, 12));
        
        // Create custom Gantt chart components
        timelineGrid = new TimelineGrid();
        timelineHeader = new TimelineHeader();
        roomHeader = new RoomHeaderColumn();
        
        // Wrap timeline grid inside JScrollPane
        scrollPane = new JScrollPane(timelineGrid);
        scrollPane.setBorder(new LineBorder(UIConstants.BORDER, 1));
        scrollPane.setBackground(Color.WHITE);
        scrollPane.getViewport().setBackground(Color.WHITE);
        
        // Set fixed components on row & column headers
        scrollPane.setColumnHeaderView(timelineHeader);
        scrollPane.setRowHeaderView(roomHeader);
        
        // Clean layout of scroll pane
        scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(UIConstants.BG_TABLE_HEADER);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(UIConstants.BORDER);
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
            }
        });
        
        container.add(scrollPane, BorderLayout.CENTER);
        return container;
    }
    
    // ===== TIMELINE COMPONENTS =====
    
    // --- Room Names frozen column on left ---
    private class RoomHeaderColumn extends JComponent {
        private List<Phong> roomsList = new ArrayList<>();
        private final int roomColWidth = 150;
        private final int rowHeight = 50;
        
        public RoomHeaderColumn() {
            setPreferredSize(new Dimension(roomColWidth, 0));
        }
        
        public void updateData(List<Phong> rooms) {
            this.roomsList = rooms;
            setPreferredSize(new Dimension(roomColWidth, rooms.size() * rowHeight));
            revalidate();
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int w = getWidth();
            int h = getHeight();
            
            for (int i = 0; i < roomsList.size(); i++) {
                Phong p = roomsList.get(i);
                int y = i * rowHeight;
                
                // Zebra stripe
                if (i % 2 == 1) {
                    g2.setColor(new Color(0xF8FAFC));
                } else {
                    g2.setColor(Color.WHITE);
                }
                g2.fillRect(0, y, w, rowHeight);
                
                // Horizontal divider
                g2.setColor(UIConstants.BORDER);
                g2.drawLine(0, y + rowHeight - 1, w, y + rowHeight - 1);
                
                // Room Number
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                g2.setColor(UIConstants.TEXT_PRIMARY);
                g2.drawString("P." + p.getMaPhong(), 14, y + 22);
                
                // Room Type
                String type = p.getLoaiPhong() != null ? p.getLoaiPhong().getTenLoaiPhong() : "Standard";
                if (type.length() > 16) type = type.substring(0, 14) + "...";
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                g2.setColor(UIConstants.TEXT_SECONDARY);
                g2.drawString(type, 14, y + 38);
                
                // Room status indicator circle
                TrangThaiPhong tt = p.getTrangThai();
                Color ttClr = UIConstants.getTrangThaiPhongColor(tt);
                g2.setColor(ttClr);
                g2.fillOval(w - 24, y + 18, 10, 10);
            }
            
            // Vertical separation border on the right
            g2.setColor(UIConstants.BORDER);
            g2.drawLine(w - 1, 0, w - 1, h);
            g2.dispose();
        }
    }
    
    // --- Timeline Date headers at top ---
    private class TimelineHeader extends JComponent {
        private LocalDate start;
        private int days;
        private final int dayWidth = 120;
        private final int headerHeight = 45;
        
        public TimelineHeader() {
            setPreferredSize(new Dimension(0, headerHeight));
        }
        
        public void updateData(LocalDate start, int days) {
            this.start = start;
            this.days = days;
            setPreferredSize(new Dimension(days * dayWidth, headerHeight));
            revalidate();
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (start == null) return;
            
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int w = getWidth();
            int h = getHeight();
            
            // Background
            g2.setColor(UIConstants.BG_TABLE_HEADER);
            g2.fillRect(0, 0, w, h);
            
            // Divider bottom
            g2.setColor(UIConstants.BORDER);
            g2.drawLine(0, h - 1, w, h - 1);
            
            LocalDate today = LocalDate.now();
            
            for (int i = 0; i < days; i++) {
                LocalDate d = start.plusDays(i);
                int x = i * dayWidth;
                
                // Vertical division line
                if (i > 0) {
                    g2.setColor(UIConstants.BORDER);
                    g2.drawLine(x, 0, x, h);
                }
                
                boolean isToday = d.equals(today);
                
                // Highlight today column
                if (isToday) {
                    g2.setColor(UIConstants.PRIMARY_LIGHT);
                    g2.fillRoundRect(x + 10, 4, dayWidth - 20, h - 8, 8, 8);
                    g2.setColor(UIConstants.PRIMARY);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(x + 10, 4, dayWidth - 20, h - 8, 8, 8);
                }
                
                // Day label e.g., T2, T3... CN
                String dow = "";
                switch (d.getDayOfWeek()) {
                    case MONDAY:    dow = "T2"; break;
                    case TUESDAY:   dow = "T3"; break;
                    case WEDNESDAY: dow = "T4"; break;
                    case THURSDAY:  dow = "T5"; break;
                    case FRIDAY:    dow = "T6"; break;
                    case SATURDAY:  dow = "T7"; break;
                    case SUNDAY:    dow = "CN"; break;
                }
                
                String dateStr = d.format(DateTimeFormatter.ofPattern("dd/MM"));
                
                g2.setFont(new Font("Segoe UI", isToday ? Font.BOLD : Font.PLAIN, 12));
                g2.setColor(isToday ? UIConstants.PRIMARY : UIConstants.TEXT_PRIMARY);
                FontMetrics fm = g2.getFontMetrics();
                
                String disp = dow + "  " + dateStr;
                int textX = x + (dayWidth - fm.stringWidth(disp)) / 2;
                int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(disp, textX, textY);
            }
            g2.dispose();
        }
    }
    
    // --- Main timeline grid drawing stay bars ---
    private class TimelineGrid extends JComponent {
        private List<Phong> roomsList = new ArrayList<>();
        private List<ChiTietDatPhong> staysList = new ArrayList<>();
        private LocalDate start;
        private int days;
        
        private final int dayWidth = 120;
        private final int rowHeight = 50;
        
        private ChiTietDatPhong hoveredStay = null;
        
        public TimelineGrid() {
            setToolTipText(""); // Trigger JToolTip lookup
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    handleGridClick(e.getPoint());
                }
            });
            
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    handleGridHover(e.getPoint());
                }
            });
        }
        
        public void updateData(List<Phong> rooms, List<ChiTietDatPhong> stays, LocalDate start, int days) {
            this.roomsList = rooms;
            this.staysList = stays;
            this.start = start;
            this.days = days;
            
            setPreferredSize(new Dimension(days * dayWidth, rooms.size() * rowHeight));
            revalidate();
            repaint();
        }
        
        private void handleGridHover(Point p) {
            ChiTietDatPhong stay = getStayAtPoint(p);
            if (stay != hoveredStay) {
                hoveredStay = stay;
                repaint();
            }
            
            if (stay != null) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                String name = stay.getDatPhong().getKhachHang() != null ? stay.getDatPhong().getKhachHang().getHoTen() : "Khách";
                String phone = stay.getDatPhong().getKhachHang() != null ? stay.getDatPhong().getKhachHang().getSdt() : "—";
                String stayPeriod = stay.getDatPhong().getNgayNhanDuKien().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        + " -> " + stay.getDatPhong().getNgayTraDuKien().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                setToolTipText("<html><body style='padding:6px; font-family:Segoe UI;'>"
                        + "<b>Mã đơn:</b> " + stay.getDatPhong().getMaDatPhong() + "<br>"
                        + "<b>Khách hàng:</b> " + name + " (" + phone + ")<br>"
                        + "<b>Lịch trình:</b> " + stayPeriod + "<br>"
                        + "<b>Trạng thái:</b> " + UIConstants.getTrangThaiDatPhongLabel(stay.getDatPhong().getTrangThai())
                        + "<br><font color='#1E40AF'><b><i>Nhấp đúp chuột để xem chi tiết</i></b></font>"
                        + "</body></html>");
            } else {
                setCursor(Cursor.getDefaultCursor());
                setToolTipText(null);
            }
        }
        
        private void handleGridClick(Point p) {
            ChiTietDatPhong stay = getStayAtPoint(p);
            if (stay != null) {
                showStayDetailsDialog(stay);
            }
        }
        
        private ChiTietDatPhong getStayAtPoint(Point p) {
            int rowIdx = p.y / rowHeight;
            if (rowIdx < 0 || rowIdx >= roomsList.size()) return null;
            
            Phong room = roomsList.get(rowIdx);
            int px = p.x;
            int py = p.y;
            
            LocalDateTime timelineStart = start.atStartOfDay();
            LocalDateTime timelineEnd = start.plusDays(days).atStartOfDay();
            
            List<ChiTietDatPhong> roomStays = staysList.stream()
                .filter(ct -> ct.getPhong() != null && ct.getPhong().getMaPhong().equals(room.getMaPhong()))
                .filter(ct -> ct.getDatPhong() != null && ct.getDatPhong().getTrangThai() != TrangThaiDatPhong.CANCELLED)
                .collect(Collectors.toList());
                
            for (ChiTietDatPhong ct : roomStays) {
                LocalDateTime s = ct.getNgayNhanThucTe() != null ? ct.getNgayNhanThucTe() : ct.getDatPhong().getNgayNhanDuKien();
                LocalDateTime e = ct.getNgayTraThucTe() != null ? ct.getNgayTraThucTe() : ct.getDatPhong().getNgayTraDuKien();
                
                if (s == null || e == null) continue;
                if (e.isBefore(timelineStart) || s.isAfter(timelineEnd)) continue;
                
                boolean continuesLeft = s.isBefore(timelineStart);
                boolean continuesRight = e.isAfter(timelineEnd);
                
                LocalDateTime renderStart = continuesLeft ? timelineStart : s;
                LocalDateTime renderEnd = continuesRight ? timelineEnd : e;
                
                double minsStart = Duration.between(timelineStart, renderStart).toMinutes();
                double minsEnd = Duration.between(timelineStart, renderEnd).toMinutes();
                
                int x1 = (int) ((minsStart / 1440.0) * dayWidth);
                int x2 = (int) ((minsEnd / 1440.0) * dayWidth);
                int barW = x2 - x1;
                if (barW < 5) barW = 5;
                
                int barH = 28;
                int barY = rowIdx * rowHeight + (rowHeight - barH) / 2;
                
                if (px >= x1 + 2 && px <= x1 + barW - 2 && py >= barY && py <= barY + barH) {
                    return ct;
                }
            }
            return null;
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int w = getWidth();
            int h = getHeight();
            
            // 1. Draw backgrounds (zebra striping)
            for (int i = 0; i < roomsList.size(); i++) {
                if (i % 2 == 1) {
                    g2.setColor(new Color(0xF8FAFC));
                } else {
                    g2.setColor(Color.WHITE);
                }
                g2.fillRect(0, i * rowHeight, w, rowHeight);
            }
            
            // 2. Draw vertical grid lines
            g2.setColor(new Color(0xE2E8F0));
            g2.setStroke(new BasicStroke(1.0f));
            for (int j = 0; j <= days; j++) {
                int x = j * dayWidth;
                g2.drawLine(x, 0, x, h);
            }
            
            // 3. Draw horizontal grid lines
            for (int i = 0; i <= roomsList.size(); i++) {
                int y = i * rowHeight;
                g2.drawLine(0, y, w, y);
            }
            
            // 4. Draw vertical "TODAY" dotted line
            LocalDate today = LocalDate.now();
            if (!start.isAfter(today) && !start.plusDays(days).isBefore(today)) {
                LocalDateTime timelineStart = start.atStartOfDay();
                LocalDateTime now = LocalDateTime.now();
                if (now.isAfter(timelineStart) && now.isBefore(start.plusDays(days).atStartOfDay())) {
                    double minutes = Duration.between(timelineStart, now).toMinutes();
                    double ratio = minutes / 1440.0;
                    int x = (int) (ratio * dayWidth);
                    
                    g2.setColor(new Color(0xEF4444));
                    float[] dash = {4f, 4f};
                    g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                    g2.drawLine(x, 0, x, h);
                    
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.fillOval(x - 4, 2, 8, 8);
                }
            }
            
            // 5. Draw reservation bars
            LocalDateTime timelineStart = start.atStartOfDay();
            LocalDateTime timelineEnd = start.plusDays(days).atStartOfDay();
            
            for (int i = 0; i < roomsList.size(); i++) {
                Phong p = roomsList.get(i);
                int y = i * rowHeight;
                
                List<ChiTietDatPhong> roomStays = staysList.stream()
                    .filter(ct -> ct.getPhong() != null && ct.getPhong().getMaPhong().equals(p.getMaPhong()))
                    .filter(ct -> ct.getDatPhong() != null && ct.getDatPhong().getTrangThai() != TrangThaiDatPhong.CANCELLED)
                    .collect(Collectors.toList());
                    
                for (ChiTietDatPhong ct : roomStays) {
                    LocalDateTime s = ct.getNgayNhanThucTe() != null ? ct.getNgayNhanThucTe() : ct.getDatPhong().getNgayNhanDuKien();
                    LocalDateTime e = ct.getNgayTraThucTe() != null ? ct.getNgayTraThucTe() : ct.getDatPhong().getNgayTraDuKien();
                    
                    if (s == null || e == null) continue;
                    if (e.isBefore(timelineStart) || s.isAfter(timelineEnd)) continue;
                    
                    boolean continuesLeft = s.isBefore(timelineStart);
                    boolean continuesRight = e.isAfter(timelineEnd);
                    
                    LocalDateTime renderStart = continuesLeft ? timelineStart : s;
                    LocalDateTime renderEnd = continuesRight ? timelineEnd : e;
                    
                    double minsStart = Duration.between(timelineStart, renderStart).toMinutes();
                    double minsEnd = Duration.between(timelineStart, renderEnd).toMinutes();
                    
                    int x1 = (int) ((minsStart / 1440.0) * dayWidth);
                    int x2 = (int) ((minsEnd / 1440.0) * dayWidth);
                    int barW = x2 - x1;
                    if (barW < 5) barW = 5;
                    
                    int barH = 28;
                    int barY = y + (rowHeight - barH) / 2;
                    
                    // Style attributes
                    Color bg1, bg2, borderClr;
                    TrangThaiDatPhong status = ct.getDatPhong().getTrangThai();
                    
                    boolean isOccupied = ct.getNgayNhanThucTe() != null && ct.getNgayTraThucTe() == null;
                    boolean isCheckedOut = ct.getNgayTraThucTe() != null;
                    
                    if (isCheckedOut) {
                        bg1 = new Color(0xCBD5E1);
                        bg2 = new Color(0x94A3B8);
                        borderClr = new Color(0x64748B);
                    } else if (isOccupied) {
                        bg1 = new Color(0x4361EE);
                        bg2 = new Color(0x3B82F6);
                        borderClr = new Color(0x1D4ED8);
                    } else if (status == TrangThaiDatPhong.CONFIRMED || status == TrangThaiDatPhong.PENDING) {
                        bg1 = new Color(0x10B981);
                        bg2 = new Color(0x05CD99);
                        borderClr = new Color(0x047857);
                    } else if (status == TrangThaiDatPhong.NO_SHOW) {
                        bg1 = new Color(0x991B1B);
                        bg2 = new Color(0xEF4444);
                        borderClr = new Color(0x7F1D1D);
                    } else if (status == TrangThaiDatPhong.WAITLIST) {
                        bg1 = new Color(0xF59E0B);
                        bg2 = new Color(0xFBBF24);
                        borderClr = new Color(0xD97706);
                    } else {
                        bg1 = new Color(0x6B7280);
                        bg2 = new Color(0x9CA3AF);
                        borderClr = new Color(0x4B5563);
                    }
                    
                    // Hover magnification
                    if (hoveredStay != null && hoveredStay.getMaChiTiet().equals(ct.getMaChiTiet())) {
                        g2.setColor(new Color(borderClr.getRed(), borderClr.getGreen(), borderClr.getBlue(), 60));
                        g2.setStroke(new BasicStroke(4.0f));
                        g2.drawRoundRect(x1 + 1, barY - 1, barW - 2, barH + 2, 10, 10);
                        bg1 = bg1.brighter();
                        bg2 = bg2.brighter();
                    }
                    
                    // Draw bar
                    GradientPaint gp = new GradientPaint(x1, barY, bg1, x1 + barW, barY + barH, bg2);
                    g2.setPaint(gp);
                    g2.fillRoundRect(x1 + 2, barY, barW - 4, barH, 10, 10);
                    
                    g2.setColor(borderClr);
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.drawRoundRect(x1 + 2, barY, barW - 4, barH, 10, 10);
                    
                    // Arrow indicators
                    g2.setColor(Color.WHITE);
                    if (continuesLeft) {
                        int[] xPoints = {x1 + 6, x1 + 12, x1 + 12};
                        int[] yPoints = {barY + barH / 2, barY + barH / 2 - 4, barY + barH / 2 + 4};
                        g2.fillPolygon(xPoints, yPoints, 3);
                    }
                    if (continuesRight) {
                        int[] xPoints = {x1 + barW - 6, x1 + barW - 12, x1 + barW - 12};
                        int[] yPoints = {barY + barH / 2, barY + barH / 2 - 4, barY + barH / 2 + 4};
                        g2.fillPolygon(xPoints, yPoints, 3);
                    }
                    
                    // Guest name text
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    FontMetrics fm = g2.getFontMetrics();
                    String name = ct.getDatPhong().getKhachHang() != null ? ct.getDatPhong().getKhachHang().getHoTen() : "Khách";
                    
                    int textPadding = 20;
                    int maxTextW = barW - textPadding;
                    if (continuesLeft) maxTextW -= 10;
                    if (continuesRight) maxTextW -= 10;
                    
                    if (maxTextW > 15) {
                        String dispName = name;
                        if (fm.stringWidth(dispName) > maxTextW) {
                            while (dispName.length() > 3 && fm.stringWidth(dispName + "...") > maxTextW) {
                                dispName = dispName.substring(0, dispName.length() - 1);
                            }
                            dispName += "...";
                        }
                        int textX = x1 + (barW - fm.stringWidth(dispName)) / 2;
                        int textY = barY + ((barH - fm.getHeight()) / 2) + fm.getAscent();
                        g2.drawString(dispName, textX, textY);
                    }
                }
            }
            g2.dispose();
        }
    }
    
    // ===== DETAILED POPUP DIALOG =====
    
    private void showStayDetailsDialog(ChiTietDatPhong stay) {
        JDialog dlg = new JDialog(parent, "Chi tiết lưu trú", true);
        dlg.setSize(520, 500);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.getContentPane().setBackground(Color.WHITE);
        
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        
        // 1. Header Banner
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x1E293B)); // Sleek dark slate
        header.setPreferredSize(new Dimension(0, 80));
        header.setBorder(BorderFactory.createEmptyBorder(15, 24, 15, 24));
        
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        
        JLabel lblGuestName = new JLabel(stay.getDatPhong().getKhachHang() != null ? stay.getDatPhong().getKhachHang().getHoTen().toUpperCase() : "KHÁCH HÀNG LẺ");
        lblGuestName.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblGuestName.setForeground(Color.WHITE);
        
        JLabel lblBookingID = new JLabel("Mã đơn đặt: " + stay.getDatPhong().getMaDatPhong() + "  |  Phòng: " + stay.getPhong().getMaPhong());
        lblBookingID.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblBookingID.setForeground(new Color(0x94A3B8));
        
        titlePanel.add(lblGuestName);
        titlePanel.add(Box.createVerticalStrut(3));
        titlePanel.add(lblBookingID);
        header.add(titlePanel, BorderLayout.CENTER);
        
        // 2. Info Grid
        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(8, 0, 8, 0);
        c.weightx = 1.0;
        
        String phone = stay.getDatPhong().getKhachHang() != null ? stay.getDatPhong().getKhachHang().getSdt() : "—";
        String cccd = stay.getDatPhong().getKhachHang() != null ? stay.getDatPhong().getKhachHang().getCccd() : "—";
        String roomType = stay.getPhong().getLoaiPhong() != null ? stay.getPhong().getLoaiPhong().getTenLoaiPhong() : "Standard";
        String rate = String.format("%,.0fđ / đêm", stay.getGiaThucTeChot() > 0 ? stay.getGiaThucTeChot() : stay.getPhong().getLoaiPhong().getGiaTheoNgay());
        
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String arrival = stay.getNgayNhanThucTe() != null ? stay.getNgayNhanThucTe().format(dtf) : stay.getDatPhong().getNgayNhanDuKien().format(dtf) + " (Dự kiến)";
        String departure = stay.getNgayTraThucTe() != null ? stay.getNgayTraThucTe().format(dtf) : stay.getDatPhong().getNgayTraDuKien().format(dtf) + " (Dự kiến)";
        
        String deposit = String.format("%,.0fđ", stay.getDatPhong().getTienDatCoc());
        String total = String.format("%,.0fđ (Tạm tính)", stay.getDatPhong().getTongTienTamTinh());
        
        int row = 0;
        addDetailRow(body, "Số điện thoại:", phone, c, row++);
        addDetailRow(body, "Số CCCD/Hộ chiếu:", cccd, c, row++);
        addDetailRow(body, "Hạng phòng:", roomType, c, row++);
        addDetailRow(body, "Giá chốt:", rate, c, row++);
        addDetailRow(body, "Thời gian nhận:", arrival, c, row++);
        addDetailRow(body, "Thời gian trả:", departure, c, row++);
        addDetailRow(body, "Tiền đặt cọc:", deposit, c, row++);
        addDetailRow(body, "Tổng tạm tính:", total, c, row++);
        
        // Status Badge
        c.gridy = row;
        c.gridx = 0;
        c.weightx = 0.3;
        JLabel lblStat = new JLabel("Trạng thái:");
        lblStat.setFont(UIConstants.FONT_BODY_BOLD);
        lblStat.setForeground(UIConstants.TEXT_PRIMARY);
        body.add(lblStat, c);
        
        c.gridx = 1;
        c.weightx = 0.7;
        TrangThaiDatPhong tt = stay.getDatPhong().getTrangThai();
        JLabel lblBadge = new JLabel(UIConstants.getTrangThaiDatPhongLabel(tt)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.getTrangThaiDatPhongColor(tt));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblBadge.setForeground(Color.WHITE);
        lblBadge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        lblBadge.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Set fixed preferred size for badge
        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        badgeWrap.setOpaque(false);
        badgeWrap.add(lblBadge);
        body.add(badgeWrap, c);
        
        // 3. Footer Action Buttons
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(new Color(0xF8FAFC));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER));
        
        JButton btnClose = RoundedComponents.grayButton("Đóng");
        btnClose.addActionListener(e -> dlg.dispose());
        
        JButton btnViewOrder = RoundedComponents.primaryButton("Xem đơn đặt");
        btnViewOrder.addActionListener(e -> {
            dlg.dispose();
            parent.navigateTo("datphong", () -> {
                // Programmatic hook to focus booking
                parent.getDatPhongPanel().focusBooking(stay.getDatPhong().getMaDatPhong());
            });
        });
        
        footer.add(btnClose);
        footer.add(btnViewOrder);
        
        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        
        dlg.setContentPane(root);
        dlg.setVisible(true);
    }
    
    private void addDetailRow(JPanel p, String labelText, String valueText, GridBagConstraints c, int row) {
        c.gridy = row;
        
        c.gridx = 0;
        c.weightx = 0.3;
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UIConstants.FONT_BODY_BOLD);
        lbl.setForeground(UIConstants.TEXT_PRIMARY);
        p.add(lbl, c);
        
        c.gridx = 1;
        c.weightx = 0.7;
        JLabel val = new JLabel(valueText);
        val.setFont(UIConstants.FONT_BODY);
        val.setForeground(new Color(0x334155));
        p.add(val, c);
    }
}
