package ui.dialogs;

import service.*;
import entity.*;
import ui.components.NotificationManager;
import ui.components.UIConstants;
import ui.components.RoundedComponents.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ChangeRepresentativeDialog extends JDialog {

    private final ThuePhongService thuePhongService = new ThuePhongService();
    private final KhachHangService khService = new KhachHangService();
    
    private final DatPhong datPhong;
    private final String soPhong;
    private boolean confirmed = false;

    private ModernTextField txtHoTen, txtSDT, txtCCCD;
    private ModernComboBox<String> cboQuocTich;
    private JLabel errHoTen, errSDT, errCCCD;

    public ChangeRepresentativeDialog(Frame owner, DatPhong dp, String soPhong) {
        super(owner, "Thay đổi người đại diện — Phòng " + soPhong, true);
        this.datPhong = dp;
        this.soPhong = soPhong;
        
        setSize(500, 520);
        setLocationRelativeTo(owner);
        setResizable(false);
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0xF8FAFC));
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER),
            BorderFactory.createEmptyBorder(20, 24, 20, 24)));
            
        JLabel lblTitle = new JLabel("Cập nhật thông tin Người đại diện");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(UIConstants.TEXT_PRIMARY);
        header.add(lblTitle, BorderLayout.NORTH);
        
        JLabel lblSub = new JLabel("Phòng " + soPhong + " | Đơn đặt: " + datPhong.getMaDatPhong());
        lblSub.setFont(UIConstants.FONT_SMALL);
        lblSub.setForeground(UIConstants.TEXT_SECONDARY);
        header.add(lblSub, BorderLayout.SOUTH);
        
        root.add(header, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 16, 0);
        g.anchor = GridBagConstraints.NORTHWEST;
        g.weightx = 1.0;

        int row = 0;

        // Current Rep info
        g.gridy = row++;
        JLabel lblCurrent = new JLabel("Người đại diện hiện tại: " + (datPhong.getKhachHang() != null ? datPhong.getKhachHang().getHoTen() : "N/A"));
        lblCurrent.setFont(UIConstants.FONT_SMALL_BOLD);
        lblCurrent.setForeground(UIConstants.PRIMARY);
        form.add(lblCurrent, g);

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0xE2E8F0));
        g.gridy = row++;
        form.add(sep, g);

        // SDT Search
        errSDT = inlineErr();
        txtSDT = new ModernTextField("Nhập số điện thoại để tìm...");
        if (datPhong.getKhachHang() != null) txtSDT.setText(datPhong.getKhachHang().getSoDienThoai());
        
        g.gridy = row++;
        form.add(fieldGroup("Số điện thoại (Tìm kiếm) *", txtSDT, errSDT), g);

        // Ho Ten
        errHoTen = inlineErr();
        txtHoTen = new ModernTextField("Họ và tên khách hàng...");
        if (datPhong.getKhachHang() != null) txtHoTen.setText(datPhong.getKhachHang().getHoTen());
        
        g.gridy = row++;
        form.add(fieldGroup("Họ và tên *", txtHoTen, errHoTen), g);

        // CCCD & Quoc Tich
        JPanel row4 = new JPanel(new GridLayout(1, 2, 16, 0));
        row4.setOpaque(false);
        
        errCCCD = inlineErr();
        txtCCCD = new ModernTextField("CCCD/Hộ chiếu...");
        if (datPhong.getKhachHang() != null) txtCCCD.setText(datPhong.getKhachHang().getCccd());
        row4.add(fieldGroup("CCCD / Hộ chiếu", txtCCCD, errCCCD));
        
        cboQuocTich = new ModernComboBox<>(new String[] { "Việt Nam", "Mỹ", "Trung Quốc", "Hàn Quốc", "Nhật Bản", "Khác" });
        if (datPhong.getKhachHang() != null && datPhong.getKhachHang().getQuocTich() != null) {
            cboQuocTich.setSelectedItem(datPhong.getKhachHang().getQuocTich());
        }
        row4.add(fieldGroup("Quốc tịch", cboQuocTich, inlineErr())); // Thêm label trống để đồng bộ layout
        
        g.gridy = row++;
        form.add(row4, g);

        root.add(form, BorderLayout.CENTER);

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btns.setBackground(new Color(0xF8FAFC));
        btns.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER),
            BorderFactory.createEmptyBorder(16, 24, 16, 24)));
            
        RoundedButton btnCancel = new RoundedButton("Hủy", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        btnCancel.setPreferredSize(new Dimension(120, 40));
        RoundedButton btnSave = new RoundedButton("Xác nhận thay đổi", UIConstants.PRIMARY, Color.WHITE);
        btnSave.setPreferredSize(new Dimension(160, 40));
        
        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> handleSave());
        
        btns.add(btnCancel);
        btns.add(btnSave);
        root.add(btns, BorderLayout.SOUTH);

        // Logic search & validation
        txtSDT.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateSDT();
                String sdt = txtSDT.getText().trim();
                if (sdt.length() >= 10) {
                    KhachHang kh = khService.getByPhone(sdt);
                    if (kh != null) {
                        txtHoTen.setText(kh.getHoTen());
                        txtCCCD.setText(kh.getCccd());
                        if (kh.getQuocTich() != null) cboQuocTich.setSelectedItem(kh.getQuocTich());
                        errSDT.setText(" ");
                    }
                }
            }
        });

        txtHoTen.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateHoTen();
            }
        });

        txtCCCD.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateCCCD();
            }
        });

        setContentPane(root);
    }

    private void handleSave() {
        if (!validateForm()) return;

        String sdt = txtSDT.getText().trim();
        KhachHang kh = khService.getByPhone(sdt);
        
        if (kh == null) {
            kh = new KhachHang();
            kh.setHoTen(txtHoTen.getText().trim());
            kh.setSdt(sdt);
            kh.setCccd(txtCCCD.getText().trim());
            kh.setQuocTich((String) cboQuocTich.getSelectedItem());
            
            String khErr = khService.them(kh);
            if (khErr != null) {
                NotificationManager.showError("Lỗi khách hàng", "Không thể tạo khách hàng mới: " + khErr);
                return;
            }
            kh = khService.getByPhone(sdt);
        }

        if (kh == null) {
            NotificationManager.showError("Lỗi", "Không tìm thấy thông tin khách hàng sau khi lưu!");
            return;
        }

        String err = thuePhongService.updateRepresentative(datPhong.getMaDatPhong(), kh, soPhong);
        if (err == null) {
            NotificationManager.showSuccess("Cập nhật thành công", "Đã thay đổi người đại diện sang: " + kh.getHoTen());
            confirmed = true;
            dispose();
        } else {
            NotificationManager.showError("Lỗi cập nhật", err);
        }
    }

    private boolean validateForm() {
        boolean vSDT = validateSDT();
        boolean vTen = validateHoTen();
        boolean vCCCD = validateCCCD();
        return vSDT && vTen && vCCCD;
    }

    private boolean validateSDT() {
        String sdt = txtSDT.getText().trim();
        errSDT.setText(" ");
        if (sdt.isEmpty()) {
            errSDT.setText("Bắt buộc - Vui lòng nhập số điện thoại");
            return false;
        } else if (!sdt.matches("^(0[35789])\\d{8}$")) {
            errSDT.setText("SĐT không hợp lệ (10 số, đầu 03/05/07/08/09)");
            return false;
        }
        return true;
    }

    private boolean validateHoTen() {
        String ten = txtHoTen.getText().trim();
        ten = formatName(ten);
        txtHoTen.setText(ten);
        errHoTen.setText(" ");
        if (ten.isEmpty()) {
            errHoTen.setText("Bắt buộc - Vui lòng nhập họ tên");
            return false;
        } else if (!ten.matches("^[\\p{L} .'-]{2,50}$")) {
            errHoTen.setText("Họ tên không hợp lệ (2-50 ký tự)");
            return false;
        }
        return true;
    }

    private boolean validateCCCD() {
        String cccd = txtCCCD.getText().trim();
        errCCCD.setText(" ");
        if (!cccd.isEmpty() && !cccd.matches("^([0-9]{9}|[0-9]{12}|[A-Z][0-9]{7,8})$")) {
            errCCCD.setText("CCCD/Passport không hợp lệ");
            return false;
        }
        return true;
    }

    private String formatName(String name) {
        if (name == null || name.isBlank()) return "";
        String[] words = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.length() > 0) {
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1) {
                    sb.append(w.substring(1).toLowerCase());
                }
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    private JPanel fieldGroup(String label, JComponent comp) { return fieldGroup(label, comp, null); }
    private JPanel fieldGroup(String label, JComponent comp, JLabel errLbl) {
        comp.setPreferredSize(new Dimension(100, 38)); 
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38)); 
        
        JPanel g = new JPanel(new BorderLayout(0, 4));
        g.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_SECONDARY);
        
        g.add(lbl, BorderLayout.NORTH);
        
        // Wrap component in a panel that doesn't stretch it vertically
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(comp, BorderLayout.NORTH);
        g.add(wrapper, BorderLayout.CENTER);
        
        if (errLbl != null) {
            g.add(errLbl, BorderLayout.SOUTH);
        }
        return g;
    }

    private JLabel inlineErr() {
        JLabel l = new JLabel(" ");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(new Color(0xEF4444));
        return l;
    }

    public boolean isConfirmed() { return confirmed; }
}
