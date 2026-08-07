package ui.dialogs;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * QRPaymentDialog – Popup hiển thị mã QR thanh toán chuyển khoản.
 * Mã QR được vẽ giả (tượng trưng). Bấm "Đã thanh toán" để xác nhận.
 *
 * Cách dùng trong HoaDonDialog.doCheckout():
 *   if ("Chuyển khoản".equals(hinhThuc)) {
 *       QRPaymentDialog qrDialog = new QRPaymentDialog((Frame) getOwner(), tongCong, maHD);
 *       qrDialog.setVisible(true);
 *       if (!qrDialog.isConfirmed()) return; // người dùng đóng dialog
 *   }
 */
public class QRPaymentDialog extends JDialog {

    private boolean confirmed = false;
    private final long    soTien;
    private final String  maHD;

    // Thông tin ngân hàng khách sạn (thay bằng thông tin thật nếu cần)
    private static final String BANK_NAME    = "ABCXYZ";
    private static final String ACCOUNT_NO   = "090807060504030201";
    private static final String ACCOUNT_NAME = "KHACH SAN 5 SAO SIEU CAP VIP PRO";

    public QRPaymentDialog(Frame parent, long soTien, String maHD) {
        super(parent, "Thanh toán chuyển khoản", true);
        this.soTien = soTien;
        this.maHD   = maHD;
        setSize(420, 600);
        setLocationRelativeTo(parent);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        root.add(buildHeader(),  BorderLayout.NORTH);
        root.add(buildBody(),    BorderLayout.CENTER);
        root.add(buildFooter(),  BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ---- Header ----
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x0066CC)); // xanh ngân hàng
        header.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("Quét mã QR để thanh toán");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel(BANK_NAME + "    TK: " + ACCOUNT_NO);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(255, 255, 255, 200));

        left.add(lblTitle);
        left.add(Box.createVerticalStrut(3));
        left.add(lblSub);

        // Icon QR ở góc phải
        JLabel icon = new JLabel("📱");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

        header.add(left,  BorderLayout.WEST);
        header.add(icon,  BorderLayout.EAST);
        return header;
    }

    // ---- Body ----
    private JPanel buildBody() {
        JPanel body = new JPanel();
        body.setBackground(Color.WHITE);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(20, 28, 10, 28));

        // Thông tin chuyển khoản
        body.add(infoRow("Ngân hàng",    BANK_NAME));
        body.add(Box.createVerticalStrut(8));
        body.add(infoRow("Số tài khoản", ACCOUNT_NO));
        body.add(Box.createVerticalStrut(8));
        body.add(infoRow("Chủ tài khoản", ACCOUNT_NAME));
        body.add(Box.createVerticalStrut(8));
        body.add(infoRow("Số tiền",
            String.format("%,.0f đ", (double) soTien)));
        body.add(Box.createVerticalStrut(8));
        body.add(infoRow("Nội dung",     "Thanh toan " + maHD));
        body.add(Box.createVerticalStrut(20));

        // QR Panel
        QRPanel qrPanel = new QRPanel(soTien, maHD);
        qrPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(qrPanel);
        body.add(Box.createVerticalStrut(14));

        // Hướng dẫn
        JLabel hint = new JLabel("Mở app ngân hàng → Quét mã QR → Xác nhận thanh toán");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hint.setForeground(new Color(0x64748B));
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(hint);

        return body;
    }

    private JPanel infoRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(new Color(0x64748B));
        lbl.setPreferredSize(new Dimension(110, 20));

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 12));
        val.setForeground(new Color(0x1E293B));

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
    }

    // ---- Footer ----
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xE2E8F0)),
            BorderFactory.createEmptyBorder(14, 28, 14, 28)));

        // Nút Huỷ
        JButton btnCancel = new JButton("Huỷ");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCancel.setForeground(new Color(0x64748B));
        btnCancel.setBackground(new Color(0xF1F5F9));
        btnCancel.setBorderPainted(false);
        btnCancel.setFocusPainted(false);
        btnCancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCancel.setPreferredSize(new Dimension(90, 40));
        btnCancel.addActionListener(e -> dispose());

        // Nút Đã thanh toán
        JButton btnConfirm = new JButton("✓  Đã thanh toán");
        btnConfirm.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.setBackground(new Color(0x16A34A)); // xanh lá
        btnConfirm.setBorderPainted(false);
        btnConfirm.setFocusPainted(false);
        btnConfirm.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnConfirm.setPreferredSize(new Dimension(180, 40));
        btnConfirm.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        // Hover effect
        btnConfirm.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnConfirm.setBackground(new Color(0x15803D));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnConfirm.setBackground(new Color(0x16A34A));
            }
        });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(btnCancel);
        btns.add(btnConfirm);

        // Thời gian hiển thị (tượng trưng)
        JLabel lblTime = new JLabel("⏱ " + new SimpleDateFormat("HH:mm dd/MM/yyyy").format(new Date()));
        lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblTime.setForeground(new Color(0x94A3B8));

        footer.add(lblTime, BorderLayout.WEST);
        footer.add(btns,    BorderLayout.EAST);
        return footer;
    }

    public boolean isConfirmed() { return confirmed; }

    // =========================================================
    // QR Panel – vẽ mã QR giả bằng Java2D
    // =========================================================
    private static class QRPanel extends JPanel {

        private final int[][] qrMatrix;
        private static final int MODULES = 25; // kích thước lưới QR giả
        private static final int CELL    = 8;  // pixel mỗi ô
        private static final int QUIET   = 3;  // vùng trắng xung quanh (quiet zone)

        QRPanel(long soTien, String maHD) {
            int size = (MODULES + QUIET * 2) * CELL;
            setPreferredSize(new Dimension(size + 20, size + 20));
            setMaximumSize(new Dimension(size + 20, size + 20));
            setOpaque(false);

            // Sinh ma trận QR giả dựa trên seed từ số tiền + maHD
            qrMatrix = generateFakeQR(soTien, maHD);
        }

        private int[][] generateFakeQR(long soTien, String maHD) {
            int[][] m = new int[MODULES][MODULES];
            Random rnd = new Random((soTien * 31L) + maHD.hashCode());

            // Điền ngẫu nhiên
            for (int r = 0; r < MODULES; r++)
                for (int c = 0; c < MODULES; c++)
                    m[r][c] = rnd.nextBoolean() ? 1 : 0;

            // Vẽ 3 ô định vị (finder patterns) ở 3 góc
            drawFinderPattern(m, 0, 0);
            drawFinderPattern(m, 0, MODULES - 7);
            drawFinderPattern(m, MODULES - 7, 0);

            // Timing patterns (dải xen kẽ đen trắng)
            for (int i = 8; i < MODULES - 8; i++) {
                m[6][i] = (i % 2 == 0) ? 1 : 0;
                m[i][6] = (i % 2 == 0) ? 1 : 0;
            }

            return m;
        }

        private void drawFinderPattern(int[][] m, int row, int col) {
            // Ô ngoài cùng (7×7 đen)
            for (int r = 0; r < 7; r++)
                for (int c = 0; c < 7; c++)
                    m[row + r][col + c] = 1;
            // Viền trắng bên trong (5×5)
            for (int r = 1; r < 6; r++)
                for (int c = 1; c < 6; c++)
                    m[row + r][col + c] = 0;
            // Ô đen 3×3 chính giữa
            for (int r = 2; r < 5; r++)
                for (int c = 2; c < 5; c++)
                    m[row + r][col + c] = 1;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int totalSize = (MODULES + QUIET * 2) * CELL;
            int offsetX   = (getWidth()  - totalSize) / 2;
            int offsetY   = (getHeight() - totalSize) / 2;

            // Nền trắng với bo góc và đổ bóng
            g2.setColor(new Color(0, 0, 0, 18));
            g2.fillRoundRect(offsetX + 4, offsetY + 4, totalSize, totalSize, 16, 16);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(offsetX, offsetY, totalSize, totalSize, 14, 14);

            // Viền nhạt
            g2.setColor(new Color(0xE2E8F0));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(offsetX, offsetY, totalSize, totalSize, 14, 14);

            // Vẽ các ô QR
            for (int r = 0; r < MODULES; r++) {
                for (int c = 0; c < MODULES; c++) {
                    if (qrMatrix[r][c] == 1) {
                        int x = offsetX + (QUIET + c) * CELL;
                        int y = offsetY + (QUIET + r) * CELL;
                        g2.setColor(new Color(0x1E293B));
                        g2.fillRoundRect(x + 1, y + 1, CELL - 2, CELL - 2, 2, 2);
                    }
                }
            }

            // Logo nhỏ ở giữa QR (tượng trưng)
            int logoSize = 28;
            int lx = offsetX + totalSize / 2 - logoSize / 2;
            int ly = offsetY + totalSize / 2 - logoSize / 2;
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(lx - 2, ly - 2, logoSize + 4, logoSize + 4, 6, 6);
            g2.setColor(new Color(0x0066CC));
            g2.fillRoundRect(lx, ly, logoSize, logoSize, 5, 5);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            String txt = "QR";
            g2.drawString(txt, lx + (logoSize - fm.stringWidth(txt)) / 2,
                          ly + (logoSize + fm.getAscent() - fm.getDescent()) / 2 - 1);

            g2.dispose();
        }
    }
}

