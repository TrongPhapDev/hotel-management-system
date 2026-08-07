package ui.dialogs;

import entity.NhatKyHeThong;
import ui.components.RoundedComponents.*;
import ui.components.UIConstants;
import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/**
 * Hộp thoại hiển thị chi tiết nhật ký hệ thống.
 * Chế độ chỉ đọc để đảm bảo tính toàn vẹn của dữ liệu audit.
 */
public class LogDialog extends JDialog {
    private final NhatKyHeThong entity;

    public LogDialog(Frame parent, NhatKyHeThong log) {
        super(parent, "Chi tiết nhật ký #" + log.getMaLog(), true);
        this.entity = log;
        setSize(550, 480);
        setLocationRelativeTo(parent);
        setResizable(false);
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // Header
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, UIConstants.PRIMARY, 0, getHeight(), UIConstants.PRIMARY_DARK));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        JLabel title = new JLabel("Chi tiết nhật ký hệ thống");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        // Body
        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
        
        JLabel lblThoiGian = new JLabel(entity.getThoiGian().format(fmt));
        lblThoiGian.setFont(UIConstants.FONT_BODY_BOLD);

        JLabel lblTaiKhoan = new JLabel(entity.getTenDangNhap());
        lblTaiKhoan.setFont(UIConstants.FONT_BODY_BOLD);
        lblTaiKhoan.setForeground(UIConstants.PRIMARY);

        ModernTextField txtHanhDong = new ModernTextField("");
        txtHanhDong.setText(entity.getHanhDong());
        txtHanhDong.setEditable(false);
        txtHanhDong.setPreferredSize(new Dimension(0, 40));

        ModernTextField txtDoiTuong = new ModernTextField("");
        txtDoiTuong.setText(entity.getDoiTuong());
        txtDoiTuong.setEditable(false);
        txtDoiTuong.setPreferredSize(new Dimension(0, 40));

        JTextArea txtChiTiet = new JTextArea(5, 20);
        txtChiTiet.setFont(UIConstants.FONT_BODY);
        txtChiTiet.setText(entity.getChiTiet());
        txtChiTiet.setEditable(false);
        txtChiTiet.setLineWrap(true);
        txtChiTiet.setWrapStyleWord(true);
        
        JScrollPane scrollChiTiet = new JScrollPane(txtChiTiet);
        scrollChiTiet.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        int row = 0;
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0.5;
        body.add(lf("Thời gian", lblThoiGian), gbc);
        
        gbc.gridx = 1;
        body.add(lf("Tài khoản", lblTaiKhoan), gbc);
        
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        body.add(lf("Hành động", txtHanhDong), gbc);
        
        gbc.gridx = 1;
        body.add(lf("Đối tượng", txtDoiTuong), gbc);
        
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        body.add(lf("Chi tiết hành động", scrollChiTiet), gbc);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xF1F5F9)));

        RoundedButton btnClose = new RoundedButton("Đóng", new Color(0xF1F5F9), UIConstants.TEXT_SECONDARY);
        btnClose.setPreferredSize(new Dimension(100, 40));
        btnClose.addActionListener(e -> dispose());
        footer.add(btnClose);

        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel lf(String l, JComponent c) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel lb = new JLabel(l);
        lb.setFont(UIConstants.FONT_SMALL_BOLD);
        lb.setForeground(UIConstants.TEXT_SECONDARY);
        p.add(lb, BorderLayout.NORTH);
        p.add(c, BorderLayout.CENTER);
        return p;
    }
}
