package ui.panels;

import ui.MainFrame;
import util.ExcelExporter;
import ui.components.RoundedComponents.*;
import ui.components.NotificationManager;
import ui.components.PaginationPanel;
import ui.components.UIConstants;
import ui.dialogs.KhachHangDialog;
import entity.KhachHang;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import service.KhachHangService;

public class KhachHangPanel extends JPanel implements ResettableFilter {
    
    @Override
    public void resetFilters() {
        if (txtSearch != null) txtSearch.setText("");
        if (cboQuocTich != null) cboQuocTich.setSelectedIndex(0);
        if (cboHang != null) cboHang.setSelectedIndex(0);
        runFilter();
    }

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger
            .getLogger(KhachHangPanel.class.getName());

    private final MainFrame mainFrame;
    private final KhachHangService service = new KhachHangService();

    private ModernTextField txtSearch;
    private ModernComboBox<String> cboQuocTich;
    private ModernComboBox<String> cboHang;
    private JTable table;
    private DefaultTableModel tableModel;
    private PaginationPanel pagination;

    private List<KhachHang> tatCaKhachHang;
    private List<KhachHang> filteredList;

    private JLabel lblTongKH, lblKhachVIP, lblKhachQuen, lblQuocTe;

    public KhachHangPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        main.add(buildHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(buildStats(), BorderLayout.NORTH);

        JPanel bot = new JPanel(new BorderLayout());
        bot.setOpaque(false);
        bot.add(Box.createVerticalStrut(14), BorderLayout.NORTH);
        bot.add(buildFilterBar(), BorderLayout.CENTER);
        bot.add(buildTableArea(), BorderLayout.SOUTH);

        center.add(bot, BorderLayout.CENTER);
        main.add(center, BorderLayout.CENTER);

        add(main, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel t = new JLabel("Hồ sơ Khách hàng");
        t.setFont(UIConstants.FONT_TITLE);
        JLabel s = new JLabel("Lưu trữ, tra cứu và quản lý thông tin lưu trú của khách hàng (CRM)");
        s.setFont(UIConstants.FONT_BODY);
        s.setForeground(UIConstants.TEXT_SECONDARY);
        left.add(t);
        left.add(Box.createVerticalStrut(2));
        left.add(s);

        RoundedButton btnThem = new RoundedButton("+ Thêm khách hàng", UIConstants.PRIMARY, Color.WHITE);
        RoundedButton btnExcel = new RoundedButton("↓ Xuất danh sách", new Color(0x16A34A), Color.WHITE);

        btnThem.addActionListener(e -> showDialog(null));
        btnExcel.addActionListener(e -> {
            if (filteredList != null && !filteredList.isEmpty()) {
                ExcelExporter.exportKhachHang(KhachHangPanel.this, filteredList);
            } else {
                JOptionPane.showMessageDialog(this, "Không có dữ liệu để xuất!");
            }
        });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(btnExcel);
        btns.add(btnThem);

        p.add(left, BorderLayout.WEST);
        p.add(btns, BorderLayout.EAST);
        return p;
    }

    private JPanel buildStats() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        lblTongKH = new JLabel("...");
        lblKhachVIP = new JLabel("...");
        lblKhachQuen = new JLabel("...");
        lblQuocTe = new JLabel("...");

        row.add(statCard("Tổng khách hàng", lblTongKH, UIConstants.PRIMARY));
        row.add(statCard("Khách VIP", lblKhachVIP, new Color(0xF59E0B))); // Gold
        row.add(statCard("Khách quay lại", lblKhachQuen, new Color(0x3B82F6))); // Blue
        row.add(statCard("Khách quốc tế", lblQuocTe, new Color(0x10B981))); // Emerald

        return row;
    }

    private RoundedPanel statCard(String label, JLabel lblValue, Color c) {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 10, 20));

        JLabel l1 = new JLabel(label);
        l1.setFont(UIConstants.FONT_SMALL);
        l1.setForeground(UIConstants.TEXT_SECONDARY);

        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblValue.setForeground(UIConstants.TEXT_PRIMARY);

        card.add(l1, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildFilterBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        txtSearch = new ModernTextField("Tìm Mã, Họ Tên, SĐT, CCCD...");
        txtSearch.setPreferredSize(new Dimension(280, 40));

        cboQuocTich = new ModernComboBox<>();
        cboQuocTich.setPreferredSize(new Dimension(185, 40));
        cboQuocTich.addItem("Quốc tịch (Tất cả)");
        cboQuocTich.addItem("Nội địa (Việt Nam)");
        cboQuocTich.addItem("Quốc tế");

        cboHang = new ModernComboBox<>();
        cboHang.setPreferredSize(new Dimension(200, 40));
        cboHang.addItem("Hạng khách (Tất cả)");
        cboHang.addItem("Hạng Thường (Bronze)");
        cboHang.addItem("Thân thiết (Silver)");
        cboHang.addItem("VIP (Gold)");

        DocumentListener dl = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                runFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                runFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                runFilter();
            }
        };
        txtSearch.getDocument().addDocumentListener(dl);

        ActionListener al = e -> runFilter();
        cboQuocTich.addActionListener(al);
        cboHang.addActionListener(al);

        p.add(txtSearch);
        p.add(cboQuocTich);
        p.add(cboHang);
        return p;
    }

    private JPanel buildTableArea() {
        String[] cols = { "Mã KH", "Họ tên", "Số ĐT", "Hạng khách", "Lượt ở", "Tổng chi tiêu", "Quốc tịch" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(tableModel);
        styleTable();

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    editSelected();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r >= 0)
                    table.setRowSelectionInterval(r, r);
            }
        });

        table.setComponentPopupMenu(buildContextMenu());

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        sp.setPreferredSize(new Dimension(0, 420));

        pagination = new PaginationPanel();
        pagination.setPageChangeListener(page -> displayCurrentPage());

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        footer.add(pagination, BorderLayout.CENTER);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(sp, BorderLayout.CENTER);
        wrap.add(footer, BorderLayout.SOUTH);

        loadData();
        return wrap;
    }

    private JPopupMenu buildContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Color.WHITE);
        menu.setBorder(BorderFactory.createLineBorder(new Color(0xE2E8F0)));

        // --- Group 1: Profile & Info ---
        JMenuItem miXem = createStyledMenuItem("Xem hồ sơ chi tiết", UIConstants.TEXT_PRIMARY);
        JMenuItem miCopy = createStyledMenuItem("Sao chép mã khách hàng", UIConstants.TEXT_PRIMARY);

        // --- Group 2: Business actions ---
        JMenuItem miDat = createStyledMenuItem("Đặt phòng mới", UIConstants.PRIMARY);
        JMenuItem miLichSu = createStyledMenuItem("Xem hóa đơn khách này", UIConstants.INFO);

        // --- Group 3: System / Dangerous ---
        JMenuItem miXoa = createStyledMenuItem("Vô hiệu hóa hồ sơ", UIConstants.DANGER);

        // Logic actions
        miXem.addActionListener(e -> editSelected());
        miCopy.addActionListener(e -> {
            KhachHang kh = getSelected();
            if (kh != null) {
                java.awt.datatransfer.StringSelection ss = new java.awt.datatransfer.StringSelection(kh.getMaKhachHang());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, ss);
                NotificationManager.showSuccess("Thành công", "Đã sao chép mã: " + kh.getMaKhachHang());
            }
        });

        miDat.addActionListener(e -> {
            KhachHang kh = getSelected();
            if (kh != null) {
                mainFrame.navigateTo("datphong", () -> {
                    if (mainFrame.getDatPhongPanel() != null) mainFrame.getDatPhongPanel().prefillKhachHang(kh);
                });
            }
        });

        miLichSu.addActionListener(e -> {
            KhachHang kh = getSelected();
            if (kh != null) {
                mainFrame.navigateTo("hoadon", () -> {
                    if (mainFrame.getHoaDonPanel() != null) mainFrame.getHoaDonPanel().prefillSearch(kh.getHoTen());
                });
            }
        });

        miXoa.addActionListener(e -> deleteSelected());

        menu.add(miXem);
        menu.add(miCopy);
        menu.addSeparator();
        menu.add(miDat);
        menu.add(miLichSu);
        menu.addSeparator();
        menu.add(miXoa);
        return menu;
    }

    private JMenuItem createStyledMenuItem(String text, Color fg) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(UIConstants.FONT_BODY);
        item.setForeground(fg);
        item.setBackground(Color.WHITE);
        item.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        return item;
    }

    private void styleTable() {
        table.setFont(UIConstants.FONT_BODY);
        table.setRowHeight(50);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(UIConstants.PRIMARY_LIGHT);
        // Căn lề cho tiêu đề
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                lbl.setBackground(UIConstants.BG_TABLE_HEADER);
                lbl.setForeground(UIConstants.TEXT_SECONDARY);
                lbl.setHorizontalAlignment(JLabel.CENTER);
                lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER));
                return lbl;
            }
        };
        table.getTableHeader().setDefaultRenderer(headerRenderer);

        // Thiết lập chiều rộng cột
        int[] widths = { 100, 180, 110, 160, 80, 130, 120 };
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row,
                    int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                lbl.setBackground(sel ? UIConstants.PRIMARY_LIGHT : (row % 2 == 0 ? Color.WHITE : new Color(0xFAFAFA)));
                lbl.setForeground(UIConstants.TEXT_PRIMARY);

                // Mặc định căn giữa cho hầu hết các cột định danh
                if (col == 0 || col == 2 || col == 3 || col == 4 || col == 6) {
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                } else if (col == 5) {
                    lbl.setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    lbl.setHorizontalAlignment(SwingConstants.LEFT);
                }

                // Tier coloring (Index 3: Hạng khách)
                if (col == 3 && val != null) {
                    String tier = val.toString();
                    if (tier.contains("VIP")) {
                        lbl.setForeground(new Color(0xD97706)); // Gold
                        lbl.setFont(UIConstants.FONT_BODY_BOLD);
                    } else if (tier.contains("Thân thiết")) {
                        lbl.setForeground(new Color(0x2563EB)); // Silver/Blue
                        lbl.setFont(UIConstants.FONT_BODY_BOLD);
                    } else {
                        lbl.setForeground(UIConstants.TEXT_SECONDARY);
                    }
                }

                // Nationality coloring (Index 6: Quốc tịch)
                if (col == 6 && val != null) {
                    String qt = val.toString();
                    if (!qt.equalsIgnoreCase("Việt Nam") && !qt.isBlank()) {
                        lbl.setForeground(new Color(0xD97706)); // Gold for Int'l
                    }
                }

                return lbl;
            }
        });
    }

    private void loadData() {
        SwingWorker<List<KhachHang>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<KhachHang> doInBackground() throws Exception {
                return service.getAll();
            }

            @Override
            protected void done() {
                try {
                    tatCaKhachHang = get();
                    updateStats();
                    runFilter();
                } catch (Exception e) {
                    LOGGER.log(java.util.logging.Level.WARNING, "UI error in KhachHangPanel", e);
                }
            }
        };
        worker.execute();
    }

    private void updateStats() {
        if (tatCaKhachHang == null)
            return;
        int total = tatCaKhachHang.size();
        long vip = tatCaKhachHang.stream().filter(k -> k.getHangKhachHang() != null && k.getHangKhachHang().contains("VIP")).count();
        long quen = tatCaKhachHang.stream().filter(k -> k.getSoLanO() > 1).count();
        long qte = tatCaKhachHang.stream()
                .filter(k -> k.getQuocTich() != null && !k.getQuocTich().equalsIgnoreCase("Việt Nam")).count();

        lblTongKH.setText(String.format("%,d", total));
        lblKhachVIP.setText(String.format("%,d", vip));
        lblKhachQuen.setText(String.format("%,d", quen));
        lblQuocTe.setText(String.format("%,d", qte));
    }

    private void runFilter() {
        if (tatCaKhachHang == null)
            return;

        String kw = txtSearch.getText().trim().toLowerCase();
        if (kw.equals("tìm mã, họ tên, sđt, cccd..."))
            kw = "";

        int idxQt = cboQuocTich.getSelectedIndex();

        final String finalKw = kw;

        filteredList = tatCaKhachHang.stream()
                .filter(kh -> {
                    boolean matchKw = finalKw.isEmpty() ||
                            (kh.getMaKhachHang() != null && kh.getMaKhachHang().toLowerCase().contains(finalKw)) ||
                            (kh.getHoTen() != null && kh.getHoTen().toLowerCase().contains(finalKw)) ||
                            (kh.getSdt() != null && kh.getSdt().contains(finalKw)) ||
                            (kh.getCccd() != null && kh.getCccd().contains(finalKw));

                    boolean matchQt = true;
                    boolean isVN = "Việt Nam".equalsIgnoreCase(kh.getQuocTich()) || kh.getQuocTich() == null
                            || kh.getQuocTich().isBlank();
                    if (idxQt == 1 && !isVN)
                        matchQt = false;
                    if (idxQt == 2 && isVN)
                        matchQt = false;

                    boolean matchHang = true;
                    String hang = kh.getHangKhachHang();
                    int idxHang = cboHang.getSelectedIndex();
                    if (idxHang == 1 && !hang.contains("Thường")) matchHang = false;
                    if (idxHang == 2 && !hang.contains("Thân thiết")) matchHang = false;
                    if (idxHang == 3 && !hang.contains("VIP")) matchHang = false;

                    return matchKw && matchQt && matchHang;
                })
                .collect(Collectors.toList());

        displayCurrentPage();
    }

    private void displayCurrentPage() {
        tableModel.setRowCount(0);
        if (filteredList == null)
            return;

        int pageSize = 12;
        int currentPage = pagination != null ? pagination.getCurrentPage() : 1;

        int totalPages = (int) Math.ceil((double) filteredList.size() / pageSize);
        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
            if (pagination != null) pagination.setCurrentPage(currentPage);
        }

        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, filteredList.size());

        for (int i = start; i < end; i++) {
            KhachHang kh = filteredList.get(i);
            String gt = kh.getGioiTinh() != null ? kh.getGioiTinh() : "Khác";
            String qte = (kh.getQuocTich() == null || kh.getQuocTich().isBlank()) ? "Việt Nam" : kh.getQuocTich();

            tableModel.addRow(new Object[] {
                    kh.getMaKhachHang(),
                    kh.getHoTen(),
                    kh.getSdt(),
                    kh.getHangKhachHang(),
                    kh.getSoLanO(),
                    String.format("%,.0f đ", kh.getTongChiTieu()),
                    qte
            });
        }

        if (pagination != null) {
            pagination.update(filteredList.size(), pageSize, currentPage);
        }
    }

    private KhachHang getSelected() {
        int row = table.getSelectedRow();
        if (row < 0)
            return null;
        return service.getById((String) tableModel.getValueAt(row, 0));
    }

    private void showDialog(KhachHang kh) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        KhachHangDialog dlg = new KhachHangDialog(owner, kh);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            NotificationManager.showSuccess("Thành công", 
                (kh == null ? "Đã thêm khách hàng mới" : "Đã cập nhật thông tin khách hàng"));
            refresh();
        }
    }

    private void editSelected() {
        KhachHang kh = getSelected();
        if (kh == null) {
            JOptionPane.showMessageDialog(this, "Chọn một hồ sơ khách hàng để mở!");
            return;
        }
        showDialog(kh);
    }

    private void deleteSelected() {
        KhachHang kh = getSelected();
        if (kh == null) {
            JOptionPane.showMessageDialog(this, "Chọn một hồ sơ khách hàng để xóa/khóa!");
            return;
        }

        int ok = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa hồ sơ khách hàng: \"" + kh.getHoTen() + "\" (" + kh.getMaKhachHang() + ")?\n"
                        + "Dữ liệu hóa đơn và phòng liên kết cũng có thể bị ảnh hưởng. Hành động này không thể hoàn tác!",
                "Xác nhận vô hiệu hóa hồ sơ", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (ok == JOptionPane.YES_OPTION) {
            String err = service.xoa(kh.getMaKhachHang());
            if (err == null) {
                NotificationManager.showSuccess("Thành công", "Đã xóa hồ sơ khách hàng " + kh.getHoTen());
                refresh();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Lỗi: " + err + "\nKhách hàng này có thể đang có Hóa đơn hoặc Phòng đang thuê.",
                        "Không thể xóa", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void refresh() {
        if (pagination != null) pagination.setCurrentPage(1);
        loadData();
    }
}
