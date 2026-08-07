package ui.dialogs;

import entity.GiaoCa;
import entity.NhanVien;
import service.AuthService;
import service.GiaoCaService;
import ui.components.RoundedComponents.*;
import ui.components.UIConstants;
import java.awt.*;
import java.util.List;
import javax.swing.*;

public class HandoverDialog extends JDialog {
    private final GiaoCaService service = new GiaoCaService();
    private final dao.NhanVienDAO nvDAO = new dao.NhanVienDAO();
    private GiaoCa currentShift;

    public HandoverDialog(Frame owner) {
        super(owner, "Bàn giao ca làm việc", true);
        this.currentShift = AuthService.getInstance().getCurrentShift();
        
        setupUI();
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIConstants.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        JLabel title = new JLabel(currentShift == null ? "Mở ca làm mới" : "Kết thúc ca làm");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // Body
        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(20, 25, 10, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 15, 0);
        gbc.weightx = 1.0;

        if (currentShift == null) {
            setupOpenShiftUI(body, gbc);
        } else {
            setupCloseShiftUI(body, gbc);
        }

        add(body, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(new Color(0xF8FAFC));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER));
        
        RoundedButton btnCancel = RoundedButton.outline("Hủy bỏ", UIConstants.TEXT_PRIMARY);
        btnCancel.addActionListener(e -> dispose());
        
        RoundedButton btnAction = new RoundedButton(currentShift == null ? "Bắt đầu ca" : "Xác nhận bàn giao", UIConstants.PRIMARY, Color.WHITE);
        btnAction.addActionListener(e -> handleAction());

        footer.add(btnCancel);
        footer.add(btnAction);
        add(footer, BorderLayout.SOUTH);

        pack();
        setSize(450, getHeight());
        setLocationRelativeTo(getOwner());
    }

    private JTextField txtTienDauCa;
    private void setupOpenShiftUI(JPanel body, GridBagConstraints gbc) {
        gbc.gridy = 0;
        body.add(new JLabel("Tiền mặt đầu ca (VNĐ):"), gbc);
        gbc.gridy++;
        txtTienDauCa = new JTextField("0");
        txtTienDauCa.setFont(UIConstants.FONT_BODY);
        txtTienDauCa.setPreferredSize(new Dimension(0, 38));
        body.add(txtTienDauCa, gbc);

        gbc.gridy++;
        JLabel info = new JLabel("<html><i>Lưu ý: Bạn nên đếm kỹ tiền mặt trong két trước khi bắt đầu.</i></html>");
        info.setFont(UIConstants.FONT_SMALL);
        info.setForeground(UIConstants.TEXT_SECONDARY);
        body.add(info, gbc);
    }

    private JLabel lblExpected;
    private JLabel lblVariance;
    private JTextField txtTienBanGiao;
    private JTextArea txtGhiChu;
    private JComboBox<NhanVienWrapper> cboNguoiNhan;
    private java.util.Map<Integer, Integer> denominationCounts;
    private double currentExpectedCash = 0;

    private void setupCloseShiftUI(JPanel body, GridBagConstraints gbc) {
        gbc.gridy = 0;
        body.add(new JLabel("Ca phát sinh từ: " + currentShift.getThoiGianBatDau().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM"))), gbc);
        
        // Dashboard tài chính
        JPanel pnlFinance = new JPanel(new GridLayout(0, 1, 2, 2));
        pnlFinance.setOpaque(false);
        pnlFinance.setBorder(BorderFactory.createTitledBorder("Báo cáo tài chính hệ thống"));
        
        double revCash = service.tinhDoanhThuTienMat(currentShift.getThoiGianBatDau());
        double revCard = service.tinhDoanhThuCa(currentShift.getThoiGianBatDau()) - revCash;
        double expenses = service.tinhChiPhiCa(currentShift.getMaGiaoCa());
        currentExpectedCash = service.getExpectedCash(currentShift);

        pnlFinance.add(new JLabel(String.format("• Tiền mặt đầu ca: %,.0f", currentShift.getTienMatDauCa())));
        pnlFinance.add(new JLabel(String.format("• Thu tiền mặt (+): %,.0f", revCash)));
        JLabel lblNonCash = new JLabel(String.format("  (Thẻ/CK: %,.0f - Không tính vào két)", revCard));
        lblNonCash.setFont(UIConstants.FONT_SMALL);
        pnlFinance.add(lblNonCash);
        pnlFinance.add(new JLabel(String.format("• Chi tiền mặt (-): %,.0f", expenses)));
        
        lblExpected = new JLabel(String.format("Dự kiến trong két: %,.0f VNĐ", currentExpectedCash));
        lblExpected.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblExpected.setForeground(UIConstants.PRIMARY);
        pnlFinance.add(lblExpected);

        gbc.gridy++;
        body.add(pnlFinance, gbc);

        // Kiểm tiền thực tế
        gbc.gridy++;
        body.add(new JLabel("Tiền mặt đếm được thực tế:"), gbc);
        gbc.gridy++;
        
        JPanel pnlActual = new JPanel(new BorderLayout(10, 0));
        pnlActual.setOpaque(false);
        txtTienBanGiao = new JTextField("0");
        txtTienBanGiao.setFont(UIConstants.FONT_BODY);
        txtTienBanGiao.setEditable(false);
        txtTienBanGiao.setPreferredSize(new Dimension(0, 38));
        pnlActual.add(txtTienBanGiao, BorderLayout.CENTER);
        
        RoundedButton btnCount = new RoundedButton("Mở bảng kiểm tiền", Color.GRAY, Color.WHITE);
        btnCount.addActionListener(e -> {
            DenominationDialog dlg = new DenominationDialog(this);
            dlg.setVisible(true);
            if (dlg.isConfirmed()) {
                denominationCounts = dlg.getCounts();
                double total = dlg.getTotalAmount();
                txtTienBanGiao.setText(String.format("%.0f", total));
                updateVariance(currentExpectedCash, total);
            }
        });
        pnlActual.add(btnCount, BorderLayout.EAST);
        body.add(pnlActual, gbc);

        gbc.gridy++;
        lblVariance = new JLabel("Chênh lệch: 0 VNĐ");
        lblVariance.setFont(UIConstants.FONT_BODY_BOLD);
        body.add(lblVariance, gbc);

        gbc.gridy++;
        body.add(new JLabel("Bàn giao cho:"), gbc);
        gbc.gridy++;
        cboNguoiNhan = new JComboBox<>();
        cboNguoiNhan.addItem(new NhanVienWrapper(null));
        List<NhanVien> ds = nvDAO.getAll();
        for (NhanVien nv : ds) {
            if (!nv.getMaNhanVien().equals(AuthService.getInstance().getCurrentMaNV())) {
                cboNguoiNhan.addItem(new NhanVienWrapper(nv));
            }
        }
        body.add(cboNguoiNhan, gbc);

        gbc.gridy++;
        body.add(new JLabel("Ghi chú bàn giao:"), gbc);
        gbc.gridy++;
        txtGhiChu = new JTextArea(2, 20);
        txtGhiChu.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        body.add(new JScrollPane(txtGhiChu), gbc);
    }

    private void updateVariance(double system, double actual) {
        double diff = actual - system;
        lblVariance.setText(String.format("Chênh lệch: %,.0f VNĐ %s", 
            Math.abs(diff), 
            diff == 0 ? "(Khớp)" : (diff > 0 ? "(Thừa)" : "(Thiếu)")));
        lblVariance.setForeground(diff == 0 ? new Color(0x10B981) : Color.RED);
    }

    private void handleAction() {
        if (currentShift == null) {
            try {
                double val = Double.parseDouble(txtTienDauCa.getText().replace(",", ""));
                GiaoCa gc = service.moCa(AuthService.getInstance().getCurrentUser(), val);
                if (gc != null) {
                    AuthService.getInstance().setCurrentShift(gc);
                    JOptionPane.showMessageDialog(this, "Đã mở ca làm việc thành công!");
                    dispose();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Tiền mặt không hợp lệ!");
            }
        } else {
            try {
                double actual = Double.parseDouble(txtTienBanGiao.getText().replace(",", ""));
                double variance = actual - currentExpectedCash;
                
                currentShift.setTienMatBanGiao(actual);
                currentShift.setTienMatChenhLech(variance);
                currentShift.setTienMatThuTrongCa(service.tinhDoanhThuTienMat(currentShift.getThoiGianBatDau()));
                currentShift.setGhiChu(txtGhiChu.getText());
                
                NhanVienWrapper wrap = (NhanVienWrapper) cboNguoiNhan.getSelectedItem();
                if (wrap.nv != null) {
                    currentShift.setMaNhanVienNhan(wrap.nv.getMaNhanVien());
                }

                if (service.chotCa(currentShift)) {
                    // Lưu chi tiết tiền mặt
                    if (denominationCounts != null) {
                        service.saveKiemTien(currentShift.getMaGiaoCa(), denominationCounts);
                    }
                    
                    AuthService.getInstance().setCurrentShift(null);
                    JOptionPane.showMessageDialog(this, "Đã hoàn thành bàn giao ca. " + 
                        (variance == 0 ? "Số dư cân khớp." : "Số dư chênh lệch: " + String.format("%,.0f", variance)));
                    dispose();
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Lỗi khi xử lý dữ liệu: " + e.getMessage());
            }
        }
    }

    static class NhanVienWrapper {
        NhanVien nv;
        NhanVienWrapper(NhanVien nv) { this.nv = nv; }
        @Override
        public String toString() { return nv == null ? "-- Chưa bàn giao cho ai --" : nv.getHoTen() + " (" + nv.getMaNhanVien() + ")"; }
    }
}
