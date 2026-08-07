package ui;

import ui.panels.TongQuanPanel;
import ui.panels.ThuePhongPanel;
import ui.panels.KhachHangPanel;
import ui.panels.LoaiPhongPanel;
import ui.panels.PhongPanel;
import ui.panels.DichVuPanel;
import ui.panels.BangGiaPanel;
import ui.panels.ThongKePanel;
import ui.panels.NhanVienPanel;
import ui.panels.DatPhongPanel;
import ui.panels.HoaDonPanel;
import ui.panels.KhuyenMaiPanel;
import ui.panels.LogPanel;
import ui.panels.ShiftHistoryPanel;
import ui.panels.KeHoachPanel;
import ui.panels.ResettableFilter;
import entity.NhanVien;
import entity.enums.VaiTro;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import service.AuthService;
import ui.components.NotificationManager;
import ui.components.UIConstants;

public class MainFrame extends JFrame {

    private JPanel contentArea;
    private CardLayout cardLayout;
    private JLabel lblCurrentTime;
    private JLabel hotelLogoLabel;
    private JPanel sidebar;
    private String activeMenu = "tongquan";
    private final java.util.Map<String, Long> lastRefreshAt = new java.util.HashMap<>();
    private final java.util.List<Runnable> menuUpdaters = new java.util.ArrayList<>();
    private static final long REFRESH_COOLDOWN_MS = 2000;
    private final java.util.Set<String> expandedGroups = new java.util.HashSet<>();

    private TongQuanPanel pTongQuan;
    private ThuePhongPanel pThuePhong;
    private KhachHangPanel pKhachHang;
    private LoaiPhongPanel pLoaiPhong;
    private PhongPanel pPhong;
    private DichVuPanel pDichVu;
    private BangGiaPanel pBangGia;
    private ThongKePanel pThongKe;
    private NhanVienPanel pNhanVien;
    private DatPhongPanel pDatPhong;
    private HoaDonPanel pHoaDon;
    private KhuyenMaiPanel pKhuyenMai;
    private ShiftHistoryPanel pShiftHistory;
    private KeHoachPanel pKeHoach;
    private LogPanel pLog;

    private ui.components.GlobalNotificationBell notificationBell;
    private final service.ThongKeService ts = new service.ThongKeService();
    private final service.DatPhongService ds = new service.DatPhongService();
    private final java.util.Set<String> dismissedAlertIds = new java.util.HashSet<>();
    private static final String DISMISSED_ALERTS_FILE = "dismissed_notifications.dat";

    public MainFrame() {
        setTitle("Hotel MS");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // setSize(1366, 820);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        initUI();
        startClock();
        startNotificationTimer();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                SwingUtilities.invokeLater(() -> navigateTo("tongquan"));
            }
        });
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UIConstants.BG_MAIN);

        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setOpaque(false);
        contentWrapper.add(buildHeader(), BorderLayout.NORTH);
        contentWrapper.add(buildContent(), BorderLayout.CENTER);

        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(contentWrapper, BorderLayout.CENTER);
        setContentPane(root);

        // Khởi tạo hệ thống thông báo đẩy
        ui.components.NotificationManager.init(this);
        loadDismissedAlerts();
    }

    private void loadDismissedAlerts() {
        java.io.File file = new java.io.File(DISMISSED_ALERTS_FILE);
        if (file.exists()) {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty())
                        dismissedAlertIds.add(line.trim());
                }
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveDismissedAlerts() {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(DISMISSED_ALERTS_FILE))) {
            for (String id : dismissedAlertIds)
                writer.println(id);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    // ===== HEADER =====
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                // Nhấn mạnh đường phân cách dưới header
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        header.setBackground(new Color(0x0F172A)); // Đồng nhất với Sidebar
        header.setPreferredSize(new Dimension(0, 60));

        // ── Góc TRÁI: Dành cho title hoặc breadcrumbs (Bỏ logo để layout sidebar trơn
        // tru) ──
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 15));
        leftPanel.setOpaque(false);

        JLabel headerTitle = new JLabel("HỆ THỐNG QUẢN LÝ KHÁCH SẠN");
        headerTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        headerTitle.setForeground(new Color(255, 255, 255, 200));
        leftPanel.add(headerTitle);

        // ── Góc PHẢI: đồng hồ + avatar + tên (click → dropdown menu) ──
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        right.setOpaque(false);

        lblCurrentTime = new JLabel();
        lblCurrentTime.setForeground(new Color(0xA0A8C0));
        lblCurrentTime.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        NhanVien user = AuthService.getInstance().getCurrentUser();
        String fullName = (user != null && user.getHoTen() != null && !user.getHoTen().trim().isEmpty())
                ? user.getHoTen().trim()
                : "Admin";
        String initials = fullName.substring(0, 1).toUpperCase();
        final NhanVien finalUser = user;

        // Avatar tròn
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(67, 97, 238));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(initials, (getWidth() - fm.stringWidth(initials)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        avatar.setPreferredSize(new Dimension(36, 36));
        avatar.setOpaque(false);
        avatar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        avatar.setToolTipText(fullName);

        JLabel lblName = new JLabel("<html>" + fullName + " <span style='font-size:9px'>&#9660;</span></html>");
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblName.setForeground(Color.WHITE);
        lblName.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Vùng click chung (avatar + tên)
        MouseAdapter openMenu = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showUserDropdownMenu(avatar, finalUser);
            }
        };
        avatar.addMouseListener(openMenu);
        lblName.addMouseListener(openMenu);

        notificationBell = new ui.components.GlobalNotificationBell(this);
        right.add(notificationBell);
        right.add(lblCurrentTime);

        // === PHÂN QUYỀN: Badge vai trò trong header ===
        VaiTro currentRole = AuthService.getInstance().getCurrentRole();
        String roleName = currentRole == VaiTro.ADMIN ? "Quản trị viên"
                : currentRole == VaiTro.MANAGER ? "Quản lý" : "Lễ tân";
        Color roleColor = currentRole == VaiTro.ADMIN ? new Color(0x7C3AED)
                : currentRole == VaiTro.MANAGER ? new Color(0x0EA5E9) : new Color(0x10B981);

        JLabel lblRole = new JLabel(roleName) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(roleColor.getRed(), roleColor.getGreen(), roleColor.getBlue(), 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblRole.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblRole.setForeground(roleColor);
        lblRole.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        lblRole.setOpaque(false);
        right.add(lblRole);

        right.add(avatar);
        right.add(lblName);

        header.add(leftPanel, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    /** Dropdown 3 lựa chọn khi click vào avatar / tên */
    private void showUserDropdownMenu(java.awt.Component invoker, NhanVien user) {
        JPopupMenu menu = new JPopupMenu() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        menu.setOpaque(false);
        menu.setBackground(new Color(0, 0, 0, 0));
        menu.setBorder(BorderFactory.createCompoundBorder(
                new ui.components.RoundedComponents.RoundedBorder(12, new Color(0xE2E8F0)),
                BorderFactory.createEmptyBorder(4, 0, 4, 0)));

        JMenuItem miInfo = styledMenuItem("Hồ sơ cá nhân", "INFO", new Color(0x1E293B), false);
        JMenuItem miShift = styledMenuItem("Bàn giao ca", "SHIFT", new Color(0x1E293B), false);
        JMenuItem miExpense = styledMenuItem("Phiếu chi (Petty Cash)", "EXPENSE", new Color(0x1E293B), false);
        JMenuItem miPwd = styledMenuItem("Đổi mật khẩu", "PWD", new Color(0x1E293B), false);
        JMenuItem miOut = styledMenuItem("Đăng xuất hệ thống", "LOGOUT", new Color(0xEF4444), true);

        miInfo.addActionListener(e -> showUserProfileDialog(user));
        miShift.addActionListener(e -> new ui.dialogs.HandoverDialog(this).setVisible(true));
        miExpense.addActionListener(e -> new ui.dialogs.ExpenseDialog(this).setVisible(true));
        miPwd.addActionListener(e -> showChangePasswordDialog());
        miOut.addActionListener(e -> doLogout());

        menu.add(miInfo);
        menu.add(miShift);
        menu.add(miExpense);
        menu.addSeparator();
        menu.add(miPwd);
        menu.add(miOut);

        menu.pack();
        int menuW = Math.max(240, menu.getPreferredSize().width);
        menu.setPreferredSize(new Dimension(menuW, menu.getPreferredSize().height));
        menu.show(invoker, invoker.getWidth() - menuW, invoker.getHeight() + 8);
    }

    private JMenuItem styledMenuItem(String text, String type, Color fg, boolean bold) {
        JMenuItem item = new JMenuItem(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                ButtonModel model = getModel();
                if (model.isArmed() || model.isRollover()) {
                    g2.setColor(new Color(0xF1F5F9));
                    g2.fillRoundRect(4, 2, getWidth() - 8, getHeight() - 4, 8, 8);
                }

                // Draw Vector Icon
                int size = 18;
                int x = 16;
                int y = (getHeight() - size) / 2;
                g2.setColor(model.isArmed() || model.isRollover() ? fg : new Color(0x64748B));
                if (type.equals("LOGOUT"))
                    g2.setColor(new Color(0xEF4444));

                g2.setStroke(new BasicStroke(1.5f));
                switch (type) {
                    case "INFO":
                        g2.drawOval(x + 4, y + 2, 10, 10);
                        g2.drawArc(x, y + 10, 18, 10, 0, 180);
                        break;
                    case "SHIFT":
                        g2.drawOval(x, y, 16, 16);
                        g2.drawLine(x + 8, y + 8, x + 8, y + 4);
                        g2.drawLine(x + 8, y + 8, x + 12, y + 8);
                        break;
                    case "EXPENSE":
                        g2.drawRect(x, y + 2, 18, 12);
                        g2.drawOval(x + 6, y + 5, 6, 6);
                        break;
                    case "PWD":
                        g2.drawRect(x + 2, y + 6, 14, 10);
                        g2.drawArc(x + 5, y + 1, 8, 10, 0, 180);
                        break;
                    case "LOGOUT":
                        g2.drawArc(x, y, 16, 16, 120, 300);
                        g2.drawLine(x + 8, y, x + 8, y + 8);
                        break;
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };
        item.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, 14));
        item.setForeground(fg);
        item.setOpaque(false);
        item.setContentAreaFilled(false);
        item.setBorder(BorderFactory.createEmptyBorder(0, 48, 0, 16));
        item.setPreferredSize(new Dimension(240, 44));
        return item;
    }

    /** Load icon từ đường dẫn file và gắn vào JMenuItem với kích thước size×size */
    private void setMenuIcon(JMenuItem item, String path, int size) {
        java.io.File f = new java.io.File(path);
        if (f.exists()) {
            ImageIcon raw = new ImageIcon(f.getAbsolutePath());
            Image scaled = raw.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
            item.setIcon(new ImageIcon(scaled));
        }
        // Nếu file không tồn tại → giữ nguyên không có icon (không báo lỗi)
    }

    /** Dialog xem thông tin chi tiết nhân viên */
    private void showUserProfileDialog(NhanVien user) {
        JDialog dlg = new JDialog(this, "Hồ sơ cá nhân", true);
        dlg.setSize(420, 500);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        // ── Dữ liệu ──
        String name = user != null && user.getHoTen() != null ? user.getHoTen() : "Admin";
        String maNV = user != null ? user.getMaNhanVien() : "—";
        String sdt = user != null && user.getSdt() != null ? user.getSdt() : "—";
        String chucVu = user != null && user.getChucVu() != null ? user.getChucVu() : "—";
        entity.TaiKhoan acc = AuthService.getInstance().getCurrentAccount();
        String tenDN = acc != null ? acc.getTenDangNhap() : "—";
        String vaiTro = acc != null && acc.getVaiTro() != null
                ? (acc.getVaiTro() == entity.enums.VaiTro.ADMIN ? "Quản trị viên"
                        : acc.getVaiTro() == entity.enums.VaiTro.MANAGER ? "Quản lý"
                                : "Nhân viên")
                : "Nhân viên";
        // Màu badge vai trò
        Color badgeColor = acc != null && acc.getVaiTro() != null
                && acc.getVaiTro() == entity.enums.VaiTro.ADMIN ? new Color(0x7C3AED)
                        : acc != null && acc.getVaiTro() != null
                                && acc.getVaiTro() == entity.enums.VaiTro.MANAGER ? new Color(0x0EA5E9)
                                        : new Color(0x10B981);

        // Initials avatar
        String initials = name.trim().isEmpty() ? "?"
                : (name.contains(" ")
                        ? ("" + name.charAt(0) + name.charAt(name.lastIndexOf(' ') + 1)).toUpperCase()
                        : name.substring(0, Math.min(2, name.length())).toUpperCase());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // ─────────────────────────────────────────
        // A. HEADER — avatar + tên + vai trò
        // ─────────────────────────────────────────
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Gradient nền header
                java.awt.GradientPaint gp = new java.awt.GradientPaint(
                        0, 0, new Color(0x1E2337), getWidth(), getHeight(), new Color(0x2D3875));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Vòng tròn trang trí góc phải
                g2.setColor(new Color(255, 255, 255, 12));
                g2.fillOval(getWidth() - 120, -60, 200, 200);
                g2.fillOval(getWidth() - 60, getHeight() - 40, 120, 120);
                g2.dispose();
            }
        };
        header.setPreferredSize(new Dimension(0, 175));
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(28, 0, 20, 0));

        // Avatar tròn lớn
        JPanel avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Vòng ngoài (ring)
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillOval(2, 2, getWidth() - 4, getHeight() - 4);
                // Nền avatar
                g2.setColor(new Color(67, 97, 238));
                g2.fillOval(6, 6, getWidth() - 12, getHeight() - 12);
                // Text
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 26));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(initials,
                        (getWidth() - fm.stringWidth(initials)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        avatarPanel.setOpaque(false);
        avatarPanel.setPreferredSize(new Dimension(72, 72));
        avatarPanel.setMaximumSize(new Dimension(72, 72));
        avatarPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblName = new JLabel(name);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblName.setForeground(Color.WHITE);
        lblName.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Badge vai trò (pill shape)
        JLabel badge = new JLabel(vaiTro) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(badgeColor.getRed(), badgeColor.getGreen(), badgeColor.getBlue(), 50));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setForeground(new Color(200, 220, 255));
        badge.setBorder(BorderFactory.createEmptyBorder(3, 12, 3, 12));
        badge.setOpaque(false);
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(avatarPanel);
        header.add(Box.createVerticalStrut(10));
        header.add(lblName);
        header.add(Box.createVerticalStrut(6));
        header.add(badge);

        // ─────────────────────────────────────────
        // B. BODY — card thông tin
        // ─────────────────────────────────────────
        JPanel body = new JPanel();
        body.setBackground(Color.WHITE);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(20, 24, 8, 24));

        // Card thông tin với border nhẹ
        JPanel card = new JPanel();
        card.setBackground(new Color(0xF8FAFC));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE2E8F0), 1),
                BorderFactory.createEmptyBorder(6, 0, 6, 0)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Các dòng thông tin
        String[][] rows = {
                { "NV", "Mã nhân viên", maNV },
                { "ID", "Chức vụ", chucVu },
                { "PH", "SĐT", sdt },
                { "TK", "Tài khoản", tenDN },
        };
        for (int i = 0; i < rows.length; i++) {
            JPanel row = new JPanel(new BorderLayout(0, 0));
            row.setOpaque(i % 2 == 0);
            row.setBackground(Color.WHITE);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            row.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

            // Icon tag nhỏ
            JLabel tag = new JLabel(rows[i][0]);
            tag.setFont(new Font("Segoe UI", Font.BOLD, 9));
            tag.setForeground(new Color(67, 97, 238));
            tag.setBackground(new Color(67, 97, 238, 18));
            tag.setOpaque(true);
            tag.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            tag.setPreferredSize(new Dimension(32, 20));
            tag.setHorizontalAlignment(SwingConstants.CENTER);
            JPanel tagWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            tagWrap.setOpaque(false);
            tagWrap.setPreferredSize(new Dimension(50, 24));
            tagWrap.add(tag);

            JLabel keyLbl = new JLabel(rows[i][1]);
            keyLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            keyLbl.setForeground(new Color(0x64748B));
            keyLbl.setPreferredSize(new Dimension(100, 24));

            JLabel valLbl = new JLabel(rows[i][2]);
            valLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            valLbl.setForeground(new Color(0x1E2337));

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            left.setOpaque(false);
            left.add(tagWrap);
            left.add(keyLbl);

            row.add(left, BorderLayout.WEST);
            row.add(valLbl, BorderLayout.EAST);

            card.add(row);
            if (i < rows.length - 1) {
                JSeparator sep = new JSeparator();
                sep.setForeground(new Color(0xE2E8F0));
                sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                card.add(sep);
            }
        }

        body.add(card);

        // ─────────────────────────────────────────
        // C. FOOTER — buttons
        // ─────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xE2E8F0)));

        JButton btnChangePwd = new JButton("Đổi mật khẩu");
        btnChangePwd.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnChangePwd.setBackground(new Color(0xF1F5F9));
        btnChangePwd.setForeground(new Color(0x374151));
        btnChangePwd.setFocusPainted(false);
        btnChangePwd.setBorderPainted(false);
        btnChangePwd.setPreferredSize(new Dimension(130, 36));
        btnChangePwd.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnChangePwd.addActionListener(e -> {
            dlg.dispose();
            showChangePasswordDialog();
        });

        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnClose.setBackground(new Color(67, 97, 238));
        btnClose.setForeground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.setBorderPainted(false);
        btnClose.setPreferredSize(new Dimension(90, 36));
        btnClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dlg.dispose());

        footer.add(btnChangePwd);
        footer.add(btnClose);

        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    // ===== SIDEBAR =====
    private JPanel buildSidebar() {
        if (sidebar == null) {
            sidebar = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    // Đường kẻ phân cách tinh tế giữa sidebar và content
                    g.setColor(new Color(255, 255, 255, 5));
                    g.fillRect(getWidth() - 1, 0, 1, getHeight());
                }
            };
            sidebar.setPreferredSize(new Dimension(280, 0));
            sidebar.setBackground(new Color(0x0F172A));
            sidebar.setLayout(new BorderLayout());
        }

        sidebar.removeAll();
        menuUpdaters.clear();

        // ── Logo Section (Sleeker)
        JPanel logoSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 25));
        logoSection.setOpaque(false);

        JPanel logoBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x6366F1), getWidth(), getHeight(), new Color(0x4F46E5)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
                String t = "H";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(t, (getWidth() - fm.stringWidth(t)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        logoBox.setPreferredSize(new Dimension(46, 46));

        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));
        JLabel hotelName = new JLabel("Hotel Pro");
        hotelName.setFont(new Font("Segoe UI", Font.BOLD, 20));
        hotelName.setForeground(Color.WHITE);
        JLabel version = new JLabel("Enterprise Edition");
        version.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        version.setForeground(new Color(0x64748B));
        textBlock.add(hotelName);
        textBlock.add(version);

        logoSection.add(logoBox);
        logoSection.add(textBlock);

        // ── Menu List with Scrollable container
        JPanel menuContainer = new JPanel();
        menuContainer.setOpaque(false);
        menuContainer.setLayout(new BoxLayout(menuContainer, BoxLayout.Y_AXIS));
        menuContainer.setBorder(BorderFactory.createEmptyBorder(0, 16, 20, 16));

        String[][] menus = {
                { "tongquan", "Tổng quan", "\uD83C\uDFE0", null },
                { "kehoach", "Kế hoạch sử dụng", "\uD83D\uDCC6", null },
                { "datphong", "Đặt phòng", "\uD83D\uDCC5", null },
                { "thuephong", "Sơ đồ phòng", "\uD83D\uDECF", null },
                { "hoadon", "Lịch sử Hóa đơn", "\uD83E\uDDFE", null },
                { "SEP", "HỆ THỐNG", "", null },
                { "khachhang", "Khách hàng", "\uD83D\uDC65", null },
                { "qlhethong", "Cấu hình hệ thống", "\u2699", "GROUP" },
                { "loaiphong", "Loại phòng", "\u2022", "qlhethong" },
                { "phong", "Phòng", "\u2022", "qlhethong" },
                { "dichvu", "Dịch vụ", "\u2022", "qlhethong" },
                { "banggia", "Bảng giá", "\u2022", "qlhethong" },
                { "nhatky", "Nhật ký hệ thống", "\u2022", "qlhethong" },
                { "lichsuca", "Lịch sử giao ca", "\u2022", "qlhethong" },
                { "khuyenmai", "Khuyến mãi & Voucher", "\u2022", "qlhethong" },
                { "thongke", "Thống kê báo cáo", "\uD83D\uDCCA", null },
                { "nhanvien", "Tài khoản nhân sự", "\uD83D\uDEE1", null }
        };

        AuthService authSvc = AuthService.getInstance();
        for (String[] m : menus) {
            String key = m[0], label = m[1], icon = m[2], parent = m[3];
            if (!"SEP".equals(key) && !authSvc.hasAccess(key))
                continue;

            if ("SEP".equals(key)) {
                if (!authSvc.isManager())
                    continue; // Hide System separator for Staff
                menuContainer.add(Box.createVerticalStrut(20));
                JLabel lbl = new JLabel(label);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lbl.setForeground(new Color(0x64748B));
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 16, 8, 0));
                lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
                JPanel sepPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                sepPanel.setOpaque(false);
                sepPanel.add(lbl);
                sepPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                menuContainer.add(sepPanel);
                continue;
            }

            if (parent != null && !"GROUP".equals(parent)) {
                if (!expandedGroups.contains(parent))
                    continue;
            }

            int indent = (parent != null && !"GROUP".equals(parent)) ? 24 : 0;
            JPanel item = buildMenuItem(key, label, icon, indent, "GROUP".equals(parent));
            menuContainer.add(item);
            menuContainer.add(Box.createVerticalStrut(2)); // Narrow gap for all items
        }

        // Push everything to the top
        menuContainer.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(menuContainer);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(0x334155);
                this.trackColor = new Color(0, 0, 0, 0);
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton jbutton = new JButton();
                jbutton.setPreferredSize(new Dimension(0, 0));
                jbutton.setMinimumSize(new Dimension(0, 0));
                jbutton.setMaximumSize(new Dimension(0, 0));
                return jbutton;
            }
        });
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // ── Footer Profile (Advanced)
        JPanel footer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 0), 0, -20, new Color(0x0F172A)));
                g2.fillRect(0, 0, getWidth(), 20);
                g2.dispose();
            }
        };
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Remove settings link as requested

        sidebar.add(logoSection, BorderLayout.NORTH);
        sidebar.add(scroll, BorderLayout.CENTER);
        sidebar.add(footer, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel buildMenuItem(String key, String label, String icon, int indent, boolean isGroup) {
        boolean[] hover = { false };

        JPanel item = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean isActive = (activeMenu.equals(key)
                        || (key.equals("nhanvien") && activeMenu.equals("taikhoan")));

                if (isActive) {
                    g2.setPaint(new GradientPaint(0, 0, new Color(0x6366F1), getWidth(), 0, new Color(0x4F46E5)));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                } else if (hover[0]) {
                    g2.setColor(new Color(255, 255, 255, 10));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                }

                if (indent > 0) {
                    g2.setColor(new Color(0x334155));
                    g2.setStroke(new BasicStroke(1.5f));
                    // Đường kẻ dọc từ trên xuống giữa
                    g2.drawLine(-22, -10, -22, getHeight() / 2);
                    // Đường kẻ ngang vào tâm icon
                    g2.drawLine(-22, getHeight() / 2, -8, getHeight() / 2);
                }
                g2.dispose();
            }
        };

        item.setOpaque(false);
        int itemH = indent > 0 ? 36 : 46;
        item.setPreferredSize(new Dimension(248, itemH));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, itemH));
        item.setBorder(BorderFactory.createEmptyBorder(0, indent > 0 ? indent + 8 : 12, 0, 16));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Fixed-width Icon container for perfect alignment
        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, indent > 0 ? 12 : 18));
        lblIcon.setPreferredSize(new Dimension(30, itemH));
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
        lblIcon.setForeground(new Color(0x94A3B8));

        JLabel lblText = new JLabel(label);
        lblText.setFont(new Font("Segoe UI", Font.PLAIN, indent > 0 ? 13 : 15));
        lblText.setForeground(new Color(0xCBD5E1));

        JPanel pnlArrow = null;
        if (isGroup) {
            pnlArrow = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    boolean active = activeMenu.equals(key)
                            || (key.equals("nhanvien") && activeMenu.equals("taikhoan"));
                    g2.setColor(active ? Color.WHITE : new Color(0x64748B));

                    int w = getWidth(), h = getHeight();
                    int size = 8;
                    int x = (w - size) / 2;
                    int y = (h - size) / 2;

                    Path2D path = new Path2D.Double();
                    if (expandedGroups.contains(key)) {
                        // Down arrow
                        path.moveTo(x, y + 2);
                        path.lineTo(x + size, y + 2);
                        path.lineTo(x + size / 2.0, y + size);
                    } else {
                        // Right arrow
                        path.moveTo(x + 2, y);
                        path.lineTo(x + size, y + size / 2.0);
                        path.lineTo(x + 2, y + size);
                    }
                    path.closePath();
                    g2.fill(path);
                    g2.dispose();
                }
            };
            pnlArrow.setOpaque(false);
            pnlArrow.setPreferredSize(new Dimension(30, itemH));
            item.add(pnlArrow, BorderLayout.EAST);
        }

        final JPanel finalArrow = pnlArrow;
        Runnable updateUI = () -> {
            boolean active = activeMenu.equals(key) || (key.equals("nhanvien") && activeMenu.equals("taikhoan"));
            lblText.setForeground(active ? Color.WHITE : (hover[0] ? Color.WHITE : new Color(0xCBD5E1)));
            lblText.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, indent > 0 ? 14 : 15));
            lblIcon.setForeground(active ? Color.WHITE : (hover[0] ? Color.WHITE : new Color(0x94A3B8)));
            if (finalArrow != null) {
                finalArrow.repaint();
            }
            item.repaint();
        };

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover[0] = true;
                updateUI.run();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover[0] = false;
                updateUI.run();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (isGroup) {
                    if (expandedGroups.contains(key))
                        expandedGroups.remove(key);
                    else
                        expandedGroups.add(key);
                    buildSidebar();
                    sidebar.revalidate();
                    sidebar.repaint();
                } else {
                    navigateTo(key);
                }
            }
        });

        menuUpdaters.add(updateUI);
        updateUI.run();

        item.add(lblIcon, BorderLayout.WEST);
        item.add(lblText, BorderLayout.CENTER);

        return item;
    }

    // ===== CONTENT (GIỮ NGUYÊN) =====
    private JPanel buildContent() {
        cardLayout = new CardLayout();
        contentArea = new JPanel(cardLayout);

        // Lazy-load: render shell first to avoid freezing right after login.
        JPanel loading = new JPanel(new GridBagLayout());
        loading.setOpaque(false);
        JLabel lbl = new JLabel("Đang tải giao diện...");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lbl.setForeground(new Color(0x64748B));
        loading.add(lbl);
        contentArea.add(loading, "loading");

        // Do not construct heavy panels here to keep login transition responsive.
        cardLayout.show(contentArea, "loading");
        return contentArea;
    }

    public DatPhongPanel getDatPhongPanel() {
        return pDatPhong;
    }

    public ThuePhongPanel getThuePhongPanel() {
        return pThuePhong;
    }

    public HoaDonPanel getHoaDonPanel() {
        return pHoaDon;
    }

    public KeHoachPanel getKeHoachPanel() {
        return pKeHoach;
    }

    public void navigateTo(String key) {
        navigateTo(key, null);
    }

    public void navigateTo(String key, Runnable postAction) {
        if (key == null)
            return;

        // Chuẩn hóa key (Ví dụ: taikhoan và nhanvien là một bản chất màn hình)
        final String rawKey = key.trim().toLowerCase();
        final String finalKey = (rawKey.equals("taikhoan")) ? "nhanvien" : rawKey;

        if (!AuthService.getInstance().hasAccess(finalKey)) {
            NotificationManager.showWarning("Truy cập bị từ chối", "Bạn không có quyền truy cập chức năng này!");
            return;
        }

        if (activeMenu.equals(finalKey) && contentArea.getComponentCount() > 1) {
            ensurePanelLoaded(finalKey, true); // Force refresh if clicked again
            return;
        }

        long now = System.currentTimeMillis();
        boolean timeToRefresh = (now - lastRefreshAt.getOrDefault(finalKey, 0L)) > REFRESH_COOLDOWN_MS;

        Cursor wait = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
        Cursor def = Cursor.getDefaultCursor();
        setCursor(wait);

        // Chạy trên thread riêng để UI vẫn smooth
        SwingWorker<JPanel, Void> worker = new SwingWorker<JPanel, Void>() {
            @Override
            protected JPanel doInBackground() throws Exception {
                return null;
            }

            @Override
            protected void done() {
                try {
                    ensurePanelLoaded(finalKey, timeToRefresh);

                    cardLayout.show(contentArea, finalKey);
                    activeMenu = finalKey;

                    if (timeToRefresh)
                        lastRefreshAt.put(finalKey, System.currentTimeMillis());

                    // Mặc định refresh Tổng Quan mỗi lần chuyển lại
                    if (finalKey.equals("tongquan") && pTongQuan != null)
                        pTongQuan.refresh();

                    // Cập nhật lại màu sắc của Sidebar Items!
                    for (Runnable r : menuUpdaters)
                        r.run();

                    revalidate();
                    repaint();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    NotificationManager.showError("Lỗi hệ thống", "Lỗi khi tải giao diện: " + ex.getMessage());
                } finally {
                    setCursor(def);
                }

                if (postAction != null) {
                    postAction.run();
                }
            }
        };
        worker.execute();
    }

    private void ensurePanelLoaded(String key, boolean refreshData) {
        switch (key) {
            case "tongquan":
                if (pTongQuan == null) {
                    pTongQuan = new TongQuanPanel(this);
                    contentArea.add(pTongQuan, key);
                } else if (refreshData)
                    pTongQuan.refresh();
                break;
            case "kehoach":
                if (pKeHoach == null) {
                    pKeHoach = new KeHoachPanel(this);
                    contentArea.add(pKeHoach, key);
                } else if (refreshData) {
                    pKeHoach.resetFilters();
                }
                break;
            case "thuephong":
                if (pThuePhong == null) {
                    pThuePhong = new ThuePhongPanel(this);
                    contentArea.add(pThuePhong, key);
                } else if (refreshData) {
                    pThuePhong.resetFilters();
                }
                break;
            case "khachhang":
                if (pKhachHang == null) {
                    pKhachHang = new KhachHangPanel(this);
                    contentArea.add(pKhachHang, key);
                } else if (refreshData)
                    pKhachHang.resetFilters();
                break;
            case "loaiphong":
                if (pLoaiPhong == null) {
                    pLoaiPhong = new LoaiPhongPanel();
                    contentArea.add(pLoaiPhong, key);
                } else if (refreshData)
                    pLoaiPhong.resetFilters();
                break;
            case "phong":
                if (pPhong == null) {
                    pPhong = new PhongPanel();
                    contentArea.add(pPhong, key);
                } else if (refreshData)
                    pPhong.resetFilters();
                break;
            case "dichvu":
                if (pDichVu == null) {
                    pDichVu = new DichVuPanel();
                    contentArea.add(pDichVu, key);
                } else if (refreshData)
                    pDichVu.resetFilters();
                break;
            case "banggia":
                if (pBangGia == null) {
                    pBangGia = new BangGiaPanel();
                    contentArea.add(pBangGia, key);
                } else if (refreshData)
                    pBangGia.resetFilters();
                break;
            case "thongke":
                if (pThongKe == null) {
                    pThongKe = new ThongKePanel(this);
                    contentArea.add(pThongKe, key);
                } else if (refreshData)
                    pThongKe.resetFilters();
                break;
            case "lichsuca":
                if (pShiftHistory == null) {
                    pShiftHistory = new ShiftHistoryPanel();
                    contentArea.add(pShiftHistory, key);
                } else if (refreshData) {
                    pShiftHistory.refresh();
                }
                break;
            case "nhatky":
                if (pLog == null) {
                    pLog = new LogPanel();
                    contentArea.add(pLog, key);
                }
                break;
            case "nhanvien":
                if (pNhanVien == null) {
                    pNhanVien = new NhanVienPanel(this);
                    contentArea.add(pNhanVien, key);
                } else if (refreshData) {
                    pNhanVien.resetFilters();
                }
                break;
            case "datphong":
                if (pDatPhong == null) {
                    pDatPhong = new DatPhongPanel(this);
                    contentArea.add(pDatPhong, key);
                } else if (refreshData)
                    pDatPhong.resetFilters();
                break;
            case "hoadon":
                if (pHoaDon == null) {
                    pHoaDon = new HoaDonPanel(this);
                    contentArea.add(pHoaDon, key);
                } else if (refreshData)
                    pHoaDon.resetFilters();
                break;
            case "khuyenmai":
                if (pKhuyenMai == null) {
                    pKhuyenMai = new KhuyenMaiPanel();
                    contentArea.add(pKhuyenMai, key);
                } else if (refreshData && pKhuyenMai instanceof ResettableFilter) {
                    ((ResettableFilter) pKhuyenMai).resetFilters();
                }
                break;
        }
    }

    public void refreshThongKe() {
        ensurePanelLoaded("thongke", false);
        if (pThongKe != null)
            pThongKe.refresh();
    }

    /**
     * Thông báo dữ liệu đã thay đổi (vd: sau khi thanh toán, đặt phòng).
     * Sẽ ép buộc các panel thống kê load lại số liệu mới ở lần xem tới.
     */
    public void notifyDataChanged() {
        lastRefreshAt.put("tongquan", 0L);
        lastRefreshAt.put("thongke", 0L);
        lastRefreshAt.put("hoadon", 0L);
        lastRefreshAt.put("thuephong", 0L);

        // Nếu đang ở màn hình chính, refresh ngay lập tức
        if ("tongquan".equals(activeMenu) && pTongQuan != null)
            pTongQuan.refresh();
        if ("thongke".equals(activeMenu) && pThongKe != null)
            pThongKe.refresh();
        if ("thuephong".equals(activeMenu) && pThuePhong != null) {
            pThuePhong.rebuildFilter();
            pThuePhong.refreshGrid();
        }
    }

    private void startClock() {
        new Timer(1000, e -> {
            String time = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new Date());
            lblCurrentTime.setText(time);
        }).start();
    }

    private void doLogout() {
        if (AuthService.getInstance().getCurrentShift() != null) {
            int confirmShift = JOptionPane.showConfirmDialog(this,
                    "Bạn đang trong ca làm việc chưa bàn giao.\nBạn có muốn thực hiện bàn giao ngay không?",
                    "Cảnh báo bàn giao ca",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirmShift == JOptionPane.YES_OPTION) {
                new ui.dialogs.HandoverDialog(this).setVisible(true);
                if (AuthService.getInstance().getCurrentShift() != null)
                    return; // Nếu vẫn chưa bàn giao thì ko logout
            } else if (confirmShift == JOptionPane.CANCEL_OPTION) {
                return;
            }
            // Nếu chọn NO_OPTION thì tiếp tục logout mà ko bàn giao (linh hoạt)
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn đăng xuất?",
                "Xác nhận",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            AuthService.getInstance().dangXuat();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    private void showChangePasswordDialog() {
        JDialog dlg = new JDialog(this, "Đổi mật khẩu", true);
        dlg.setSize(420, 520);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.getContentPane().setBackground(Color.WHITE);

        // ── Layout gốc ──
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // ── Header xanh đậm ──
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x1E2337));
        header.setPreferredSize(new Dimension(0, 90));
        header.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JLabel iconLbl = new JLabel("\uD83D\uDD12") {
            @Override
            protected void paintComponent(Graphics g) {
                // Vẽ vòng tròn nền
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(67, 97, 238, 60));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
        iconLbl.setPreferredSize(new Dimension(54, 54));
        iconLbl.setHorizontalAlignment(SwingConstants.CENTER);
        iconLbl.setVerticalAlignment(SwingConstants.CENTER);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel t1 = new JLabel("Đổi mật khẩu");
        t1.setFont(new Font("Segoe UI", Font.BOLD, 17));
        t1.setForeground(Color.WHITE);
        JLabel t2 = new JLabel("Cập nhật mật khẩu tài khoản của bạn");
        t2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t2.setForeground(new Color(0xA0A8C0));
        titleBox.add(t1);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(t2);

        header.add(iconLbl, BorderLayout.WEST);
        header.add(Box.createHorizontalStrut(14), BorderLayout.CENTER);
        JPanel titleWrap = new JPanel(new GridBagLayout());
        titleWrap.setOpaque(false);
        titleWrap.add(titleBox);
        header.add(titleWrap, BorderLayout.CENTER);

        // ── Body form ──
        JPanel body = new JPanel();
        body.setBackground(Color.WHITE);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(22, 28, 8, 28));

        // Helper: tạo field group (label + field + error label)
        JPasswordField txtOld = makePwdField();
        JPasswordField txtNew = makePwdField();
        JPasswordField txtCfm = makePwdField();

        JLabel errOld = errLabel(); // lỗi mật khẩu cũ
        JLabel errNew = errLabel(); // lỗi mật khẩu mới
        JLabel errCfm = errLabel(); // lỗi xác nhận

        // Thanh độ mạnh mật khẩu
        JPanel strengthBar = new JPanel(new GridLayout(1, 4, 4, 0));
        strengthBar.setOpaque(false);
        strengthBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        strengthBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel[] segs = new JPanel[4];
        for (int i = 0; i < 4; i++) {
            segs[i] = new JPanel();
            segs[i].setBackground(new Color(0xE2E8F0));
            segs[i].setOpaque(true);
            segs[i].setPreferredSize(new Dimension(0, 5));
            strengthBar.add(segs[i]);
        }
        JLabel strengthTxt = new JLabel(" ");
        strengthTxt.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        strengthTxt.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Lắng nghe mật khẩu mới → cập nhật thanh độ mạnh
        txtNew.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void update() {
                String p = new String(txtNew.getPassword());
                int score = 0;
                if (p.length() >= 8)
                    score++;
                if (p.matches(".*[A-Z].*"))
                    score++;
                if (p.matches(".*[0-9].*"))
                    score++;
                if (p.matches(".*[^A-Za-z0-9].*"))
                    score++;
                Color[] clrs = { new Color(0xEF4444), new Color(0xF97316), new Color(0xEAB308), new Color(0x22C55E) };
                String[] txts = { "Rất yếu", "Yếu", "Trung bình", "Mạnh" };
                for (int i = 0; i < 4; i++)
                    segs[i].setBackground(i < score ? clrs[score - 1] : new Color(0xE2E8F0));
                strengthTxt.setText(p.isEmpty() ? " " : txts[Math.max(0, score - 1)]);
                strengthTxt.setForeground(p.isEmpty() ? Color.GRAY : clrs[Math.max(0, score - 1)]);
                strengthBar.repaint();
                // xoá lỗi khi đang nhập
                errNew.setText(" ");
                errCfm.setText(" ");
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                update();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                update();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
            }
        });

        body.add(fieldGroup("Mật khẩu hiện tại", txtOld, errOld));
        body.add(Box.createVerticalStrut(10));
        body.add(fieldGroup("Mật khẩu mới", txtNew, null));
        // Thêm thanh độ mạnh
        body.add(Box.createVerticalStrut(5));
        body.add(strengthBar);
        body.add(Box.createVerticalStrut(2));
        body.add(strengthTxt);
        body.add(errNew);
        body.add(Box.createVerticalStrut(10));
        body.add(fieldGroup("Xác nhận mật khẩu mới", txtCfm, errCfm));

        // Ghi chú yêu cầu
        JLabel hint = new JLabel("<html><font color='#64748B'>Mật khẩu phải có ít nhất <b>8 ký tự</b>,"
                + " gồm <b>chữ hoa</b>, <b>chữ số</b> và <b>ký tự đặc biệt</b>.</font></html>");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        hint.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xE2E8F0)),
                BorderFactory.createEmptyBorder(10, 0, 0, 0)));
        body.add(Box.createVerticalStrut(10));
        body.add(hint);

        // ── Buttons ──
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 16));
        btnRow.setBackground(Color.WHITE);
        btnRow.setBorder(BorderFactory.createEmptyBorder(0, 20, 4, 20));

        JButton btnCancel = new JButton("Huỷ");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCancel.setBackground(new Color(0xF1F5F9));
        btnCancel.setForeground(new Color(0x374151));
        btnCancel.setFocusPainted(false);
        btnCancel.setBorderPainted(false);
        btnCancel.setPreferredSize(new Dimension(90, 36));
        btnCancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCancel.addActionListener(e -> dlg.dispose());

        JButton btnSave = new JButton("Lưu thay đổi");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSave.setBackground(new Color(67, 97, 238));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setBorderPainted(false);
        btnSave.setPreferredSize(new Dimension(130, 36));
        btnSave.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Regex ràng buộc mật khẩu mới
        // Ít nhất 8 ký tự, có chữ hoa, chữ thường, chữ số, ký tự đặc biệt
        final java.util.regex.Pattern PWD_PATTERN = java.util.regex.Pattern.compile(
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$");

        btnSave.addActionListener(e -> {
            boolean ok = true;
            String oldPwd = new String(txtOld.getPassword());
            String newPwd = new String(txtNew.getPassword());
            String cfmPwd = new String(txtCfm.getPassword());

            // Validate mật khẩu cũ
            if (oldPwd.isEmpty()) {
                errOld.setText("Vui lòng nhập mật khẩu hiện tại");
                ok = false;
            } else {
                entity.TaiKhoan acc = AuthService.getInstance().getCurrentAccount();
                if (acc == null || !acc.getMatKhau().equals(oldPwd)) {
                    errOld.setText("Mật khẩu hiện tại không đúng");
                    ok = false;
                } else {
                    errOld.setText(" ");
                }
            }

            // Validate mật khẩu mới
            if (newPwd.isEmpty()) {
                errNew.setText("Vui lòng nhập mật khẩu mới");
                ok = false;
            } else if (!PWD_PATTERN.matcher(newPwd).matches()) {
                errNew.setText("Mật khẩu phải có ít nhất 8 ký tự, chữ hoa, chữ số và ký tự đặc biệt");
                ok = false;
            } else if (newPwd.equals(oldPwd)) {
                errNew.setText("Mật khẩu mới không được trùng mật khẩu cũ");
                ok = false;
            } else {
                errNew.setText(" ");
            }

            // Validate xác nhận
            if (cfmPwd.isEmpty()) {
                errCfm.setText("Vui lòng xác nhận mật khẩu mới");
                ok = false;
            } else if (!newPwd.equals(cfmPwd)) {
                errCfm.setText("Mật khẩu xác nhận không khớp");
                ok = false;
            } else {
                errCfm.setText(" ");
            }

            if (!ok)
                return;

            entity.TaiKhoan acc = AuthService.getInstance().getCurrentAccount();
            acc.setMatKhau(newPwd);
            new dao.TaiKhoanDAO().doiMatKhau(acc.getTenDangNhap(), newPwd);

            // Hiển thị thành công ngay trong dialog thay vì JOptionPane
            NotificationManager.showSuccess("Thành công", "Đổi mật khẩu thành công!");
            dlg.dispose();
        });

        btnRow.add(btnCancel);
        btnRow.add(btnSave);

        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        root.add(btnRow, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    /** Tạo JPasswordField chuẩn style */
    private JPasswordField makePwdField() {
        JPasswordField f = new JPasswordField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setPreferredSize(new Dimension(Integer.MAX_VALUE, 36));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setBorder(BorderFactory.createEmptyBorder());
        f.setOpaque(false);
        return f;
    }

    /** Tạo label lỗi inline (đỏ, nhỏ) */
    private JLabel errLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(new Color(0xEF4444));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    /** Tạo nhóm label + field + errLabel */
    private JPanel fieldGroup(String labelText, JPasswordField field, JLabel errLbl) {
        JPanel g = new JPanel();
        g.setOpaque(false);
        g.setLayout(new BoxLayout(g, BoxLayout.Y_AXIS));
        g.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel cap = new JLabel(labelText);
        cap.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cap.setForeground(new Color(0x374151));
        cap.setAlignmentX(Component.LEFT_ALIGNMENT);
        g.add(cap);
        g.add(Box.createVerticalStrut(5));

        // Wrapper cho field và nút hiện mật khẩu
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Color.WHITE);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        wrap.setBorder(BorderFactory.createLineBorder(new Color(0xCBD5E1), 1));

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                wrap.setBorder(BorderFactory.createLineBorder(new Color(67, 97, 238), 2));
            }
            @Override
            public void focusLost(FocusEvent e) {
                wrap.setBorder(BorderFactory.createLineBorder(new Color(0xCBD5E1), 1));
            }
        });

        JButton btnEye = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x94A3B8));
                int w = getWidth(), h = getHeight();
                int ew = 20, eh = 12;
                int x = (w - ew) / 2, y = (h - eh) / 2;
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Almond shape
                java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
                path.moveTo(x, y + eh / 2.0);
                path.quadTo(x + ew / 2.0, y - 2, x + ew, y + eh / 2.0);
                path.quadTo(x + ew / 2.0, y + eh + 2, x, y + eh / 2.0);
                g2.draw(path);

                // Pupil
                g2.fillOval(x + ew / 2 - 3, y + eh / 2 - 3, 6, 6);

                if (field.getEchoChar() != (char) 0) {
                    g2.setStroke(new BasicStroke(1.8f));
                    g2.drawLine(x, y + eh - 1, x + ew, y + 1);
                }
                g2.dispose();
            }
        };
        btnEye.setPreferredSize(new Dimension(36, 36));
        btnEye.setContentAreaFilled(false);
        btnEye.setBorderPainted(false);
        btnEye.setFocusPainted(false);
        btnEye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEye.addActionListener(e -> {
            field.setEchoChar(field.getEchoChar() == (char) 0 ? '•' : (char) 0);
            btnEye.repaint();
        });

        wrap.add(field, BorderLayout.CENTER);
        wrap.add(btnEye, BorderLayout.EAST);

        g.add(wrap);
        if (errLbl != null)
            g.add(errLbl);
        return g;
    }

    private void startNotificationTimer() {
        Timer timer = new Timer(60000, e -> refreshNotifications()); // 1 phút / lần
        timer.setInitialDelay(5000); // Đợi 5s sau khi load xong mới check lần đầu
        timer.start();
        refreshNotifications(); // Chạy ngay lần đầu
    }

    public void refreshNotifications() {
        if (notificationBell == null)
            return;

        new Thread(() -> {
            try {
                java.util.List<ui.components.GlobalNotificationBell.AlertItem> items = new java.util.ArrayList<>();

                // 1. Lấy cảnh báo từ Thống kê (Vận hành)
                java.util.List<java.util.Map<String, Object>> ops = ts.getAlerts();
                for (java.util.Map<String, Object> op : ops) {
                    String id = String.valueOf(op.getOrDefault("id", "op_" + System.currentTimeMillis()));
                    if (dismissedAlertIds.contains(id))
                        continue;

                    String title = String.valueOf(op.getOrDefault("title", ""));
                    String desc = String.valueOf(op.getOrDefault("desc", ""));
                    String type = String.valueOf(op.getOrDefault("type", "warning"));

                    Runnable action = null;
                    if (title.contains("P.")) {
                        action = () -> navigateTo("thuephong");
                    }

                    ui.components.GlobalNotificationBell.AlertItem item = new ui.components.GlobalNotificationBell.AlertItem(
                            id, title, desc, type, action);
                    item.onDismiss = () -> {
                        dismissedAlertIds.add(id);
                        saveDismissedAlerts();
                        refreshNotifications();
                    };
                    items.add(item);
                }

                // 2. Lấy cảnh báo từ Đặt phòng
                java.util.List<entity.DatPhong> noShows = ds.getNoShowBookings(2);
                for (entity.DatPhong dp : noShows) {
                    String id = "noshow_" + dp.getMaDatPhong();
                    if (dismissedAlertIds.contains(id))
                        continue;

                    ui.components.GlobalNotificationBell.AlertItem item = new ui.components.GlobalNotificationBell.AlertItem(
                            id,
                            "P." + (dp.getDsChiTiet() != null && !dp.getDsChiTiet().isEmpty()
                                    ? dp.getDsChiTiet().get(0).getPhong().getSoPhong()
                                    : "??") + " quá hạn check-in",
                            "Đơn " + dp.getMaDatPhong() + " ("
                                    + (dp.getKhachHang() != null ? dp.getKhachHang().getHoTen() : "Khách") + ")",
                            "danger", () -> navigateTo("datphong"));
                    item.onDismiss = () -> {
                        dismissedAlertIds.add(id);
                        saveDismissedAlerts();
                        refreshNotifications();
                    };
                    items.add(item);
                }

                // 2.1 Cảnh báo phòng chưa sẵn sàng (Dirty room for arrivals)
                java.util.List<entity.DatPhong> upcomingToday = ds.getUpcomingArrivals(4);
                for (entity.DatPhong dp : upcomingToday) {
                    boolean needsCleaning = false;
                    String roomNum = "";
                    if (dp.getDsChiTiet() != null) {
                        for (entity.ChiTietDatPhong ct : dp.getDsChiTiet()) {
                            if (ct.getPhong() != null
                                    && (ct.getPhong().getTrangThai() == entity.enums.TrangThaiPhong.CLEANING || ct
                                            .getPhong().getTrangThai() == entity.enums.TrangThaiPhong.MAINTENANCE)) {
                                needsCleaning = true;
                                roomNum = ct.getPhong().getSoPhong();
                                break;
                            }
                        }
                    }
                    if (needsCleaning) {
                        String id = "dirty_" + dp.getMaDatPhong();
                        if (!dismissedAlertIds.contains(id)) {
                            ui.components.GlobalNotificationBell.AlertItem item = new ui.components.GlobalNotificationBell.AlertItem(
                                    id, "P." + roomNum + " chưa dọn dẹp",
                                    "Khách sắp đến nhận phòng nhưng phòng vẫn đang bẩn.",
                                    "warning", () -> navigateTo("thuephong"));
                            item.onDismiss = () -> {
                                dismissedAlertIds.add(id);
                                saveDismissedAlerts();
                                refreshNotifications();
                            };
                            items.add(item);
                        }
                    }
                }

                // 2.2 Cảnh báo hạn nộp cọc
                java.util.List<entity.DatPhong> overdueDep = ds.getOverdueDepositBookings();
                for (entity.DatPhong dp : overdueDep) {
                    String id = "overdue_dep_" + dp.getMaDatPhong();
                    if (!dismissedAlertIds.contains(id)) {
                        ui.components.GlobalNotificationBell.AlertItem item = new ui.components.GlobalNotificationBell.AlertItem(
                                id, "⚠️ Quá hạn nộp cọc",
                                "Đơn " + dp.getMaDatPhong() + " (" + dp.getTenKhachHang() + ") đã quá hạn cọc.",
                                "danger", () -> navigateTo("datphong"));
                        item.onDismiss = () -> {
                            dismissedAlertIds.add(id);
                            saveDismissedAlerts();
                            refreshNotifications();
                        };
                        items.add(item);
                    }
                }

                java.util.List<entity.DatPhong> nearDep = ds.getUpcomingDepositDeadlines(12);
                for (entity.DatPhong dp : nearDep) {
                    String id = "near_dep_" + dp.getMaDatPhong();
                    if (!dismissedAlertIds.contains(id)) {
                        ui.components.GlobalNotificationBell.AlertItem item = new ui.components.GlobalNotificationBell.AlertItem(
                                id, "⏳ Sắp đến hạn cọc",
                                "Đơn " + dp.getMaDatPhong() + " của " + dp.getTenKhachHang() + " sắp hết hạn cọc.",
                                "warning", () -> navigateTo("datphong"));
                        item.onDismiss = () -> {
                            dismissedAlertIds.add(id);
                            saveDismissedAlerts();
                            refreshNotifications();
                        };
                        items.add(item);
                    }
                }

                java.util.List<entity.DatPhong> pending = ds.getLongPendingBookings(0);
                if (!pending.isEmpty()) {
                    String id = "pending_group_" + pending.size();
                    if (!dismissedAlertIds.contains(id)) {
                        ui.components.GlobalNotificationBell.AlertItem item = new ui.components.GlobalNotificationBell.AlertItem(
                                id,
                                pending.size() + " đơn đặt phòng chưa duyệt",
                                "Có các đơn đặt phòng mới đang chờ xác nhận.",
                                "warning", () -> navigateTo("datphong"));
                        item.onDismiss = () -> {
                            dismissedAlertIds.add(id);
                            saveDismissedAlerts();
                            refreshNotifications();
                        };
                        items.add(item);
                    }
                }

                SwingUtilities.invokeLater(() -> notificationBell.setAlerts(items));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void refreshDashboard() {
        if (pTongQuan != null) {
            pTongQuan.refresh();
        }
    }

}
