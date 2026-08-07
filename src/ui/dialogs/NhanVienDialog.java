package ui.dialogs;

import service.NhanVienService;
import entity.NhanVien;
import entity.TaiKhoan;
import entity.enums.VaiTro;
import ui.components.UIConstants;
import ui.components.RoundedComponents.*;

import javax.swing.*;
import java.awt.*;

public class NhanVienDialog extends JDialog {

    private final NhanVienService service = new NhanVienService();
    private final NhanVien entity;
    private final boolean readOnly;
    private boolean confirmed = false;

    private JTextField txtTen, txtSDT, txtEmail, txtCCCD, txtDiaChi;
    private ui.components.DatePicker dtNgaySinh, dtNgayVaoLam;
    private JPasswordField txtMatKhau;
    private JComboBox<String> cboChucVu, cboVaiTro, cboGioiTinh;
    private JCheckBox chkActive;

    // Regex hợp lệ
    private static final String REGEX_TEN = "^[\\p{L} .'-]{2,50}$";
    private static final String REGEX_SDT = "^(0[35789])[0-9]{8}$";
    private static final String REGEX_MK = "^.{6,32}$";
    private static final String REGEX_EMAIL = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
    private static final String REGEX_CCCD = "^(\\d{9}|\\d{12})$";

    private JLabel errTen, errSDT, errMK, errEmail, errCCCD, errNgay;

    public NhanVienDialog(Frame parent, NhanVien nv) {
        this(parent, nv, false);
    }

    public NhanVienDialog(Frame parent, NhanVien nv, boolean readOnly) {
        super(parent, nv == null ? "Thêm nhân viên mới"
                : (readOnly ? "Xem thông tin – " + nv.getHoTen() : "Chỉnh sửa – " + nv.getHoTen()), true);
        this.entity = nv;
        this.readOnly = readOnly;
        setSize(450, 580);
        setLocationRelativeTo(parent);
        setResizable(false);
        buildUI();
        if (nv != null)
            fillData();
        if (readOnly)
            applyReadOnlyMode();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIConstants.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        JLabel title = new JLabel(entity == null ? "Thêm nhân viên mới" : "Chỉnh sửa nhân viên");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(Color.WHITE);
        JLabel lblMa = new JLabel(entity != null ? "Mã: " + entity.getMaNhanVien() : "Mã sẽ được tự động tạo");
        lblMa.setFont(UIConstants.FONT_SMALL);
        lblMa.setForeground(new Color(255, 255, 255, 180));
        JPanel hLeft = new JPanel();
        hLeft.setOpaque(false);
        hLeft.setLayout(new BoxLayout(hLeft, BoxLayout.Y_AXIS));
        hLeft.add(title);
        hLeft.add(lblMa);
        header.add(hLeft, BorderLayout.WEST);

        // Form
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(20, 24, 8, 24));

        txtTen = field();
        txtSDT = field();
        txtEmail = field();
        txtCCCD = field();
        txtDiaChi = field();
        dtNgaySinh = new ui.components.DatePicker(null);
        dtNgayVaoLam = new ui.components.DatePicker(new java.util.Date()); // Hôm nay
        txtMatKhau = new JPasswordField();
        styleField(txtMatKhau);

        errTen = errLabel();
        errSDT = errLabel();
        errMK = errLabel();
        errEmail = errLabel();
        errCCCD = errLabel();
        errNgay = errLabel();

        txtTen.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                validateTen();
            }
        });
        txtSDT.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                validateSDT();
            }
        });
        txtEmail.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                validateEmail();
            }
        });
        txtCCCD.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                validateCCCD();
            }
        });
        txtMatKhau.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                validateMK();
            }
        });

        // Chức vụ hiển thị & vai trò hệ thống đồng nhất
        cboChucVu = combo("Lễ tân", "Quản lý", "Quản trị viên");
        cboVaiTro = combo("RECEPTIONIST", "MANAGER", "ADMIN");
        cboGioiTinh = combo("Nam", "Nữ", "Khác");
        chkActive = new JCheckBox("Đang làm việc", true);
        chkActive.setBackground(Color.WHITE);
        chkActive.setFont(UIConstants.FONT_BODY);

        // Đồng bộ chức vụ ↔ vai trò
        cboChucVu.addActionListener(e -> {
            int idx = cboChucVu.getSelectedIndex();
            if (idx >= 0 && idx < cboVaiTro.getItemCount())
                cboVaiTro.setSelectedIndex(idx);
        });
        cboVaiTro.addActionListener(e -> {
            int idx = cboVaiTro.getSelectedIndex();
            if (idx >= 0 && idx < cboChucVu.getItemCount())
                cboChucVu.setSelectedIndex(idx);
        });

        String pwHint = entity == null ? " *" : " (để trống = giữ nguyên)";

        // Cấu trúc form: 2 cột cho các thông tin ngắn
        JPanel fContent = new JPanel(new GridLayout(0, 2, 10, 8));
        fContent.setOpaque(false);
        fContent.add(labelField("Họ và tên *", txtTen, errTen));
        fContent.add(labelField("Số điện thoại *", txtSDT, errSDT));
        fContent.add(labelField("Email", txtEmail, errEmail));
        fContent.add(labelField("Số CCCD/ID Card *", txtCCCD, errCCCD));
        fContent.add(labelField("Ngày sinh *", dtNgaySinh, errNgay));
        fContent.add(labelField("Giới tính", cboGioiTinh, null));
        fContent.add(labelField("Ngày vào làm", dtNgayVaoLam, null));
        fContent.add(labelField("Chức vụ", cboChucVu, null));
        fContent.add(labelField("Vai trò hệ thống", cboVaiTro, null));
        fContent.add(labelField("Mật khẩu" + pwHint, txtMatKhau, errMK));
        fContent.add(labelField("Trạng thái", chkActive, null));

        body.add(fContent);
        body.add(Box.createVerticalStrut(8));
        body.add(labelField("Địa chỉ liên lạc", txtDiaChi, null));

        setSize(700, 720); // Tăng kích thước dialog vì nhiều thông tin hơn
        setLocationRelativeTo(null); // Đưa ra giữa trung tâm màn hình sau khi tăng kích thước

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER));
        RoundedButton btnCancel = new RoundedButton(readOnly ? "Đóng" : "Huỷ", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        btnCancel.addActionListener(e -> dispose());
        footer.add(btnCancel);

        // === PHÂN QUYỀN: Ẩn nút Lưu khi chế độ chỉ xem ===
        if (!readOnly) {
            RoundedButton btnSave = new RoundedButton(entity == null ? "Thêm nhân viên" : "Lưu thay đổi",
                    UIConstants.PRIMARY, Color.WHITE);
            btnSave.addActionListener(e -> doSave());
            getRootPane().setDefaultButton(btnSave);
            footer.add(btnSave);
        }

        JScrollPane scrollPane = new JScrollPane(body);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        root.add(header, BorderLayout.NORTH);
        root.add(scrollPane, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void fillData() {
        txtTen.setText(entity.getHoTen());
        txtSDT.setText(entity.getSdt());
        txtEmail.setText(entity.getEmail());
        txtCCCD.setText(entity.getCccd());
        txtDiaChi.setText(entity.getDiaChi());
        if (entity.getNgaySinh() != null)
            dtNgaySinh.setDate(java.sql.Date.valueOf(entity.getNgaySinh()));
        if (entity.getNgayVaoLam() != null)
            dtNgayVaoLam.setDate(java.sql.Date.valueOf(entity.getNgayVaoLam()));
        chkActive.setSelected(entity.isDangLamViec());
        setCombo(cboGioiTinh, entity.getGioiTinh());

        // KHÔNG điền mật khẩu vào trường – để trống khi sửa
        if (entity.getTaiKhoan() != null) {
            VaiTro vaiTro = entity.getTaiKhoan().getVaiTro();
            if (vaiTro != null) {
                setCombo(cboVaiTro, vaiTro.name());
                // Đồng bộ chức vụ hiển thị
                int idx = cboVaiTro.getSelectedIndex();
                if (idx >= 0)
                    cboChucVu.setSelectedIndex(idx);
            }
        } else {
            setCombo(cboChucVu, entity.getChucVu());
        }
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
        if (ten.isEmpty()) {
            errTen.setText("Bắt buộc - Vui lòng nhập họ và tên");
            return false;
        }
        if (!ten.matches(REGEX_TEN)) {
            errTen.setText("Họ tên không hợp lệ (2-50 ký tự, chỉ gồm chữ cái)");
            return false;
        }
        return true;
    }

    private boolean validateSDT() {
        String sdt = txtSDT.getText().trim();
        errSDT.setText(" ");
        if (sdt.isEmpty()) {
            errSDT.setText("Bắt buộc - Vui lòng nhập số điện thoại");
            return false;
        }
        if (!sdt.matches(REGEX_SDT)) {
            errSDT.setText("SĐT không đúng định dạng (10 số, bắt đầu 03/05/07/08/09)");
            return false;
        }
        return true;
    }

    private boolean validateMK() {
        String pw = new String(txtMatKhau.getPassword()).trim();
        errMK.setText(" ");
        if (entity == null && pw.isEmpty()) {
            errMK.setText("Bắt buộc khi thêm mới!");
            return false;
        }
        if (!pw.isEmpty() && !pw.matches(REGEX_MK)) {
            errMK.setText("Mật khẩu từ 6-32 ký tự!");
            return false;
        }
        return true;
    }

    private boolean validateEmail() {
        String email = txtEmail.getText().trim();
        errEmail.setText(" ");
        if (!email.isEmpty() && !email.matches(REGEX_EMAIL)) {
            errEmail.setText("Email không hợp lệ!");
            return false;
        }
        return true;
    }

    private boolean validateCCCD() {
        String cccd = txtCCCD.getText().trim();
        errCCCD.setText(" ");
        if (cccd.isEmpty()) {
            errCCCD.setText("Bắt buộc nhập CCCD!");
            return false;
        }
        if (!cccd.matches(REGEX_CCCD)) {
            errCCCD.setText("CCCD phải 9 hoặc 12 số!");
            return false;
        }
        return true;
    }

    private boolean validateNgay() {
        errNgay.setText(" ");
        try {
            java.util.Date birthDate = dtNgaySinh.getDate();
            if (birthDate == null) {
                errNgay.setText("Bắt buộc chọn ngày sinh!");
                return false;
            }
            java.time.LocalDate birth = birthDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            if (birth.isAfter(java.time.LocalDate.now().minusYears(18))) {
                errNgay.setText("Nhân viên phải từ 18 tuổi!");
                return false;
            }
        } catch (Exception e) {
            errNgay.setText("Vui lòng chọn ngày hợp lệ");
            return false;
        }
        return true;
    }

    private void doSave() {
        boolean ok1 = validateTen();
        boolean ok2 = validateSDT();
        boolean ok3 = validateMK();
        boolean ok4 = validateEmail();
        boolean ok5 = validateCCCD();
        boolean ok7 = validateNgay();
        if (!ok1 || !ok2 || !ok3 || !ok4 || !ok5 || !ok7)
            return;

        String ten = txtTen.getText().trim();
        String sdt = txtSDT.getText().trim();
        String pw = new String(txtMatKhau.getPassword()).trim();

        NhanVien nv = entity != null ? entity : new NhanVien();
        nv.setHoTen(ten);
        nv.setSdt(sdt);
        nv.setEmail(txtEmail.getText().trim());
        nv.setCccd(txtCCCD.getText().trim());
        nv.setDiaChi(txtDiaChi.getText().trim());
        nv.setDangLamViec(chkActive.isSelected());
        nv.setGioiTinh((String) cboGioiTinh.getSelectedItem());

        try {
            if (dtNgaySinh.getDate() != null)
                nv.setNgaySinh(dtNgaySinh.getDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());

            if (dtNgayVaoLam.getDate() != null)
                nv.setNgayVaoLam(
                        dtNgayVaoLam.getDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
        } catch (Exception e) {
            showErr("Lỗi định dạng ngày!");
            return;
        }

        // Chức vụ từ combo
        String[] chucVuMap = { "Lễ tân", "Quản lý", "Quản trị viên" };
        int cvIdx = cboChucVu.getSelectedIndex();
        nv.setChucVu(cvIdx >= 0 ? chucVuMap[cvIdx] : "Lễ tân");

        // Tài khoản
        if (nv.getTaiKhoan() == null) {
            TaiKhoan tk = new TaiKhoan();
            tk.setTenDangNhap(nv.getMaNhanVien() != null ? nv.getMaNhanVien() : "");
            tk.setVaiTro(VaiTro.RECEPTIONIST);
            tk.setTrangThai(chkActive.isSelected());
            nv.setTaiKhoan(tk);
            tk.setNhanVien(nv);
        } else {
            nv.getTaiKhoan().setTrangThai(chkActive.isSelected());
        }

        String rolStr = (String) cboVaiTro.getSelectedItem();
        try {
            nv.getTaiKhoan().setVaiTro(VaiTro.valueOf(rolStr));
        } catch (Exception ex) {
            nv.getTaiKhoan().setVaiTro(VaiTro.RECEPTIONIST);
        }

        if (!pw.isEmpty()) {
            nv.getTaiKhoan().setMatKhau(pw);
        } else if (entity == null) {
            nv.getTaiKhoan().setMatKhau("123456");
        }

        String err = entity == null ? service.them(nv) : service.sua(nv);
        if (err == null) {
            confirmed = true;
            dispose();
        } else
            showErr(err);
    }

    private void showErr(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Dữ liệu không hợp lệ", JOptionPane.WARNING_MESSAGE);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private JTextField field() {
        JTextField f = new JTextField();
        styleField(f);
        return f;
    }

    private void styleField(JTextField f) {
        f.setFont(UIConstants.FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(5, 9, 5, 9)));
        f.setPreferredSize(new Dimension(0, 34));
    }

    private JComboBox<String> combo(String... items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setFont(UIConstants.FONT_BODY);
        c.setPreferredSize(new Dimension(0, 34));
        return c;
    }

    private JPanel labelField(String label, JComponent comp, JLabel errLbl) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_PRIMARY);
        p.add(lbl, BorderLayout.NORTH);
        JPanel c = new JPanel(new BorderLayout());
        c.setOpaque(false);
        c.add(comp, BorderLayout.NORTH);
        if (errLbl != null)
            c.add(errLbl, BorderLayout.CENTER);
        p.add(c, BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, errLbl != null ? 64 : 54));
        return p;
    }

    private JLabel errLabel() {
        JLabel err = new JLabel(" ");
        err.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        err.setForeground(UIConstants.DANGER);
        return err;
    }

    private void setCombo(JComboBox<String> c, String val) {
        if (val == null)
            return;
        for (int i = 0; i < c.getItemCount(); i++)
            if (val.equals(c.getItemAt(i))) {
                c.setSelectedIndex(i);
                return;
            }
    }

    /** === PHÂN QUYỀN: Khóa toàn bộ form khi chế độ chỉ xem === */
    private void applyReadOnlyMode() {
        txtTen.setEditable(false);
        txtSDT.setEditable(false);
        txtEmail.setEditable(false);
        txtCCCD.setEditable(false);
        txtDiaChi.setEditable(false);
        txtMatKhau.setEditable(false);
        cboChucVu.setEnabled(false);
        cboVaiTro.setEnabled(false);
        cboGioiTinh.setEnabled(false);
        chkActive.setEnabled(false);
        dtNgaySinh.setEnabled(false);
        dtNgayVaoLam.setEnabled(false);

        // Đổi màu nền nhẹ để người dùng nhận biết là read-only
        Color readOnlyBg = new Color(0xF8FAFC);
        for (JTextField f : new JTextField[]{txtTen, txtSDT, txtEmail, txtCCCD, txtDiaChi}) {
            f.setBackground(readOnlyBg);
        }
        txtMatKhau.setBackground(readOnlyBg);
    }
}


