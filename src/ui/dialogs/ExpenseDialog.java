package ui.dialogs;

import dao.ChiPhiDAO;
import entity.ChiPhi;
import entity.GiaoCa;
import service.AuthService;
import ui.components.RoundedComponents.RoundedButton;
import ui.components.UIConstants;
import java.awt.*;
import java.time.LocalDateTime;
import javax.swing.*;

public class ExpenseDialog extends JDialog {
    private final ChiPhiDAO dao = new ChiPhiDAO();
    private JTextField txtAmount;
    private JTextArea txtReason;

    public ExpenseDialog(Frame owner) {
        super(owner, "Ghi nhận chi phí phát sinh", true);
        setupUI();
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIConstants.WARNING); // Màu cam nhẹ cho chi phí
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        JLabel title = new JLabel("Ghi nhận chi phí");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 15, 0);
        gbc.weightx = 1.0;

        gbc.gridy = 0;
        body.add(new JLabel("Số tiền chi (VNĐ):"), gbc);
        gbc.gridy++;
        txtAmount = new JTextField();
        txtAmount.setFont(UIConstants.FONT_BODY);
        txtAmount.setPreferredSize(new Dimension(0, 38));
        body.add(txtAmount, gbc);

        gbc.gridy++;
        body.add(new JLabel("Lý do chi:"), gbc);
        gbc.gridy++;
        txtReason = new JTextArea(3, 20);
        txtReason.setFont(UIConstants.FONT_BODY);
        txtReason.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        body.add(new JScrollPane(txtReason), gbc);

        add(body, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(new Color(0xF8FAFC));
        
        RoundedButton btnCancel = RoundedButton.outline("Hủy bỏ", UIConstants.TEXT_PRIMARY);
        btnCancel.addActionListener(e -> dispose());
        
        RoundedButton btnSave = new RoundedButton("Lưu phiếu chi", UIConstants.WARNING, Color.WHITE);
        btnSave.addActionListener(e -> handleSave());

        footer.add(btnCancel);
        footer.add(btnSave);
        add(footer, BorderLayout.SOUTH);

        pack();
        setSize(400, getHeight());
        setLocationRelativeTo(getOwner());
    }

    private void handleSave() {
        GiaoCa current = AuthService.getInstance().getCurrentShift();
        if (current == null) {
            JOptionPane.showMessageDialog(this, "Bạn phải bắt đầu ca làm việc trước khi ghi nhận chi phí!");
            return;
        }

        try {
            double amount = Double.parseDouble(txtAmount.getText().replace(",", ""));
            String reason = txtReason.getText().trim();
            if (reason.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập lý do chi!");
                return;
            }

            ChiPhi cp = new ChiPhi();
            cp.setNhanVien(AuthService.getInstance().getCurrentUser());
            cp.setSoTien(amount);
            cp.setLyDo(reason);
            cp.setThoiGian(LocalDateTime.now());
            cp.setMaGiaoCa(current.getMaGiaoCa());

            if (dao.insert(cp)) {
                JOptionPane.showMessageDialog(this, "Đã ghi nhận chi phí thành công!");
                dispose();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Số tiền không hợp lệ!");
        }
    }
}
