package ui.dialogs;

import service.DatPhongService;
import entity.DatPhong;
import ui.components.UIConstants;
import ui.components.RoundedComponents.RoundedButton;
import ui.components.RoundedComponents.ModernTextField;
import ui.components.NotificationManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Hộp thoại Hủy phòng chuyên nghiệp với tính phí phạt tự động và Manager Override.
 */
public class HuyDatPhongDialog extends JDialog {

    private final DatPhongService datPhongService = new DatPhongService();
    private final String maDatPhong;
    private final String tenKhach;
    private double phiPhatDuyKien = 0;
    private boolean confirmed = false;

    private ModernTextField txtPhiPhat;
    private JTextArea txtLyDo;
    private JCheckBox chkOverride;

    public HuyDatPhongDialog(Frame parent, String maDatPhong, String tenKhach) {
        super(parent, "Hủy đặt phòng", true);
        this.maDatPhong = maDatPhong;
        this.tenKhach = tenKhach;
        this.phiPhatDuyKien = datPhongService.tinhPhiHuyDien(maDatPhong);

        setSize(500, 520);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        buildUI();
    }

    private void buildUI() {
        // --- Header ---
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x1E293B));
        header.setPreferredSize(new Dimension(0, 70));
        header.setBorder(new EmptyBorder(0, 24, 0, 24));

        JLabel lblTitle = new JLabel("HỦY ĐẶT PHÒNG");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);
        header.add(lblTitle, BorderLayout.CENTER);
        
        JLabel lblSub = new JLabel("Xác nhận phí phạt và lý do hủy đơn");
        lblSub.setFont(UIConstants.FONT_TINY);
        lblSub.setForeground(new Color(0x94A3B8));
        header.add(lblSub, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // --- Content ---
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(24, 24, 24, 24));

        // Info
        JPanel info = new JPanel(new GridLayout(2, 1, 0, 4));
        info.setBackground(new Color(0xF1F5F9));
        info.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER),
            new EmptyBorder(12, 16, 12, 16)
        ));
        info.add(new JLabel("Mã đơn: " + maDatPhong) {{ setFont(UIConstants.FONT_SMALL_BOLD); }});
        info.add(new JLabel("Khách hàng: " + tenKhach) {{ setFont(UIConstants.FONT_BODY_BOLD); }});
        main.add(info);
        main.add(Box.createVerticalStrut(20));

        // Penalty Section
        JLabel lblPenalty = new JLabel("Phí phạt hủy muộn (VNĐ):");
        lblPenalty.setFont(UIConstants.FONT_SMALL_BOLD);
        main.add(lblPenalty);
        main.add(Box.createVerticalStrut(8));

        txtPhiPhat = new ModernTextField("0");
        txtPhiPhat.setText(String.format("%.0f", phiPhatDuyKien));
        txtPhiPhat.setEditable(false);
        txtPhiPhat.setBackground(new Color(0xF8FAFC));
        txtPhiPhat.setFont(new Font("Segoe UI", Font.BOLD, 16));
        txtPhiPhat.setForeground(UIConstants.DANGER);
        txtPhiPhat.setHorizontalAlignment(JTextField.RIGHT);
        main.add(txtPhiPhat);
        
        chkOverride = new JCheckBox("Manager Override (Ghi đè phí phạt)");
        chkOverride.setFont(UIConstants.FONT_TINY);
        chkOverride.setForeground(UIConstants.TEXT_SECONDARY);
        chkOverride.setOpaque(false);
        chkOverride.addActionListener(e -> {
            txtPhiPhat.setEditable(chkOverride.isSelected());
            txtPhiPhat.setBackground(chkOverride.isSelected() ? Color.WHITE : new Color(0xF8FAFC));
        });
        main.add(chkOverride);
        main.add(Box.createVerticalStrut(16));

        // Reason Section
        JLabel lblReason = new JLabel("Lý do hủy *:");
        lblReason.setFont(UIConstants.FONT_SMALL_BOLD);
        main.add(lblReason);
        main.add(Box.createVerticalStrut(8));

        txtLyDo = new JTextArea(4, 0);
        txtLyDo.setFont(UIConstants.FONT_BODY);
        txtLyDo.setLineWrap(true);
        txtLyDo.setWrapStyleWord(true);
        txtLyDo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER),
            new EmptyBorder(8, 10, 8, 10)
        ));
        main.add(new JScrollPane(txtLyDo));

        add(main, BorderLayout.CENTER);

        // --- Footer ---
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(new Color(0xF8FAFC));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER));

        RoundedButton btnClose = new RoundedButton("Đóng", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        RoundedButton btnHuy = new RoundedButton("Xác nhận Hủy Đơn", UIConstants.DANGER, Color.WHITE);
        btnHuy.setPreferredSize(new Dimension(160, 38));

        btnClose.addActionListener(e -> dispose());
        btnHuy.addActionListener(e -> handleHuy());

        footer.add(btnClose);
        footer.add(btnHuy);
        add(footer, BorderLayout.SOUTH);
    }

    private void handleHuy() {
        String lyDo = txtLyDo.getText().trim();
        if (lyDo.isEmpty()) {
            NotificationManager.showWarning("Thiếu thông tin", "Vui lòng nhập lý do hủy đơn!");
            return;
        }

        double penalty = 0;
        try {
            penalty = Double.parseDouble(txtPhiPhat.getText().trim().replace(",", "").replace(".", ""));
        } catch (Exception e) {
            NotificationManager.showError("Lỗi", "Số tiền phạt không hợp lệ!");
            return;
        }

        String err = datPhongService.huyDatPhong(maDatPhong, penalty, lyDo);
        if (err == null) {
            NotificationManager.showSuccess("Hoàn tất", "Đã hủy đơn đặt phòng thành công.");
            confirmed = true;
            dispose();
        } else {
            NotificationManager.showError("Lỗi hệ thống", err);
        }
    }

    public boolean isConfirmed() { return confirmed; }
}
