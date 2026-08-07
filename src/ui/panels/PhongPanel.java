package ui.panels;

import ui.components.RoundedComponents.*;
import ui.components.NotificationManager;
import ui.components.UIConstants;
import ui.dialogs.PhongDialog;
import entity.Phong;
import entity.enums.TrangThaiPhong;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.table.*;
import service.PhongService;
import service.BangGiaService;
import ui.components.PaginationPanel;

public class PhongPanel extends JPanel implements ResettableFilter {

    @Override
    public void resetFilters() {
        if (txtSearch != null) txtSearch.setText("");
        if (cboTrangThai != null) cboTrangThai.setSelectedIndex(0);
        loadData();
    }

    private final PhongService phongService = new PhongService();
    private final BangGiaService bangGiaService = new BangGiaService();

    private ModernTextField txtSearch;
    private ModernComboBox<String> cboTrangThai;
    private DefaultTableModel tableModel;
    private JTable table;
    private PaginationPanel pagination;
    private List<Phong> tatCaPhong;
    private List<Phong> filteredList;

    public PhongPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        main.add(buildHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(buildStats(), BorderLayout.NORTH);
        center.add(buildTableArea(), BorderLayout.CENTER);

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
        JLabel t = new JLabel("Quản lý phòng");
        t.setFont(UIConstants.FONT_TITLE);
        JLabel s = new JLabel("Quản lý tình trạng và thông tin từng phòng trong khách sạn");
        s.setFont(UIConstants.FONT_BODY);
        s.setForeground(UIConstants.TEXT_SECONDARY);
        left.add(t);
        left.add(Box.createVerticalStrut(2));
        left.add(s);

        RoundedButton btnAdd = new RoundedButton("+ Thêm phòng mới", UIConstants.PRIMARY, Color.WHITE);
        btnAdd.addActionListener(e -> showDialog(null));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(btnAdd);

        p.add(left, BorderLayout.WEST);
        p.add(btns, BorderLayout.EAST);
        return p;
    }

    private JPanel buildStats() {
        Map<String, Integer> tt = phongService.getThongKeTrangThai();
        JPanel row = new JPanel(new GridLayout(1, 5, 12, 0));
        row.setOpaque(false);
        row.add(statCard("Có sẵn", tt.getOrDefault("Có sẵn", 0) + " phòng", UIConstants.SUCCESS));
        row.add(statCard("Đang thuê", tt.getOrDefault("Đang thuê", 0) + " phòng", UIConstants.PRIMARY));
        row.add(statCard("Đã đặt", tt.getOrDefault("Đã đặt", 0) + " phòng", UIConstants.WARNING));
        row.add(statCard("Vệ sinh", tt.getOrDefault("Vệ sinh", 0) + " phòng", new Color(0x06B6D4)));
        row.add(statCard("Bảo trì", tt.getOrDefault("Bảo trì", 0) + " phòng", UIConstants.DANGER));
        return row;
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
        // Filter bar
        txtSearch = new ModernTextField("Tìm theo số phòng, loại phòng...");
        txtSearch.setPreferredSize(new Dimension(300, 40));

        cboTrangThai = new ModernComboBox<>();
        cboTrangThai.addItem("Tất cả trạng thái");
        cboTrangThai.addItem("Có sẵn");
        cboTrangThai.addItem("Đang thuê");
        cboTrangThai.addItem("Đã đặt");
        cboTrangThai.addItem("Vệ sinh");
        cboTrangThai.addItem("Bảo trì");
        cboTrangThai.setPreferredSize(new Dimension(200, 40));

        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                loadData();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                loadData();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                loadData();
            }
        });
        cboTrangThai.addActionListener(e -> loadData());

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        filterBar.setOpaque(false);
        filterBar.add(txtSearch);
        filterBar.add(cboTrangThai);

        // Table
        String[] cols = { "_sp", "Số phòng", "Loại phòng", "View", "Tầng", "Sức chứa", "Giá/đêm", "Khách hiện tại",
                "Trạng thái" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = buildStyledTable(tableModel);
        TableColumn colSp = table.getColumn("_sp");
        table.removeColumn(colSp);

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

        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);
        wrap.add(filterBar, BorderLayout.NORTH);
        wrap.add(sp, BorderLayout.CENTER);
        wrap.add(footer, BorderLayout.SOUTH);

        loadData();
        return wrap;
    }

    private JPopupMenu buildContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem miEdit = new JMenuItem("Xem / Sửa");
        JMenuItem miAdd = new JMenuItem("Thêm phòng mới");
        JMenuItem miDel = new JMenuItem("Xóa phòng");
        miEdit.setFont(UIConstants.FONT_BODY);
        miAdd.setFont(UIConstants.FONT_BODY);
        miDel.setFont(UIConstants.FONT_BODY);
        miDel.setForeground(UIConstants.DANGER);
        miEdit.addActionListener(e -> editSelected());
        miAdd.addActionListener(e -> showDialog(null));
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
        int[] widths = { 100, 150, 100, 80, 100, 150, 200, 150 };
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
                if (col == 0 || col == 3 || col == 4 || col == 7) {
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                } else if (col == 5) {
                    lbl.setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    lbl.setHorizontalAlignment(SwingConstants.LEFT);
                }

                String v = val != null ? val.toString() : "";
                if (col == 7) { // Trạng thái
                    lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    if (v.contains("Có sẵn")) {
                        lbl.setForeground(UIConstants.SUCCESS);
                    } else if (v.contains("Đang thuê")) {
                        lbl.setForeground(UIConstants.PRIMARY);
                    } else if (v.contains("Đã đặt")) {
                        lbl.setForeground(UIConstants.WARNING);
                    } else if (v.contains("Bảo trì")) {
                        lbl.setForeground(UIConstants.DANGER);
                    } else if (v.contains("Vệ sinh")) {
                        lbl.setForeground(new Color(0x06B6D4));
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
        String kw = txtSearch != null ? txtSearch.getText().trim().toLowerCase() : "";
        String filterTT = cboTrangThai != null && cboTrangThai.getSelectedIndex() > 0
                ? cboTrangThai.getSelectedItem().toString()
                : null;

        tatCaPhong = phongService.getAllPhong();
        filteredList = tatCaPhong.stream().filter(p -> {
            boolean matchKw = kw.isEmpty() || p.getSoPhong().toLowerCase().contains(kw)
                    || p.getTenLoaiPhong().toLowerCase().contains(kw);

            boolean matchStatus = true;
            if (filterTT != null) {
                String ttLabel = UIConstants.getTrangThaiPhongLabel(p.getTrangThai());
                matchStatus = ttLabel.contains(filterTT);
            }

            return matchKw && matchStatus;
        }).collect(Collectors.toList());

        if (pagination != null)
            pagination.setCurrentPage(1);
        displayPage();
    }

    private void displayPage() {
        tableModel.setRowCount(0);
        if (filteredList == null)
            return;

        int pageSize = 12;
        int currentPage = pagination != null ? pagination.getCurrentPage() : 1;

        int totalPages = (int) Math.ceil((double) filteredList.size() / pageSize);
        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
            if (pagination != null)
                pagination.setCurrentPage(currentPage);
        }

        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, filteredList.size());

        for (int i = start; i < end; i++) {
            Phong p = filteredList.get(i);
            String ttLabel = UIConstants.getTrangThaiPhongLabel(p.getTrangThai());

            // PRIORITY: Active Price Table > Base Price
            double giaHienHanh = bangGiaService
                    .layGiaHienHanh(p.getLoaiPhong() != null ? p.getLoaiPhong().getMaLoaiPhong() : "");

            tableModel.addRow(new Object[] {
                    p.getSoPhong(), p.getSoPhong(), p.getTenLoaiPhong(), p.getView(),
                    "Tầng " + p.getTang(),
                    p.getSucChua() + " người",
                    String.format("%,.0fđ", giaHienHanh),
                    (p.getTrangThai() == entity.enums.TrangThaiPhong.OCCUPIED && p.getTenKhachHienTai() != null)
                            ? p.getTenKhachHienTai()
                            : "—",
                    ttLabel
            });
        }

        if (pagination != null) {
            pagination.update(filteredList.size(), pageSize, currentPage);
        }
    }

    private Phong getSelected() {
        int row = table.getSelectedRow();
        if (row < 0)
            return null;
        return phongService.getPhongById((String) tableModel.getValueAt(row, 0));
    }

    private void showDialog(Phong p) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        PhongDialog dlg = new PhongDialog(owner, p);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            NotificationManager.showSuccess("Thành công",
                    (p == null ? "Đã thêm phòng mới" : "Đã cập nhật thông tin phòng"));
            refresh();
        }
    }

    private void editSelected() {
        Phong p = getSelected();
        if (p == null) {
            JOptionPane.showMessageDialog(this, "Chọn một phòng để sửa!");
            return;
        }
        showDialog(p);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Chọn một phòng để xóa!");
            return;
        }
        String sp = (String) tableModel.getValueAt(row, 0);
        Phong p = phongService.getPhongById(sp);
        if (p == null)
            return;
        TrangThaiPhong tt = p.getTrangThai();
        if (tt == TrangThaiPhong.OCCUPIED || tt == TrangThaiPhong.CLEANING) {
            JOptionPane.showMessageDialog(this,
                    "Không thể xóa phòng đang có khách hoặc đang vệ sinh!", "Không thể xóa",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                "Xóa phòng " + sp + "?\nHành động này không thể hoàn tác!",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            String err = phongService.xoaPhong(sp);
            if (err == null) {
                NotificationManager.showSuccess("Thành công", "Đã xóa phòng " + p.getSoPhong());
                refresh();
            } else
                JOptionPane.showMessageDialog(this, "Lỗi: " + err, "Không thể xóa", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refresh() {
        loadData();
    }
}
