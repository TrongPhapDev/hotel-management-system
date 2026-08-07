package ui.dialogs;

import service.AuthService;
import service.ThuePhongService;
import service.PhongService;
import service.KhachHangService;
import entity.*;
import ui.components.UIConstants;
import ui.components.RoundedComponents.*;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.print.*;
import java.text.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Dialog thanh toán tổng hợp đoàn (Master Bill / Gom Bill).
 * Hiển thị chi tiết tất cả phòng + dịch vụ + phụ phí trong 1 hóa đơn duy nhất.
 */
public class MasterBillDialog extends JDialog {

    private final ThuePhongService thuePhongService = new ThuePhongService();
    private final PhongService phongService = new PhongService();
    private final KhachHangService khService = new KhachHangService();

    private final DatPhong datPhong;
    private final List<ChiTietDatPhong> allRooms;
    private boolean confirmed = false;

    private DefaultTableModel tableModel;
    private JLabel lblTongPhong, lblTongDV, lblTongPhuPhi, lblTienCoc, lblTongCong;
    private JTextField txtTienKhachDua;
    private JLabel lblTienThua;
    private JComboBox<String> cboHinhThuc;

    public MasterBillDialog(Frame parent, DatPhong dp, List<ChiTietDatPhong> rooms) {
        super(parent, "Gom Bill Đoàn — " + (dp.getTenDoan() != null ? dp.getTenDoan() : dp.getMaDatPhong()), true);
        this.datPhong = dp;
        this.allRooms = rooms;
        setSize(780, 780);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(Color.WHITE);
        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        add(buildBanner(), BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(buildBody());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    // ---- Banner ----
    private JPanel buildBanner() {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(UIConstants.PRIMARY); // Purple for group
        banner.setBorder(BorderFactory.createEmptyBorder(18, 28, 18, 28));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel t1 = new JLabel("HÓA ĐƠN ĐOÀN (MASTER BILL)");
        t1.setFont(new Font("Segoe UI", Font.BOLD, 20));
        t1.setForeground(Color.WHITE);

        String tenDoan = datPhong.getTenDoan() != null ? datPhong.getTenDoan() : "Khách đoàn";
        JLabel t2 = new JLabel(tenDoan + "  " + allRooms.size() + " phòng");
        t2.setFont(UIConstants.FONT_BODY);
        t2.setForeground(new Color(255, 255, 255, 200));
        left.add(t1);
        left.add(Box.createVerticalStrut(2));
        left.add(t2);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JLabel lma = new JLabel("Mã ĐP: " + datPhong.getMaDatPhong());
        lma.setFont(UIConstants.FONT_SMALL_BOLD);
        lma.setForeground(Color.WHITE);
        lma.setHorizontalAlignment(SwingConstants.RIGHT);
        JLabel ldt = new JLabel("Xuất: " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()));
        ldt.setFont(UIConstants.FONT_SMALL);
        ldt.setForeground(new Color(255, 255, 255, 180));
        ldt.setHorizontalAlignment(SwingConstants.RIGHT);
        right.add(lma);
        right.add(Box.createVerticalStrut(2));
        right.add(ldt);

        banner.add(left, BorderLayout.WEST);
        banner.add(right, BorderLayout.EAST);
        return banner;
    }

    // ---- Body ----
    private JPanel buildBody() {
        JPanel body = new JPanel();
        body.setBackground(Color.WHITE);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(20, 28, 16, 28));

        body.add(buildGroupInfoSection());
        body.add(Box.createVerticalStrut(18));
        body.add(buildBillSection());
        body.add(Box.createVerticalStrut(16));
        body.add(buildSummarySection());
        return body;
    }

    private JPanel buildGroupInfoSection() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 24, 0));
        panel.setOpaque(false);

        KhachHang kh = datPhong.getKhachHang();
        if (kh != null && (kh.getHoTen() == null || kh.getHoTen().isBlank()) && kh.getMaKhachHang() != null) {
            kh = khService.getById(kh.getMaKhachHang());
        }
        String ten = kh != null && kh.getHoTen() != null ? kh.getHoTen() : "—";
        String sdt = kh != null && kh.getSoDienThoai() != null ? kh.getSoDienThoai() : "—";
        String cccd = kh != null && kh.getCccd() != null ? kh.getCccd() : "—";

        panel.add(infoBlock("THÔNG TIN ĐOÀN", new String[][]{
                {"Người đại diện:", ten},
                {"SĐT:", sdt},
                {"CCCD:", cccd},
                {"Tên đoàn:", datPhong.getTenDoan() != null ? datPhong.getTenDoan() : "—"},
                {"Loại khách:", datPhong.getLoaiKhachLabel()}
        }));

        // Liệt kê danh sách phòng
        StringBuilder roomList = new StringBuilder();
        for (ChiTietDatPhong ct : allRooms) {
            if (roomList.length() > 0) roomList.append(", ");
            roomList.append(ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?");
        }

        DateTimeFormatter sdf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String ngayNhan = datPhong.getNgayNhanDuKien() != null ? datPhong.getNgayNhanDuKien().format(sdf) : "—";
        String ngayTra = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());

        panel.add(infoBlock("CHI TIẾT LƯU TRÚ", new String[][]{
                {"Danh sách phòng:", roomList.toString()},
                {"Số phòng:", allRooms.size() + " phòng"},
                {"Nhận phòng:", ngayNhan},
                {"Thanh toán:", ngayTra}
        }));
        return panel;
    }

    private JPanel infoBlock(String heading, String[][] rows) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel lbl = new JLabel(heading);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_MUTED);
        panel.add(lbl);
        panel.add(Box.createVerticalStrut(8));
        for (String[] row : rows) {
            JPanel r = new JPanel(new BorderLayout(8, 0));
            r.setOpaque(false);
            r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
            JLabel k = new JLabel(row[0]);
            k.setFont(UIConstants.FONT_SMALL);
            k.setForeground(UIConstants.TEXT_SECONDARY);
            k.setPreferredSize(new Dimension(100, 18));
            JLabel v = new JLabel(row[1]);
            v.setFont(UIConstants.FONT_SMALL);
            r.add(k, BorderLayout.WEST);
            r.add(v, BorderLayout.CENTER);
            panel.add(r);
            panel.add(Box.createVerticalStrut(3));
        }
        return panel;
    }

    private JPanel buildBillSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        JLabel hdr = new JLabel("BẢNG CHI TIẾT THANH TOÁN ĐOÀN");
        hdr.setFont(UIConstants.FONT_SMALL_BOLD);
        hdr.setForeground(UIConstants.TEXT_MUTED);
        hdr.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        String[] cols = {"Phòng", "Khoản mục", "Đơn giá", "Số lượng", "Thành tiền"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        table.setFont(UIConstants.FONT_BODY);
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(UIConstants.PRIMARY_LIGHT);
        table.getTableHeader().setFont(UIConstants.FONT_SMALL_BOLD);
        table.getTableHeader().setBackground(UIConstants.BG_TABLE_HEADER);
        table.getTableHeader().setForeground(UIConstants.TEXT_SECONDARY);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER));

        // Custom renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                lbl.setBackground(sel ? UIConstants.PRIMARY_LIGHT : Color.WHITE);
                lbl.setFont(UIConstants.FONT_BODY);
                if (col == 4) {
                    lbl.setFont(UIConstants.FONT_BODY_BOLD);
                    lbl.setForeground(UIConstants.SUCCESS);
                } else if (col == 0) {
                    lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                    lbl.setForeground(UIConstants.PRIMARY);
                } else {
                    lbl.setForeground(UIConstants.TEXT_PRIMARY);
                }
                return lbl;
            }
        });

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(250);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);
        table.getColumnModel().getColumn(4).setPreferredWidth(120);

        // Init summary labels BEFORE loadBillRows
        lblTongPhong = new JLabel("0 đ");
        lblTongPhong.setFont(UIConstants.FONT_BODY);
        lblTongDV = new JLabel("0 đ");
        lblTongDV.setFont(UIConstants.FONT_BODY);
        lblTongPhuPhi = new JLabel("0 đ");
        lblTongPhuPhi.setFont(UIConstants.FONT_BODY);
        lblTienCoc = new JLabel("Không cọc");
        lblTienCoc.setFont(UIConstants.FONT_BODY);
        lblTienCoc.setForeground(new java.awt.Color(0xF97316)); // cam
        lblTongCong = new JLabel("0 đ");
        lblTongCong.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTongCong.setForeground(UIConstants.PRIMARY);

        loadBillRows();

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        sp.setPreferredSize(new Dimension(0, 220));
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));

        panel.add(hdr, BorderLayout.NORTH);
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    private void loadBillRows() {
        tableModel.setRowCount(0);
        double totalPhong = 0, totalDV = 0, totalPhuPhi = 0;

        for (ChiTietDatPhong ct : allRooms) {
            String roomId = ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?";
            String loaiPhong = ct.getTenLoaiPhong() != null ? ct.getTenLoaiPhong() : "";

            // Tính tiền phòng
            LocalDateTime ngayNhanResolved = ct.getNgayNhanThucTe();
            if (ngayNhanResolved == null && datPhong.getNgayNhanDuKien() != null) {
                ngayNhanResolved = datPhong.getNgayNhanDuKien();
            }
            LocalDateTime ngayTraResolved = ct.getNgayTraThucTe() != null ? ct.getNgayTraThucTe() : LocalDateTime.now();
            long soNgay = Math.max(1, thuePhongService.tinhSoNgay(
                    ngayNhanResolved != null ? java.util.Date.from(ngayNhanResolved.atZone(java.time.ZoneId.systemDefault()).toInstant()) : new Date(),
                    java.util.Date.from(ngayTraResolved.atZone(java.time.ZoneId.systemDefault()).toInstant())
            ));
            service.BangGiaService bangGiaService = new service.BangGiaService();
            double policyPrice = bangGiaService.layGiaHienHanh(ct.getPhong() != null && ct.getPhong().getLoaiPhong() != null ? ct.getPhong().getLoaiPhong().getMaLoaiPhong() : "");
            
            double donGia = policyPrice > 0 ? policyPrice : (ct.getGiaThucTeChot() > 0 ? ct.getGiaThucTeChot() : 400000);
            double tienPhong = donGia * soNgay;
            totalPhong += tienPhong;

            tableModel.addRow(new Object[]{
                    roomId,
                    "Tiền phòng – " + loaiPhong,
                    String.format("%,.0f đ/đêm", donGia),
                    soNgay + " đêm",
                    String.format("%,.0f đ", tienPhong)
            });

            // Tính phụ phí bằng StandardRoomCalculator
            service.StandardRoomCalculator calc = new service.StandardRoomCalculator();
            double phiCheckinOut = (ngayNhanResolved != null) ? calc.tinhPhuPhi(ct, ngayNhanResolved, ngayTraResolved) : 0;
            double phuPhi = phiCheckinOut + ct.getPhuPhiPhatSinh();
            if (phuPhi > 0) {
                totalPhuPhi += phuPhi;
                tableModel.addRow(new Object[]{
                        roomId, "Phụ phí", "—", "—", String.format("%,.0f đ", phuPhi)
                });
            }

            // Dịch vụ phòng
            List<SuDungDichVu> dsDichVu = thuePhongService.getDichVuByChiTiet(ct.getMaChiTiet());
            for (SuDungDichVu sddv : dsDichVu) {
                double tienDV = sddv.tinhThanhTien();
                totalDV += tienDV;
                tableModel.addRow(new Object[]{
                        roomId,
                        sddv.getTenDichVu() != null ? sddv.getTenDichVu() : "Dịch vụ",
                        String.format("%,.0f đ", sddv.getDonGia()),
                        sddv.getSoLuong() + "x",
                        String.format("%,.0f đ", tienDV)
                });
            }
        }

        java.math.BigDecimal bdTotalPhong = java.math.BigDecimal.valueOf(totalPhong);
        java.math.BigDecimal bdTotalDV = java.math.BigDecimal.valueOf(totalDV);
        java.math.BigDecimal bdTotalPhuPhi = java.math.BigDecimal.valueOf(totalPhuPhi);

        // Tiền cọc — hiển thị và trừ khỏi tổng
        double tienCoc = datPhong.getTienDatCoc();

        if (lblTongPhong  != null) lblTongPhong.setText(String.format("%,.0f đ", bdTotalPhong.doubleValue()));
        if (lblTongDV     != null) lblTongDV.setText(String.format("%,.0f đ", bdTotalDV.doubleValue()));
        if (lblTongPhuPhi != null) lblTongPhuPhi.setText(String.format("%,.0f đ", bdTotalPhuPhi.doubleValue()));
        if (lblTienCoc    != null) lblTienCoc.setText(tienCoc > 0 ? String.format("-%,.0f đ", tienCoc) : "Không cọc");
        if (lblTongCong   != null) {
            double tongSauCoc = Math.max(0,
                    bdTotalPhong.add(bdTotalDV).add(bdTotalPhuPhi)
                    .subtract(java.math.BigDecimal.valueOf(tienCoc)).doubleValue());
            lblTongCong.setText(String.format("%,.0f đ", tongSauCoc));
            updateTienThua();
        }
    }

    private JPanel buildSummarySection() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 350));

        panel.add(Box.createVerticalStrut(10));
        panel.add(sumRow("Tổng tiền phòng (" + allRooms.size() + " phòng)", lblTongPhong, false));
        panel.add(Box.createVerticalStrut(4));
        panel.add(sumRow("Tổng phụ phí", lblTongPhuPhi, false));
        panel.add(Box.createVerticalStrut(4));
        panel.add(sumRow("Tổng dịch vụ", lblTongDV, false));
        panel.add(Box.createVerticalStrut(4));
        panel.add(sumRow("Tiền cọc đã đặt (khấu trừ)", lblTienCoc, false));
        panel.add(Box.createVerticalStrut(6));
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(sep);
        panel.add(Box.createVerticalStrut(6));
        panel.add(sumRow("TỔNG CỘNG ĐOÀN", lblTongCong, true));

        panel.add(Box.createVerticalStrut(12));
        JPanel rowHT = new JPanel(new BorderLayout());
        rowHT.setOpaque(false);
        rowHT.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JLabel lblHT = new JLabel("Hình thức thanh toán");
        lblHT.setFont(UIConstants.FONT_BODY);
        lblHT.setForeground(UIConstants.TEXT_SECONDARY);
        rowHT.add(lblHT, BorderLayout.WEST);

        String[] options = {"Tiền mặt", "Chuyển khoản", "Thẻ tín dụng"};
        cboHinhThuc = new JComboBox<>(options);
        cboHinhThuc.setFont(UIConstants.FONT_BODY);
        cboHinhThuc.setBackground(Color.WHITE);
        cboHinhThuc.setPreferredSize(new Dimension(140, 28));

        JPanel comboPan = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        comboPan.setOpaque(false);
        comboPan.add(cboHinhThuc);
        rowHT.add(comboPan, BorderLayout.EAST);

        panel.add(rowHT);

        // Thêm text field Tiền khách đưa và Tiền thừa
        JPanel rowTienKhachDua = new JPanel(new BorderLayout());
        rowTienKhachDua.setOpaque(false);
        rowTienKhachDua.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        JLabel lblTienKhachDua = new JLabel("Tiền khách đưa");
        lblTienKhachDua.setFont(UIConstants.FONT_BODY);
        lblTienKhachDua.setForeground(UIConstants.TEXT_SECONDARY);
        rowTienKhachDua.add(lblTienKhachDua, BorderLayout.WEST);

        txtTienKhachDua = new ui.components.RoundedComponents.ModernTextField("");
        txtTienKhachDua.setPreferredSize(new Dimension(180, 38));
        txtTienKhachDua.setFont(new Font("Segoe UI", Font.BOLD, 15));
        txtTienKhachDua.setHorizontalAlignment(SwingConstants.RIGHT);
        txtTienKhachDua.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateTienThua(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateTienThua(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateTienThua(); }
        });

        JPanel rowKhachDuaRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rowKhachDuaRight.setOpaque(false);
        rowKhachDuaRight.add(txtTienKhachDua);
        rowTienKhachDua.add(rowKhachDuaRight, BorderLayout.EAST);

        panel.add(Box.createVerticalStrut(4));
        panel.add(rowTienKhachDua);

        JPanel rowTienThua = new JPanel(new BorderLayout());
        rowTienThua.setOpaque(false);
        rowTienThua.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JLabel lblTienThuaTitle = new JLabel("Tiền thối lại");
        lblTienThuaTitle.setFont(UIConstants.FONT_BODY);
        lblTienThuaTitle.setForeground(UIConstants.TEXT_SECONDARY);
        rowTienThua.add(lblTienThuaTitle, BorderLayout.WEST);

        lblTienThua = new JLabel("0 đ");
        lblTienThua.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTienThua.setForeground(UIConstants.SUCCESS);

        JPanel rowTienThuaRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rowTienThuaRight.setOpaque(false);
        rowTienThuaRight.add(lblTienThua);
        rowTienThua.add(rowTienThuaRight, BorderLayout.EAST);

        panel.add(Box.createVerticalStrut(4));
        panel.add(rowTienThua);

        cboHinhThuc.addActionListener(e -> {
            boolean isCash = "Tiền mặt".equals(cboHinhThuc.getSelectedItem());
            rowTienKhachDua.setVisible(isCash);
            rowTienThua.setVisible(isCash);
            if (!isCash) txtTienKhachDua.setText("");
            updateTienThua();
        });

        return panel;
    }

    private void updateTienThua() {
        if (lblTongCong == null || txtTienKhachDua == null || lblTienThua == null) return;
        
        String tongText = lblTongCong.getText().replaceAll("[^\\d]", "");
        long tongCong = 0;
        try { tongCong = Long.parseLong(tongText); } catch (Exception ignored) {}
        
        String khachText = txtTienKhachDua.getText().replaceAll("[^\\d]", "");
        long khachDua = 0;
        try { khachDua = Long.parseLong(khachText); } catch (Exception ignored) {}
        
        long tienThua = khachDua - tongCong;
        if (khachDua == 0 || khachText.isEmpty()) {
            lblTienThua.setText("0 đ");
            lblTienThua.setForeground(UIConstants.TEXT_SECONDARY);
        } else if (tienThua < 0) {
            lblTienThua.setText("Thiếu " + String.format("%,d đ", -tienThua));
            lblTienThua.setForeground(UIConstants.DANGER);
        } else {
            lblTienThua.setText(String.format("%,d đ", tienThua));
            lblTienThua.setForeground(UIConstants.SUCCESS);
        }
    }

    private JPanel sumRow(String label, JLabel valLabel, boolean bold) {
        JPanel r = new JPanel(new BorderLayout());
        r.setOpaque(false);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JLabel lbl = new JLabel(label);
        lbl.setFont(bold ? new Font("Segoe UI", Font.BOLD, 15) : UIConstants.FONT_BODY);
        lbl.setForeground(bold ? UIConstants.TEXT_PRIMARY : UIConstants.TEXT_SECONDARY);
        r.add(lbl, BorderLayout.WEST);
        r.add(valLabel, BorderLayout.EAST);
        return r;
    }

    // ---- Footer ----
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(12, 28, 12, 28)));

        JButton btnPrint = new JButton("🖨 In hóa đơn đoàn");
        btnPrint.setFont(UIConstants.FONT_BODY);
        btnPrint.setForeground(UIConstants.TEXT_SECONDARY);
        btnPrint.setBorderPainted(false);
        btnPrint.setContentAreaFilled(false);
        btnPrint.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPrint.addActionListener(e -> printMasterBill());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        RoundedButton btnCancel = new RoundedButton("Huỷ", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        RoundedButton btnConfirm = new RoundedButton("Xác nhận thanh toán đoàn", UIConstants.PRIMARY, Color.WHITE);
        btnConfirm.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCancel.addActionListener(e -> dispose());
        btnConfirm.addActionListener(e -> doMasterCheckout());
        btns.add(btnCancel);
        btns.add(btnConfirm);

        footer.add(btnPrint, BorderLayout.WEST);
        footer.add(btns, BorderLayout.EAST);
        return footer;
    }

    // ---- MASTER CHECKOUT ----
    private void doMasterCheckout() {
        String hinhThuc = (String) cboHinhThuc.getSelectedItem();
        if (hinhThuc == null) return;

        if ("Tiền mặt".equals(hinhThuc)) {
            String tongText = lblTongCong.getText().replaceAll("[^\\d]", "");
            long tongCong = 0;
            try { tongCong = Long.parseLong(tongText); } catch (Exception ignored) {}

            String khachText = txtTienKhachDua.getText().replaceAll("[^\\d]", "");
            long khachDua = 0;
            try { khachDua = Long.parseLong(khachText); } catch (Exception ignored) {}

            if (khachDua < tongCong) {
                ui.components.NotificationManager.showError("Lỗi thanh toán", "Tiền khách đưa chưa đủ để thanh toán!");
                return;
            }
        }

        // Nếu chuyển khoản → hiện QR
        if ("Chuyển khoản".equals(hinhThuc)) {
            String tongText = lblTongCong.getText().replaceAll("[^\\d]", "");
            long tongCong = 0;
            try {
                tongCong = Long.parseLong(tongText);
            } catch (NumberFormatException ignored) {}

            String maHD = "HD-GRP-" + datPhong.getMaDatPhong();
            QRPaymentDialog qrDialog = new QRPaymentDialog((Frame) getOwner(), tongCong, maHD);
            qrDialog.setVisible(true);
            if (!qrDialog.isConfirmed()) return;
        }

        int ok = JOptionPane.showConfirmDialog(this,
                "Xác nhận thanh toán gom bill TOÀN BỘ ĐOÀN?\n"
                        + "Đoàn: " + (datPhong.getTenDoan() != null ? datPhong.getTenDoan() : datPhong.getMaDatPhong()) + "\n"
                        + "Số phòng: " + allRooms.size() + "\n"
                        + "Tổng tiền: " + lblTongCong.getText(),
                "Xác nhận Gom Bill Đoàn", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        String err = thuePhongService.checkOutMasterBill(
                datPhong.getMaDatPhong(),
                AuthService.getInstance().getCurrentMaNV(),
                datPhong.getMaKhuyenMai());

        if (err == null) {
            confirmed = true;
            ui.components.NotificationManager.showSuccess("Thanh toán đoàn thành công", 
                allRooms.size() + " phòng đã được thanh toán.");
            dispose();
        } else {
            ui.components.NotificationManager.showError("Lỗi", err);
        }
    }

    // ---- PRINT ----
    private void printMasterBill() {
        JPanel printPanel = buildPrintPanel();

        JWindow dummyWindow = new JWindow();
        dummyWindow.getContentPane().add(printPanel);
        dummyWindow.pack();

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("MasterBill_" + datPhong.getMaDatPhong());

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) graphics;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double scaleX = pageFormat.getImageableWidth() / printPanel.getPreferredSize().getWidth();
            double scaleY = pageFormat.getImageableHeight() / printPanel.getPreferredSize().getHeight();
            double scale = Math.min(scaleX, scaleY);
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            g2.scale(scale, scale);
            printPanel.printAll(g2);
            return Printable.PAGE_EXISTS;
        });

        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException ex) {
                ui.components.NotificationManager.showError("Lỗi in", ex.getMessage());
            }
        }
        dummyWindow.dispose();
    }

    private JPanel buildPrintPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(500, 800));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        KhachHang kh = datPhong.getKhachHang();
        if (kh != null && kh.getMaKhachHang() != null) {
            KhachHang full = khService.getById(kh.getMaKhachHang());
            if (full != null) kh = full;
        }

        // Header
        JLabel title = new JLabel("HÓA ĐƠN ĐOÀN (MASTER BILL)", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel maDP = new JLabel("Mã ĐP: " + datPhong.getMaDatPhong(), SwingConstants.CENTER);
        maDP.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        maDP.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel ngay = new JLabel("Ngày: " + sdf.format(new Date()), SwingConstants.CENTER);
        ngay.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        ngay.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(2));
        panel.add(maDP);
        panel.add(ngay);
        panel.add(Box.createVerticalStrut(12));
        panel.add(printSep());
        panel.add(Box.createVerticalStrut(8));

        // Info
        panel.add(printRow("Đoàn:", datPhong.getTenDoan() != null ? datPhong.getTenDoan() : "—"));
        panel.add(printRow("Người đại diện:", kh != null ? kh.getHoTen() : "—"));
        panel.add(printRow("SĐT:", kh != null && kh.getSoDienThoai() != null ? kh.getSoDienThoai() : "—"));
        panel.add(printRow("Số phòng:", allRooms.size() + " phòng"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(printSep());
        panel.add(Box.createVerticalStrut(8));

        // Chi tiết từng phòng
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String phong = String.valueOf(tableModel.getValueAt(i, 0));
            String noiDung = String.valueOf(tableModel.getValueAt(i, 1));
            String thanhTien = String.valueOf(tableModel.getValueAt(i, 4));
            panel.add(printRow("[" + phong + "] " + noiDung, thanhTien));
        }
        panel.add(Box.createVerticalStrut(8));
        panel.add(printSep());
        panel.add(Box.createVerticalStrut(8));

        double tienCoc = datPhong.getTienDatCoc();
        if (tienCoc > 0) {
            panel.add(printRow("Tiền cọc đã đặt (khấu trừ):", String.format("-%,.0f đ", tienCoc)));
            panel.add(Box.createVerticalStrut(8));
            panel.add(printSep());
            panel.add(Box.createVerticalStrut(8));
        }

        JLabel total = new JLabel("TỔNG CỘNG:   " + lblTongCong.getText());
        total.setFont(new Font("Segoe UI", Font.BOLD, 15));
        total.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(total);
        panel.add(Box.createVerticalStrut(16));
        JLabel thanks = new JLabel("Cảm ơn quý đoàn đã tin tưởng và sử dụng dịch vụ!", SwingConstants.CENTER);
        thanks.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        thanks.setForeground(Color.GRAY);
        thanks.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(thanks);
        return panel;
    }

    private JPanel printRow(String label, String value) {
        JPanel r = new JPanel(new BorderLayout());
        r.setBackground(Color.WHITE);
        r.setOpaque(true);
        r.setPreferredSize(new Dimension(450, 22));
        r.setMaximumSize(new Dimension(450, 22));
        r.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel k = new JLabel(label);
        k.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        k.setPreferredSize(new Dimension(260, 20));
        JLabel v = new JLabel(value, SwingConstants.RIGHT);
        v.setFont(new Font("Segoe UI", Font.BOLD, 11));
        r.add(k, BorderLayout.WEST);
        r.add(v, BorderLayout.EAST);
        return r;
    }

    private JSeparator printSep() {
        JSeparator sep = new JSeparator();
        sep.setPreferredSize(new Dimension(450, 1));
        sep.setMaximumSize(new Dimension(450, 1));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        return sep;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
