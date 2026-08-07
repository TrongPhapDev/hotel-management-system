package ui.dialogs;

import entity.GiaoCa;
import service.GiaoCaService;
import ui.components.RoundedComponents.*;
import ui.components.UIConstants;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Hộp thoại hiển thị chi tiết lịch sử giao ca.
 * Bao gồm báo cáo tài chính và chi tiết kiểm tiền mệnh giá.
 */
public class ShiftDetailDialog extends JDialog {
    private final GiaoCa entity;
    private final GiaoCaService service = new GiaoCaService();

    public ShiftDetailDialog(Frame parent, GiaoCa gc) {
        super(parent, "Chi tiết ca làm #" + gc.getMaGiaoCa(), true);
        this.entity = gc;
        setSize(700, 650);
        setLocationRelativeTo(parent);
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
        JLabel title = new JLabel("Chi tiết lịch sử giao ca");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        // Sidebar / Info Panel
        JPanel content = new JPanel(new BorderLayout(20, 0));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel left = new JPanel(new GridBagLayout());
        left.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0;

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        
        int row = 0;
        addInfo(left, row++, "Mã ca:", entity.getMaGiaoCa(), gbc);
        addInfo(left, row++, "Nhân viên:", entity.getNhanVien().getHoTen(), gbc);
        addInfo(left, row++, "Bắt đầu:", entity.getThoiGianBatDau().format(fmt), gbc);
        addInfo(left, row++, "Kết thúc:", entity.getThoiGianKetThuc() != null ? entity.getThoiGianKetThuc().format(fmt) : "--", gbc);
        
        gbc.gridy = row++; gbc.gridx = 0; gbc.gridwidth = 2;
        left.add(new JSeparator(), gbc);
        
        addMoney(left, row++, "Vốn đầu ca:", entity.getTienMatDauCa(), gbc);
        addMoney(left, row++, "Thu trong ca:", entity.getTienMatThuTrongCa(), gbc);
        addMoney(left, row++, "Bàn giao:", entity.getTienMatBanGiao(), gbc);
        
        JLabel lblDiff = new JLabel(String.format("%,.0f VNĐ", Math.abs(entity.getTienMatChenhLech())));
        lblDiff.setFont(UIConstants.FONT_BODY_BOLD);
        lblDiff.setForeground(entity.getTienMatChenhLech() == 0 ? new Color(0x10B981) : Color.RED);
        gbc.gridy = row; gbc.gridx = 0; gbc.gridwidth = 1;
        left.add(new JLabel("Chênh lệch:"), gbc);
        gbc.gridx = 1;
        left.add(lblDiff, gbc);

        // Denomination Table
        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.setOpaque(false);
        right.setBorder(BorderFactory.createTitledBorder("Chi tiết kiểm tiền"));
        
        String[] cols = {"Mệnh giá", "Số lượng", "Thành tiền"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        JTable table = new JTable(model);
        table.setRowHeight(30);
        
        Map<Integer, Integer> map = service.getDenominations(entity.getMaGiaoCa());
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            model.addRow(new Object[]{
                String.format("%,d", entry.getKey()),
                entry.getValue(),
                String.format("%,d", entry.getKey() * entry.getValue())
            });
        }
        
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        table.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);

        right.add(new JScrollPane(table), BorderLayout.CENTER);

        content.add(left, BorderLayout.WEST);
        content.add(right, BorderLayout.CENTER);

        // Footer / Notes
        JPanel footer = new JPanel(new BorderLayout(0, 10));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER),
            BorderFactory.createEmptyBorder(15, 24, 15, 24)
        ));

        JTextArea txtNotes = new JTextArea(3, 20);
        txtNotes.setText("Ghi chú: " + (entity.getGhiChu() == null ? "Không có" : entity.getGhiChu()));
        txtNotes.setEditable(false);
        txtNotes.setLineWrap(true);
        txtNotes.setWrapStyleWord(true);
        txtNotes.setBackground(new Color(0xF8FAFC));
        txtNotes.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        RoundedButton btnClose = new RoundedButton("Đóng", UIConstants.PRIMARY, Color.WHITE);
        btnClose.setPreferredSize(new Dimension(120, 40));
        btnClose.addActionListener(e -> dispose());
        
        footer.add(new JScrollPane(txtNotes), BorderLayout.CENTER);
        footer.add(btnClose, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);
        root.add(content, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void addInfo(JPanel p, int row, String label, String val, GridBagConstraints gbc) {
        gbc.gridy = row; gbc.gridx = 0; gbc.gridwidth = 1;
        p.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        JLabel lbVal = new JLabel(val);
        lbVal.setFont(UIConstants.FONT_BODY_BOLD);
        p.add(lbVal, gbc);
    }

    private void addMoney(JPanel p, int row, String label, double val, GridBagConstraints gbc) {
        gbc.gridy = row; gbc.gridx = 0; gbc.gridwidth = 1;
        p.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        JLabel lbVal = new JLabel(String.format("%,.0f VNĐ", val));
        lbVal.setFont(UIConstants.FONT_BODY_BOLD);
        p.add(lbVal, gbc);
    }
}
