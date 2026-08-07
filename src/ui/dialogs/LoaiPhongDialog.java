package ui.dialogs;

import service.*;
import entity.*;
import ui.components.UIConstants;
import ui.components.RoundedComponents.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dialog thêm/sửa Loại Phòng.
 * - Tiện nghi: checkbox grid nhóm theo nhóm (Cơ bản, Giải trí, Phòng tắm...)
 * - Giá cơ sở (rack rate) nằm ở LoaiPhong
 * - Giá theo mùa nằm ở BangGia / ChiTietBangGia
 */
public class LoaiPhongDialog extends JDialog {
    private final PhongService service = new PhongService();
    private final LoaiPhong entity;
    private String sourceIdForDuplicate; // Để lấy tiện nghi từ LP cũ nếu là nhân bản
    private boolean confirmed = false;

    private ModernTextField txtTen, txtSucChua, txtGiaCoSo, txtMoTa;
    private ModernComboBox<String> cboTrangThai;

    // Amenity checkboxes
    private final Map<String, JCheckBox> checkboxMap = new LinkedHashMap<>(); // maTienNghi -> JCheckBox
    private List<TienNghi> allTienNghi;

    public LoaiPhongDialog(Frame parent, LoaiPhong lp) {
        this(parent, lp, false);
    }

    public LoaiPhongDialog(Frame parent, LoaiPhong lp, boolean isDuplicate) {
        super(parent, isDuplicate ? "Nhân bản loại phòng" : (lp == null ? "Thêm loại phòng mới" : "Chỉnh sửa – " + lp.getTenLoai()), true);
        if (isDuplicate && lp != null) {
            this.sourceIdForDuplicate = lp.getMaLoai();
            // Clone basic properties into a new object
            this.entity = new LoaiPhong();
            this.entity.setTenLoai(lp.getTenLoai() + " (Bản sao)");
            this.entity.setSucChua(lp.getSucChua());
            this.entity.setGiaTheoNgay(lp.getGiaTheoNgay());
            this.entity.setMoTa(lp.getMoTa());
            this.entity.setTrangThai(lp.getTrangThai());
        } else {
            this.entity = lp;
        }
        
        setSize(600, 580);
        setLocationRelativeTo(parent);
        setResizable(false);
        buildUI();
        
        if (this.entity != null)
            fillData();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // Header with gradient
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

        JLabel title = new JLabel(entity == null ? "Thêm loại phòng mới" : (sourceIdForDuplicate != null ? "Nhân bản loại phòng" : "Chỉnh sửa loại phòng"));
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel(entity != null && sourceIdForDuplicate == null ? "Mã: " + entity.getMaLoai() : "Mã sẽ được tự động tạo");
        sub.setFont(UIConstants.FONT_SMALL);
        sub.setForeground(new Color(220, 230, 255));
        JPanel hl = new JPanel();
        hl.setOpaque(false);
        hl.setLayout(new BoxLayout(hl, BoxLayout.Y_AXIS));
        hl.add(title);
        hl.add(Box.createVerticalStrut(2));
        hl.add(sub);
        header.add(hl, BorderLayout.WEST);

        // Form body — top section (basic fields)
        JPanel basicFields = new JPanel(new GridLayout(0, 2, 14, 14));
        basicFields.setBackground(Color.WHITE);
        basicFields.setBorder(BorderFactory.createEmptyBorder(20, 24, 8, 24));

        txtTen = new ModernTextField("Nhập tên loại phòng...");
        txtSucChua = new ModernTextField("2");
        txtGiaCoSo = new ModernTextField("500000");
        txtMoTa = new ModernTextField("Mô tả thêm...");
        cboTrangThai = new ModernComboBox<>(new String[]{"Hoạt động", "Ngừng"});
        cboTrangThai.setPreferredSize(new Dimension(0, 40));

        basicFields.add(lf("Tên loại phòng *", txtTen));
        basicFields.add(lf("Sức chứa (người) *", txtSucChua));
        basicFields.add(lf("Giá cơ sở (đ/đêm) *", txtGiaCoSo));
        basicFields.add(lf("Trạng thái", cboTrangThai));
        basicFields.add(lf("Mô tả", txtMoTa));

        // Amenity checkbox grid
        JPanel amenitySection = buildAmenitySection();

        // Scroll wrapper for amenity
        JScrollPane amenityScroll = new JScrollPane(amenitySection);
        amenityScroll.setBorder(BorderFactory.createEmptyBorder());
        amenityScroll.getVerticalScrollBar().setUnitIncrement(16);
        amenityScroll.setPreferredSize(new Dimension(0, 200));

        // Hint about pricing
        JPanel hintPanel = new JPanel(new BorderLayout());
        hintPanel.setBackground(new Color(0xEFF6FF));
        hintPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xE2E8F0)),
                BorderFactory.createEmptyBorder(10, 24, 10, 24)));
        JLabel hintLbl = new JLabel("<html> <b>Giá cơ sở</b> là giá niêm yết mặc định. "
                + "Để thay đổi giá theo mùa/sự kiện, hãy dùng chức năng <b>Cách tính tiền</b> (Bảng giá).</html>");
        hintLbl.setFont(UIConstants.FONT_SMALL);
        hintLbl.setForeground(UIConstants.PRIMARY);
        hintPanel.add(hintLbl, BorderLayout.CENTER);

        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setBackground(Color.WHITE);
        centerWrap.add(basicFields, BorderLayout.NORTH);
        centerWrap.add(amenityScroll, BorderLayout.CENTER);
        centerWrap.add(hintPanel, BorderLayout.SOUTH);

        // Footer
        JPanel footer = footer();
        RoundedButton btnCancel = new RoundedButton("Huỷ", new Color(0xF1F5F9), UIConstants.TEXT_SECONDARY);
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnCancel.setPreferredSize(new Dimension(100, 42));
        
        RoundedButton btnSave = new RoundedButton(entity == null ? "Thêm loại phòng" : "Lưu thay đổi",
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

    /**
     * Xây dựng panel checkbox tiện nghi, nhóm theo nhomTienNghi.
     * Layout:
     * ┌─ Cơ bản ────────────────────────┐
     * │ ☑ 📶 Wifi ☑ ❄️ Điều hòa ... │
     * ├─ Giải trí ──────────────────────┤
     * │ ☑ 📺 Tivi ☐ 🍷 Minibar ... │
     * └──────────────────────────────────┘
     */
    private JPanel buildAmenitySection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(Color.WHITE);
        section.setBorder(BorderFactory.createEmptyBorder(4, 24, 8, 24));

        // Section title
        JLabel sectionTitle = new JLabel("Tiện nghi phòng");
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sectionTitle.setForeground(UIConstants.TEXT_PRIMARY);
        sectionTitle.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(sectionTitle);

        // Load all amenities from DB
        allTienNghi = service.getAllTienNghi();

        // Group by nhomTienNghi
        Map<String, List<TienNghi>> grouped = new LinkedHashMap<>();
        for (TienNghi tn : allTienNghi) {
            String nhom = tn.getNhomTienNghi() != null ? tn.getNhomTienNghi() : "Khác";
            grouped.computeIfAbsent(nhom, k -> new ArrayList<>()).add(tn);
        }

        // Build checkbox groups
        for (Map.Entry<String, List<TienNghi>> entry : grouped.entrySet()) {
            String nhom = entry.getKey();
            List<TienNghi> items = entry.getValue();

            // Group label
            JLabel groupLabel = new JLabel("  " + nhom);
            groupLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
            groupLabel.setForeground(UIConstants.TEXT_SECONDARY);
            groupLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xF1F5F9)),
                    BorderFactory.createEmptyBorder(6, 0, 4, 0)));
            groupLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            section.add(groupLabel);

            // Checkbox row (3 columns)
            JPanel checkboxRow = new JPanel(new GridLayout(0, 3, 8, 4));
            checkboxRow.setBackground(Color.WHITE);
            checkboxRow.setAlignmentX(Component.LEFT_ALIGNMENT);

            for (TienNghi tn : items) {
                JCheckBox cb = new JCheckBox(tn.getDisplayText());
                cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                cb.setBackground(Color.WHITE);
                cb.setFocusPainted(false);
                cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                checkboxMap.put(tn.getMaTienNghi(), cb);
                checkboxRow.add(cb);
            }
            section.add(checkboxRow);
        }

        return section;
    }

    private void fillData() {
        txtTen.setText(entity.getTenLoai());
        txtSucChua.setText(String.valueOf(entity.getSucChua()));
        txtGiaCoSo.setText(String.valueOf((long) entity.getGiaTheoNgay()));
        txtMoTa.setText(entity.getMoTa() != null ? entity.getMoTa() : "");
        sc(cboTrangThai, entity.getTrangThai());

        // Tick checkboxes for existing amenities
        String lookupId = sourceIdForDuplicate != null ? sourceIdForDuplicate : entity.getMaLoai();
        List<TienNghi> existing = (lookupId != null) ? service.getTienNghiByLoaiPhong(lookupId) : new ArrayList<>();
        Set<String> existingIds = existing.stream()
                .map(TienNghi::getMaTienNghi)
                .collect(Collectors.toSet());
        for (Map.Entry<String, JCheckBox> e : checkboxMap.entrySet()) {
            e.getValue().setSelected(existingIds.contains(e.getKey()));
        }
    }

    private void doSave() {
        String ten = txtTen.getText().trim();
        if (ten.isEmpty()) {
            err("Tên loại phòng không được để trống!");
            txtTen.requestFocus();
            return;
        }
        if (ten.length() < 2 || ten.length() > 100) {
            err("Tên loại phòng phải từ 2 đến 100 ký tự!");
            txtTen.requestFocus();
            return;
        }
        int sucChua;
        try {
            sucChua = Integer.parseInt(txtSucChua.getText().trim());
            if (sucChua < 1 || sucChua > 20)
                throw new Exception();
        } catch (Exception e) {
            err("Sức chứa phải là số nguyên từ 1–20!");
            txtSucChua.requestFocus();
            return;
        }

        long giaCoSo;
        try {
            giaCoSo = Long.parseLong(txtGiaCoSo.getText().trim().replace(",", "").replace(".", ""));
            if (giaCoSo <= 0)
                throw new Exception();
        } catch (Exception e) {
            err("Giá cơ sở phải là số dương!");
            txtGiaCoSo.requestFocus();
            return;
        }

        LoaiPhong lp = entity != null ? entity : new LoaiPhong();
        lp.setTenLoai(ten);
        lp.setSucChua(sucChua);
        lp.setGiaTheoNgay(giaCoSo);
        lp.setMoTa(txtMoTa.getText().trim());
        lp.setTrangThai((String) cboTrangThai.getSelectedItem());

        // Save LoaiPhong first
        String error;
        if (entity == null) {
            error = service.themLoaiPhong(lp);
        } else {
            error = service.suaLoaiPhong(lp);
        }

        if (error != null) {
            err(error);
            return;
        }

        // Save amenity mapping (M:N)
        List<String> selectedTienNghi = checkboxMap.entrySet().stream()
                .filter(e -> e.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        service.updateTienNghiForLoaiPhong(lp.getMaLoaiPhong(), selectedTienNghi);

        confirmed = true;
        dispose();
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

    private JComboBox<String> cb(String... i) {
        JComboBox<String> c = new JComboBox<>(i);
        c.setFont(UIConstants.FONT_BODY);
        return c;
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

    private JPanel footer() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xF1F5F9)));
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
