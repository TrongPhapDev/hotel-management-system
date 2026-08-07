package ui;

import ui.components.RoundedComponents.RoundedBorder;
import ui.components.RoundedComponents.RoundedButton;
import ui.components.UIConstants;
import entity.NhanVien;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import service.AuthService;

public class LoginFrame extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private RoundedButton btnLogin;
    private JLabel lblError;
    private JLabel lblAttempts; // Hiển thị số lần còn lại
    private volatile boolean loginInProgress = false;

    // === CƠ CHẾ KHÓA KHI SAI QUÁ NHIỀU LẦN ===
    private static final int MAX_ATTEMPTS = 5; // Tối đa 5 lần sai
    private static final int LOCKOUT_SECONDS = 30; // Khóa 30 giây (Theo yêu cầu người dùng)
    private static final String LOCKOUT_FILE = "login_lockout.dat";
    private int failedAttempts = 0;
    private boolean isLockedOut = false;
    private Timer lockoutTimer;
    private int lockoutRemaining = 0; // Số giây còn lại
    private long lockoutEndTime = 0;

    public LoginFrame() {
        setTitle("Đăng nhập – Hotel MS");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 520);
        setLocationRelativeTo(null);
        setResizable(false);
        loadLockoutState();
        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new GridLayout(1, 2));
        root.setBackground(Color.WHITE);

        root.add(buildLeftPanel());
        root.add(buildRightPanel());

        setContentPane(root);
    }

    // ---- Panel trái: logo + branding (xanh gradient) ----
    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(0x3B82F6),
                        getWidth(), getHeight(), new Color(0x1E40AF));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Circles decoration
                g2.setColor(new Color(255, 255, 255, 25));
                g2.fillOval(-60, -60, 220, 220);
                g2.fillOval(getWidth() - 80, getHeight() - 80, 200, 200);
                g2.dispose();
            }
        };
        panel.setLayout(new GridBagLayout());

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Logo
        JPanel logoBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 50));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 32));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("H", (getWidth() - fm.stringWidth("H")) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        logoBox.setOpaque(false);
        logoBox.setPreferredSize(new Dimension(72, 72));
        logoBox.setMaximumSize(new Dimension(72, 72));
        logoBox.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitle = new JLabel("Hotel MS");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("Hệ thống quản lý khách sạn");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(new Color(255, 255, 255, 200));
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(Box.createVerticalStrut(10));
        content.add(logoBox);
        content.add(Box.createVerticalStrut(16));
        content.add(lblTitle);
        content.add(Box.createVerticalStrut(6));
        content.add(lblSub);
        content.add(Box.createVerticalStrut(30));

        // Features list
        String[] features = { "Quản lý phòng & đặt phòng", "Theo dõi khách hàng", "Báo cáo & thống kê" };
        for (String f : features) {
            JLabel lbl = new JLabel(f);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lbl.setForeground(new Color(255, 255, 255, 180));
            lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(lbl);
            content.add(Box.createVerticalStrut(6));
        }

        // Version footer
        content.add(Box.createVerticalStrut(30));
        JLabel lblVer = new JLabel("v1.0.0 \u2014 Enterprise Edition");
        lblVer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblVer.setForeground(new Color(255, 255, 255, 100));
        lblVer.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(lblVer);

        panel.add(content);
        return panel;
    }

    // ---- Panel phải: form đăng nhập ----
    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setPreferredSize(new Dimension(300, 380));

        JLabel lblWelcome = new JLabel("Chào mừng trở lại!");
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblWelcome.setForeground(UIConstants.TEXT_PRIMARY);
        lblWelcome.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblNote = new JLabel("Vui lòng đăng nhập để tiếp tục");
        lblNote.setFont(UIConstants.FONT_BODY);
        lblNote.setForeground(UIConstants.TEXT_SECONDARY);
        lblNote.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(lblWelcome);
        form.add(Box.createVerticalStrut(4));
        form.add(lblNote);
        form.add(Box.createVerticalStrut(28));

        // Username
        form.add(buildFieldLabel("Tên đăng nhập"));
        form.add(Box.createVerticalStrut(4));
        txtUsername = new JTextField();
        styleField(txtUsername);
        txtUsername.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateUsername();
            }
        });
        txtUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(txtUsername);
        form.add(Box.createVerticalStrut(14));

        // Password
        form.add(buildFieldLabel("Mật khẩu"));
        form.add(Box.createVerticalStrut(4));
        txtPassword = new JPasswordField();
        txtPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(wrapPasswordField(txtPassword));
        form.add(Box.createVerticalStrut(12));

        // Error label (hỗ trợ HTML multi-line)
        lblError = new JLabel(" ");
        lblError.setFont(UIConstants.FONT_SMALL);
        lblError.setForeground(UIConstants.DANGER);
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblError);

        // Attempts label (số lần đăng nhập còn lại)
        lblAttempts = new JLabel(" ");
        lblAttempts.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblAttempts.setForeground(UIConstants.TEXT_MUTED);
        lblAttempts.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblAttempts);
        form.add(Box.createVerticalStrut(8));

        // Login button
        btnLogin = new RoundedButton("Đăng nhập", UIConstants.PRIMARY, Color.WHITE);
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(btnLogin);
        form.add(Box.createVerticalStrut(14));

        // Footer hint
        JLabel lblHint = new JLabel("Liên hệ quản trị viên nếu quên mật khẩu");
        lblHint.setFont(UIConstants.FONT_SMALL);
        lblHint.setForeground(UIConstants.TEXT_MUTED);
        lblHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblHint);

        panel.add(form);

        // Events
        btnLogin.addActionListener(e -> doLogin());
        txtPassword.addActionListener(e -> doLogin());
        txtUsername.addActionListener(e -> txtPassword.requestFocus());

        return panel;
    }

    private JLabel buildFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UIConstants.FONT_BODY_BOLD);
        lbl.setForeground(UIConstants.TEXT_PRIMARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private void styleField(JTextField field) {
        field.setFont(UIConstants.FONT_BODY);
        field.setPreferredSize(new Dimension(300, 38));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        field.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                resetFieldBorder(field);
                field.setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.PRIMARY),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)));
                // Chỉ xóa lỗi nếu không đang lockout
                if (!isLockedOut) {
                    lblError.setForeground(UIConstants.DANGER);
                    lblError.setText(" ");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                // Chỉ reset về border bình thường nếu không có lỗi
                Boolean hasError = (Boolean) field.getClientProperty("hasError");
                if (hasError == null || !hasError) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                            new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
                            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
                }
            }
        });
    }

    // ========================================================
    // VALIDATION — Thông báo rõ ràng theo từng lỗi cụ thể
    // ========================================================

    private boolean validateUsername() {
        String username = txtUsername.getText().trim();
        resetFieldBorder(txtUsername);
        lblError.setForeground(UIConstants.DANGER);

        if (username.isEmpty()) {
            setErrorBorder(txtUsername);
            lblError.setText("Tên đăng nhập không được để trống.");
            return false;
        }

        if (username.length() < 3) {
            setErrorBorder(txtUsername);
            lblError.setText("Tên đăng nhập phải có ít nhất 3 ký tự.");
            return false;
        }

        if (username.length() > 20) {
            setErrorBorder(txtUsername);
            lblError.setText("Tên đăng nhập không được vượt quá 20 ký tự.");
            return false;
        }

        if (username.contains(" ")) {
            setErrorBorder(txtUsername);
            lblError.setText("Tên đăng nhập không được chứa khoảng trắng.");
            return false;
        }

        if (!username.matches("^[a-zA-Z].*")) {
            setErrorBorder(txtUsername);
            lblError.setText("Tên đăng nhập phải bắt đầu bằng chữ cái (a-z).");
            return false;
        }

        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            setErrorBorder(txtUsername);
            lblError.setText("Chỉ được chứa chữ cái (a-z), số (0-9) và dấu gạch dưới (_).");
            return false;
        }

        return true;
    }

    private boolean validatePassword() {
        String password = new String(txtPassword.getPassword());
        resetFieldBorder(txtPassword);
        lblError.setForeground(UIConstants.DANGER);

        if (password.isEmpty()) {
            setErrorBorder(txtPassword);
            lblError.setText("Mật khẩu không được để trống.");
            return false;
        }

        if (password.length() < 3) {
            setErrorBorder(txtPassword);
            lblError.setText("Mật khẩu phải có ít nhất 3 ký tự.");
            return false;
        }

        if (password.length() > 50) {
            setErrorBorder(txtPassword);
            lblError.setText("Mật khẩu không được vượt quá 50 ký tự.");
            return false;
        }

        return true;
    }

    private boolean validateFields() {
        // Validate tuần tự: username trước, password sau (tránh hiển thị 2 lỗi cùng
        // lúc)
        boolean isUserValid = validateUsername();
        if (!isUserValid)
            return false;

        boolean isPassValid = validatePassword();
        if (!isPassValid)
            return false;

        lblError.setText(" ");
        return true;
    }

    // ========================================================
    // LOCKOUT — Khóa đăng nhập khi sai quá 5 lần
    // ========================================================

    /** Bắt đầu đếm ngược khóa */
    private void startLockout() {
        isLockedOut = true;

        if (lockoutRemaining <= 0) {
            lockoutRemaining = LOCKOUT_SECONDS;
            lockoutEndTime = System.currentTimeMillis() + (LOCKOUT_SECONDS * 1000);
        }

        saveLockoutState();

        btnLogin.setEnabled(false);
        txtUsername.setEnabled(false);
        txtPassword.setEnabled(false);

        lblError.setForeground(UIConstants.DANGER);
        updateLockoutDisplay();

        // Timer đếm ngược mỗi giây
        lockoutTimer = new Timer(1000, e -> {
            lockoutRemaining--;
            if (lockoutRemaining <= 0) {
                endLockout();
            } else {
                updateLockoutDisplay();
            }
        });
        lockoutTimer.start();
    }

    /** Cập nhật hiển thị thời gian chờ */
    private void updateLockoutDisplay() {
        int minutes = lockoutRemaining / 60;
        int seconds = lockoutRemaining % 60;
        lblError.setText(String.format(
                "<html>Đăng nhập sai quá %d lần!<br>Vui lòng thử lại sau <b>%02d:%02d</b></html>",
                MAX_ATTEMPTS, minutes, seconds));
        lblAttempts.setText(" ");
        btnLogin.setText("Đang khóa...");
    }

    /** Kết thúc lockout, mở khóa lại form */
    private void endLockout() {
        isLockedOut = false;
        failedAttempts = 0;
        lockoutRemaining = 0;
        if (lockoutTimer != null) {
            lockoutTimer.stop();
            lockoutTimer = null;
        }
        lockoutEndTime = 0;
        saveLockoutState();

        btnLogin.setEnabled(true);
        txtUsername.setEnabled(true);
        txtPassword.setEnabled(true);
        btnLogin.setText("Đăng nhập");

        lblError.setForeground(new Color(0x16A34A)); // Xanh lá — thành công
        lblError.setText("Đã mở khóa. Bạn có thể đăng nhập lại.");
        lblAttempts.setText(" ");

        txtUsername.setText("");
        txtPassword.setText("");
        txtUsername.requestFocus();
    }

    /** Cập nhật hiển thị số lần thử còn lại */
    private void updateAttemptsLabel() {
        if (failedAttempts == 0) {
            lblAttempts.setText(" ");
        } else {
            int remaining = MAX_ATTEMPTS - failedAttempts;
            lblAttempts.setForeground(remaining <= 2 ? UIConstants.DANGER : UIConstants.WARNING);
            lblAttempts.setText("Còn " + remaining + "/" + MAX_ATTEMPTS + " lần thử");
        }
    }

    // ========================================================
    // LOGIN FLOW
    // ========================================================

    private void setErrorBorder(JTextField field) {
        field.putClientProperty("hasError", true);
        Component target = field;
        int padding = 6;
        
        // Nếu field được bọc (có con mắt), ta áp dụng border vào khung bọc ngoài cùng
        if (Boolean.TRUE.equals(field.getClientProperty("isWrapped"))) {
            target = field.getParent().getParent(); // wrap panel
            padding = 0;
        }
        
        ((JComponent) target).setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.DANGER),
                BorderFactory.createEmptyBorder(padding, 10, padding, 10)));
    }

    private void resetFieldBorder(JTextField field) {
        field.putClientProperty("hasError", false);
        Component target = field;
        int padding = 6;
        
        if (Boolean.TRUE.equals(field.getClientProperty("isWrapped"))) {
            target = field.getParent().getParent(); // wrap panel
            padding = 0;
        }
        
        ((JComponent) target).setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(padding, 10, padding, 10)));
    }

    private JPanel wrapPasswordField(JPasswordField field) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(true);
        wrap.setBackground(Color.WHITE);
        wrap.setPreferredSize(new Dimension(300, 38));
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));

        field.setFont(UIConstants.FONT_BODY);
        field.setOpaque(false);
        field.setBorder(null); 
        field.putClientProperty("isWrapped", true); // Đánh dấu để xử lý border ở setError/resetBorder

        JButton btnEye = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x94A3B8));
                int w = getWidth(), h = getHeight();
                int ew = 20, eh = 12;
                int x = (w - ew) / 2, y = (h - eh) / 2;
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Almond shape
                java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
                path.moveTo(x, y + eh / 2.0);
                path.quadTo(x + ew / 2.0, y - 2, x + ew, y + eh / 2.0);
                path.quadTo(x + ew / 2.0, y + eh + 2, x, y + eh / 2.0);
                g2.draw(path);

                // Pupil
                g2.fillOval(x + ew / 2 - 3, y + eh / 2 - 3, 6, 6);

                if (field.getEchoChar() != (char) 0) {
                    g2.setStroke(new BasicStroke(1.8f));
                    g2.drawLine(x, y + eh - 1, x + ew, y + 1);
                }
                g2.dispose();
            }
        };
        btnEye.setPreferredSize(new Dimension(38, 38));
        btnEye.setContentAreaFilled(false);
        btnEye.setBorderPainted(false);
        btnEye.setFocusPainted(false);
        btnEye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEye.addActionListener(e -> {
            field.setEchoChar(field.getEchoChar() == (char) 0 ? '•' : (char) 0);
            btnEye.repaint();
        });

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                wrap.setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.PRIMARY),
                        BorderFactory.createEmptyBorder(0, 0, 0, 0)));
                if (!isLockedOut) {
                    lblError.setForeground(UIConstants.DANGER);
                    lblError.setText(" ");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                validatePassword();
                Boolean hasError = (Boolean) field.getClientProperty("hasError");
                if (hasError == null || !hasError) {
                    wrap.setBorder(BorderFactory.createCompoundBorder(
                            new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
                            BorderFactory.createEmptyBorder(0, 0, 0, 0)));
                }
            }
        });

        JPanel fieldInnerWrap = new JPanel(new BorderLayout());
        fieldInnerWrap.setOpaque(false);
        fieldInnerWrap.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        fieldInnerWrap.add(field, BorderLayout.CENTER);

        wrap.add(fieldInnerWrap, BorderLayout.CENTER);
        wrap.add(btnEye, BorderLayout.EAST);
        return wrap;
    }

    private void doLogin() {
        if (loginInProgress)
            return;
        if (isLockedOut)
            return;
        if (!validateFields())
            return;

        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        setLoginLoading(true);
        lblError.setForeground(UIConstants.TEXT_SECONDARY);
        lblError.setText("Đang xác thực...");

        SwingWorker<NhanVien, Void> worker = new SwingWorker<NhanVien, Void>() {
            @Override
            protected NhanVien doInBackground() {
                return AuthService.getInstance().dangNhap(username, password);
            }

            @Override
            protected void done() {
                try {
                    NhanVien nv = get();
                    if (nv != null) {
                        // Đăng nhập thành công → reset counter
                        failedAttempts = 0;
                        saveLockoutState();
                        try {
                            MainFrame mainFrame = new MainFrame();
                            mainFrame.setVisible(true);
                            dispose();
                        } catch (Exception uiEx) {
                            java.util.logging.Logger.getLogger(LoginFrame.class.getName())
                                    .log(java.util.logging.Level.SEVERE, "Lỗi mở MainFrame", uiEx);
                            lblError.setForeground(UIConstants.DANGER);
                            lblError.setText("Không thể mở màn hình chính. Kiểm tra dữ liệu.");
                            setLoginLoading(false);
                        }
                    } else {
                        // Đăng nhập thất bại
                        failedAttempts++;
                        saveLockoutState();

                        if (AuthService.getInstance().isAccountDisabled(username, password)) {
                            // Tài khoản bị khóa bởi Admin
                            lblError.setForeground(UIConstants.WARNING);
                            lblError.setText(
                                    "<html>Tài khoản đã bị vô hiệu hóa.<br>Vui lòng liên hệ quản trị viên.</html>");
                            setErrorBorder(txtUsername);
                        } else if (failedAttempts >= MAX_ATTEMPTS) {
                            // Sai quá 5 lần → khóa
                            setLoginLoading(false);
                            startLockout();
                            return;
                        } else {
                            // Sai thông tin bình thường
                            lblError.setForeground(UIConstants.DANGER);
                            lblError.setText("Tên đăng nhập hoặc mật khẩu không chính xác.");
                            setErrorBorder(txtUsername);
                            setErrorBorder(txtPassword);
                        }

                        updateAttemptsLabel();
                        txtPassword.setText("");
                        txtPassword.requestFocus();
                        setLoginLoading(false);
                    }
                } catch (Exception ex) {
                    lblError.setForeground(UIConstants.DANGER);
                    lblError.setText("Lỗi kết nối máy chủ. Vui lòng thử lại.");
                    setLoginLoading(false);
                }
            }
        };

        // UI safety timeout: never keep button locked forever when DB hangs.
        Timer watchdog = new Timer(12000, e -> {
            if (!worker.isDone()) {
                lblError.setForeground(UIConstants.WARNING);
                lblError.setText("Kết nối máy chủ quá lâu. Vui lòng thử lại.");
                setLoginLoading(false);
            }
        });
        watchdog.setRepeats(false);
        watchdog.start();

        worker.execute();
    }

    private void setLoginLoading(boolean loading) {
        loginInProgress = loading;
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Đang đăng nhập..." : "Đăng nhập");
    }

    private void saveLockoutState() {
        try (java.io.DataOutputStream out = new java.io.DataOutputStream(new java.io.FileOutputStream(LOCKOUT_FILE))) {
            out.writeInt(failedAttempts);
            out.writeLong(lockoutEndTime);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void loadLockoutState() {
        java.io.File file = new java.io.File(LOCKOUT_FILE);
        if (!file.exists())
            return;

        try (java.io.DataInputStream in = new java.io.DataInputStream(new java.io.FileInputStream(file))) {
            failedAttempts = in.readInt();
            lockoutEndTime = in.readLong();

            long now = System.currentTimeMillis();
            if (lockoutEndTime > now) {
                lockoutRemaining = (int) ((lockoutEndTime - now) / 1000);
                if (lockoutRemaining > 0) {
                    // Cần dùng invokeLater vì UI chưa build xong hoàn toàn trong constructor
                    SwingUtilities.invokeLater(this::startLockout);
                }
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
