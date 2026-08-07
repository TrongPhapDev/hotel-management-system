package ui.panels;

import entity.DichVu;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import service.DichVuService;
import ui.components.NotificationManager;
import ui.components.PaginationPanel;
import ui.components.RoundedComponents.*;
import ui.components.RoundedComponents.ModernComboBox;
import ui.components.RoundedComponents.ModernTextField;
import ui.components.RoundedComponents.RoundedButton;
import ui.components.RoundedComponents.RoundedPanel;
import ui.components.UIConstants;
import ui.dialogs.DichVuDialog;

public class DichVuPanel extends JPanel implements ResettableFilter {

    @Override
    public void resetFilters() {
        if (txtSearch != null) txtSearch.setText("");
        if (cboLoai != null) cboLoai.setSelectedIndex(0);
        if (cboTT != null) cboTT.setSelectedIndex(0);
        loadData();
    }

    private final DichVuService service = new DichVuService();

    private ModernTextField txtSearch;
    private ModernComboBox<String> cboLoai, cboTT;
    private DefaultTableModel tableModel;
    private JTable table;
    private PaginationPanel pagination;
    private List<DichVu> fullList;

    public DichVuPanel() {
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
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel t = new JLabel("Quản lý dịch vụ");
        t.setFont(new Font("Segoe UI", Font.BOLD, 28));
        t.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel s = new JLabel("Danh sách và cấu hình các dịch vụ bổ sung của khách sạn");
        s.setFont(UIConstants.FONT_BODY);
        s.setForeground(UIConstants.TEXT_SECONDARY);
        left.add(t);
        left.add(Box.createVerticalStrut(4));
        left.add(s);

        RoundedButton btnAdd = new RoundedButton("+ Thêm dịch vụ", UIConstants.PRIMARY, Color.WHITE);
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnAdd.setPreferredSize(new Dimension(160, 42));
        btnAdd.addActionListener(e -> showDialog(null));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btns.setOpaque(false);
        btns.add(btnAdd);

        p.add(left, BorderLayout.WEST);
        p.add(btns, BorderLayout.EAST);
        return p;
    }

    private JPanel buildStats() {
        int all = service.countAll();
        int active = service.countActive();
        int suspended = service.countSuspended();
        double avg = service.getGiaTrungBinh();

        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.add(statCard("Tổng dịch vụ", all + " dịch vụ", UIConstants.PRIMARY));
        row.add(statCard("Đang cung cấp", active + " dịch vụ", UIConstants.SUCCESS));
        row.add(statCard("Tạm ngừng", suspended + " dịch vụ", UIConstants.WARNING));
        row.add(statCard("Giá trung bình", String.format("%,.0fđ", avg), UIConstants.INFO));
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
        // Filter bar (Modern Card Style)
        RoundedPanel filterCard = new RoundedPanel(16);
        filterCard.setBackground(Color.WHITE);
        filterCard.setShadow(true);
        filterCard.setLayout(new GridBagLayout());
        filterCard.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.VERTICAL;
        g.insets = new Insets(0, 0, 0, 12);
        g.weighty = 0;

        txtSearch = new ModernTextField("Tìm tên dịch vụ...");
        txtSearch.setPreferredSize(new Dimension(280, 40));
        g.gridx = 0; g.weightx = 0; g.fill = GridBagConstraints.VERTICAL;
        g.anchor = GridBagConstraints.WEST;
        filterCard.add(txtSearch, g);

        cboLoai = new ModernComboBox<>(
                new String[] { "Tất cả loại", "Ăn uống", "Spa & Làm đẹp", "Vận chuyển", "Dịch vụ phòng", "Khác" });
        cboLoai.setPreferredSize(new Dimension(200, 40));
        g.gridx = 1; g.weightx = 0; g.fill = GridBagConstraints.VERTICAL;
        filterCard.add(cboLoai, g);

        cboTT = new ModernComboBox<>(new String[] { "Tất cả trạng thái", "Hoạt động", "Tạm ngừng" });
        cboTT.setPreferredSize(new Dimension(180, 40));
        g.gridx = 2; g.weightx = 0; g.insets = new Insets(0, 0, 0, 0);
        filterCard.add(cboTT, g);

        // Spacer để đẩy các nút về bên trái
        g.gridx = 3; g.weightx = 1.0; g.fill = GridBagConstraints.HORIZONTAL;
        filterCard.add(new JPanel() {{ setOpaque(false); }}, g);

        // Table
        String[] cols = { "_ma", "#", "Tên dịch vụ", "Loại", "Giá", "Đơn vị", "SL tối thiểu", "Trạng thái" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = buildStyledTable(tableModel);
        TableColumn colMa = table.getColumn("_ma");
        table.removeColumn(colMa);

        // Thiết lập chiều rộng cột
        int[] widths = { 50, 200, 150, 120, 100, 110, 120 };
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

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

        // Listeners
        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                runFilter();
            }

            public void removeUpdate(DocumentEvent e) {
                runFilter();
            }

            public void changedUpdate(DocumentEvent e) {
            }
        };
        txtSearch.getDocument().addDocumentListener(dl);
        cboLoai.addActionListener(e -> runFilter());
        cboTT.addActionListener(e -> runFilter());

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
        wrap.add(filterCard, BorderLayout.NORTH);
        wrap.add(sp, BorderLayout.CENTER);
        wrap.add(footer, BorderLayout.SOUTH);

        loadData();
        return wrap;
    }

    private JPopupMenu buildContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem miEdit = new JMenuItem("Xem / Sửa");
        JMenuItem miAdd = new JMenuItem("Thêm mới");
        JMenuItem miDel = new JMenuItem("Xóa dịch vụ");
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
                if (col == 0 || col == 2 || col == 4 || col == 5 || col == 6) {
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                } else if (col == 3) {
                    lbl.setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    lbl.setHorizontalAlignment(SwingConstants.LEFT);
                }

                String v = val != null ? val.toString() : "";
                if (col == 6) { // Trạng thái
                    lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    if (v.equals("Hoạt động")) {
                        lbl.setForeground(UIConstants.SUCCESS);
                    } else if (v.equals("Tạm ngừng")) {
                        lbl.setForeground(UIConstants.WARNING);
                    }
                }
                return lbl;
            }
        });
        return t;
    }

    private void loadData() {
        String kw = txtSearch != null ? txtSearch.getText() : null;
        String loai = cboLoai != null ? cboLoai.getSelectedItem().toString() : null;
        String tt = cboTT != null ? cboTT.getSelectedItem().toString() : null;

        String l = (loai == null || loai.equals("Tất cả loại")) ? null : loai;
        String t = (tt == null || tt.equals("Tất cả trạng thái")) ? null : tt; // Fixed: "Tất cả trạng thái"
        fullList = service.search(
                (kw == null || kw.isBlank()) ? null : kw, l, t);

        if (pagination != null) pagination.setCurrentPage(1);
        displayPage();
    }

    private void displayPage() {
        tableModel.setRowCount(0);
        if (fullList == null) return;

        int pageSize = 12;
        int currentPage = pagination != null ? pagination.getCurrentPage() : 1;

        int totalPages = (int) Math.ceil((double) fullList.size() / pageSize);
        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
            if (pagination != null) pagination.setCurrentPage(currentPage);
        }

        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, fullList.size());

        for (int i = start; i < end; i++) {
            DichVu dv = fullList.get(i);
            tableModel.addRow(new Object[] {
                    dv.getMaDV(), i + 1, dv.getTenDV(), dv.getLoai(),
                    String.format("%,.0fđ", dv.getGia()),
                    dv.getDonVi(), dv.getSoLuongMin(), dv.getTrangThai()
            });
        }

        if (pagination != null) {
            pagination.update(fullList.size(), pageSize, currentPage);
        }
    }

    private void runFilter() {
        loadData();
    }

    private DichVu getSelected() {
        int row = table.getSelectedRow();
        if (row < 0)
            return null;
        return service.getById((String) tableModel.getValueAt(row, 0));
    }

    private void showDialog(DichVu dv) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        DichVuDialog dlg = new DichVuDialog(owner, dv);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            NotificationManager.showSuccess("Thành công", 
                (dv == null ? "Đã thêm dịch vụ mới" : "Đã cập nhật dịch vụ"));
            refresh();
        }
    }

    private void editSelected() {
        DichVu dv = getSelected();
        if (dv == null) {
            JOptionPane.showMessageDialog(this, "Chọn một dịch vụ để sửa!");
            return;
        }
        showDialog(dv);
    }

    private void deleteSelected() {
        DichVu dv = getSelected();
        if (dv == null) {
            JOptionPane.showMessageDialog(this, "Chọn một dịch vụ để xóa!");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                "Xóa dịch vụ \"" + dv.getTenDV() + "\"?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            String err = service.xoa(dv.getMaDV());
            if (err == null) {
                NotificationManager.showSuccess("Thành công", "Đã xóa dịch vụ " + dv.getTenDichVu());
                refresh();
            } else
                JOptionPane.showMessageDialog(this, "Lỗi: " + err, "Không thể xóa", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refresh() {
        loadData();
    }
}
