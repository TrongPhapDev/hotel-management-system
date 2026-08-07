package ui.panels;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;
import ui.MainFrame;
import ui.components.RoundedComponents.RoundedButton;
import ui.components.RoundedComponents.RoundedPanel;
import ui.components.UIConstants;

/**
 * QLHeThongPanel — Hub điều hướng đến 4 panel quản lý hệ thống:
 *   - BangGiaPanel   (Cách tính tiền)
 *   - LoaiPhongPanel (Loại phòng)
 *   - PhongPanel     (Phòng)
 *   - DichVuPanel    (Dịch vụ)
 *
 * Mỗi sub-panel là một file riêng trong ui/panels.
 */
public class QLHeThongPanel extends JPanel {

    @SuppressWarnings("unused")
    private final MainFrame mainFrame;

    private CardLayout cardLayout;
    private JPanel cardArea;

    // Lazy-init sub-panels
    private BangGiaPanel   pBangGia;
    private LoaiPhongPanel pLoaiPhong;
    private PhongPanel     pPhong;
    private DichVuPanel    pDichVu;
    private LogPanel       pLog;
    private ShiftHistoryPanel pShift;

    public QLHeThongPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        cardLayout = new CardLayout();
        cardArea   = new JPanel(cardLayout);
        cardArea.setOpaque(false);

        // Card 1: Hub (trang chủ)
        cardArea.add(buildHub(), "hub");

        add(cardArea, BorderLayout.CENTER);
        cardLayout.show(cardArea, "hub");
    }

    // =====================================================================
    // HUB — trang chọn chức năng
    // =====================================================================
    private JPanel buildHub() {
        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // Title
        JPanel hdr = new JPanel();
        hdr.setOpaque(false);
        hdr.setLayout(new BoxLayout(hdr, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Hệ thống & Dịch vụ");
        title.setFont(UIConstants.FONT_TITLE);
        JLabel sub = new JLabel("Chọn một chức năng để quản lý");
        sub.setFont(UIConstants.FONT_BODY);
        sub.setForeground(UIConstants.TEXT_SECONDARY);
        hdr.add(title);
        hdr.add(Box.createVerticalStrut(4));
        hdr.add(sub);
        hdr.add(Box.createVerticalStrut(32));

        // 6 cards
        JPanel cards = new JPanel(new GridLayout(2, 3, 20, 20));
        cards.setOpaque(false);
        cards.add(hubCard("Cách tính tiền", "Bảng giá & phụ thu",     "banggia",   "./icon/save-money.png"));
        cards.add(hubCard("Loại phòng",     "Danh mục loại phòng",    "loaiphong", "./icon/bed.png"));
        cards.add(hubCard("Phòng",          "Quản lý từng phòng",     "phong",     "./icon/home.png"));
        cards.add(hubCard("Dịch vụ",        "Quản lý menu dịch vụ",   "dichvu",    "./icon/room-service.png"));
        cards.add(hubCard("Nhật ký",        "Lịch sử hoạt động",      "nhatky",    "./icon/log.png"));
        cards.add(hubCard("Lịch sử ca",     "Danh sách bàn giao ca",  "lichsuca",  "./icon/shift-history.png"));

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(cards);

        root.add(hdr,    BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        return root;
    }

    private RoundedPanel hubCard(String title, String desc, String key, String iconPath) {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(28, 24, 28, 24));
        card.setPreferredSize(new Dimension(180, 160));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel iconLbl = new JLabel();
        iconLbl.setHorizontalAlignment(SwingConstants.CENTER);
        loadIcon(iconLbl, iconPath);

        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setFont(UIConstants.FONT_BODY_BOLD);
        JLabel d = new JLabel(desc,  SwingConstants.CENTER);
        d.setFont(UIConstants.FONT_SMALL);
        d.setForeground(UIConstants.TEXT_MUTED);

        JPanel ct = new JPanel();
        ct.setOpaque(false);
        ct.setLayout(new BoxLayout(ct, BoxLayout.Y_AXIS));
        iconLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        t.setAlignmentX(Component.CENTER_ALIGNMENT);
        d.setAlignmentX(Component.CENTER_ALIGNMENT);
        ct.add(iconLbl);
        ct.add(Box.createVerticalStrut(12));
        ct.add(t);
        ct.add(Box.createVerticalStrut(4));
        ct.add(d);
        card.add(ct, BorderLayout.CENTER);

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { navSub(key); }
            @Override public void mouseEntered(MouseEvent e) { card.setBackground(UIConstants.PRIMARY_LIGHT); card.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { card.setBackground(Color.WHITE); card.repaint(); }
        });
        return card;
    }

    // =====================================================================
    // ĐIỀU HƯỚNG — chuyển đến sub‑panel tương ứng
    // =====================================================================
    private void navSub(String key) {
        ensureSubPanel(key);
        cardLayout.show(cardArea, key);
    }

    private void ensureSubPanel(String key) {
        switch (key) {
            case "banggia" -> {
                if (pBangGia == null) {
                    pBangGia = new BangGiaPanel();
                    cardArea.add(wrapWithBack(pBangGia), "banggia");
                } else {
                    pBangGia.refresh();
                }
            }
            case "loaiphong" -> {
                if (pLoaiPhong == null) {
                    pLoaiPhong = new LoaiPhongPanel();
                    cardArea.add(wrapWithBack(pLoaiPhong), "loaiphong");
                } else {
                    pLoaiPhong.refresh();
                }
            }
            case "phong" -> {
                if (pPhong == null) {
                    pPhong = new PhongPanel();
                    cardArea.add(wrapWithBack(pPhong), "phong");
                } else {
                    pPhong.refresh();
                }
            }
            case "dichvu" -> {
                if (pDichVu == null) {
                    pDichVu = new DichVuPanel();
                    cardArea.add(wrapWithBack(pDichVu), "dichvu");
                } else {
                    pDichVu.refresh();
                }
            }
            case "nhatky" -> {
                if (pLog == null) {
                    pLog = new LogPanel();
                    cardArea.add(wrapWithBack(pLog), "nhatky");
                } else {
                    pLog.refresh();
                }
            }
            case "lichsuca" -> {
                if (pShift == null) {
                    pShift = new ShiftHistoryPanel();
                    cardArea.add(wrapWithBack(pShift), "lichsuca");
                } else {
                    pShift.refresh();
                }
            }
        }
    }

    /**
     * Bọc một sub-panel bằng nút "← Quay lại" phía trên cùng.
     */
    private JPanel wrapWithBack(JPanel subPanel) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        // Back button bar
        JPanel backBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 12));
        backBar.setOpaque(false);

        RoundedButton btnBack = new RoundedButton("← Quay lại", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        btnBack.setFont(UIConstants.FONT_BODY_BOLD);
        btnBack.setPreferredSize(new Dimension(120, 36));
        btnBack.addActionListener(e -> cardLayout.show(cardArea, "hub"));
        backBar.add(btnBack);

        wrapper.add(backBar, BorderLayout.NORTH);
        wrapper.add(subPanel, BorderLayout.CENTER);
        return wrapper;
    }

    // =====================================================================
    // HELPERS
    // =====================================================================
    private void loadIcon(JLabel lbl, String iconPath) {
        File f = new File(iconPath);
        if (f.exists()) {
            try {
                ImageIcon img = new ImageIcon(iconPath);
                lbl.setIcon(new ImageIcon(img.getImage().getScaledInstance(36, 36, Image.SCALE_SMOOTH)));
            } catch (Exception ex) {
                lbl.setText("□");
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 36));
                lbl.setForeground(UIConstants.PRIMARY);
            }
        } else {
            lbl.setText("□");
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 36));
            lbl.setForeground(UIConstants.PRIMARY);
        }
    }

    public void refresh() {
        // Quay về Hub và reset các sub-panel đã tải
        cardLayout.show(cardArea, "hub");
        if (pBangGia   != null) pBangGia.refresh();
        if (pLoaiPhong != null) pLoaiPhong.refresh();
        if (pPhong     != null) pPhong.refresh();
        if (pDichVu    != null) pDichVu.refresh();
        if (pLog       != null) pLog.refresh();
        if (pShift     != null) pShift.refresh();
    }
}
