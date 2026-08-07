package ui.panels;

import service.*;
import dao.PhongDAO;
import ui.MainFrame;
import ui.components.UIConstants;
import ui.components.RoundedComponents.*;

import ui.components.WrapLayout;
import entity.Phong;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * TongQuanPanel — Dashboard tổng quan
 *
 * FIX (2026-04-23):
 * 1. Toàn bộ DB call được chuyển ra khỏi EDT bằng SwingWorker.
 * 2. refresh() thực sự rebuild nội dung các card (không còn stale data).
 * 3. getDashboardStats() chỉ gọi 1 lần, truyền kết quả vào tất cả card.
 * 4. updateRecentActivity() được implement đầy đủ.
 * 5. Labels lblStatXxx được lưu tham chiếu thực và cập nhật đúng.
 */
public class TongQuanPanel extends JPanel {

    private final MainFrame mainFrame;
    private final ThongKeService thongKeService = new ThongKeService();
    private final PhongService phongService = new PhongService();

    // ── Stat card labels ─────────────────────────────────────────────────────
    private JLabel lblStatEmpty, lblStatOccupied, lblStatReserved,
            lblStatCheckin, lblStatCheckout, lblStatRevenue;

    // ── Content containers ───────────────────────────────────────────────────
    private JPanel pnlCheckinList, pnlCheckoutList, pnlActivityList, pnlAlertList;
    private JLabel lblCheckinCount, lblCheckoutCount, lblAlertCount;

    // ── Room grid ────────────────────────────────────────────────────────────
    private List<Phong> allRooms = new ArrayList<>();
    private String curFloor = "all";
    private JPanel pnlRoomTabs;

    // ── Revenue chart ────────────────────────────────────────────────────────
    private JPanel pnlRevenueChart;
    private List<long[]> revenue7 = new ArrayList<>();

    // ── Occupancy ────────────────────────────────────────────────────────────
    private JPanel pnlGauge;
    private JLabel lblOccCnt, lblEmptyCnt, lblOtherCnt;

    private volatile boolean isRefreshing = false;
    private final Set<String> pushedAlertIds = new HashSet<>();
    private int hoverIdx = -1;
    private final Set<String> reservedRoomNums = new HashSet<>();

    // ─────────────────────────────────────────────────────────────────────────
    public TongQuanPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI(); // Dựng khung UI (không gọi DB)
        loadDataAsync(); // Fetch data trên background thread
    }

    // ─── PUBLIC API ──────────────────────────────────────────────────────────

    /** Gọi khi cần refresh toàn bộ dashboard */
    public void refresh() {
        loadDataAsync();
    }

    // BƯỚC 1: Dựng khung UI tĩnh (không chạm DB)
    private void buildUI() {
        JScrollPane scroll = new JScrollPane(buildInner());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollBar vBarMain = scroll.getVerticalScrollBar();
        vBarMain.setPreferredSize(new Dimension(8, vBarMain.getPreferredSize().height));
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildInner() {
        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        inner.add(buildTitleRow());
        inner.add(Box.createVerticalStrut(16));
        inner.add(buildStatsRow());
        inner.add(Box.createVerticalStrut(16));
        inner.add(buildMiddleRow());
        inner.add(Box.createVerticalStrut(16));
        inner.add(buildBottomRow());

        return inner;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BƯỚC 2: Fetch data trên background thread, điền vào UI khi xong
    // ─────────────────────────────────────────────────────────────────────────
    private void loadDataAsync() {
        if (isRefreshing)
            return;
        isRefreshing = true;
        final PhongDAO pdao = new PhongDAO();

        SwingWorker<DashboardData, Void> worker = new SwingWorker<DashboardData, Void>() {

            @Override
            protected DashboardData doInBackground() {
                DashboardData data = new DashboardData();
                data.stats = thongKeService.getDashboardStats();
                data.checkins = thongKeService.getCheckinHomNay();
                data.checkouts = thongKeService.getCheckoutHomNay();
                data.activities = thongKeService.getHoatDongGanDay(10);
                data.alerts = thongKeService.getAlerts();
                data.rooms = phongService.getAllPhong();
                data.revenue7 = thongKeService.getDoanhThu7Ngay();
                data.reservedRooms = pdao.getReservedRoomsForDate(java.time.LocalDateTime.now());
                return data;
            }

            @Override
            protected void done() {
                try {
                    DashboardData data = get();
                    applyDataToUI(data);
                } catch (Exception e) {
                    System.err.println("[TongQuanPanel] ERROR loading data: " + e.getMessage());
                } finally {
                    isRefreshing = false;
                }
            }
        };

        worker.execute();
    }

    /** Phân phối data vào từng section của UI — chỉ chạy trên EDT */
    private void applyDataToUI(DashboardData data) {
        Map<String, Object> stats = data.stats;

        // ── 1. Stat cards ────────────────────────────────────────────────────
        int phongTrong = (int) stats.getOrDefault("phongTrong", 0);
        int dangO = (int) stats.getOrDefault("dangO", 0);
        int daDat = (int) stats.getOrDefault("daDat", 0);
        int tongPhong = (int) stats.getOrDefault("tongPhong", 0);
        int checkinCnt = (int) stats.getOrDefault("checkinHomNay", 0);
        int checkoutCnt = (int) stats.getOrDefault("checkoutHomNay", 0);
        long doanhThu = (long) stats.getOrDefault("doanhThuHomNay", 0L);

        if (lblStatEmpty != null)
            lblStatEmpty.setText(String.valueOf(phongTrong));
        if (lblStatOccupied != null)
            lblStatOccupied.setText(String.valueOf(dangO));
        if (lblStatReserved != null)
            lblStatReserved.setText(String.valueOf(daDat));
        if (lblStatCheckin != null)
            lblStatCheckin.setText(String.valueOf(checkinCnt));
        if (lblStatCheckout != null)
            lblStatCheckout.setText(String.valueOf(checkoutCnt));
        if (lblStatRevenue != null)
            lblStatRevenue.setText(String.format("%,.1fM", doanhThu / 1_000_000.0));

        // ── 2. Room Grid ─────────────────────────────────────────────────────
        allRooms = data.rooms != null ? data.rooms : new ArrayList<>();
        reservedRoomNums.clear();
        if (data.reservedRooms != null) {
            reservedRoomNums.addAll(data.reservedRooms.keySet());
        }
        rebuildRoomTabs();
        filterAndRenderGrid(curFloor);

        // ── 3. Revenue chart ─────────────────────────────────────────────────
        revenue7 = data.revenue7 != null ? data.revenue7 : new ArrayList<>();
        if (pnlRevenueChart != null)
            pnlRevenueChart.repaint();

        // ── 4. Occupancy counts ──────────────────────────────────────────────
        int other = Math.max(0, tongPhong - dangO - phongTrong);
        if (lblOccCnt != null)
            lblOccCnt.setText(String.valueOf(dangO));
        if (lblEmptyCnt != null)
            lblEmptyCnt.setText(String.valueOf(phongTrong));
        if (lblOtherCnt != null)
            lblOtherCnt.setText(String.valueOf(other));
        if (pnlGauge != null)
            pnlGauge.repaint();

        // ── 3. Check-in list ─────────────────────────────────────────────────
        reservedRoomNums.clear();
        if (lblCheckinCount != null)
            lblCheckinCount.setText(data.checkins.size() + " lượt");
        if (pnlCheckinList != null) {
            pnlCheckinList.removeAll();
            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm");
            if (data.checkins.isEmpty()) {
                pnlCheckinList.add(buildEmptyLabel("Chưa có check-in hôm nay"));
            } else {
                for (Map<String, Object> ci : data.checkins) {
                    pnlCheckinList.add(buildCheckinRow(
                            (String) ci.get("hoTen"),
                            ci.get("soPhong") + " – " + ci.get("tenLoai"),
                            ci.get("ngayNhan") != null ? timeFmt.format(ci.get("ngayNhan")) : "--:--",
                            "Chờ xác nhận"));
                }
            }
            pnlCheckinList.revalidate();
            pnlCheckinList.repaint();
        }

        // ── 4. Check-out list ────────────────────────────────────────────────
        if (lblCheckoutCount != null)
            lblCheckoutCount.setText(data.checkouts.size() + " lượt");
        if (pnlCheckoutList != null) {
            pnlCheckoutList.removeAll();
            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm");
            if (data.checkouts.isEmpty()) {
                pnlCheckoutList.add(buildEmptyLabel("Chưa có check-out hôm nay"));
            } else {
                for (Map<String, Object> co : data.checkouts) {
                    pnlCheckoutList.add(buildCheckoutRow(co, timeFmt));
                }
            }
            pnlCheckoutList.revalidate();
            pnlCheckoutList.repaint();
        }

        // ── 5. Activity list ─────────────────────────────────────────────────
        if (pnlActivityList != null) {
            pnlActivityList.removeAll();
            if (data.activities.isEmpty()) {
                pnlActivityList.add(buildEmptyLabel("Chưa có hoạt động"));
            } else {
                SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm dd/MM");
                for (Map<String, Object> act : data.activities) {
                    pnlActivityList.add(buildActivityRow(act, timeFmt));
                }
            }
            pnlActivityList.revalidate();
            pnlActivityList.repaint();
        }

        // ── 6. Alerts list ───────────────────────────────────────────────────
        if (lblAlertCount != null) {
            lblAlertCount.setText(data.alerts.size() + " mục");
            lblAlertCount.setForeground(data.alerts.isEmpty() ? UIConstants.SUCCESS : UIConstants.DANGER);
        }
        if (pnlAlertList != null) {
            pnlAlertList.removeAll();
            if (data.alerts.isEmpty()) {
                JLabel ok = buildEmptyLabel("Mọi thứ đều ổn");
                ok.setForeground(UIConstants.SUCCESS);
                pnlAlertList.add(Box.createVerticalStrut(30));
                pnlAlertList.add(ok);
            } else {
                for (Map<String, Object> alert : data.alerts) {
                    pnlAlertList.add(buildAlertRow(alert));
                    pnlAlertList.add(new JSeparator(JSeparator.HORIZONTAL));
                }
            }
            pnlAlertList.revalidate();
            pnlAlertList.repaint();

            // Cập nhật pushedAlertIds và push thông báo mới
            Set<String> currentIds = new HashSet<>();
            for (Map<String, Object> alert : data.alerts) {
                String id = String.valueOf(alert.getOrDefault("id", alert.getOrDefault("title", "")));
                currentIds.add(id);
                if (!pushedAlertIds.contains(id)) {
                    JOptionPane.showMessageDialog(this, (String) alert.get("title"), "Cảnh báo mới",
                            JOptionPane.WARNING_MESSAGE);
                    pushedAlertIds.add(id);
                }
            }
            pushedAlertIds.retainAll(currentIds);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUILD UI — Title
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildTitleRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, 80));

        service.AuthService auth = service.AuthService.getInstance();
        String name = auth.getCurrentUser() != null ? auth.getCurrentUser().getHoTen() : "Quản trị viên";
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greet = hour < 12 ? "Chào buổi sáng" : hour < 18 ? "Chào buổi chiều" : "Chào buổi tối";

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel lblGreet = new JLabel(greet + ", " + name);
        lblGreet.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblGreet.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Đây là tổng quan hoạt động khách sạn hôm nay. Chúc bạn ca làm việc suôn sẻ!");
        lblSub.setFont(UIConstants.FONT_SUBTITLE);
        lblSub.setForeground(UIConstants.TEXT_SECONDARY);

        left.add(lblGreet);
        left.add(Box.createVerticalStrut(4));
        left.add(lblSub);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btnPanel.setOpaque(false);

        RoundedButton btnCheckout = new RoundedButton("Trả phòng nhanh", Color.WHITE, UIConstants.PRIMARY);
        btnCheckout.setBorder(BorderFactory.createLineBorder(UIConstants.PRIMARY, 1, true));
        btnCheckout.setFont(UIConstants.FONT_SMALL_BOLD);
        btnCheckout.setPreferredSize(new Dimension(155, 38));
        btnCheckout.addActionListener(e -> handleDashboardCheckout());

        RoundedButton btnCheckin = new RoundedButton("Nhận phòng", UIConstants.PRIMARY, Color.WHITE);
        btnCheckin.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnCheckin.setPreferredSize(new Dimension(145, 38));
        btnCheckin.addActionListener(e -> handleDashboardCheckin());

        btnPanel.add(btnCheckout);
        btnPanel.add(btnCheckin);

        row.add(left, BorderLayout.WEST);
        row.add(btnPanel, BorderLayout.EAST);
        return row;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUILD UI — Stats row (dùng placeholder "..." — sau fill bằng applyDataToUI)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildStatsRow() {
        service.AuthService auth = service.AuthService.getInstance();
        boolean showRevenue = auth.isManager();

        int cols = showRevenue ? 6 : 5;

        // Wrapper để fill full width
        JPanel wrapper = new JPanel(new GridLayout(1, cols, 16, 0));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Short.MAX_VALUE, 90));
        wrapper.setPreferredSize(new Dimension(1000, 90));

        lblStatEmpty = addStatCard(wrapper, "Phòng trống", "...", "\u2705", UIConstants.SUCCESS);
        lblStatOccupied = addStatCard(wrapper, "Đang ở", "...", "\uD83D\uDCC8", UIConstants.PRIMARY);
        lblStatReserved = addStatCard(wrapper, "Đặt trước", "...", "\uD83D\uDCC5", UIConstants.WARNING);
        lblStatCheckin = addStatCard(wrapper, "Check-in", "...", "\uD83D\uDEEB", new Color(0x06B6D4));
        lblStatCheckout = addStatCard(wrapper, "Check-out", "...", "\uD83D\uDEEC", UIConstants.ORANGE);

        if (showRevenue) {
            lblStatRevenue = addStatCard(wrapper, "Doanh thu", "...", "\uD83D\uDCB0", new Color(0x8B5CF6));
        }
        return wrapper;
    }

    /** Tạo 1 stat card hiện đại với icon, gradient accent, hover */
    private JLabel addStatCard(JPanel parent, String label, String initialValue, String icon, Color accent) {
        RoundedPanel card = new RoundedPanel(14);
        card.setBackground(Color.WHITE);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setPreferredSize(new Dimension(160, 90));

        // Icon Container - Thay icon lỗi bằng hình tròn màu đậm
        Color bgLight = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30);
        JPanel iconContainer = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgLight);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        iconContainer.setOpaque(false);
        iconContainer.setLayout(new GridBagLayout());
        iconContainer.setPreferredSize(new Dimension(44, 44));

        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        lblIcon.setForeground(accent);
        iconContainer.add(lblIcon);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblLabel.setForeground(UIConstants.TEXT_SECONDARY);

        JLabel lblValue = new JLabel(initialValue);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblValue.setForeground(UIConstants.TEXT_PRIMARY);

        textPanel.add(lblLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(lblValue);

        card.add(iconContainer, BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);

        parent.add(card);
        return lblValue;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUILD UI — Middle row (Room status + Check-in)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildMiddleRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 16, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, 400));
        row.add(buildRoomGridCard());
        row.add(buildRevenueChartCard());
        row.add(buildOccupancyCard());
        return row;
    }

    private JPanel pnlMiniGrid;

    private JPanel buildRoomGridCard() {
        RoundedPanel card = new RoundedPanel(12);
        card.setBackground(Color.WHITE);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 14, 18));

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        JLabel title = new JLabel("Sơ đồ phòng");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        JButton btnMore = new JButton("Xem đầy đủ →");
        btnMore.setFont(UIConstants.FONT_SMALL_BOLD);
        btnMore.setForeground(UIConstants.PRIMARY);
        btnMore.setBorderPainted(false);
        btnMore.setContentAreaFilled(false);
        btnMore.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnMore.addActionListener(e -> mainFrame.navigateTo("thuephong"));
        hdr.add(title, BorderLayout.WEST);
        hdr.add(btnMore, BorderLayout.EAST);
        card.add(hdr, BorderLayout.NORTH);

        pnlRoomTabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        pnlRoomTabs.setOpaque(false);
        // Khởi tạo tab mặc định (sau này sẽ được rebuild trong applyDataToUI)
        rebuildRoomTabs();

        pnlMiniGrid = new ScrollablePanel(new WrapLayout(FlowLayout.LEFT, 8, 8));
        pnlMiniGrid.setOpaque(false);
        JScrollPane gs = new JScrollPane(pnlMiniGrid);
        gs.setOpaque(false);
        gs.getViewport().setOpaque(false);
        gs.setBorder(null);
        gs.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        gs.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS); // Thay đổi thành ALWAYS để ép hiện thanh cuộn
        gs.getVerticalScrollBar().setUnitIncrement(16);
        gs.setPreferredSize(new Dimension(200, 225)); // Giới hạn chiều cao để kích hoạt thanh cuộn
        
        JScrollBar vBar = gs.getVerticalScrollBar();
        vBar.setPreferredSize(new Dimension(8, 0)); // Dùng kích thước mặc định hoặc lớn hơn xíu để chắc chắn vẽ được thumb
        vBar.setOpaque(true);

        JPanel center = new JPanel(new BorderLayout(0, 5));
        center.setOpaque(false);
        center.add(pnlRoomTabs, BorderLayout.NORTH);
        center.add(gs, BorderLayout.CENTER);
        card.add(center, BorderLayout.CENTER);

        JPanel leg = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 3));
        leg.setOpaque(false);
        leg.add(mkDot("Trống", UIConstants.SUCCESS));
        leg.add(mkDot("Đang ở", UIConstants.PRIMARY));
        leg.add(mkDot("Đặt trước", UIConstants.WARNING));
        leg.add(mkDot("Cần dọn", new Color(0x06B6D4)));
        leg.add(mkDot("Bảo trì", UIConstants.DANGER));
        card.add(leg, BorderLayout.SOUTH);
        return card;
    }

    private void rebuildRoomTabs() {
        if (pnlRoomTabs == null) return;
        pnlRoomTabs.removeAll();
        
        Set<String> floors = new TreeSet<>();
        for (Phong p : allRooms) {
            String num = p.getSoPhong() != null ? p.getSoPhong().replaceAll("[^0-9]", "") : "";
            if (num.length() > 0) floors.add(String.valueOf(num.charAt(0)));
        }
        
        // Nếu không có data, dùng mặc định
        if (floors.isEmpty()) {
            floors.addAll(Arrays.asList("1", "2", "3", "4"));
        }
        
        List<String> items = new ArrayList<>();
        items.add("Tất cả");
        for (String f : floors) {
            items.add("Tầng " + f);
        }
        
        ModernComboBox<String> cboFloor = new ModernComboBox<>(items.toArray(new String[0]));
        cboFloor.setPreferredSize(new Dimension(140, 32));
        
        boolean foundCurFloor = "all".equals(curFloor);
        if ("all".equals(curFloor)) {
            cboFloor.setSelectedItem("Tất cả");
        } else if (floors.contains(curFloor)) {
            cboFloor.setSelectedItem("Tầng " + curFloor);
            foundCurFloor = true;
        }
        
        if (!foundCurFloor) {
            cboFloor.setSelectedItem("Tất cả");
            curFloor = "all";
        }
        
        cboFloor.addActionListener(e -> {
            String sel = (String) cboFloor.getSelectedItem();
            if ("Tất cả".equals(sel)) {
                curFloor = "all";
            } else if (sel != null) {
                curFloor = sel.replace("Tầng ", "");
            }
            filterAndRenderGrid(curFloor);
        });
        
        pnlRoomTabs.add(cboFloor);
        pnlRoomTabs.revalidate();
        pnlRoomTabs.repaint();
    }


    private JPanel mkDot(String text, Color color) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        p.setOpaque(false);
        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 4, 9, 9);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(9, 17));
        JLabel lbl = new JLabel(text);
        lbl.setFont(UIConstants.FONT_TINY);
        lbl.setForeground(UIConstants.TEXT_SECONDARY);
        p.add(dot);
        p.add(lbl);
        return p;
    }

    private void filterAndRenderGrid(String floor) {
        if (pnlMiniGrid == null)
            return;
        pnlMiniGrid.removeAll();
        for (Phong r : allRooms) {
            String num = r.getSoPhong() != null ? r.getSoPhong().replaceAll("[^0-9]", "") : "";
            String fl = num.length() > 0 ? String.valueOf(num.charAt(0)) : "?";
            if ("all".equals(floor) || floor.equals(fl))
                pnlMiniGrid.add(createRoomChip(r));
        }
        pnlMiniGrid.revalidate();
        pnlMiniGrid.repaint();
    }

    private JPanel createRoomChip(Phong r) {
        entity.enums.TrangThaiPhong physicalStatus = r.getTrangThai();
        boolean isReservedToday = reservedRoomNums.contains(r.getSoPhong());
        
        Color c;
        String st;
        
        if (physicalStatus == entity.enums.TrangThaiPhong.AVAILABLE && isReservedToday) {
            c = UIConstants.WARNING; // Orange for reserved
            st = "ĐĐặt"; // Short for Đặt trước
        } else {
            c = UIConstants.getTrangThaiPhongColor(physicalStatus);
            st = UIConstants.getTrangThaiPhongLabel(physicalStatus);
            if (st.startsWith("Có") || st.startsWith("Không"))
                st = "Trống";
            else if (st.startsWith("Đang"))
                st = "ĐỞ";
            else if (st.startsWith("Vệ"))
                st = "Dọn";
            else if (st.startsWith("Bảo"))
                st = "BT";
        }

        Color bg = new Color(c.getRed(), c.getGreen(), c.getBlue(), 20);
        JPanel chip = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Light background from UIConstants
                Color bgChip = UIConstants.getTrangThaiPhongBg(physicalStatus);
                if (physicalStatus == entity.enums.TrangThaiPhong.AVAILABLE && isReservedToday) {
                    bgChip = UIConstants.WARNING_LIGHT;
                }
                
                g2.setColor(bgChip);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                
                // Thin border
                g2.setColor(c);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        chip.setOpaque(false);
        chip.setLayout(new BoxLayout(chip, BoxLayout.Y_AXIS));
        chip.setPreferredSize(new Dimension(54, 48));
        chip.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        JLabel lNum = new JLabel(r.getSoPhong(), SwingConstants.CENTER);
        lNum.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lNum.setForeground(c.darker());
        lNum.setAlignmentX(0.5f);
        
        JLabel lSt = new JLabel(st, SwingConstants.CENTER);
        lSt.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lSt.setForeground(c.darker());
        lSt.setAlignmentX(0.5f);
        
        chip.add(Box.createVerticalGlue());
        chip.add(lNum);
        chip.add(lSt);
        chip.add(Box.createVerticalGlue());
        chip.setToolTipText("<html><b>" + r.getSoPhong() + "</b><br>"
                + UIConstants.getTrangThaiPhongLabel(r.getTrangThai()) + "</html>");
        return chip;
    }

    // ── Revenue Chart Card ─────────────────────────────────────────────────────
    private JPanel buildRevenueChartCard() {
        RoundedPanel card = new RoundedPanel(12);
        card.setBackground(Color.WHITE);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        JLabel title = new JLabel("Doanh thu 7 ngày");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        JButton btnDetail = new JButton("Chi tiết →");
        btnDetail.setFont(UIConstants.FONT_SMALL_BOLD);
        btnDetail.setForeground(UIConstants.PRIMARY);
        btnDetail.setBorderPainted(false);
        btnDetail.setContentAreaFilled(false);
        btnDetail.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        hdr.add(title, BorderLayout.WEST);
        hdr.add(btnDetail, BorderLayout.EAST);
        card.add(hdr, BorderLayout.NORTH);

        // Bar chart panel
        pnlRevenueChart = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                
                // Dynamic day labels (6 days ago -> today)
                String[] dayLabels = new String[7];
                String[] dow = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, -6);
                for(int i=0; i<7; i++) {
                    dayLabels[i] = dow[cal.get(Calendar.DAY_OF_WEEK)-1];
                    cal.add(Calendar.DATE, 1);
                }

                long[] vals = new long[7];
                for (long[] row : revenue7) {
                    int idx = (int) (6 - row[0]);
                    if (idx >= 0 && idx < 7) vals[idx] = row[1];
                }
                
                long maxVal = 1000000; // Default scale 1M
                for (long v : vals) if (v > maxVal) maxVal = v;
                
                int w = getWidth(), h = getHeight();
                int padL = 40, padR = 10, padT = 30, padB = 25; // Increased padL for Y-axis
                int chartW = w - padL - padR;
                int chartH = h - padT - padB;
                int barW = chartW / 7;

                // Draw Y-axis grid lines
                g2.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{2}, 0));
                g2.setColor(new Color(0xE2E8F0));
                for (int i = 0; i <= 4; i++) {
                    int gy = padT + chartH - (chartH * i / 4);
                    g2.drawLine(padL, gy, w - padR, gy);
                    
                    // Y labels
                    g2.setColor(UIConstants.TEXT_MUTED);
                    long val = maxVal * i / 4;
                    String label = val >= 1000000 ? String.format("%.1fM", val/1000000.0) : String.format("%.0fK", val/1000.0);
                    g2.drawString(label, 5, gy + 4);
                    g2.setColor(new Color(0xE2E8F0));
                }

                for (int i = 0; i < 7; i++) {
                    int barH = (int) (chartH * vals[i] / maxVal);
                    if (vals[i] > 0 && barH < 4) barH = 4; 

                    int bx = padL + i * barW + 6;
                    int by = padT + (chartH - barH);
                    int bw = barW - 12;
                    
                    boolean isToday = (i == 6);
                    boolean isHover = (i == hoverIdx);
                    Color baseColor = isToday ? UIConstants.PRIMARY : new Color(0x93C5FD);
                    
                    if (isHover) {
                        g2.setColor(baseColor.brighter());
                        g2.fillRoundRect(bx, by, bw, barH, 6, 6);
                        
                        // Draw value on top
                        g2.setColor(UIConstants.TEXT_PRIMARY);
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                        String valStr = String.format("%,d", vals[i]);
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(valStr, bx + (bw - fm.stringWidth(valStr)) / 2, by - 8);
                        g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                    } else {
                        g2.setColor(baseColor);
                        g2.fillRoundRect(bx, by, bw, barH, 6, 6);
                    }
                    
                    // Label
                    g2.setColor((isToday || isHover) ? UIConstants.PRIMARY : UIConstants.TEXT_MUTED);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(dayLabels[i], bx + (bw - fm.stringWidth(dayLabels[i])) / 2, h - 6);
                }
                g2.dispose();
            }
        };
        pnlRevenueChart.setOpaque(false);
        MouseAdapter chartAdapter = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int w = pnlRevenueChart.getWidth();
                int padL = 40, padR = 10;
                int chartW = w - padL - padR;
                int barW = chartW / 7;
                int x = e.getX();
                if (x >= padL && x < w - padR) {
                    int idx = (x - padL) / barW;
                    if (idx >= 0 && idx < 7) {
                        long val = 0;
                        for (long[] row : revenue7) {
                            if ((int) (6 - row[0]) == idx) {
                                val = row[1];
                                break;
                            }
                        }
                        
                        // Dynamic day names for tooltip
                        String[] dayNames = new String[7];
                        String[] fullDow = {"Chủ nhật", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7"};
                        Calendar c = Calendar.getInstance();
                        c.add(Calendar.DATE, -6);
                        for(int i=0; i<7; i++) {
                            dayNames[i] = fullDow[c.get(Calendar.DAY_OF_WEEK)-1];
                            c.add(Calendar.DATE, 1);
                        }

                        pnlRevenueChart.setToolTipText(String.format("<html><b>%s</b><br>Doanh thu: %,d VNĐ</html>", 
                                dayNames[idx], val));
                        if (hoverIdx != idx) {
                            hoverIdx = idx;
                            pnlRevenueChart.repaint();
                        }
                        return;
                    }
                }
                if (hoverIdx != -1) {
                    hoverIdx = -1;
                    pnlRevenueChart.repaint();
                }
                pnlRevenueChart.setToolTipText(null);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverIdx = -1;
                pnlRevenueChart.repaint();
            }
        };
        pnlRevenueChart.addMouseListener(chartAdapter);
        pnlRevenueChart.addMouseMotionListener(chartAdapter);
        card.add(pnlRevenueChart, BorderLayout.CENTER);
        return card;
    }

    // ── Occupancy Card (with counts) ───────────────────────────────────────────
    private JPanel buildOccupancyCard() {
        RoundedPanel card = new RoundedPanel(12);
        card.setBackground(Color.WHITE);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 14, 18));

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        JLabel title = new JLabel("Công suất phòng");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        hdr.add(title, BorderLayout.WEST);
        card.add(hdr, BorderLayout.NORTH);

        // Gauge
        pnlGauge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int occ = lblOccCnt != null ? parseIntSafe(lblOccCnt.getText()) : 0;
                int emp = lblEmptyCnt != null ? parseIntSafe(lblEmptyCnt.getText()) : 0;
                int total = Math.max(1, occ + emp);
                int pct = (int) (100.0 * occ / total);
                int margin = 20, size = Math.min(getWidth(), getHeight()) - margin * 2;
                if (size < 30) {
                    g2.dispose();
                    return;
                }
                int x = (getWidth() - size) / 2, y = (getHeight() - size) / 2;
                float stroke = size * 0.1f;
                g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(0xF1F5F9));
                g2.drawArc(x, y, size, size, 135, 270);
                Color arcClr = pct > 80 ? UIConstants.DANGER : pct > 50 ? UIConstants.WARNING : UIConstants.PRIMARY;
                g2.setColor(arcClr);
                g2.drawArc(x, y, size, size, 405, -(int) (2.7 * pct));
                int cx = getWidth() / 2, cy = getHeight() / 2;
                g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(14, size / 5)));
                g2.setColor(UIConstants.TEXT_PRIMARY);
                String ps = pct + "%";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(ps, cx - fm.stringWidth(ps) / 2, cy + fm.getAscent() / 4);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, Math.max(9, size / 13)));
                g2.setColor(UIConstants.TEXT_MUTED);
                String sub = "công suất";
                fm = g2.getFontMetrics();
                g2.drawString(sub, cx - fm.stringWidth(sub) / 2, cy + fm.getAscent() / 4 + fm.getHeight());
                g2.dispose();
            }
        };
        pnlGauge.setOpaque(false);
        card.add(pnlGauge, BorderLayout.CENTER);

        // Counts row
        JPanel counts = new JPanel(new GridLayout(1, 3, 4, 0));
        counts.setOpaque(false);
        lblOccCnt = new JLabel("0", SwingConstants.CENTER);
        lblEmptyCnt = new JLabel("0", SwingConstants.CENTER);
        lblOtherCnt = new JLabel("0", SwingConstants.CENTER);
        counts.add(buildCountCell(lblOccCnt, "Có khách", UIConstants.PRIMARY));
        counts.add(buildCountCell(lblEmptyCnt, "Còn trống", UIConstants.SUCCESS));
        counts.add(buildCountCell(lblOtherCnt, "Khác", UIConstants.WARNING));
        card.add(counts, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildCountCell(JLabel numLbl, String label, Color color) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        numLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        numLbl.setForeground(color);
        numLbl.setAlignmentX(0.5f);
        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        lbl.setForeground(UIConstants.TEXT_MUTED);
        lbl.setAlignmentX(0.5f);
        p.add(numLbl);
        p.add(lbl);
        return p;
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s != null ? s.replaceAll("[^0-9]", "") : "0");
        } catch (Exception e) {
            return 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUILD UI — Bottom row (Activity + Checkout + Alerts)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildBottomRow() {
        JPanel row = new JPanel(new GridLayout(2, 2, 16, 16));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, 650));
        row.add(buildActivityCard());
        row.add(buildCheckinCard());
        row.add(buildCheckoutCard());
        row.add(buildAlertCard());
        return row;
    }

    private JPanel buildActivityCard() {
        RoundedPanel card = new RoundedPanel(16);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Hoạt động gần đây");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        card.add(title, BorderLayout.NORTH);

        pnlActivityList = new JPanel();
        pnlActivityList.setOpaque(false);
        pnlActivityList.setLayout(new BoxLayout(pnlActivityList, BoxLayout.Y_AXIS));
        pnlActivityList.add(buildLoadingLabel());

        JScrollPane scroll = new JScrollPane(pnlActivityList);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(12);

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildCheckinCard() {
        RoundedPanel card = new RoundedPanel(16);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Check-in hôm nay");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblCheckinCount = new JLabel("...");
        lblCheckinCount.setFont(UIConstants.FONT_SMALL_BOLD);
        lblCheckinCount.setForeground(new Color(0x06B6D4));

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.add(title, BorderLayout.WEST);
        hdr.add(lblCheckinCount, BorderLayout.EAST);
        card.add(hdr, BorderLayout.NORTH);

        pnlCheckinList = new JPanel();
        pnlCheckinList.setOpaque(false);
        pnlCheckinList.setLayout(new BoxLayout(pnlCheckinList, BoxLayout.Y_AXIS));
        pnlCheckinList.add(buildLoadingLabel());

        JScrollPane scroll = new JScrollPane(pnlCheckinList);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(12);

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildCheckoutCard() {
        RoundedPanel card = new RoundedPanel(16);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Check-out hôm nay");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblCheckoutCount = new JLabel("...");
        lblCheckoutCount.setFont(UIConstants.FONT_SMALL_BOLD);
        lblCheckoutCount.setForeground(UIConstants.ORANGE);

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.add(title, BorderLayout.WEST);
        hdr.add(lblCheckoutCount, BorderLayout.EAST);
        card.add(hdr, BorderLayout.NORTH);

        pnlCheckoutList = new JPanel();
        pnlCheckoutList.setOpaque(false);
        pnlCheckoutList.setLayout(new BoxLayout(pnlCheckoutList, BoxLayout.Y_AXIS));
        pnlCheckoutList.add(buildLoadingLabel());

        JScrollPane scroll = new JScrollPane(pnlCheckoutList);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(12);

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildAlertCard() {
        RoundedPanel card = new RoundedPanel(16);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Cảnh báo hôm nay");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblAlertCount = new JLabel("...");
        lblAlertCount.setFont(UIConstants.FONT_SMALL_BOLD);
        lblAlertCount.setForeground(UIConstants.DANGER);

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.add(title, BorderLayout.WEST);
        hdr.add(lblAlertCount, BorderLayout.EAST);
        card.add(hdr, BorderLayout.NORTH);

        pnlAlertList = new JPanel();
        pnlAlertList.setOpaque(false);
        pnlAlertList.setLayout(new BoxLayout(pnlAlertList, BoxLayout.Y_AXIS));
        pnlAlertList.add(buildLoadingLabel());

        JScrollPane scroll = new JScrollPane(pnlAlertList);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(12);

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ROW BUILDERS — dùng trong applyDataToUI
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildCheckinRow(String name, String room, String time, String status) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        String initial = name != null && !name.isEmpty() ? String.valueOf(name.charAt(0)).toUpperCase() : "?";
        Color[] colors = { UIConstants.SUCCESS, UIConstants.PRIMARY, UIConstants.WARNING, UIConstants.DANGER,
                UIConstants.INFO };
        Color avatarColor = colors[Math.abs((name != null ? name : "?").hashCode()) % colors.length];

        JPanel av = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(avatarColor);
                g2.fillOval(0, 0, 32, 32);
                g2.setColor(Color.WHITE);
                g2.setFont(UIConstants.FONT_BODY_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(initial, (32 - fm.stringWidth(initial)) / 2, (32 + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        av.setOpaque(false);
        av.setPreferredSize(new Dimension(32, 32));

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel lblName = new JLabel(name != null ? name : "");
        lblName.setFont(UIConstants.FONT_BODY_BOLD);
        JLabel lblRoom = new JLabel(room);
        lblRoom.setFont(UIConstants.FONT_SMALL);
        lblRoom.setForeground(UIConstants.TEXT_MUTED);
        info.add(lblName);
        info.add(lblRoom);

        JPanel right = new JPanel(new GridLayout(2, 1));
        right.setOpaque(false);
        JLabel lblTime = new JLabel(time, SwingConstants.RIGHT);
        lblTime.setFont(UIConstants.FONT_SMALL);
        lblTime.setForeground(UIConstants.TEXT_MUTED);
        JLabel lblStatus = new JLabel(status, SwingConstants.RIGHT);
        lblStatus.setFont(UIConstants.FONT_SMALL_BOLD);
        lblStatus.setForeground(UIConstants.WARNING);
        right.add(lblTime);
        right.add(lblStatus);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(av);
        left.add(info);

        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private JPanel buildCheckoutRow(Map<String, Object> co, SimpleDateFormat timeFmt) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JLabel lblRoom = new JLabel("P." + co.get("soPhong"));
        lblRoom.setFont(UIConstants.FONT_BODY_BOLD);
        lblRoom.setForeground(UIConstants.PRIMARY);
        lblRoom.setPreferredSize(new Dimension(60, 20));

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(String.valueOf(co.get("hoTen")));
        name.setFont(UIConstants.FONT_SMALL_BOLD);
        String timeStr = co.get("ngayTraDK") != null ? timeFmt.format(co.get("ngayTraDK")) : "--:--";
        JLabel t = new JLabel(timeStr);
        t.setFont(UIConstants.FONT_TINY);
        t.setForeground(UIConstants.TEXT_MUTED);
        info.add(name);
        info.add(t);

        String trangThai = String.valueOf(co.get("trangThai"));
        boolean daTraP = "Đã trả".equals(trangThai);

        JLabel badge = new JLabel(daTraP ? " Đã trả " : " Chờ trả ");
        badge.setFont(UIConstants.FONT_TINY);
        badge.setOpaque(true);
        badge.setBackground(daTraP ? UIConstants.SUCCESS_LIGHT : UIConstants.WARNING_LIGHT);
        badge.setForeground(daTraP ? UIConstants.SUCCESS : UIConstants.WARNING);

        row.add(lblRoom, BorderLayout.WEST);
        row.add(info, BorderLayout.CENTER);
        row.add(badge, BorderLayout.EAST);
        return row;
    }

    private JPanel buildActivityRow(Map<String, Object> act, SimpleDateFormat timeFmt) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        String loai = String.valueOf(act.get("loai"));
        boolean isCheckin = "Nhận phòng".equals(loai);
        Color accent = isCheckin ? UIConstants.SUCCESS : UIConstants.INFO;

        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent);
                g2.fillOval(4, 8, 8, 8);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(16, 24));

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel lblAct = new JLabel(loai + " - P." + act.get("soPhong"));
        lblAct.setFont(UIConstants.FONT_SMALL_BOLD);
        lblAct.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel lblName = new JLabel(act.get("hoTen") != null ? String.valueOf(act.get("hoTen")) : "");
        lblName.setFont(UIConstants.FONT_TINY);
        lblName.setForeground(UIConstants.TEXT_MUTED);
        info.add(lblAct);
        info.add(lblName);

        String timeStr = act.get("thoiGian") instanceof java.util.Date
                ? timeFmt.format((java.util.Date) act.get("thoiGian"))
                : "--:--";
        JLabel lblTime = new JLabel(timeStr);
        lblTime.setFont(UIConstants.FONT_TINY);
        lblTime.setForeground(UIConstants.TEXT_MUTED);

        row.add(dot, BorderLayout.WEST);
        row.add(info, BorderLayout.CENTER);
        row.add(lblTime, BorderLayout.EAST);
        return row;
    }

    private JPanel buildAlertRow(Map<String, Object> alert) {
        String type = String.valueOf(alert.getOrDefault("type", "warning"));
        String titleStr = String.valueOf(alert.getOrDefault("title", ""));
        String descStr = String.valueOf(alert.getOrDefault("desc", ""));

        Color barColor, bgColor;
        String icon;
        switch (type) {
            case "danger" -> {
                barColor = UIConstants.DANGER;
                bgColor = new Color(0xFEF2F2);
                icon = "\u25CF";
            }
            case "info" -> {
                barColor = UIConstants.INFO;
                bgColor = new Color(0xF5F3FF);
                icon = "\u25CF";
            }
            default -> {
                barColor = UIConstants.WARNING;
                bgColor = new Color(0xFFFBEB);
                icon = "\u25CF";
            }
        }

        RoundedPanel item = new RoundedPanel(8);
        item.setBackground(bgColor);
        item.setLayout(new BorderLayout(10, 0));
        item.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        lblIcon.setPreferredSize(new Dimension(24, 24));
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel l1 = new JLabel(titleStr);
        l1.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l1.setForeground(barColor);
        JLabel l2 = new JLabel(descStr);
        l2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        l2.setForeground(UIConstants.TEXT_SECONDARY);
        info.add(l1);
        info.add(Box.createVerticalStrut(2));
        info.add(l2);

        JPanel indicator = new JPanel();
        indicator.setBackground(barColor);
        indicator.setPreferredSize(new Dimension(4, 30));

        JPanel content = new JPanel(new BorderLayout(8, 0));
        content.setOpaque(false);
        content.add(lblIcon, BorderLayout.WEST);
        content.add(info, BorderLayout.CENTER);

        item.add(indicator, BorderLayout.WEST);
        item.add(content, BorderLayout.CENTER);
        return item;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private JLabel buildLoadingLabel() {
        JLabel lbl = new JLabel("Đang tải...");
        lbl.setFont(UIConstants.FONT_BODY);
        lbl.setForeground(UIConstants.TEXT_MUTED);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lbl;
    }

    private JLabel buildEmptyLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UIConstants.FONT_BODY);
        lbl.setForeground(UIConstants.TEXT_MUTED);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lbl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WORKFLOWS (Checkin / Checkout nhanh từ Dashboard)
    // ─────────────────────────────────────────────────────────────────────────
    private void handleDashboardCheckout() {
        mainFrame.navigateTo("thuephong", () -> {
            ThuePhongPanel tp = mainFrame.getThuePhongPanel();
            if (tp != null) {
                tp.applyOccupiedFilter();
            }
        });
    }

    private void handleDashboardCheckin() {
        mainFrame.navigateTo("datphong", () -> {
            DatPhongPanel dp = mainFrame.getDatPhongPanel();
            if (dp != null) {
                dp.applyConfirmedFilter();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DATA CONTAINER
    // ─────────────────────────────────────────────────────────────────────────
    public static class DashboardData {
        public Map<String, Object> stats = new LinkedHashMap<>();
        public List<Map<String, Object>> checkins = new ArrayList<>();
        public List<Map<String, Object>> checkouts = new ArrayList<>();
        public List<Map<String, Object>> activities = new ArrayList<>();
        public List<Map<String, Object>> alerts = new ArrayList<>();
        public List<Phong> rooms = new ArrayList<>();
        public List<long[]> revenue7 = new ArrayList<>();
        public Map<String, String[]> reservedRooms = new HashMap<>();
    }

    // ── Inner Component: Scrollable Panel for Grid ───────────────────────────
    private static class ScrollablePanel extends JPanel implements Scrollable {
        public ScrollablePanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
            addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    revalidate();
                }
            });
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 100;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
