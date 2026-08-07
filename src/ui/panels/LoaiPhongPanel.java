package ui.panels;

import ui.components.RoundedComponents.*;
import ui.components.NotificationManager;
import ui.components.UIConstants;
import ui.dialogs.LoaiPhongDialog;
import entity.LoaiPhong;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import service.PhongService;
import ui.components.PaginationPanel;
import java.util.List;

public class LoaiPhongPanel extends JPanel implements ResettableFilter {

    @Override
    public void resetFilters() {
        if (txtSearch != null) txtSearch.setText("");
        if (cboTrangThai != null) cboTrangThai.setSelectedIndex(0);
        runFilter();
    }

    private final PhongService service = new PhongService();

    private ModernTextField txtSearch;
    private ModernComboBox<String> cboTrangThai;
    private DefaultTableModel tableModel;
    private JTable table;
    private PaginationPanel pagination;
    private List<LoaiPhong> tatCaLoaiPhong;
    private List<LoaiPhong> filteredList;

    public LoaiPhongPanel() {
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
        JLabel t = new JLabel("Quản lý loại phòng");
        t.setFont(UIConstants.FONT_TITLE);
        JLabel s = new JLabel("Cấu hình danh mục, tiện nghi và mức giá từng loại phòng");
        s.setFont(UIConstants.FONT_BODY);
        s.setForeground(UIConstants.TEXT_SECONDARY);
        left.add(t);
        left.add(Box.createVerticalStrut(2));
        left.add(s);

        RoundedButton btnAdd = new RoundedButton("+ Thêm loại phòng", UIConstants.PRIMARY, Color.WHITE);
        btnAdd.addActionListener(e -> showDialog(null, false));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(btnAdd);

        p.add(left, BorderLayout.WEST);
        p.add(btns, BorderLayout.EAST);
        return p;
    }

    private JPanel buildStats() {
        JPanel row = new JPanel(new GridLayout(1, 3, 16, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));
        row.add(statCard("Tổng loại phòng", service.getAllLoaiPhong().size() + " loại", UIConstants.PRIMARY));
        row.add(statCard("Đang hoạt động", service.countLoaiPhongActive() + " loại", UIConstants.SUCCESS));
        row.add(statCard("Ngừng sử dụng", service.countLoaiPhongNgung() + " loại", UIConstants.WARNING));
        return row;
    }

    private JPanel buildFilterBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        txtSearch = new ModernTextField("Tìm theo tên loại phòng...");
        txtSearch.setPreferredSize(new Dimension(300, 40));

        cboTrangThai = new ModernComboBox<>();
        cboTrangThai.setPreferredSize(new Dimension(200, 40));
        cboTrangThai.addItem("Tất cả trạng thái");
        cboTrangThai.addItem("Hoạt động");
        cboTrangThai.addItem("Ngừng");

        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { runFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { runFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { runFilter(); }
        });
        cboTrangThai.addActionListener(e -> runFilter());

        p.add(txtSearch);
        p.add(cboTrangThai);
        return p;
    }

    private RoundedPanel statCard(String label, String val, Color c) {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 8, 14));

        JLabel l1 = new JLabel(label);
        l1.setFont(UIConstants.FONT_SMALL);
        l1.setForeground(UIConstants.TEXT_SECONDARY);
        JLabel l2 = new JLabel(val);
        l2.setFont(new Font("Segoe UI", Font.BOLD, 18));

        card.add(l1, BorderLayout.NORTH);
        card.add(l2, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildTableArea() {
        String[] cols = { "_ma", "Tên loại phòng", "Danh mục", "Sức chứa", "Giá từ (đ)", "Giá cao (đ)", "Tiện nghi",
                "Trạng thái" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = buildStyledTable(tableModel);
        TableColumn colMa = table.getColumn("_ma");
        table.removeColumn(colMa);

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

        pagination = new PaginationPanel();
        pagination.setPageChangeListener(page -> displayPage());

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER));
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
        JMenuItem miEdit = new JMenuItem("Xem / Sửa");
        JMenuItem miAdd = new JMenuItem("Thêm mới");
        JMenuItem miDel = new JMenuItem("Xóa loại phòng");
        miEdit.setFont(UIConstants.FONT_BODY);
        miAdd.setFont(UIConstants.FONT_BODY);
        miDel.setFont(UIConstants.FONT_BODY);
        miDel.setForeground(UIConstants.DANGER);
        miEdit.addActionListener(e -> editSelected());
        miAdd.addActionListener(e -> showDialog(null, false));
        miDel.addActionListener(e -> deleteSelected());
        menu.add(miEdit);
        menu.add(miAdd);
        menu.addSeparator();
        menu.add(miDel);
        return menu;
    }

    private JTable buildStyledTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setFont(UIConstants.FONT_BODY);
        t.setRowHeight(40);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setBackground(Color.WHITE);
        t.setSelectionBackground(UIConstants.PRIMARY_LIGHT);
        
        // Căn lề cho tiêu đề
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object v, boolean s, boolean f, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, v, s, f, r, c);
                lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                lbl.setBackground(UIConstants.BG_TABLE_HEADER);
                lbl.setForeground(UIConstants.TEXT_SECONDARY);
                lbl.setHorizontalAlignment(JLabel.CENTER);
                lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER));
                return lbl;
            }
        };
        t.getTableHeader().setDefaultRenderer(headerRenderer);

        // Thiết lập chiều rộng cột
        int[] widths = { 240, 100, 100, 110, 110, 180, 120 };
        // Chờ bảng được gắn vào scrollpane để thiết lập preferred width
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < widths.length; i++) {
                if (i < t.getColumnModel().getColumnCount()) {
                    t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
                }
            }
        });
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean foc, int row,
                    int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                lbl.setBackground(sel ? UIConstants.PRIMARY_LIGHT : (row % 2 == 0 ? Color.WHITE : new Color(0xFAFAFA)));
                lbl.setFont(UIConstants.FONT_BODY);
                lbl.setForeground(UIConstants.TEXT_PRIMARY);

                // Căn lề
                if (col == 1 || col == 2 || col == 6) {
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                } else if (col == 3 || col == 4) {
                    lbl.setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    lbl.setHorizontalAlignment(SwingConstants.LEFT);
                }

                String v = val != null ? val.toString() : "";
                if (col == 6) { // Trạng thái
                    lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                    if (v.equals("Hoạt động")) {
                        lbl.setForeground(UIConstants.SUCCESS);
                    } else if (v.equals("Ngừng")) {
                        lbl.setForeground(UIConstants.DANGER);
                    }
                }
                return lbl;
            }
        });
        return t;
    }

    private JLabel buildFooter(String hint) {
        JLabel lbl = new JLabel("  " + hint);
        lbl.setFont(UIConstants.FONT_SMALL);
        lbl.setForeground(UIConstants.TEXT_MUTED);
        lbl.setBackground(Color.WHITE);
        lbl.setOpaque(true);
        lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(6, 0, 6, 0)));
        return lbl;
    }

    private void loadData() {
        tatCaLoaiPhong = service.getAllLoaiPhong();
        runFilter();
    }

    private void runFilter() {
        if (tatCaLoaiPhong == null) return;

        String kw = txtSearch.getText().trim().toLowerCase();
        int idxStatus = cboTrangThai.getSelectedIndex();

        filteredList = tatCaLoaiPhong.stream()
                .filter(lp -> {
                    boolean matchKw = kw.isEmpty() || lp.getTenLoai().toLowerCase().contains(kw);
                    boolean matchStatus = true;
                    if (idxStatus == 1) matchStatus = "Hoạt động".equals(lp.getTrangThai());
                    if (idxStatus == 2) matchStatus = "Ngừng".equals(lp.getTrangThai());
                    return matchKw && matchStatus;
                })
                .collect(java.util.stream.Collectors.toList());

        if (pagination != null) pagination.setCurrentPage(1);
        displayPage();
    }

    private void displayPage() {
        tableModel.setRowCount(0);
        if (filteredList == null) return;

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
            LoaiPhong lp = filteredList.get(i);
            tableModel.addRow(new Object[] {
                    lp.getMaLoai(), lp.getTenLoai(), lp.getDanhMuc(),
                    lp.getSucChua() + " người",
                    String.format("%,.0f đ", (double) lp.getGiaThapNhat()),
                    String.format("%,.0f đ", (double) lp.getGiaCaoNhat()),
                    lp.getTiNghi(),
                    lp.getTrangThai()
            });
        }

        if (pagination != null) {
            pagination.update(filteredList.size(), pageSize, currentPage);
        }
    }

    private LoaiPhong getSelected() {
        int row = table.getSelectedRow();
        if (row < 0)
            return null;
        return service.getLoaiPhongById((String) tableModel.getValueAt(row, 0));
    }

    private void showDialog(LoaiPhong lp, boolean isDuplicate) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        LoaiPhongDialog dlg = new LoaiPhongDialog(owner, lp, isDuplicate);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            NotificationManager.showSuccess("Thành công", 
                (isDuplicate ? "Đã nhân bản loại phòng" : (lp == null ? "Đã thêm loại phòng mới" : "Đã cập nhật loại phòng")));
            refresh();
        }
    }

    private void editSelected() {
        LoaiPhong lp = getSelected();
        if (lp == null) {
            JOptionPane.showMessageDialog(this, "Chọn một loại phòng để sửa!");
            return;
        }
        showDialog(lp, false);
    }


    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Chọn một loại phòng để xóa!");
            return;
        }
        String ma = (String) tableModel.getValueAt(row, 0);
        String ten = (String) tableModel.getValueAt(row, 1);
        int ok = JOptionPane.showConfirmDialog(this,
                "Xóa loại phòng \"" + ten + "\"?\nCác phòng thuộc loại này sẽ bị ảnh hưởng!",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            String err = service.xoaLoaiPhong(ma);
            if (err == null) {
                NotificationManager.showSuccess("Thành công", "Đã xóa loại phòng " + ten);
                refresh();
            } else
                JOptionPane.showMessageDialog(this, "Lỗi: " + err, "Không thể xóa", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refresh() {
        loadData();
    }
}
