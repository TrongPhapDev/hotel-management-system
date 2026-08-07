package ui.dialogs;

import ui.components.RoundedComponents.*;
import ui.components.UIConstants;
import ui.components.DatePicker;
import entity.*;
import java.awt.*;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import service.BangGiaService;
import service.PhongService;

/**
 * Dialog thêm/sửa Bảng Giá (Rate Plan).
 * Bao gồm:
 * - Thông tin bảng giá (tên, loại, đối tượng, ưu tiên, ngày, trạng thái)
 * - Bảng chi tiết giá cho TỪNG loại phòng (giá/ngày, giá/giờ, giá cuối tuần, phụ phí trả trễ)
 */
public class BangGiaDialog extends JDialog {

    private final BangGiaService service = new BangGiaService();
    private final PhongService phongService = new PhongService();
    private final BangGia entity;
    private boolean confirmed = false;

    private ModernTextField txtTen, txtMoTa;
    private DatePicker spnBatDau, spnKetThuc;
    private ModernSpinner spnUuTien;
    private JCheckBox cbTrangThai;
    private ModernComboBox<String> cboLoaiBangGia, cboDoiTuong;
    private DefaultTableModel chiTietModel;
    private List<LoaiPhong> allLoaiPhong;

    public BangGiaDialog(Frame parent, BangGia bg) {
        super(parent, bg == null ? "Thêm bảng giá mới" : "Chỉnh sửa – " + bg.getTenBangGia(), true);
        this.entity = bg;
        setSize(780, 680);
        setLocationRelativeTo(parent);
        setResizable(false);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        allLoaiPhong = phongService.getAllLoaiPhong();
        buildUI();
        if (bg != null)
            fillData();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // ===== HEADER =====
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
        header.setPreferredSize(new Dimension(0, 90));
        header.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JLabel title = new JLabel(entity == null ? "Thêm Rate Plan mới" : "Chỉnh sửa Rate Plan");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel(entity == null
                ? "Thiết lập giá theo mùa/sự kiện/đối tượng cho từng loại phòng"
                : "Mã: " + entity.getMaBangGia() + " | " + entity.getLoaiBangGiaLabel());
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(220, 230, 255));

        JPanel headerContent = new JPanel();
        headerContent.setOpaque(false);
        headerContent.setLayout(new BoxLayout(headerContent, BoxLayout.Y_AXIS));
        headerContent.add(title);
        headerContent.add(Box.createVerticalStrut(2));
        headerContent.add(subtitle);
        header.add(headerContent, BorderLayout.WEST);

        // ===== BODY =====
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(16, 24, 8, 24));

        // -- Top form: 3 rows --
        JPanel topForm = new JPanel(new GridLayout(3, 2, 14, 8));
        topForm.setBackground(Color.WHITE);

        txtTen = new ModernTextField("Bảng giá mới...");
        txtTen.setText("Bảng giá tháng " + java.time.YearMonth.now());
        txtTen.setPreferredSize(new Dimension(0, 40));

        // Loại bảng giá
        cboLoaiBangGia = new ModernComboBox<>(new String[]{"RACK", "SEASONAL", "CORPORATE", "OTA", "PROMOTION"});
        cboLoaiBangGia.setPreferredSize(new Dimension(0, 40));
        cboLoaiBangGia.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, index, sel, focus);
                if (value != null) {
                    switch (value.toString()) {
                        case "RACK": setText("Rack Rate (Giá gốc)"); break;
                        case "SEASONAL": setText("Giá theo mùa"); break;
                        case "CORPORATE": setText("Giá doanh nghiệp"); break;
                        case "OTA": setText("Giá OTA"); break;
                        case "PROMOTION": setText("Khuyến mãi"); break;
                    }
                }
                return this;
            }
        });

        // Đối tượng áp dụng
        cboDoiTuong = new ModernComboBox<>(new String[]{"ALL", "CA_NHAN", "DOAN", "CORPORATE", "VIP"});
        cboDoiTuong.setPreferredSize(new Dimension(0, 40));
        cboDoiTuong.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, index, sel, focus);
                if (value != null) {
                    switch (value.toString()) {
                        case "ALL": setText("Tất cả khách"); break;
                        case "CA_NHAN": setText("Khách lẻ"); break;
                        case "DOAN": setText("Khách đoàn"); break;
                        case "CORPORATE": setText("Doanh nghiệp"); break;
                        case "VIP": setText("Khách VIP"); break;
                    }
                }
                return this;
            }
        });

        spnBatDau = new DatePicker(new java.util.Date());
        spnKetThuc = new DatePicker(new java.util.Date());

        // Mức ưu tiên
        spnUuTien = new ModernSpinner(new SpinnerNumberModel(100, 1, 999, 10));
        spnUuTien.setPreferredSize(new Dimension(0, 40));
        spnUuTien.setToolTipText("Số nhỏ = ưu tiên cao. VD: 30 (mùa cao điểm) > 50 (doanh nghiệp) > 100 (rack rate)");

        cbTrangThai = new JCheckBox("Kích hoạt ngay");
        cbTrangThai.setFont(UIConstants.FONT_BODY);
        cbTrangThai.setOpaque(false);
        cbTrangThai.setSelected(true);
        cbTrangThai.setForeground(UIConstants.TEXT_PRIMARY);

        topForm.add(createFormField("Tên bảng giá *", txtTen));
        topForm.add(createFormField("Loại Rate Plan *", cboLoaiBangGia));
        topForm.add(createFormField("Ngày bắt đầu *", spnBatDau));
        topForm.add(createFormField("Ngày kết thúc *", spnKetThuc));
        topForm.add(createFormField("Đối tượng áp dụng", cboDoiTuong));

        // Ưu tiên + trạng thái trên cùng dòng
        JPanel priorityRow = new JPanel(new GridLayout(1, 2, 10, 0));
        priorityRow.setOpaque(false);
        priorityRow.add(createFormField("Mức ưu tiên", spnUuTien));
        priorityRow.add(createFormField("Trạng thái", cbTrangThai));
        topForm.add(priorityRow);

        // -- Bottom: Chi tiết giá cho từng loại phòng --
        JPanel tablePanel = new JPanel(new BorderLayout(0, 8));
        tablePanel.setBackground(Color.WHITE);

        JLabel lblTable = new JLabel("Chi tiết giá theo loại phòng");
        lblTable.setFont(UIConstants.FONT_BODY_BOLD);
        lblTable.setForeground(UIConstants.TEXT_PRIMARY);

        String[] cols = { "Mã", "Loại phòng", "Giá cơ sở (đ)", "Giá mới/đêm (đ)", "Giá cuối tuần (đ)", "Giá/giờ (đ)",
                "Phụ phí trả trễ (đ)" };
        chiTietModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col >= 3; // Chỉ cho sửa giá mới, giá cuối tuần, giá/giờ, phụ phí
            }
        };

        JTable table = new JTable(chiTietModel);
        table.setFont(UIConstants.FONT_BODY);
        table.setRowHeight(36);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(UIConstants.PRIMARY_LIGHT);
        table.getTableHeader().setFont(UIConstants.FONT_SMALL_BOLD);
        table.getTableHeader().setBackground(UIConstants.BG_TABLE_HEADER);
        table.getTableHeader().setForeground(UIConstants.TEXT_SECONDARY);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER));

        // Hide mã column
        table.getColumn("Mã").setMaxWidth(0);
        table.getColumn("Mã").setMinWidth(0);
        table.getColumn("Mã").setPreferredWidth(0);

        // Custom renderer for giá columns
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row,
                    int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                lbl.setFont(UIConstants.FONT_BODY);
                lbl.setBackground(sel ? UIConstants.PRIMARY_LIGHT : (row % 2 == 0 ? Color.WHITE : new Color(0xFAFAFA)));

                if (col == 2) { // Giá cơ sở - gray italic
                    lbl.setForeground(UIConstants.TEXT_MUTED);
                    lbl.setFont(UIConstants.FONT_SMALL);
                } else if (col == 4) { // Giá cuối tuần - orange
                    lbl.setForeground(UIConstants.ORANGE);
                    lbl.setFont(UIConstants.FONT_BODY_BOLD);
                } else if (col >= 3) { // Editable cells
                    lbl.setForeground(UIConstants.SUCCESS);
                    lbl.setFont(UIConstants.FONT_BODY_BOLD);
                } else {
                    lbl.setForeground(UIConstants.TEXT_PRIMARY);
                }
                return lbl;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);

        // Populate with all loai phong
        loadLoaiPhongRows(null);

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        scrollTable.setPreferredSize(new Dimension(0, 200));

        JLabel hintLbl = new JLabel(
                "<html><b>Giá cơ sở</b> từ Loại phòng. <b>Giá cuối tuần</b> áp dụng T7-CN. Để trống = dùng giá cơ sở. <b>Mức ưu tiên</b>: số nhỏ = ưu tiên cao hơn.</html>");
        hintLbl.setFont(UIConstants.FONT_SMALL);
        hintLbl.setForeground(UIConstants.TEXT_SECONDARY);

        tablePanel.add(lblTable, BorderLayout.NORTH);
        tablePanel.add(scrollTable, BorderLayout.CENTER);
        tablePanel.add(hintLbl, BorderLayout.SOUTH);

        body.add(topForm, BorderLayout.NORTH);
        body.add(tablePanel, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xF1F5F9)));

        RoundedButton btnCancel = new RoundedButton("Huỷ bỏ", new Color(0xF1F5F9), UIConstants.TEXT_SECONDARY);
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnCancel.setPreferredSize(new Dimension(100, 42));
        btnCancel.addActionListener(e -> dispose());

        RoundedButton btnSave = new RoundedButton(
                entity == null ? "Thêm bảng giá" : "Lưu thay đổi",
                UIConstants.PRIMARY, Color.WHITE);
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSave.setPreferredSize(new Dimension(160, 42));
        btnSave.addActionListener(e -> doSave());
        getRootPane().setDefaultButton(btnSave);

        footer.add(btnCancel);
        footer.add(btnSave);

        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void loadLoaiPhongRows(List<ChiTietBangGia> existingCT) {
        chiTietModel.setRowCount(0);
        for (LoaiPhong lp : allLoaiPhong) {
            String giaCoSo = String.format("%,.0f", lp.getGiaTheoNgay());
            String giaMoi = "";
            String giaCuoiTuan = "";
            String giaGio = "";
            String phuPhi = "";

            // If editing, fill existing values
            if (existingCT != null) {
                for (ChiTietBangGia ct : existingCT) {
                    if (ct.getLoaiPhong() != null && lp.getMaLoaiPhong().equals(ct.getLoaiPhong().getMaLoaiPhong())) {
                        giaMoi = ct.getGiaNgay() > 0 ? String.valueOf((long) ct.getGiaNgay()) : "";
                        giaCuoiTuan = ct.getGiaCuoiTuan() > 0 ? String.valueOf((long) ct.getGiaCuoiTuan()) : "";
                        giaGio = ct.getGiaGioDau() > 0 ? String.valueOf((long) ct.getGiaGioDau()) : "";
                        phuPhi = ct.getPhuPhiTraTre() > 0 ? String.valueOf((long) ct.getPhuPhiTraTre()) : "";
                        break;
                    }
                }
            }

            chiTietModel.addRow(new Object[] {
                    lp.getMaLoaiPhong(),
                    lp.getTenLoaiPhong(),
                    giaCoSo,
                    giaMoi,
                    giaCuoiTuan,
                    giaGio,
                    phuPhi
            });
        }
    }

    private void fillData() {
        txtTen.setText(entity.getTenBangGia());
        if (entity.getNgayBatDau() != null) {
            java.util.Date dateBD = java.util.Date.from(
                    entity.getNgayBatDau().atZone(ZoneId.systemDefault()).toInstant());
            spnBatDau.setDate(dateBD);
        }
        if (entity.getNgayKetThuc() != null) {
            java.util.Date dateKT = java.util.Date.from(
                    entity.getNgayKetThuc().atZone(ZoneId.systemDefault()).toInstant());
            spnKetThuc.setDate(dateKT);
        }
        cbTrangThai.setSelected(entity.isTrangThai());

        // Rate Plan fields
        if (entity.getLoaiBangGia() != null) cboLoaiBangGia.setSelectedItem(entity.getLoaiBangGia());
        if (entity.getDoiTuongApDung() != null) cboDoiTuong.setSelectedItem(entity.getDoiTuongApDung());
        spnUuTien.setValue(entity.getMucUuTien());

        // Load existing chi tiet
        List<ChiTietBangGia> existingCT = service.getChiTiet(entity.getMaBangGia());
        loadLoaiPhongRows(existingCT);
    }

    private void doSave() {
        String ten = txtTen.getText().trim();
        if (ten.isEmpty()) {
            err("Tên bảng giá không được để trống!");
            return;
        }

        java.util.Date d1 = spnBatDau.getDate();
        java.util.Date d2 = spnKetThuc.getDate();
        if (d1 == null || d2 == null) {
            err("Vui lòng chọn ngày bắt đầu và ngày kết thúc!");
            return;
        }
        if (!d2.after(d1)) {
            err("Ngày kết thúc phải sau ngày bắt đầu!");
            return;
        }

        // Collect chi tiet from table
        List<ChiTietBangGia> dsCT = new ArrayList<>();
        for (int i = 0; i < chiTietModel.getRowCount(); i++) {
            String maLoai = (String) chiTietModel.getValueAt(i, 0);
            String giaMoiStr = chiTietModel.getValueAt(i, 3) != null ? chiTietModel.getValueAt(i, 3).toString().trim() : "";
            String giaCTuanStr = chiTietModel.getValueAt(i, 4) != null ? chiTietModel.getValueAt(i, 4).toString().trim() : "";
            String giaGioStr = chiTietModel.getValueAt(i, 5) != null ? chiTietModel.getValueAt(i, 5).toString().trim() : "";
            String phuPhiStr = chiTietModel.getValueAt(i, 6) != null ? chiTietModel.getValueAt(i, 6).toString().trim() : "";

            // Skip empty rows
            if (giaMoiStr.isEmpty() && giaCTuanStr.isEmpty() && giaGioStr.isEmpty() && phuPhiStr.isEmpty())
                continue;

            ChiTietBangGia ct = new ChiTietBangGia();
            LoaiPhong lp = new LoaiPhong();
            lp.setMaLoaiPhong(maLoai);
            ct.setLoaiPhong(lp);

            try {
                if (!giaMoiStr.isEmpty())
                    ct.setGiaNgay(Double.parseDouble(giaMoiStr.replace(",", "").replace(".", "")));
                if (!giaCTuanStr.isEmpty())
                    ct.setGiaCuoiTuan(Double.parseDouble(giaCTuanStr.replace(",", "").replace(".", "")));
                if (!giaGioStr.isEmpty())
                    ct.setGiaGioDau(Double.parseDouble(giaGioStr.replace(",", "").replace(".", "")));
                if (!phuPhiStr.isEmpty())
                    ct.setPhuPhiTraTre(Double.parseDouble(phuPhiStr.replace(",", "").replace(".", "")));
            } catch (NumberFormatException e) {
                err("Giá trị không hợp lệ ở dòng " + (i + 1) + "! Chỉ nhập số.");
                return;
            }

            if (ct.getGiaNgay() < 0 || ct.getGiaGioDau() < 0 || ct.getPhuPhiTraTre() < 0 || ct.getGiaCuoiTuan() < 0) {
                err("Giá trị không được âm!");
                return;
            }

            dsCT.add(ct);
        }

        // Save BangGia header
        BangGia bg = entity != null ? entity : new BangGia();
        bg.setTenBangGia(ten);
        bg.setNgayBatDau(d1.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        bg.setNgayKetThuc(d2.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        bg.setTrangThai(cbTrangThai.isSelected());

        // Rate Plan fields
        bg.setLoaiBangGia((String) cboLoaiBangGia.getSelectedItem());
        bg.setDoiTuongApDung((String) cboDoiTuong.getSelectedItem());
        bg.setMucUuTien((Integer) spnUuTien.getValue());

        String error;
        if (entity == null) {
            error = service.them(bg);
        } else {
            error = service.sua(bg);
        }

        if (error != null) {
            err(error);
            return;
        }

        // Save chi tiet
        String maBG = bg.getMaBangGia();
        if (!dsCT.isEmpty()) {
            String ctError = service.saveChiTiet(maBG, dsCT);
            if (ctError != null) {
                err(ctError);
                return;
            }
        }

        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void err(String m) {
        JOptionPane.showMessageDialog(this, m, "Lỗi dữ liệu", JOptionPane.WARNING_MESSAGE);
    }

    private JTextField createModernTextField() {
        JTextField t = new JTextField();
        t.setFont(UIConstants.FONT_BODY);
        t.setBackground(new Color(249, 250, 251));
        t.setForeground(UIConstants.TEXT_PRIMARY);
        t.setCaretColor(UIConstants.PRIMARY);
        t.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        t.setPreferredSize(new Dimension(0, 36));
        return t;
    }

    private JSpinner createDateSpinner() {
        JSpinner spinner = new JSpinner();
        spinner.setModel(new SpinnerDateModel(new java.util.Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "dd/MM/yyyy");
        editor.getTextField().setFont(UIConstants.FONT_BODY);
        spinner.setEditor(editor);
        spinner.setPreferredSize(new Dimension(0, 36));
        spinner.setBackground(new Color(249, 250, 251));
        spinner.setBorder(BorderFactory.createLineBorder(new Color(203, 213, 225), 1));
        return spinner;
    }

    private JPanel createFormField(String label, JComponent component) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel lb = new JLabel(label);
        lb.setFont(UIConstants.FONT_SMALL_BOLD);
        lb.setForeground(UIConstants.TEXT_PRIMARY);
        p.add(lb, BorderLayout.NORTH);
        p.add(component, BorderLayout.CENTER);
        return p;
    }
}
