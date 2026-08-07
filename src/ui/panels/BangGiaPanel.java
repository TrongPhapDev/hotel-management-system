package ui.panels;

import ui.components.RoundedComponents.*;
import ui.components.NotificationManager;
import ui.components.UIConstants;
import ui.dialogs.BangGiaDialog;
import entity.BangGia;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import service.BangGiaService;
import ui.components.PaginationPanel;
import java.util.ArrayList;

public class BangGiaPanel extends JPanel implements ResettableFilter {

    @Override
    public void resetFilters() {
        if (txtSearch != null) txtSearch.setText("");
        if (cboTrangThai != null) cboTrangThai.setSelectedIndex(0);
        runFilter();
    }

    private final BangGiaService service = new BangGiaService();

    private ModernTextField txtSearch;
    private ModernComboBox<String> cboTrangThai;
    private DefaultTableModel tableModel;
    private JTable table;
    private PaginationPanel pagination;
    private List<BangGia> tatCaBangGia;
    private List<BangGia> filteredList;

    public BangGiaPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        main.add(buildHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 20));
        center.setOpaque(false);
        center.add(buildStats(), BorderLayout.NORTH);

        JPanel tableWrapper = new JPanel(new BorderLayout(0, 14));
        tableWrapper.setOpaque(false);
        tableWrapper.add(buildFilterBar(), BorderLayout.NORTH);
        tableWrapper.add(buildTableArea(), BorderLayout.CENTER);

        center.add(tableWrapper, BorderLayout.CENTER);
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
        JLabel t = new JLabel("Quản lý cách tính tiền");
        t.setFont(UIConstants.FONT_TITLE);
        JLabel s = new JLabel("Cấu hình bảng giá phòng và chính sách giá theo mùa / sự kiện");
        s.setFont(UIConstants.FONT_BODY);
        s.setForeground(UIConstants.TEXT_SECONDARY);
        left.add(t);
        left.add(Box.createVerticalStrut(2));
        left.add(s);

        RoundedButton btnAdd = new RoundedButton("+ Thêm bảng giá", UIConstants.PRIMARY, Color.WHITE);
        btnAdd.addActionListener(e -> showDialog(null));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(btnAdd);

        p.add(left, BorderLayout.WEST);
        p.add(btns, BorderLayout.EAST);
        return p;
    }

    private JPanel buildStats() {
        List<BangGia> all = service.getAll();
        int total = all.size();
        long active = all.stream().filter(BangGia::isTrangThai).count();
        long expired = all.stream().filter(bg -> bg.getNgayKetThuc() != null 
                        && bg.getNgayKetThuc().isBefore(java.time.LocalDateTime.now())).count();

        JPanel row = new JPanel(new GridLayout(1, 3, 16, 0));
        row.setOpaque(false);
        row.add(statCard("Tổng bảng giá", total + " bản ghi", UIConstants.PRIMARY));
        row.add(statCard("Đang kích hoạt", active + " bảng giá", UIConstants.SUCCESS));
        row.add(statCard("Đã hết hạn", expired + " bảng giá", UIConstants.WARNING));
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

    private JPanel buildFilterBar() {
        txtSearch = new ModernTextField("Tìm theo tên bảng giá...");
        txtSearch.setPreferredSize(new Dimension(300, 40));

        cboTrangThai = new ModernComboBox<>();
        cboTrangThai.addItem("Tất cả trạng thái");
        cboTrangThai.addItem("Đang kích hoạt");
        cboTrangThai.addItem("Ngừng");
        cboTrangThai.setPreferredSize(new Dimension(200, 40));

        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { runFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { runFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { runFilter(); }
        });
        cboTrangThai.addActionListener(e -> runFilter());

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        p.setOpaque(false);
        p.add(txtSearch);
        p.add(cboTrangThai);
        return p;
    }

    private JPanel buildTableArea() {
        String[] cols = { "_ma", "#", "Tên bảng giá", "Loại", "Đối tượng", "Ngày bắt đầu", "Ngày kết thúc", "Ưu tiên", "Trạng thái" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = buildStyledTable(tableModel);
        TableColumn colMa = table.getColumn("_ma");
        table.removeColumn(colMa);
        TableColumn colStt = table.getColumn("#");
        colStt.setMinWidth(40);
        colStt.setMaxWidth(80);
        colStt.setPreferredWidth(60);

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

        // Info hint
        JPanel hintPanel = new JPanel(new BorderLayout());
        hintPanel.setBackground(new Color(0xEFF6FF));
        hintPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xBFDBFE)),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        JLabel hintLbl = new JLabel(
                "<html>💡 <b>Bảng giá kích hoạt</b> sẽ ghi đè giá cơ sở của từng Loại phòng trong khoảng thời gian xác định. "
                        + "Chỉ nên có <b>một bảng giá kích hoạt</b> tại một thời điểm.</html>");
        hintLbl.setFont(UIConstants.FONT_SMALL);
        hintLbl.setForeground(UIConstants.PRIMARY);
        hintPanel.add(hintLbl, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER));
        footer.add(pagination, BorderLayout.CENTER);

        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);
        wrap.add(sp, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(hintPanel, BorderLayout.NORTH);
        south.add(footer, BorderLayout.SOUTH);
        wrap.add(south, BorderLayout.SOUTH);

        loadData();
        return wrap;
    }

    private JPopupMenu buildContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem miEdit = new JMenuItem("Xem / Sửa");
        JMenuItem miAdd = new JMenuItem("Thêm bảng giá mới");
        JMenuItem miDel = new JMenuItem("Xóa bảng giá");
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
        t.setRowHeight(44);
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
                if (col == 0 || col == 2 || col == 3 || col == 4 || col == 5 || col == 6 || col == 7) {
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    lbl.setHorizontalAlignment(SwingConstants.LEFT);
                }

                // Trạng thái boolean hiển thị
                if (val instanceof Boolean) {
                    Boolean active = (Boolean) val;
                    lbl.setText(active ? "Đang kích hoạt" : "Ngừng");
                    lbl.setForeground(active ? UIConstants.SUCCESS : UIConstants.TEXT_MUTED);
                    lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
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

        tatCaBangGia = service.getAll();
        filteredList = tatCaBangGia.stream().filter(bg -> {
            boolean matchKw = kw.isEmpty() || bg.getTenBangGia().toLowerCase().contains(kw);
            
            boolean matchStatus = true;
            if (filterTT != null) {
                boolean active = "Đang kích hoạt".equals(filterTT);
                matchStatus = (bg.isTrangThai() == active);
            }
            
            return matchKw && matchStatus;
        }).collect(java.util.stream.Collectors.toList());

        if (pagination != null) pagination.setCurrentPage(1);
        displayPage();
    }

    private void runFilter() {
        loadData();
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

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        for (int i = start; i < end; i++) {
            BangGia bg = filteredList.get(i);
            tableModel.addRow(new Object[] {
                    bg.getMaBangGia(), i + 1, bg.getTenBangGia(),
                    bg.getLoaiBangGiaLabel(), bg.getDoiTuongApDungLabel(),
                    bg.getNgayBatDau() != null ? sdf.format(java.sql.Timestamp.valueOf(bg.getNgayBatDau())) : "",
                    bg.getNgayKetThuc() != null ? sdf.format(java.sql.Timestamp.valueOf(bg.getNgayKetThuc())) : "",
                    bg.getMucUuTien(),
                    bg.isTrangThai()
            });
        }

        if (pagination != null) {
            pagination.update(filteredList.size(), pageSize, currentPage);
        }
    }

    private BangGia getSelected() {
        int row = table.getSelectedRow();
        if (row < 0)
            return null;
        return service.getById((String) tableModel.getValueAt(row, 0));
    }

    private void showDialog(BangGia bg) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        BangGiaDialog dlg = new BangGiaDialog(owner, bg);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            NotificationManager.showSuccess("Thành công", 
                (bg == null ? "Đã thêm bảng giá mới" : "Đã cập nhật bảng giá"));
            refresh();
        }
    }

    private void editSelected() {
        BangGia bg = getSelected();
        if (bg == null) {
            JOptionPane.showMessageDialog(this, "Chọn một bảng giá để sửa!");
            return;
        }
        showDialog(bg);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Chọn một bảng giá để xóa!");
            return;
        }
        String ma = (String) tableModel.getValueAt(row, 0);
        String ten = (String) tableModel.getValueAt(row, 2);
        int ok = JOptionPane.showConfirmDialog(this,
                "Xóa bảng giá \"" + ten + "\"?\nHành động này không thể hoàn tác!",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            String err = service.xoa(ma);
            if (err == null) {
                NotificationManager.showSuccess("Thành công", "Đã xóa bảng giá " + ten);
                refresh();
            } else
                JOptionPane.showMessageDialog(this, "Lỗi: " + err, "Không thể xóa", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refresh() {
        loadData();
    }
}
