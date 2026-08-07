package ui.dialogs;

import ui.components.RoundedComponents.RoundedBorder;
import ui.components.RoundedComponents.RoundedButton;
import ui.components.UIConstants;
import entity.KhachHang;
import java.awt.*;
import javax.swing.*;
import service.KhachHangService;

public class KhachHangDialog extends JDialog {

    private final KhachHangService service = new KhachHangService();
    private final KhachHang        entity;
    private boolean confirmed = false;

    private JTextField    txtTen, txtSDT, txtCCCD, txtQuocTich;
    private ui.components.DatePicker dtNgaySinh;
    private JComboBox<String> cboGioiTinh;
    private JLabel        lblMa;
    private RoundedButton btnSave;

    // === Passport / Visa fields ===
    private JComboBox<String> cboLoaiGiayTo;
    private JTextField txtSoHoChieu, txtNoiCapHC, txtSoVisa;
    private ui.components.DatePicker dtHetHanVisa, dtNhapCanh;
    private JPanel passportPanel; // Panel chứa các trường passport/visa (ẩn/hiện)

    // Regex validation
    private static final String REGEX_TEN  = "^[\\p{L} .'-]{2,50}$";
    private static final String REGEX_SDT  = "^(0[35789])[0-9]{8}$";
    private static final String REGEX_CCCD = "^([0-9]{9}|[0-9]{12}|[A-Z][0-9]{7,8})$";
    
    private JLabel errTen, errSDT, errCCCD, errNgaySinh, errPassport;

    public KhachHangDialog(Frame parent, KhachHang kh) {
        super(parent, kh == null ? "Thêm khách hàng mới" : "Chỉnh sửa – " + kh.getHoTen(), true);
        this.entity = kh;
        setSize(500, 720);
        setLocationRelativeTo(parent);
        setResizable(false);
        buildUI();
        if (kh != null) fillData();
        togglePassportFields();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIConstants.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        JLabel title = new JLabel(entity == null ? "Thêm khách hàng mới" : "Chỉnh sửa khách hàng");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(Color.WHITE);
        lblMa = new JLabel(entity != null ? "Mã: " + entity.getMaKhachHang() : "Mã sẽ được tự động tạo");
        lblMa.setFont(UIConstants.FONT_SMALL); lblMa.setForeground(new Color(255,255,255,180));
        JPanel hLeft = new JPanel(); hLeft.setOpaque(false);
        hLeft.setLayout(new BoxLayout(hLeft, BoxLayout.Y_AXIS));
        hLeft.add(title); hLeft.add(lblMa);
        header.add(hLeft, BorderLayout.WEST);

        // Form body (scrollable)
        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 0, 12, 0);

        txtTen  = field(); txtSDT = field(); txtCCCD = field();
        dtNgaySinh = new ui.components.DatePicker(null); txtQuocTich = field(); txtQuocTich.setText("Việt Nam");
        cboGioiTinh = new JComboBox<>(new String[]{"Nam", "Nữ", "Khác"});
        cboGioiTinh.setFont(UIConstants.FONT_BODY);
        cboGioiTinh.setBackground(Color.WHITE);
        cboGioiTinh.setPreferredSize(new Dimension(0, 36));

        cboLoaiGiayTo = new JComboBox<>(new String[]{"CCCD", "CMND", "PASSPORT"});
        cboLoaiGiayTo.setFont(UIConstants.FONT_BODY);
        cboLoaiGiayTo.setBackground(Color.WHITE);
        cboLoaiGiayTo.setPreferredSize(new Dimension(0, 36));
        
        errTen = errLabel(); errSDT = errLabel(); errCCCD = errLabel();
        errNgaySinh = errLabel(); errPassport = errLabel();

        // Listeners
        txtTen.addFocusListener(new java.awt.event.FocusAdapter() { @Override public void focusLost(java.awt.event.FocusEvent e) { validateTen(); } });
        txtSDT.addFocusListener(new java.awt.event.FocusAdapter() { @Override public void focusLost(java.awt.event.FocusEvent e) { validateSDT(); } });
        txtCCCD.addFocusListener(new java.awt.event.FocusAdapter() { @Override public void focusLost(java.awt.event.FocusEvent e) { validateCCCD(); } });
        txtQuocTich.addFocusListener(new java.awt.event.FocusAdapter() { @Override public void focusLost(java.awt.event.FocusEvent e) { togglePassportFields(); } });

        setPlaceholder(txtSDT,  "VD: 0901234567");
        setPlaceholder(txtCCCD, "VD: 012345678901 (9 hoặc 12 số)");

        gbc.gridy = 0; body.add(labelField("Họ và tên *", txtTen, errTen), gbc);
        gbc.gridy++;   body.add(labelField("Số điện thoại *", txtSDT, errSDT), gbc);
        gbc.gridy++;   body.add(labelField("Loại giấy tờ", cboLoaiGiayTo, null), gbc);
        gbc.gridy++;   body.add(labelField("Số CCCD / CMND", txtCCCD, errCCCD), gbc);
        gbc.gridy++;   body.add(labelField("Ngày sinh", dtNgaySinh, errNgaySinh), gbc);
        gbc.gridy++;   body.add(labelField("Giới tính", cboGioiTinh, null), gbc);
        gbc.gridy++;   body.add(labelField("Quốc tịch", txtQuocTich, null), gbc);

        // === Passport / Visa Panel ===
        passportPanel = new JPanel(new GridBagLayout());
        passportPanel.setBackground(new Color(0xFFF7ED));
        passportPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xFBBF24), 1),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        GridBagConstraints pgbc = new GridBagConstraints();
        pgbc.fill = GridBagConstraints.HORIZONTAL;
        pgbc.weightx = 1.0;
        pgbc.gridx = 0; pgbc.gridy = 0;
        pgbc.insets = new Insets(0, 0, 8, 0);

        JLabel lblPassportTitle = new JLabel("🛂 Thông tin hộ chiếu / visa (cho khách nước ngoài)");
        lblPassportTitle.setFont(UIConstants.FONT_SMALL_BOLD);
        lblPassportTitle.setForeground(new Color(0x92400E));
        passportPanel.add(lblPassportTitle, pgbc);

        txtSoHoChieu = field(); txtNoiCapHC = field(); txtSoVisa = field();
        dtHetHanVisa = new ui.components.DatePicker(null);
        dtNhapCanh = new ui.components.DatePicker(null);
        setPlaceholder(txtSoHoChieu, "VD: US1234567");

        pgbc.gridy++; passportPanel.add(labelField("Số hộ chiếu (Passport) *", txtSoHoChieu, errPassport), pgbc);
        pgbc.gridy++; passportPanel.add(labelField("Nơi cấp hộ chiếu", txtNoiCapHC, null), pgbc);
        pgbc.gridy++; passportPanel.add(labelField("Số Visa", txtSoVisa, null), pgbc);
        pgbc.gridy++; passportPanel.add(labelField("Ngày hết hạn Visa", dtHetHanVisa, null), pgbc);
        pgbc.gridy++; passportPanel.add(labelField("Ngày nhập cảnh", dtNhapCanh, null), pgbc);

        gbc.gridy++; gbc.insets = new Insets(8, 0, 12, 0);
        body.add(passportPanel, gbc);

        // Vertical pusher (Glue) to keep fields at top
        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        body.add(new JPanel() {{ setOpaque(false); }}, gbc);

        JScrollPane scrollPane = new JScrollPane(body);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);

        // Footer buttons
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 12));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER));
        RoundedButton btnCancel = new RoundedButton("Huỷ",  new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        btnSave = new RoundedButton(entity == null ? "Thêm khách hàng" : "Lưu thay đổi", UIConstants.PRIMARY, Color.WHITE);
        btnSave.setPreferredSize(new Dimension(160, 38));
        btnCancel.setPreferredSize(new Dimension(80, 38));
        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e   -> doSave());
        footer.add(btnCancel);
        footer.add(btnSave);

        root.add(header, BorderLayout.NORTH);
        root.add(scrollPane, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
    }

    /** Ẩn/hiện panel passport/visa dựa trên quốc tịch */
    private void togglePassportFields() {
        String qt = txtQuocTich.getText().trim();
        boolean isNuocNgoai = !qt.isEmpty() 
            && !qt.equalsIgnoreCase("Việt Nam") 
            && !qt.equalsIgnoreCase("Viet Nam")
            && !qt.equalsIgnoreCase("VN");
        
        passportPanel.setVisible(isNuocNgoai);
        if (isNuocNgoai) {
            cboLoaiGiayTo.setSelectedItem("PASSPORT");
        }
        revalidate();
        repaint();
    }

    private void fillData() {
        txtTen.setText(entity.getHoTen());
        txtSDT.setText(entity.getSdt());
        txtCCCD.setText(entity.getCccd() != null ? entity.getCccd() : "");
        if (entity.getNgaySinh() != null) dtNgaySinh.setDate(java.sql.Date.valueOf(entity.getNgaySinh()));
        if (entity.getGioiTinh() != null) cboGioiTinh.setSelectedItem(entity.getGioiTinh());
        txtQuocTich.setText(entity.getQuocTich() != null ? entity.getQuocTich() : "Việt Nam");
        
        // Passport / Visa
        if (entity.getLoaiGiayTo() != null) cboLoaiGiayTo.setSelectedItem(entity.getLoaiGiayTo());
        if (entity.getSoHoChieu() != null) txtSoHoChieu.setText(entity.getSoHoChieu());
        if (entity.getNoiCapHoChieu() != null) txtNoiCapHC.setText(entity.getNoiCapHoChieu());
        if (entity.getSoVisa() != null) txtSoVisa.setText(entity.getSoVisa());
        if (entity.getNgayHetHanVisa() != null) dtHetHanVisa.setDate(java.sql.Date.valueOf(entity.getNgayHetHanVisa()));
        if (entity.getNgayNhapCanh() != null) dtNhapCanh.setDate(java.sql.Date.valueOf(entity.getNgayNhapCanh()));
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

    private boolean validateTen() {
        String ten = txtTen.getText().trim();
        ten = formatName(ten);
        txtTen.setText(ten);
        
        errTen.setText(" ");
        if (ten.isEmpty()) { errTen.setText("Bắt buộc - Vui lòng nhập họ và tên"); return false; }
        if (!ten.matches(REGEX_TEN)) { errTen.setText("Họ tên không hợp lệ (2-50 ký tự, chỉ gồm chữ cái)"); return false; }
        return true;
    }

    private boolean validateSDT() {
        String sdt = txtSDT.getText().trim();
        errSDT.setText(" ");
        if (sdt.isEmpty()) { errSDT.setText("Bắt buộc - Vui lòng nhập số điện thoại"); return false; }
        if (!sdt.matches(REGEX_SDT)) { errSDT.setText("SĐT không đúng định dạng (10 số, bắt đầu 03/05/07/08/09)"); return false; }
        return true;
    }

    private boolean validateCCCD() {
        String cccd = txtCCCD.getText().trim();
        errCCCD.setText(" ");
        if (!cccd.isEmpty() && !cccd.matches(REGEX_CCCD)) {
            errCCCD.setText("CCCD/Passport không hợp lệ (9 số, 12 số, hoặc passport)"); return false;
        }
        return true;
    }

    private boolean validateNgaySinh() {
        errNgaySinh.setText(" ");
        return true;
    }

    /** Validate passport/visa cho khách nước ngoài */
    private boolean validatePassport() {
        errPassport.setText(" ");
        String qt = txtQuocTich.getText().trim();
        boolean isNuocNgoai = !qt.isEmpty() 
            && !qt.equalsIgnoreCase("Việt Nam") 
            && !qt.equalsIgnoreCase("Viet Nam")
            && !qt.equalsIgnoreCase("VN");
        
        if (!isNuocNgoai) return true; // Khách VN → không cần validate

        String hc = txtSoHoChieu.getText().trim();
        if (hc.isEmpty()) {
            errPassport.setText("Bắt buộc: Khách nước ngoài phải có số hộ chiếu (theo pháp luật VN)");
            return false;
        }

        // Cảnh báo visa hết hạn (soft warning, không block)
        try {
            if (dtHetHanVisa.getDate() != null) {
                java.time.LocalDate hhv = dtHetHanVisa.getDate().toInstant()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                if (hhv.isBefore(java.time.LocalDate.now())) {
                    int choice = JOptionPane.showConfirmDialog(this,
                        "⚠️ Visa đã hết hạn ngày " + hhv + ".\n\nBạn vẫn muốn tiếp tục?",
                        "Cảnh báo visa hết hạn", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (choice != JOptionPane.YES_OPTION) return false;
                }
            }
        } catch (Exception ignored) {}

        return true;
    }

    private void doSave() {
        boolean ok1 = validateTen();
        boolean ok2 = validateSDT();
        boolean ok3 = validateCCCD();
        boolean ok4 = validateNgaySinh();
        boolean ok5 = validatePassport();
        if (!ok1 || !ok2 || !ok3 || !ok4 || !ok5) return;

        String ten  = txtTen.getText().trim();
        String sdt  = txtSDT.getText().trim();
        String cccd = txtCCCD.getText().trim();

        KhachHang kh = entity != null ? entity : new KhachHang();
        kh.setHoTen(ten);
        kh.setSdt(sdt);
        kh.setCccd(cccd.isEmpty() ? null : cccd);
        
        try {
            if (dtNgaySinh.getDate() != null) {
                kh.setNgaySinh(dtNgaySinh.getDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
            } else {
                kh.setNgaySinh(null);
            }
        } catch (Exception ignored) {}
        
        kh.setGioiTinh((String) cboGioiTinh.getSelectedItem());
        kh.setQuocTich(txtQuocTich.getText().trim().isEmpty() ? "Việt Nam" : txtQuocTich.getText().trim());

        // Loại giấy tờ
        kh.setLoaiGiayTo((String) cboLoaiGiayTo.getSelectedItem());

        // Passport / Visa
        String hc = txtSoHoChieu.getText().trim();
        kh.setSoHoChieu(hc.isEmpty() ? null : hc);
        String nc = txtNoiCapHC.getText().trim();
        kh.setNoiCapHoChieu(nc.isEmpty() ? null : nc);
        String visa = txtSoVisa.getText().trim();
        kh.setSoVisa(visa.isEmpty() ? null : visa);

        try {
            if (dtHetHanVisa.getDate() != null) {
                kh.setNgayHetHanVisa(dtHetHanVisa.getDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
            }
        } catch (Exception ignored) {}
        try {
            if (dtNhapCanh.getDate() != null) {
                kh.setNgayNhapCanh(dtNhapCanh.getDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
            }
        } catch (Exception ignored) {}

        String err = entity == null ? service.them(kh) : service.sua(kh);
        if (err == null) {
            confirmed = true;
            dispose();
        } else {
            showError(err);
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Dữ liệu không hợp lệ", JOptionPane.WARNING_MESSAGE);
    }

    public boolean isConfirmed() { return confirmed; }

    // ---- Helpers ----
    private JTextField field() {
        JTextField f = new JTextField();
        f.setFont(UIConstants.FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
            BorderFactory.createEmptyBorder(5, 9, 5, 9)));
        f.setPreferredSize(new Dimension(0, 34));
        return f;
    }

    private void setPlaceholder(JTextField f, String hint) {
        f.setToolTipText(hint);
        f.putClientProperty("JTextField.placeholderText", hint);
    }

    private JPanel labelField(String label, JComponent comp, JLabel errLbl) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_PRIMARY);
        p.add(lbl, BorderLayout.NORTH); 
        
        JPanel content = new JPanel(new BorderLayout(0, 2));
        content.setOpaque(false);
        content.add(comp, BorderLayout.CENTER);
        if (errLbl != null) {
            content.add(errLbl, BorderLayout.SOUTH);
        }
        
        p.add(content, BorderLayout.CENTER);
        // Ensure fixed height for form rows to prevent stretching in GridBagLayout
        int preferredHeight = (errLbl != null) ? 68 : 58;
        p.setPreferredSize(new Dimension(300, preferredHeight));
        p.setMinimumSize(new Dimension(300, preferredHeight));
        return p;
    }

    private JLabel errLabel() {
        JLabel err = new JLabel(" ");
        err.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        err.setForeground(UIConstants.DANGER);
        return err;
    }
}
