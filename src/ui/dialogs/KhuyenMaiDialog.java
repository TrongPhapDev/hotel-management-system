package ui.dialogs;

import dao.KhuyenMaiDAO;
import entity.KhuyenMai;
import entity.enums.LoaiGiam;
import ui.components.DateTimePicker;
import ui.components.UIConstants;
import ui.components.RoundedComponents.ModernTextField;
import ui.components.RoundedComponents.ModernComboBox;
import ui.components.RoundedComponents.RoundedButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class KhuyenMaiDialog extends JDialog {

    private final KhuyenMaiDAO kmDAO = new KhuyenMaiDAO();
    private KhuyenMai km;
    private final boolean isEdit;
    private final boolean isViewOnly;

    // UI Components
    private ModernTextField txtMa, txtTen, txtGiaTri, txtSoLuong, txtMinBill;
    private DateTimePicker pickBatDau, pickKetThuc;
    private ModernComboBox<LoaiGiam> cbLoai;
    private JCheckBox chkActive;

    public KhuyenMaiDialog(Window owner, KhuyenMai km, boolean isEdit, boolean isViewOnly) {
        super(owner, isViewOnly ? "Chi tiết khuyến mãi" : (isEdit ? "Chỉnh sửa khuyến mãi" : "Thêm khuyến mãi mới"), ModalityType.APPLICATION_MODAL);
        this.km = km;
        this.isEdit = isEdit;
        this.isViewOnly = isViewOnly;
        
        setSize(500, 750);
        setLocationRelativeTo(owner);
        setResizable(false);
        
        initComponents();
        if ((isEdit || isViewOnly) && km != null) fillData();
        
        if (isViewOnly) {
            setAllFieldsEditable(false);
        }
        
        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.white);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIConstants.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        
        JLabel title = new JLabel(isViewOnly ? "Chi tiết khuyến mãi" : (isEdit ? "Chỉnh sửa khuyến mãi" : "Thêm khuyến mãi mới"));
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        
        JLabel lblSub = new JLabel(isViewOnly ? km.getMaKhuyenMai() : "Nhập đầy đủ thông tin chương trình");
        lblSub.setFont(UIConstants.FONT_SMALL);
        lblSub.setForeground(new Color(255, 255, 255, 180));
        
        JPanel hText = new JPanel();
        hText.setOpaque(false);
        hText.setLayout(new BoxLayout(hText, BoxLayout.Y_AXIS));
        hText.add(title);
        hText.add(lblSub);
        header.add(hText, BorderLayout.WEST);
        
        // Form Body
        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 12, 0);
        gbc.gridx = 0;

        int row = 0;
        txtMa = addField(body, "Mã khuyến mãi (Voucher Code) *", "VD: SUMMER2024", gbc, row++);
        txtTen = addField(body, "Tên chương trình *", "VD: Giảm 10% mùa hè rực rỡ", gbc, row++);
        
        // Row with 2 columns: Type and Value
        JPanel row2 = new JPanel(new GridLayout(1, 2, 12, 0));
        row2.setOpaque(false);
        
        JPanel pLoai = new JPanel(new BorderLayout(0, 5));
        pLoai.setOpaque(false);
        JLabel lblLoai = new JLabel("Loại giảm giá");
        lblLoai.setFont(UIConstants.FONT_SMALL_BOLD);
        cbLoai = new ModernComboBox<>(LoaiGiam.values());
        pLoai.add(lblLoai, BorderLayout.NORTH);
        pLoai.add(cbLoai, BorderLayout.CENTER);
        
        JPanel pGtri = new JPanel(new BorderLayout(0, 5));
        pGtri.setOpaque(false);
        JLabel lblGtri = new JLabel("Giá trị (%) hoặc (VNĐ) *");
        lblGtri.setFont(UIConstants.FONT_SMALL_BOLD);
        txtGiaTri = new ModernTextField("0");
        pGtri.add(lblGtri, BorderLayout.NORTH);
        pGtri.add(txtGiaTri, BorderLayout.CENTER);
        
        row2.add(pLoai);
        row2.add(pGtri);
        
        gbc.gridy = row++;
        body.add(row2, gbc);

        pickBatDau = addDatePickerField(body, "Ngày bắt đầu (dd/MM/yyyy HH:mm) *", gbc, row++);
        pickKetThuc = addDatePickerField(body, "Ngày kết thúc (dd/MM/yyyy HH:mm) *", gbc, row++);
        
        // Row with 2 columns: Quota and Min Bill
        JPanel row3 = new JPanel(new GridLayout(1, 2, 12, 0));
        row3.setOpaque(false);
        txtSoLuong = addSubField(row3, "Số lượng phát hành *", "100");
        txtMinBill = addSubField(row3, "Đơn hàng tối thiểu (VNĐ) *", "0");
        gbc.gridy = row++;
        body.add(row3, gbc);

        // Active status
        chkActive = new JCheckBox("Kích hoạt chương trình khuyến mãi này");
        chkActive.setSelected(true);
        chkActive.setBackground(Color.WHITE);
        chkActive.setFont(UIConstants.FONT_BODY);
        gbc.gridy = row++;
        body.add(chkActive, gbc);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(new Color(0xF8FAFC));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER));
        
        RoundedButton btnCancel = new RoundedButton(isViewOnly ? "Đóng" : "Hủy bỏ", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        btnCancel.setPreferredSize(new Dimension(100, 38));
        btnCancel.addActionListener(e -> dispose());
        footer.add(btnCancel);

        if (!isViewOnly) {
            RoundedButton btnSave = new RoundedButton("Lưu thông tin", UIConstants.PRIMARY, Color.WHITE);
            btnSave.setPreferredSize(new Dimension(140, 38));
            btnSave.addActionListener(e -> save());
            footer.add(btnSave);
        }

        root.add(header, BorderLayout.NORTH);
        root.add(new JScrollPane(body), BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private DateTimePicker addDatePickerField(JPanel p, String label, GridBagConstraints gbc, int row) {
        JPanel wrap = new JPanel(new BorderLayout(0, 5));
        wrap.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_PRIMARY);
        DateTimePicker pick = new DateTimePicker(new Date());
        pick.setPreferredSize(new Dimension(0, 42));
        wrap.add(lbl, BorderLayout.NORTH);
        wrap.add(pick, BorderLayout.CENTER);
        gbc.gridy = row;
        p.add(wrap, gbc);
        return pick;
    }

    private ModernTextField addField(JPanel p, String label, String hint, GridBagConstraints gbc, int row) {
        JPanel wrap = new JPanel(new BorderLayout(0, 5));
        wrap.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_PRIMARY);
        ModernTextField txt = new ModernTextField(hint);
        txt.setPreferredSize(new Dimension(0, 42));
        wrap.add(lbl, BorderLayout.NORTH);
        wrap.add(txt, BorderLayout.CENTER);
        gbc.gridy = row;
        p.add(wrap, gbc);
        return txt;
    }

    private ModernTextField addSubField(JPanel p, String label, String hint) {
        JPanel wrap = new JPanel(new BorderLayout(0, 5));
        wrap.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_PRIMARY);
        ModernTextField txt = new ModernTextField(hint);
        txt.setPreferredSize(new Dimension(0, 42));
        wrap.add(lbl, BorderLayout.NORTH);
        wrap.add(txt, BorderLayout.CENTER);
        p.add(wrap);
        return txt;
    }

    private void fillData() {
        txtMa.setText(km.getMaKhuyenMai());
        txtTen.setText(km.getTenKhuyenMai());
        cbLoai.setSelectedItem(km.getLoaiGiam());
        txtGiaTri.setText(String.valueOf(km.getGiaTriGiam()));
        pickBatDau.setDate(java.util.Date.from(km.getNgayBatDau().atZone(ZoneId.systemDefault()).toInstant()));
        pickKetThuc.setDate(java.util.Date.from(km.getNgayKetThuc().atZone(ZoneId.systemDefault()).toInstant()));
        txtSoLuong.setText(String.valueOf(km.getSoLuong()));
        txtMinBill.setText(String.valueOf(km.getDieuKienToiThieu()));
        chkActive.setSelected(km.isTrangThai());
    }

    private void setAllFieldsEditable(boolean editable) {
        txtMa.setEditable(false); 
        txtTen.setEditable(editable);
        txtGiaTri.setEditable(editable);
        pickBatDau.setEnabled(editable);
        pickKetThuc.setEnabled(editable);
        txtSoLuong.setEditable(editable);
        txtMinBill.setEditable(editable);
        cbLoai.setEnabled(editable);
        chkActive.setEnabled(editable);
    }

    private void save() {
        try {
            if (km == null) km = new KhuyenMai();
            
            km.setMaKhuyenMai(txtMa.getText().trim());
            km.setTenKhuyenMai(txtTen.getText().trim());
            km.setLoaiGiam((LoaiGiam) cbLoai.getSelectedItem());
            km.setGiaTriGiam(Double.parseDouble(txtGiaTri.getText()));
            
            km.setNgayBatDau(pickBatDau.getLocalDateTime());
            km.setNgayKetThuc(pickKetThuc.getLocalDateTime());
            
            km.setSoLuong(Integer.parseInt(txtSoLuong.getText()));
            km.setDieuKienToiThieu(Double.parseDouble(txtMinBill.getText()));
            km.setTrangThai(chkActive.isSelected());

            if (km.getMaKhuyenMai().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Mã không được để trống!");
                return;
            }

            boolean success = isEdit ? kmDAO.update(km) : kmDAO.insert(km);
            if (success) {
                JOptionPane.showMessageDialog(this, "Đã lưu thành công!");
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Lỗi khi lưu dữ liệu!");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi định dạng dữ liệu: " + e.getMessage());
        }
    }
}
