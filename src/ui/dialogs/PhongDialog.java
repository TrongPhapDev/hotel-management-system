package ui.dialogs;

import service.PhongService;
import service.BangGiaService;
import entity.*;
import entity.enums.TrangThaiPhong;
import ui.components.UIConstants;
import ui.components.RoundedComponents.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Dialog thêm/sửa Phòng.
 * - Loại phòng: dropdown từ DB
 * - Hướng nhìn: dropdown từ bảng HuongNhin (thay vì hardcode)
 * - Giá, sức chứa tự lấy từ Loại Phòng (readonly)
 */
public class PhongDialog extends JDialog {

    private final PhongService service = new PhongService();
    private final BangGiaService bangGiaService = new BangGiaService();
    private final Phong entity;
    private boolean confirmed = false;

    private ModernTextField txtSoPhong, txtTang;
    private JLabel lblGia, lblSucChua, lblGiaHienHanh;
    private ModernComboBox<String> cboLoai, cboView, cboTrangThai;
    private List<LoaiPhong> loaiList;
    private List<HuongNhin> huongNhinList;

    public PhongDialog(Frame parent, Phong p) {
        super(parent, p == null ? "Thêm phòng mới" : "Chỉnh sửa – Phòng " + p.getSoPhong(), true);
        this.entity = p;
        setSize(520, 500);
        setLocationRelativeTo(parent);
        setResizable(false);
        buildUI();
        if (p != null)
            fillData();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // Header
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, UIConstants.PRIMARY, 0, getHeight(), UIConstants.PRIMARY_DARK));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        JLabel title = new JLabel(entity == null ? "Thêm phòng mới" : "Chỉnh sửa phòng " + entity.getSoPhong());
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        // Load loại phòng
        loaiList = service.getActiveLoaiPhong();
        String[] loaiNames = loaiList.stream()
                .map(lp -> lp.getMaLoai() + " – " + lp.getTenLoai() + " (" + lp.getSucChua() + " người)")
                .toArray(String[]::new);
        cboLoai = new ModernComboBox<>(loaiNames);
        cboLoai.setPreferredSize(new Dimension(140, 40));

        // Load hướng nhìn từ DB (KHÔNG hardcode)
        huongNhinList = service.getAllHuongNhin();
        String[] viewNames = huongNhinList.stream()
                .map(HuongNhin::getTenHuongNhin)
                .toArray(String[]::new);
        cboView = new ModernComboBox<>(viewNames);
        cboView.setPreferredSize(new Dimension(140, 40));

        // Trạng thái
        if (entity != null && entity.getTrangThai() != null) {
            cboTrangThai = new ModernComboBox<>(new String[] { entity.getTrangThaiString() });
            cboTrangThai.setEnabled(false);
        } else {
            cboTrangThai = new ModernComboBox<>(new String[] { "Có sẵn" });
            cboTrangThai.setEnabled(false);
        }
        cboTrangThai.setPreferredSize(new Dimension(140, 40));

        txtSoPhong = new ModernTextField("Nhập số phòng...");
        txtTang = new ModernTextField("1");
        txtTang.setPreferredSize(new Dimension(100, 40));
        txtSoPhong.setPreferredSize(new Dimension(140, 40));

        // Read-only labels
        lblSucChua = new JLabel("—");
        lblSucChua.setFont(UIConstants.FONT_BODY);
        lblSucChua.setForeground(UIConstants.TEXT_SECONDARY);

        lblGia = new JLabel("—");
        lblGia.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblGia.setForeground(UIConstants.SUCCESS);

        lblGiaHienHanh = new JLabel("");
        lblGiaHienHanh.setFont(UIConstants.FONT_SMALL);
        lblGiaHienHanh.setForeground(UIConstants.PRIMARY);

        if (entity != null) {
            txtSoPhong.setEditable(false);
            // ModernTextField will handle background automatically if disabled/unalbe to edit
        }

        cboLoai.addActionListener(e -> updateGiaFromLoai());
        if (loaiList.size() > 0 && entity == null)
            updateGiaFromLoai();

        // Form layout
        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(20, 24, 8, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        int row = 0;
        addField(body, gbc, row++, "Số phòng *", txtSoPhong, "Tầng *", txtTang);
        addField(body, gbc, row++, "Loại phòng *", cboLoai, "Hướng nhìn", cboView);

        JPanel giaPanel = new JPanel(new BorderLayout(0, 2));
        giaPanel.setOpaque(false);
        giaPanel.add(lblGia, BorderLayout.CENTER);
        giaPanel.add(lblGiaHienHanh, BorderLayout.SOUTH);

        addField(body, gbc, row++, "Giá cơ sở (từ loại phòng)", giaPanel, "Sức chứa", lblSucChua);
        addField(body, gbc, row++, "Trạng thái", cboTrangThai, null, null);

        // Hint
        JPanel hintPanel = new JPanel(new BorderLayout());
        hintPanel.setBackground(new Color(0xFFFBEB));
        hintPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xE2E8F0)),
                BorderFactory.createEmptyBorder(10, 24, 10, 24)));
        JLabel hintLbl = new JLabel("<html>Giá và sức chứa được quản lý tại <b>Loại phòng</b>. "
                + "Trạng thái phòng thay đổi qua check-in/check-out.</html>");
        hintLbl.setFont(UIConstants.FONT_SMALL);
        hintLbl.setForeground(new Color(0x92400E));
        hintPanel.add(hintLbl, BorderLayout.CENTER);

        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setBackground(Color.WHITE);
        centerWrap.add(body, BorderLayout.CENTER);
        centerWrap.add(hintPanel, BorderLayout.SOUTH);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xF1F5F9)));

        RoundedButton btnCancel = new RoundedButton("Huỷ", new Color(0xF1F5F9), UIConstants.TEXT_SECONDARY);
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnCancel.setPreferredSize(new Dimension(100, 42));

        RoundedButton btnSave = new RoundedButton(entity == null ? "Thêm phòng" : "Lưu thay đổi", 
                UIConstants.PRIMARY, Color.WHITE);
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSave.setPreferredSize(new Dimension(160, 42));

        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> doSave());
        getRootPane().setDefaultButton(btnSave);
        footer.add(btnCancel);
        footer.add(btnSave);

        root.add(header, BorderLayout.NORTH);
        root.add(centerWrap, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void addField(JPanel body, GridBagConstraints gbc, int row,
            String label1, JComponent comp1, String label2, JComponent comp2) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0.5;
        if (label1 != null && comp1 != null) {
            body.add(lf(label1, comp1), gbc);
        }
        gbc.gridx = 1;
        if (label2 != null && comp2 != null) {
            body.add(lf(label2, comp2), gbc);
        }
    }

    private void updateGiaFromLoai() {
        int idx = cboLoai.getSelectedIndex();
        if (idx >= 0 && idx < loaiList.size()) {
            LoaiPhong lp = loaiList.get(idx);
            lblSucChua.setText(lp.getSucChua() + " người tối đa");
            lblGia.setText(String.format("%,.0fđ/đêm", lp.getGiaTheoNgay()));

            double giaHienHanh = bangGiaService.layGiaHienHanh(lp.getMaLoaiPhong());
            if (giaHienHanh > 0 && giaHienHanh != lp.getGiaTheoNgay()) {
                String tenBG = bangGiaService.getTenBangGiaActive();
                lblGiaHienHanh.setText("→ Giá hiện hành: " + String.format("%,.0fđ", giaHienHanh)
                        + (tenBG != null ? " (" + tenBG + ")" : ""));
            } else {
                lblGiaHienHanh.setText("");
            }
        }
    }

    private void fillData() {
        txtSoPhong.setText(entity.getSoPhong());
        txtTang.setText(String.valueOf(entity.getTang()));
        // Select loại phòng
        for (int i = 0; i < loaiList.size(); i++) {
            if (loaiList.get(i).getMaLoai().equals(entity.getMaLoai())) {
                cboLoai.setSelectedIndex(i);
                break;
            }
        }
        // Select hướng nhìn by maHuongNhin
        if (entity.getHuongNhin() != null) {
            String maHN = entity.getHuongNhin().getMaHuongNhin();
            for (int i = 0; i < huongNhinList.size(); i++) {
                if (huongNhinList.get(i).getMaHuongNhin().equals(maHN)) {
                    cboView.setSelectedIndex(i);
                    break;
                }
            }
        }
        updateGiaFromLoai();
    }

    private void doSave() {
        String soPhong = txtSoPhong.getText().trim();
        if (soPhong.isEmpty()) {
            err("Số phòng không được để trống!");
            txtSoPhong.requestFocus();
            return;
        }
        if (!soPhong.matches("^[A-Za-z0-9]{1,10}$")) {
            err("Số phòng không hợp lệ!\n(1–10 ký tự, chỉ gồm chữ và số)");
            txtSoPhong.requestFocus();
            return;
        }
        if (loaiList.isEmpty()) {
            err("Chưa có loại phòng nào trong hệ thống!");
            return;
        }

        int tang;
        try {
            tang = Integer.parseInt(txtTang.getText().trim());
            if (tang < 1 || tang > 99)
                throw new Exception();
        } catch (Exception e) {
            err("Tầng phải là số nguyên từ 1–99!");
            return;
        }

        Phong p = entity != null ? entity : new Phong();
        p.setSoPhong(soPhong);

        int idx = cboLoai.getSelectedIndex();
        if (idx >= 0 && idx < loaiList.size()) {
            p.setLoaiPhong(loaiList.get(idx));
        }
        p.setTang(tang);

        // Set HuongNhin from dropdown
        int viewIdx = cboView.getSelectedIndex();
        if (viewIdx >= 0 && viewIdx < huongNhinList.size()) {
            p.setHuongNhin(huongNhinList.get(viewIdx));
        }

        if (entity == null) {
            p.setTrangThai(TrangThaiPhong.AVAILABLE);
        }

        String error = entity == null ? service.themPhong(p) : service.suaPhong(p);
        if (error == null) {
            confirmed = true;
            dispose();
        } else
            err(error);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private JTextField f() {
        JTextField t = new JTextField();
        t.setFont(UIConstants.FONT_BODY);
        t.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(5, 9, 5, 9)));
        t.setPreferredSize(new Dimension(0, 36));
        return t;
    }

    private JPanel lf(String l, JComponent c) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel lb = new JLabel(l);
        lb.setFont(UIConstants.FONT_SMALL_BOLD);
        p.add(lb, BorderLayout.NORTH);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private void sc(JComboBox<String> c, String v) {
        if (v == null)
            return;
        for (int i = 0; i < c.getItemCount(); i++)
            if (v.equals(c.getItemAt(i))) {
                c.setSelectedIndex(i);
                return;
            }
    }

    private void err(String m) {
        JOptionPane.showMessageDialog(this, m, "Lỗi dữ liệu", JOptionPane.WARNING_MESSAGE);
    }
}
