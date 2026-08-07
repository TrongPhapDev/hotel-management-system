package ui.dialogs;

import service.*;
import entity.*;
import entity.enums.TrangThaiDatPhong;
import ui.components.NotificationManager;
import ui.components.UIConstants;
import ui.components.RoundedComponents.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.time.LocalDateTime;
import java.util.List;

public class DatPhongDialog extends JDialog {

    private final DatPhongService datPhongService = new DatPhongService();
    private final KhachHangService khService = new KhachHangService();
    private final BangGiaService bangGiaService = new BangGiaService();

    private final List<Phong> selectedRoomsList;
    private final Date checkIn;
    private final Date checkOut;
    private final Runnable onSuccess;

    public DatPhongDialog(Frame owner, List<Phong> selectedRoomsList, Date checkIn, Date checkOut, Runnable onSuccess) {
        super(owner, true);
        this.selectedRoomsList = selectedRoomsList;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.onSuccess = onSuccess;

        String titleStr = selectedRoomsList.size() == 1
                ? "P." + selectedRoomsList.get(0).getSoPhong()
                : selectedRoomsList.size() + " phòng";
        setTitle("Đặt phòng — " + titleStr);
        setSize(600, 720);
        setLocationRelativeTo(owner);
        setResizable(false);

        buildUI(titleStr);
    }

    private void buildUI(String titleStr) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // ---- Top Summary Card ----
        JPanel summaryCard = new JPanel(new BorderLayout(0, 10));
        summaryCard.setBackground(new Color(0xF8FAFC));
        summaryCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)));

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        long soNgay = new ThuePhongService().tinhSoNgay(checkIn, checkOut);

        int totalSucChua = 0;
        int totalAbsMax = 0;
        double pricePerNight = 0;
        StringBuilder roomNames = new StringBuilder();
        for (Phong p : selectedRoomsList) {
            totalSucChua += p.getSucChua();
            totalAbsMax += (p.getSucChua() + 2);
            double giaBook = bangGiaService.layGiaVaoThoiDiem(
                    p.getLoaiPhong() != null ? p.getLoaiPhong().getMaLoaiPhong() : "",
                    checkIn.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
            pricePerNight += giaBook;
            roomNames.append("P.").append(p.getSoPhong()).append(", ");
        }
        if (roomNames.length() > 2)
            roomNames.setLength(roomNames.length() - 2);

        JLabel bannerTitle = new JLabel("Xác nhận đặt phòng " + titleStr);
        bannerTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        bannerTitle.setForeground(UIConstants.TEXT_PRIMARY);

        JPanel detailsGrid = new JPanel(new GridLayout(2, 2, 20, 8));
        detailsGrid.setOpaque(false);

        String[][] details = {
                { "Phòng:", roomNames.toString() },
                { "Thời gian:", sdf.format(checkIn) + " - " + sdf.format(checkOut) + " (" + soNgay + " đêm)" },
                { "Sức chứa:", totalSucChua + " người (Tối đa " + totalAbsMax + ")" },
                { "Giá tạm tính:", String.format("%,.0fđ", pricePerNight) }
        };

        for (String[] d : details) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            p.setOpaque(false);
            JLabel lblKey = new JLabel(d[0] + " ");
            lblKey.setFont(UIConstants.FONT_SMALL);
            lblKey.setForeground(UIConstants.TEXT_SECONDARY);
            JLabel lblVal = new JLabel(d[1]);
            lblVal.setFont(UIConstants.FONT_SMALL_BOLD);
            lblVal.setForeground(UIConstants.TEXT_PRIMARY);
            if (d[0].startsWith("Gia"))
                lblVal.setForeground(UIConstants.SUCCESS);
            p.add(lblKey);
            p.add(lblVal);
            detailsGrid.add(p);
        }

        summaryCard.add(bannerTitle, BorderLayout.NORTH);
        summaryCard.add(detailsGrid, BorderLayout.CENTER);
        root.add(summaryCard, BorderLayout.NORTH);

        // ---- Form content (wrapped in ScrollPane) ----
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 16, 0);
        g.anchor = GridBagConstraints.NORTHWEST;

        int rowIdx = 0;

        // Section: Loai khach
        g.gridx = 0;
        g.gridy = rowIdx++;
        g.gridwidth = 2;
        g.weightx = 1.0;
        form.add(modernSectionLabel("Loại khách"), g);

        JComboBox<String> cboLoaiKhach = new ModernComboBox<>(new String[] { "Khách lẻ (Cá nhân)", "Khách đoàn" });
        ModernTextField txtTenDoan = new ModernTextField("Tên đoàn / công ty...");
        txtTenDoan.setEnabled(false);
        txtTenDoan.setBackground(new Color(0xF1F5F9));

        g.gridwidth = 1;
        g.weightx = 0.5;
        g.gridy = rowIdx;
        g.gridx = 0;
        g.insets = new Insets(0, 0, 16, 8);
        form.add(fieldGroup("Phân loại *", cboLoaiKhach), g);
        g.gridx = 1;
        g.insets = new Insets(0, 8, 16, 0);
        form.add(fieldGroup("Tên đoàn / Công ty", txtTenDoan), g);
        rowIdx++;

        ModernTextField txtGiamGia = new ModernTextField("Giảm giá (%) - VD: 10");
        txtGiamGia.setEnabled(false);
        txtGiamGia.setBackground(new Color(0xF1F5F9));

        g.gridx = 0;
        g.gridy = rowIdx++;
        g.gridwidth = 1;
        g.weightx = 0.5;
        g.insets = new Insets(0, 0, 24, 8);
        form.add(fieldGroup("Giảm giá đoàn (%)", txtGiamGia), g);

        cboLoaiKhach.addActionListener(e -> {
            boolean isDoan = cboLoaiKhach.getSelectedIndex() == 1;
            txtTenDoan.setEnabled(isDoan);
            txtGiamGia.setEnabled(isDoan);
            txtTenDoan.setBackground(isDoan ? Color.WHITE : new Color(0xF1F5F9));
            txtGiamGia.setBackground(isDoan ? Color.WHITE : new Color(0xF1F5F9));
        });

        // Section: Thong tin khach hàng
        g.gridx = 0;
        g.gridy = rowIdx++;
        g.gridwidth = 2;
        g.weightx = 1.0;
        g.insets = new Insets(0, 0, 16, 0);
        form.add(modernSectionLabel("Thông tin khách hàng (Người đại diện)"), g);

        JLabel errHoTen = inlineErr();
        ModernTextField txtHoTen = new ModernTextField("Nhập họ và tên...");
        JLabel errSDT = inlineErr();
        ModernTextField txtSDT = new ModernTextField("Nhập số điện thoại...");

        g.gridwidth = 1;
        g.weightx = 0.5;
        g.gridy = rowIdx;
        g.gridx = 0;
        g.insets = new Insets(0, 0, 12, 8);
        form.add(fieldGroup("Họ và tên *", txtHoTen, errHoTen), g);
        g.gridx = 1;
        g.insets = new Insets(0, 8, 12, 0);
        form.add(fieldGroup("Số điện thoại *", txtSDT, errSDT), g);
        rowIdx++;

        // Real-time validation for Name and Phone
        txtHoTen.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateTenKH(txtHoTen, errHoTen);
            }
        });
        txtSDT.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateSDT(txtSDT, errSDT);
            }
        });
        KeyAdapter liveVal = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getSource() == txtHoTen && !errHoTen.getText().equals(" "))
                    validateTenKH(txtHoTen, errHoTen);
                if (e.getSource() == txtSDT && !errSDT.getText().equals(" "))
                    validateSDT(txtSDT, errSDT);
            }
        };
        txtHoTen.addKeyListener(liveVal);
        txtSDT.addKeyListener(liveVal);

        JLabel errCCCD = inlineErr();
        ModernTextField txtCCCD = new ModernTextField("CCCD/Hộ chiếu (tùy chọn)...");
        ModernComboBox<String> cboQuocTich = new ModernComboBox<>(
                new String[] { "Việt Nam", "Mỹ", "Trung Quốc", "Hàn Quốc", "Nhật Bản", "Khác" });

        g.gridy = rowIdx;
        g.gridwidth = 1;
        g.weightx = 0.5;
        g.gridx = 0;
        g.insets = new Insets(0, 0, 16, 8);
        form.add(fieldGroup("CCCD / Hộ chiếu", txtCCCD, errCCCD), g);
        g.gridx = 1;
        g.insets = new Insets(0, 8, 16, 0);
        form.add(fieldGroup("Quốc tịch", cboQuocTich), g);
        rowIdx++;

        txtCCCD.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateCCCD(txtCCCD, errCCCD);
            }
        });
        txtCCCD.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (!errCCCD.getText().equals(" "))
                    validateCCCD(txtCCCD, errCCCD);
            }
        });

        ModernSpinner spnKhach = new ModernSpinner(new SpinnerNumberModel(1, 1, totalAbsMax > 0 ? totalAbsMax : 50, 1));
        spnKhach.setToolTipText("Lưu ý: Nếu số khách nhập vào lớn hơn tiêu chuẩn (" + totalSucChua
                + "), phụ thu sẽ áp dụng khi nhận phòng.");

        JComboBox<entity.KenhDatPhong> cboKenh = new ModernComboBox<>();
        try {
            for (entity.KenhDatPhong kenh : new dao.KenhDatPhongDAO().getAll()) {
                cboKenh.addItem(kenh);
            }
        } catch (Exception e) {
        }

        g.gridy = rowIdx;
        g.gridx = 0;
        g.insets = new Insets(0, 0, 16, 8);
        form.add(fieldGroup("Khách dự kiến", spnKhach), g);
        g.gridx = 1;
        g.insets = new Insets(0, 8, 16, 0);
        form.add(fieldGroup("Kênh đặt phòng", cboKenh), g);
        rowIdx++;

        ModernTextField txtOTA = new ModernTextField("Mã xác nhận OTA (nếu có)...");
        txtOTA.setEnabled(false);
        txtOTA.setBackground(new Color(0xF1F5F9));

        cboKenh.addActionListener(ev -> {
            entity.KenhDatPhong k = (entity.KenhDatPhong) cboKenh.getSelectedItem();
            boolean isOta = k != null && ("OTA".equals(k.getLoaiKenh()) || "TA".equals(k.getLoaiKenh()));
            txtOTA.setEnabled(isOta);
            txtOTA.setBackground(isOta ? Color.WHITE : new Color(0xF1F5F9));
        });

        g.gridy = rowIdx;
        g.gridx = 0;
        g.insets = new Insets(0, 0, 24, 8);
        form.add(fieldGroup("Mã OTA / TA", txtOTA), g);
        g.gridx = 1;
        form.add(new JPanel() {
            {
                setOpaque(false);
            }
        }, g);
        rowIdx++;

        // Auto-fill logic + Blacklist check
        txtSDT.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String sdt = txtSDT.getText().trim();
                if (sdt.length() >= 10) {
                    KhachHang kh = khService.getByPhone(sdt);
                    if (kh != null) {
                        txtHoTen.setText(kh.getHoTen());
                        txtCCCD.setText(kh.getCccd());
                        if (kh.getQuocTich() != null)
                            cboQuocTich.setSelectedItem(kh.getQuocTich());

                        // Cảnh báo Blacklist
                        if (kh.isBlacklist()) {
                            NotificationManager.showWarning("CẢNH BÁO",
                                    "Khách hàng này đang nằm trong DANH SÁCH ĐEN!\nGhi chú: " + kh.getPreferences());
                            txtHoTen.setForeground(Color.RED);
                        } else {
                            txtHoTen.setForeground(UIConstants.TEXT_PRIMARY);
                            if (kh.getPreferences() != null && !kh.getPreferences().isBlank()) {
                                NotificationManager.showInfo("Sở thích/Lưu ý", kh.getPreferences());
                            }
                        }
                    }
                }
            }
        });

        // Section: Thanh toan
        g.gridx = 0;
        g.gridy = rowIdx++;
        g.gridwidth = 2;
        g.weightx = 1.0;
        g.insets = new Insets(0, 0, 16, 0);
        form.add(modernSectionLabel("Thông tin thanh toán"), g);

        double tongTien = pricePerNight * soNgay;
        ModernTextField txtTongTien = new ModernTextField(String.format("%,.0fđ", tongTien));
        txtTongTien.setEditable(false);
        txtTongTien.setBackground(new Color(0xF1F5F9));

        String[] cocOptions = { "Không đặt cọc", "30% (" + String.format("%,.0fđ", tongTien * 0.3) + ")",
                "50% (" + String.format("%,.0fđ", tongTien * 0.5) + ")",
                "100% (" + String.format("%,.0fđ", tongTien) + ")" };
        JComboBox<String> cboCoc = new ModernComboBox<>(cocOptions);

        g.gridy = rowIdx;
        g.gridwidth = 1;
        g.weightx = 0.5;
        g.gridx = 0;
        g.insets = new Insets(0, 0, 16, 8);
        form.add(fieldGroup("Tổng tiền phòng (ước tính)", txtTongTien), g);
        g.gridx = 1;
        g.insets = new Insets(0, 8, 16, 0);
        form.add(fieldGroup("Đặt cọc", cboCoc), g);
        rowIdx++;

        // Hạn nộp cọc
        ui.components.DateTimePicker txtHanCoc = new ui.components.DateTimePicker(
                new Date(System.currentTimeMillis() + 86400000));
        txtHanCoc.setEnabled(false); // Default disabled because "Không đặt cọc" is selected
        ModernTextField txtVoucher = new ModernTextField("Mã voucher...");

        cboCoc.addActionListener(e -> {
            boolean hasCoc = cboCoc.getSelectedIndex() > 0;
            txtHanCoc.setEnabled(hasCoc);
        });

        g.gridy = rowIdx;
        g.gridx = 0;
        g.insets = new Insets(0, 0, 24, 8);
        form.add(fieldGroup("Hạn nộp cọc (Deadline)", txtHanCoc), g);
        g.gridx = 1;
        g.insets = new Insets(0, 8, 24, 0);
        form.add(fieldGroup("Mã voucher", txtVoucher), g);
        rowIdx++;

        // Section: Ghi chu
        g.gridx = 0;
        g.gridy = rowIdx++;
        g.gridwidth = 2;
        g.weightx = 1.0;
        g.insets = new Insets(0, 0, 16, 0);
        form.add(modernSectionLabel("Ghi chú / Yêu cầu đặc biệt"), g);

        JTextArea txtGhiChu = new JTextArea(3, 0);
        txtGhiChu.setFont(UIConstants.FONT_BODY);
        txtGhiChu.setLineWrap(true);
        txtGhiChu.setWrapStyleWord(true);
        txtGhiChu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        txtGhiChu.setBackground(Color.WHITE);
        String hint = "VD: Cần giường phụ, yêu cầu tầng cao, check-in muộn...";
        txtGhiChu.setText(hint);
        txtGhiChu.setForeground(UIConstants.TEXT_MUTED);
        txtGhiChu.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (txtGhiChu.getText().equals(hint)) {
                    txtGhiChu.setText("");
                    txtGhiChu.setForeground(UIConstants.TEXT_PRIMARY);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (txtGhiChu.getText().isBlank()) {
                    txtGhiChu.setText(hint);
                    txtGhiChu.setForeground(UIConstants.TEXT_MUTED);
                }
            }
        });

        JScrollPane spGhiChu = new JScrollPane(txtGhiChu);
        g.gridx = 0;
        g.gridy = rowIdx++;
        g.gridwidth = 2;
        g.weightx = 1.0;
        g.weighty = 0.1;
        g.fill = GridBagConstraints.BOTH;
        g.insets = new Insets(0, 0, 0, 0);
        form.add(spGhiChu, g);

        // ---- Buttons ----
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setBackground(new Color(0xF8FAFC));
        btns.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        RoundedButton btnCancel = new RoundedButton("Hủy", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        btnCancel.setPreferredSize(new Dimension(90, 38));
        RoundedButton btnOk = new RoundedButton("Xác nhận đặt phòng", UIConstants.PRIMARY, Color.WHITE);
        btnOk.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnOk.setPreferredSize(new Dimension(200, 38));

        btnCancel.addActionListener(e -> dispose());

        final int finalTotalAbsMax = totalAbsMax;
        btnOk.addActionListener(e -> {
            String hoTen = txtHoTen.getText().trim();
            String sdt = txtSDT.getText().trim();
            String cccd = txtCCCD.getText().trim();
            int soKhach = (int) spnKhach.getValue();
            String ghiChuText = txtGhiChu.getText().trim();
            if (ghiChuText.equals(hint))
                ghiChuText = "";

            if (!validateTenKH(txtHoTen, errHoTen) || !validateSDT(txtSDT, errSDT) || !validateCCCD(txtCCCD, errCCCD)) {
                NotificationManager.showError("Lỗi", "Vui lòng kiểm tra lại tính hợp lệ của thông tin!");
                return;
            }

            if (soKhach > finalTotalAbsMax && finalTotalAbsMax > 0) {
                NotificationManager.showError("Lỗi", "Số lượng khách dự kiến vượt quá sức chứa tối đa!");
                return;
            }

            // Save logic
            KhachHang kh = khService.getByPhone(sdt);
            if (kh == null) {
                kh = new KhachHang();
                kh.setHoTen(hoTen);
                kh.setSoDienThoai(sdt);
                kh.setCccd(cccd);
                kh.setQuocTich((String) cboQuocTich.getSelectedItem());
                khService.them(kh);
                kh = khService.getByPhone(sdt);
            } else if (!cccd.isEmpty() && (kh.getCccd() == null || kh.getCccd().isBlank())) {
                kh.setCccd(cccd);
            }

            DatPhong dp = new DatPhong();
            dp.setKhachHang(kh);
            dp.setNgayNhanDuKien(checkIn.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
            dp.setNgayTraDuKien(checkOut.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
            dp.setSoLuongKhach(soKhach);
            dp.setTienDatCoc(tongTien * (new double[] { 0, 0.3, 0.5, 1.0 })[cboCoc.getSelectedIndex()]);
            dp.setGhiChu(ghiChuText.isEmpty() ? null : ghiChuText);
            dp.setTrangThai(TrangThaiDatPhong.PENDING);

            // Section: Check Deadline
            dp.setHanNopCoc(txtHanCoc.getLocalDateTime());

            entity.KenhDatPhong kenh = (entity.KenhDatPhong) cboKenh.getSelectedItem();
            dp.setMaKenh(kenh != null ? kenh.getMaKenh() : "DIR");
            String ota = txtOTA.getText().trim();
            dp.setMaXacNhanKenh(ota.isEmpty() ? null : ota);

            dp.setHanCheckIn(dp.getNgayNhanDuKien().toLocalDate().atTime(18, 0));
            if (dp.getHanCheckIn().isBefore(dp.getNgayNhanDuKien()))
                dp.setHanCheckIn(dp.getNgayNhanDuKien().plusHours(4));

            boolean isDoan = cboLoaiKhach.getSelectedIndex() == 1;
            dp.setLoaiKhach(isDoan ? "DOAN" : "CA_NHAN");
            if (isDoan)
                dp.setTenDoan(txtTenDoan.getText().trim());

            NhanVien nv = new NhanVien();
            nv.setMaNhanVien(AuthService.getInstance().getCurrentMaNV());
            dp.setNhanVien(nv);

            String vCode = txtVoucher.getText().trim();
            if (!vCode.isEmpty())
                dp.setMaKhuyenMai(vCode);

            double discount = 0;
            if (isDoan) {
                try {
                    discount = Double.parseDouble(txtGiamGia.getText().trim());
                } catch (Exception ignored) {
                }
            }

            for (Phong ph : selectedRoomsList) {
                ChiTietDatPhong ct = new ChiTietDatPhong();
                ct.setPhong(ph);
                ct.setDatPhong(dp);
                double base = bangGiaService.layGiaVaoThoiDiem(
                        ph.getLoaiPhong() != null ? ph.getLoaiPhong().getMaLoaiPhong() : "", dp.getNgayNhanDuKien());
                ct.setGiaThucTeChot(discount > 0 ? base * (1 - discount / 100.0) : base);
                dp.getDsChiTiet().add(ct);
            }

            String err = datPhongService.datPhong(dp);
            if (err == null) {
                NotificationManager.showSuccess("Thành công", "Đặt phòng " + dp.getMaDatPhong() + " thành công!");
                dispose();
                if (onSuccess != null)
                    onSuccess.run();
            } else {
                NotificationManager.showError("Lỗi", err);
            }
        });

        btns.add(btnCancel);
        btns.add(btnOk);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        formScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.getVerticalScrollBar().setUnitIncrement(10);

        root.add(formScroll, BorderLayout.CENTER);
        root.add(btns, BorderLayout.SOUTH);

        setContentPane(root);

        // Focus first field
        SwingUtilities.invokeLater(() -> {
            txtHoTen.requestFocusInWindow();
        });
    }

    private JPanel fieldGroup(String label, JComponent comp) {
        return fieldGroup(label, comp, null);
    }

    private JPanel fieldGroup(String label, JComponent comp, JLabel errLbl) {
        JPanel g = new JPanel(new BorderLayout(0, 2));
        g.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_SECONDARY);
        g.add(lbl, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(0, 1));
        center.setOpaque(false);
        center.add(comp, BorderLayout.NORTH);
        if (errLbl != null)
            center.add(errLbl, BorderLayout.CENTER);
        g.add(center, BorderLayout.CENTER);
        return g;
    }

    private JLabel inlineErr() {
        JLabel l = new JLabel(" ");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(new Color(0xEF4444));
        return l;
    }

    private String formatName(String name) {
        if (name == null || name.isBlank())
            return "";
        String[] words = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.length() > 0) {
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1)
                    sb.append(w.substring(1).toLowerCase());
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    private boolean validateTenKH(ModernTextField txt, JLabel err) {
        String val = txt.getText().trim();
        val = formatName(val);
        txt.setText(val);
        err.setText(" ");
        if (val.isEmpty()) {
            err.setText("Bắt buộc nhập họ tên");
            return false;
        }
        if (!val.matches("^[\\p{L} .'-]{2,50}$")) {
            err.setText("Họ tên 2-50 ký tự, không chứa số");
            return false;
        }
        return true;
    }

    private boolean validateSDT(ModernTextField txt, JLabel err) {
        String val = txt.getText().trim();
        err.setText(" ");
        if (val.isEmpty()) {
            err.setText("Bắt buộc nhập SĐT");
            return false;
        }
        if (!val.matches("^(0[35789])\\d{8}$")) {
            err.setText("SĐT 10 số, đầu 03/05/07/08/09");
            return false;
        }
        return true;
    }

    private boolean validateCCCD(ModernTextField txt, JLabel err) {
        String val = txt.getText().trim();
        err.setText(" ");
        if (!val.isEmpty() && !val.matches("^([0-9]{9}|[0-9]{12}|[A-Z][0-9]{7,8})$")) {
            err.setText("9/12 số hoặc A1234567");
            return false;
        }
        return true;
    }

    private JPanel modernSectionLabel(String text) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        JLabel lbl = new JLabel(text.toUpperCase());
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(UIConstants.PRIMARY);
        p.add(lbl, BorderLayout.WEST);
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0xE2E8F0));
        JPanel sepWrap = new JPanel(new BorderLayout());
        sepWrap.setOpaque(false);
        sepWrap.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        sepWrap.add(sep, BorderLayout.CENTER);
        p.add(sepWrap, BorderLayout.CENTER);
        return p;
    }
}
