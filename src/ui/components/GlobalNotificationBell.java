package ui.components;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import ui.MainFrame;
import ui.components.UIConstants;

/**
 * Global Notification Bell icon in the Header.
 * Aggregates alerts from Operations and Bookings.
 */
public class GlobalNotificationBell extends JPanel {
    private final MainFrame mainFrame;
    private List<AlertItem> alerts = new ArrayList<>();
    private final JButton bellBtn;
    private JPopupMenu popup;

    public GlobalNotificationBell(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(false);
        setLayout(new BorderLayout());

        bellBtn = new JButton() {
            @Override
            public Dimension getPreferredSize() { return new Dimension(42, 38); }
            @Override
            public Dimension getMinimumSize()   { return getPreferredSize(); }
            @Override
            public Dimension getMaximumSize()   { return getPreferredSize(); }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int count = alerts.size();
                Color bg = count > 0 ? new Color(30, 41, 59, 180) : new Color(255, 255, 255, 10);
                Color border = count > 0 ? new Color(0x6366F1) : new Color(255, 255, 255, 30);
                
                // Background & Border
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

                // Bell Icon (Modern flat style)
                g2.setColor(count > 0 ? Color.WHITE : new Color(0x94A3B8));
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2, cy = getHeight() / 2 - 1;
                
                // Bell shape
                g2.drawArc(cx - 7, cy - 7, 14, 14, 0, 180); // top
                g2.drawLine(cx - 7, cy, cx - 7, cy + 6);    // left
                g2.drawLine(cx + 7, cy, cx + 7, cy + 6);    // right
                g2.drawLine(cx - 9, cy + 6, cx + 9, cy + 6); // base
                g2.fillOval(cx - 2, cy + 7, 4, 3);          // clapper

                // Badge
                if (count > 0) {
                    g2.setColor(new Color(0xEF4444)); // Red badge
                    g2.fillOval(cx + 4, cy - 12, 12, 12);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
                    String text = count > 9 ? "9+" : String.valueOf(count);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(text, cx + 10 - fm.stringWidth(text) / 2, cy - 3);
                }
                g2.dispose();
            }
        };

        bellBtn.setContentAreaFilled(false);
        bellBtn.setBorderPainted(false);
        bellBtn.setFocusPainted(false);
        bellBtn.setOpaque(false);
        bellBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bellBtn.setToolTipText("System notifications");

        bellBtn.addActionListener(e -> showPopup());
        add(bellBtn, BorderLayout.CENTER);
    }

    public void setAlerts(List<AlertItem> newAlerts) {
        this.alerts = newAlerts;
        bellBtn.repaint();
        bellBtn.setToolTipText(alerts.isEmpty() ? "Không có cảnh báo mới" : "Bạn có " + alerts.size() + " thông báo cần xử lý");
    }

    private void showPopup() {
        if (popup != null && popup.isVisible()) {
            popup.setVisible(false);
            return;
        }

        popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createEmptyBorder());
        popup.setBackground(Color.WHITE);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createLineBorder(new Color(0xE2E8F0)));
        panel.setPreferredSize(new Dimension(320, Math.min(450, 60 + alerts.size() * 65)));

        // Header của popup
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0xF8FAFC));
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JLabel title = new JLabel("Thông báo hệ thống (" + alerts.size() + ")");
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(new Color(0x1E293B));
        header.add(title, BorderLayout.WEST);

        if (!alerts.isEmpty()) {
            JButton btnClearAll = new JButton("Xóa tất cả");
            btnClearAll.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btnClearAll.setForeground(new Color(0x6366F1));
            btnClearAll.setContentAreaFilled(false);
            btnClearAll.setBorder(null);
            btnClearAll.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnClearAll.setFocusPainted(false);
            btnClearAll.addActionListener(e -> {
                int choice = JOptionPane.showConfirmDialog(this, 
                    "Bạn có chắc chắn muốn xóa tất cả thông báo không?", 
                    "Xác nhận", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    popup.setVisible(false);
                    for (AlertItem item : new ArrayList<>(alerts)) {
                        if (item.onDismiss != null) item.onDismiss.run();
                    }
                }
            });
            header.add(btnClearAll, BorderLayout.EAST);
        }
        
        panel.add(header, BorderLayout.NORTH);

        // Body: danh sách các thông báo
        if (alerts.isEmpty()) {
            JPanel empty = new JPanel(new GridBagLayout());
            empty.setBackground(Color.WHITE);
            JLabel lbl = new JLabel("✓ Không có thông báo nào");
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lbl.setForeground(new Color(0x10B981));
            empty.add(lbl);
            panel.add(empty, BorderLayout.CENTER);
        } else {
            JPanel list = new JPanel();
            list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
            list.setBackground(Color.WHITE);

            for (AlertItem alert : alerts) {
                list.add(createItemPanel(alert));
            }

            JScrollPane scroll = new JScrollPane(list);
            scroll.setBorder(null);
            scroll.getVerticalScrollBar().setUnitIncrement(10);
            scroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
            panel.add(scroll, BorderLayout.CENTER);
        }

        popup.add(panel);
        popup.show(bellBtn, bellBtn.getWidth() - panel.getPreferredSize().width, bellBtn.getHeight() + 4);
    }

    private JPanel createItemPanel(AlertItem item) {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Màu sắc dựa trên type
        Color accent = UIConstants.WARNING;
        if ("danger".equals(item.type)) accent = UIConstants.DANGER;
        if ("info".equals(item.type)) accent = UIConstants.INFO;
        if ("success".equals(item.type)) accent = UIConstants.SUCCESS;

        final Color finalAccent = accent;
        JPanel indicator = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(finalAccent);
                g2.fillOval(0, 0, 8, 8);
                g2.dispose();
            }
        };
        indicator.setOpaque(false);
        indicator.setPreferredSize(new Dimension(8, 8));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        left.setOpaque(false);
        left.add(indicator);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        
        JLabel t = new JLabel(item.title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.setForeground(new Color(0x1E293B));
        
        JLabel d = new JLabel("<html><body style='width: 180px;'>" + item.desc + "</body></html>");
        d.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        d.setForeground(new Color(0x64748B));

        center.add(t);
        center.add(Box.createVerticalStrut(2));
        center.add(d);

        p.add(left, BorderLayout.WEST);
        p.add(center, BorderLayout.CENTER);

        // Nút xóa (X) - mặc định ẩn, hiện khi hover
        JButton btnDismiss = new JButton("×");
        btnDismiss.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        btnDismiss.setForeground(new Color(0x94A3B8));
        btnDismiss.setContentAreaFilled(false);
        btnDismiss.setBorder(null);
        btnDismiss.setFocusPainted(false);
        btnDismiss.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnDismiss.setVisible(false);
        btnDismiss.setToolTipText("Xóa thông báo này");
        btnDismiss.addActionListener(e -> {
            popup.setVisible(false);
            if (item.onDismiss != null) item.onDismiss.run();
        });
        p.add(btnDismiss, BorderLayout.EAST);

        // Hover effect
        p.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { 
                p.setBackground(new Color(0xF1F5F9)); 
                btnDismiss.setVisible(true);
            }
            @Override public void mouseExited(MouseEvent e) { 
                p.setBackground(Color.WHITE); 
                btnDismiss.setVisible(false);
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                if (btnDismiss.getBounds().contains(e.getPoint())) return; // Tránh click nhầm vào nút xóa
                popup.setVisible(false);
                if (item.action != null) item.action.run();
            }
        });

        // Separator line
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(p, BorderLayout.CENTER);
        JPanel sep = new JPanel();
        sep.setBackground(new Color(0xF1F5F9));
        sep.setPreferredSize(new Dimension(0, 1));
        wrapper.add(sep, BorderLayout.SOUTH);

        return wrapper;
    }

    /** Model đại diện cho một thông báo */
    public static class AlertItem {
        public String id;
        public String title;
        public String desc;
        public String type; // danger, warning, info
        public Runnable action;
        public Runnable onDismiss;

        public AlertItem(String id, String title, String desc, String type, Runnable action) {
            this.id = id;
            this.title = title;
            this.desc = desc;
            this.type = type;
            this.action = action;
        }
    }
}
