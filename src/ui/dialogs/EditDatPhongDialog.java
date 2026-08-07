package ui.dialogs;

import entity.DatPhong;
import entity.enums.TrangThaiDatPhong;
import service.DatPhongService;
import ui.components.DateTimePicker;
import ui.components.NotificationManager;
import ui.components.RoundedComponents.*;
import ui.components.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * EditDatPhongDialog - Giao diện sửa thông tin đặt phòng.
 * Khôi phục từ bản thiết kế chuyên nghiệp 11:40 PM.
 */
public class EditDatPhongDialog extends JDialog {

    private final DatPhongService datPhongService = new DatPhongService();
    private boolean confirmed = false;
    private final DatPhong dp;

    private DateTimePicker txtCheckIn, txtCheckOut;
    private ModernTextField txtSoKhach;
    private JTextArea txtGhiChu;
    private ModernTextField txtTienCoc;

    public EditDatPhongDialog(Frame parent, DatPhong dp) {
        super(parent, "Sửa thông tin đặt phòng", true);
        this.dp = dp;
        
        setLayout(new BorderLayout());
        setSize(540, 700);
        setLocationRelativeTo(parent);
        setResizable(false);
        
        buildUI();
        loadData();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UIConstants.BG_MAIN);

        // --- Header Section ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UIConstants.PRIMARY_LIGHT);
        headerPanel.setBorder(new EmptyBorder(25, 30, 25, 30));
        
        JLabel lblTitle = new JLabel("Chỉnh sửa đơn đặt phòng");
        lblTitle.setFont(UIConstants.FONT_TITLE);
        lblTitle.setForeground(UIConstants.PRIMARY);
        
        JLabel lblSubTitle = new JLabel("Mã đơn: " + dp.getMaDatPhong() + " | Trạng thái: " + UIConstants.getTrangThaiDatPhongLabel(dp.getTrangThai()));
        lblSubTitle.setFont(UIConstants.FONT_SMALL);
        lblSubTitle.setForeground(UIConstants.TEXT_SECONDARY);
        
        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblSubTitle, BorderLayout.CENTER);
        root.add(headerPanel, BorderLayout.NORTH);

        // --- Body Section ---
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(20, 30, 20, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.weightx = 1.0;

        int row = 0;

        // Check-in & Check-out in two columns
        JPanel datesPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        datesPanel.setOpaque(false);
        
        JPanel checkinBox = createInputGroup("Ngày nhận phòng dự kiến", txtCheckIn = new DateTimePicker(new Date()));
        JPanel checkoutBox = createInputGroup("Ngày trả phòng dự kiến", txtCheckOut = new DateTimePicker(new Date()));
        
        datesPanel.add(checkinBox);
        datesPanel.add(checkoutBox);
        
        gbc.gridy = row++;
        body.add(datesPanel, gbc);

        // So khach & Tien coc
        JPanel infoPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        infoPanel.setOpaque(false);
        
        txtSoKhach = new ModernTextField("Ví dụ: 2");
        txtTienCoc = new ModernTextField("0");
        
        infoPanel.add(createInputGroup("Số lượng khách", txtSoKhach));
        infoPanel.add(createInputGroup("Tiền đặt cọc (VNĐ)", txtTienCoc));
        
        gbc.gridy = row++;
        body.add(infoPanel, gbc);

        // Ghi chú
        gbc.gridy = row++;
        JLabel lblNote = new JLabel("Ghi chú / Yêu cầu đặc biệt");
        lblNote.setFont(UIConstants.FONT_SMALL_BOLD);
        lblNote.setForeground(UIConstants.TEXT_SECONDARY);
        body.add(lblNote, gbc);
        
        txtGhiChu = new JTextArea(5, 20);
        txtGhiChu.setFont(UIConstants.FONT_BODY);
        txtGhiChu.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane scrollGhiChu = new JScrollPane(txtGhiChu);
        scrollGhiChu.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        
        gbc.gridy = row++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        body.add(scrollGhiChu, gbc);

        root.add(body, BorderLayout.CENTER);

        // --- Footer Section ---
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 20));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER));

        RoundedButton btnCancel = RoundedButton.outline("Hủy bỏ", UIConstants.BORDER);
        btnCancel.setPreferredSize(new Dimension(120, 42));
        btnCancel.addActionListener(e -> dispose());
        
        RoundedButton btnSave = new RoundedButton("Lưu thay đổi", UIConstants.PRIMARY, Color.WHITE);
        btnSave.setPreferredSize(new Dimension(160, 42));
        btnSave.addActionListener(e -> doSave());

        footer.add(btnCancel);
        footer.add(btnSave);
        root.add(footer, BorderLayout.SOUTH);

        add(root);
    }

    private JPanel createInputGroup(String label, JComponent comp) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_SECONDARY);
        
        comp.setPreferredSize(new Dimension(0, 40));
        
        panel.add(lbl, BorderLayout.NORTH);
        panel.add(comp, BorderLayout.CENTER);
        return panel;
    }

    private void loadData() {
        if (dp.getNgayNhanDuKien() != null) {
            txtCheckIn.setDate(java.sql.Timestamp.valueOf(dp.getNgayNhanDuKien()));
        }
        if (dp.getNgayTraDuKien() != null) {
            txtCheckOut.setDate(java.sql.Timestamp.valueOf(dp.getNgayTraDuKien()));
        }
        txtSoKhach.setText(String.valueOf(dp.getSoLuongKhach()));
        txtTienCoc.setText(String.format("%.0f", dp.getTienDatCoc()));
        txtGhiChu.setText(dp.getGhiChu());
    }

    private void doSave() {
        try {
            LocalDateTime in = txtCheckIn.getLocalDateTime();
            LocalDateTime out = txtCheckOut.getLocalDateTime();
            
            if (out.isBefore(in)) {
                NotificationManager.showError("Lỗi", "Ngày trả không thể trước ngày nhận!");
                return;
            }

            dp.setNgayNhanDuKien(in);
            dp.setNgayTraDuKien(out);
            dp.setSoLuongKhach(Integer.parseInt(txtSoKhach.getText()));
            dp.setTienDatCoc(Double.parseDouble(txtTienCoc.getText()));
            dp.setGhiChu(txtGhiChu.getText());

            String err = datPhongService.suaDatPhong(dp);
            if (err == null) {
                confirmed = true;
                NotificationManager.showSuccess("Thành công", "Đã cập nhật thông tin đơn đặt phòng!");
                dispose();
            } else {
                NotificationManager.showError("Lỗi", err);
            }
        } catch (NumberFormatException ex) {
            NotificationManager.showError("Lỗi", "Vui lòng nhập số hợp lệ!");
        }
    }

    public boolean isConfirmed() { return confirmed; }
}
