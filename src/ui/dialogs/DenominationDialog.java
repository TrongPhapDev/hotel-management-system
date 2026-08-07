package ui.dialogs;

import ui.components.RoundedComponents.RoundedButton;
import ui.components.UIConstants;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

public class DenominationDialog extends JDialog {
    private final int[] denominations = {500000, 200000, 100000, 50000, 20000, 10000, 5000, 2000, 1000, 500};
    private final Map<Integer, JTextField> fields = new HashMap<>();
    private final JLabel lblTotal = new JLabel("Tổng cộng: 0 VNĐ");
    private double totalAmount = 0;
    private boolean confirmed = false;

    public DenominationDialog(Dialog owner) {
        super(owner, "Bảng đếm tiền mặt thực tế", true);
        setupUI();
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        JPanel body = new JPanel(new GridLayout(0, 2, 15, 10));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        for (int d : denominations) {
            String label = String.format("%,d VNĐ", d);
            body.add(new JLabel(label));
            
            JTextField txt = new JTextField("0");
            txt.setHorizontalAlignment(JTextField.RIGHT);
            txt.addCaretListener(e -> calculateTotal());
            fields.put(d, txt);
            body.add(txt);
        }

        add(new JScrollPane(body), BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(0xF8FAFC));
        footer.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTotal.setForeground(UIConstants.PRIMARY);
        footer.add(lblTotal, BorderLayout.WEST);

        RoundedButton btnConfirm = new RoundedButton("Xác nhận số tiền", UIConstants.PRIMARY, Color.WHITE);
        btnConfirm.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        footer.add(btnConfirm, BorderLayout.EAST);

        add(footer, BorderLayout.SOUTH);

        setSize(400, 550);
        setLocationRelativeTo(getOwner());
    }

    private void calculateTotal() {
        double total = 0;
        for (int d : denominations) {
            try {
                int count = Integer.parseInt(fields.get(d).getText().trim());
                total += (double) d * count;
            } catch (NumberFormatException ignored) {}
        }
        totalAmount = total;
        lblTotal.setText(String.format("Tổng cộng: %,.0f VNĐ", total));
    }

    public boolean isConfirmed() { return confirmed; }
    public double getTotalAmount() { return totalAmount; }
    public Map<Integer, Integer> getCounts() {
        Map<Integer, Integer> counts = new HashMap<>();
        for (int d : denominations) {
            try {
                counts.put(d, Integer.parseInt(fields.get(d).getText().trim()));
            } catch (NumberFormatException e) {
                counts.put(d, 0);
            }
        }
        return counts;
    }
}
