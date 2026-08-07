package ui.panels;

import dao.KhuyenMaiDAO;
import entity.KhuyenMai;
import entity.enums.LoaiGiam;
import ui.components.RoundedComponents.ModernTextField;
import ui.components.RoundedComponents.ModernComboBox;
import ui.components.RoundedComponents.RoundedButton;
import ui.components.RoundedComponents.RoundedPanel;
import ui.components.RoundedComponents.StatusBadge;
import ui.components.UIConstants;
import ui.components.PaginationPanel;
import ui.dialogs.KhuyenMaiDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class KhuyenMaiPanel extends JPanel implements ResettableFilter {

    @Override
    public void resetFilters() {
        if (txtSearch != null) txtSearch.setText("");
        if (cbStatus != null) cbStatus.setSelectedIndex(0);
        loadData();
    }

    private final KhuyenMaiDAO kmDAO = new KhuyenMaiDAO();
    private JTable table;
    private DefaultTableModel model;
    private ModernTextField txtSearch;
    private ModernComboBox<String> cbStatus;
    private PaginationPanel pagination;
    private List<KhuyenMai> allKhuyenMai;
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public KhuyenMaiPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());
        initComponents();
        loadData();
    }

    private void initComponents() {
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

        String[] cols = { "Mã KM", "Tên chương trình", "Loại", "Giá trị", "Bắt đầu", "Kết thúc", "Tối thiểu", "Sử dụng",
                "Trạng thái" };
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        table = new JTable(model);
        styleTable();

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(230);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(140);
        table.getColumnModel().getColumn(5).setPreferredWidth(140);
        table.getColumnModel().getColumn(7).setPreferredWidth(120);
        table.getColumnModel().getColumn(8).setPreferredWidth(130);

        // Context Menu
        JPopupMenu popup = new JPopupMenu();
        JMenuItem viewItem = new JMenuItem("Xem chi tiết");
        viewItem.addActionListener(e -> viewSelected());

        JMenuItem editItem = new JMenuItem("Chỉnh sửa");
        editItem.setIcon(new ImageIcon("d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/icon/edit.png"));
        editItem.addActionListener(e -> editSelected());

        JMenuItem deleteItem = new JMenuItem("Xóa");
        deleteItem.setIcon(new ImageIcon("d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/icon/delete.png"));
        deleteItem.addActionListener(e -> deleteSelected());

        popup.add(viewItem);
        popup.add(editItem);
        popup.addSeparator();
        popup.add(deleteItem);

        // Mouse Listener for Double-click and Right-click selection
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && row < table.getRowCount()) {
                    table.setRowSelectionInterval(row, row);
                }
                if (e.isPopupTrigger())
                    showPopup(e);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger())
                    showPopup(e);
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    viewSelected();
                }
            }

            private void showPopup(java.awt.event.MouseEvent e) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);

        // Pagination
        pagination = new PaginationPanel();
        pagination.setPageChangeListener(page -> displayCurrentPage());

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER));
        footer.add(pagination, BorderLayout.CENTER);

        tableWrapper.add(scroll, BorderLayout.CENTER);
        tableWrapper.add(footer, BorderLayout.SOUTH);

        center.add(tableWrapper, BorderLayout.CENTER);
        main.add(center, BorderLayout.CENTER);

        add(main, BorderLayout.CENTER);
    }

    private JPanel buildStats() {
        List<KhuyenMai> all = kmDAO.getAll();
        int total = all.size();
        
        long active = all.stream().filter(km -> "HOẠT ĐỘNG".equals(determineStatus(km))).count();
        long expired = all.stream().filter(km -> "HẾT HẠN".equals(determineStatus(km))).count();

        JPanel row = new JPanel(new GridLayout(1, 3, 16, 0));
        row.setOpaque(false);
        row.add(statCard("Tổng số chương trình", total + " mã KM", UIConstants.PRIMARY));
        row.add(statCard("Đang hoạt động", active + " mã đang chạy", UIConstants.SUCCESS));
        row.add(statCard("Hết hiệu lực", expired + " mã đã dừng", UIConstants.WARNING));
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

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("Quản lý Khuyến mãi & Voucher");
        lblTitle.setFont(UIConstants.FONT_TITLE);
        lblTitle.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel lblSubTitle = new JLabel("Quản lý các chương trình ưu đãi và mã giảm giá toàn hệ thống");
        lblSubTitle.setFont(UIConstants.FONT_BODY);
        lblSubTitle.setForeground(UIConstants.TEXT_SECONDARY);

        titlePanel.add(lblTitle);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(lblSubTitle);
        p.add(titlePanel, BorderLayout.WEST);

        RoundedButton btnAdd = new RoundedButton("+ Thêm khuyến mãi", UIConstants.PRIMARY, Color.WHITE);
        btnAdd.setPreferredSize(new Dimension(180, 42));
        btnAdd.addActionListener(e -> {
            new KhuyenMaiDialog((Window) SwingUtilities.getWindowAncestor(this), null, false, false).setVisible(true);
            loadData();
        });
        p.add(btnAdd, BorderLayout.EAST);

        return p;
    }

    private JPanel buildFilterBar() {
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        filterPanel.setOpaque(false);

        txtSearch = new ModernTextField("Tìm mã KM, tên chương trình...");
        txtSearch.setPreferredSize(new Dimension(300, 42));
        txtSearch.addActionListener(e -> loadData());

        cbStatus = new ModernComboBox<>(new String[] { "Tất cả trạng thái", "Hoạt động", "Tạm dừng", "Hết hạn" });
        cbStatus.setPreferredSize(new Dimension(180, 42));
        cbStatus.addActionListener(e -> loadData());

        filterPanel.add(txtSearch);
        filterPanel.add(cbStatus);
        return filterPanel;
    }

    private void styleTable() {
        table.setRowHeight(40);
        table.setFont(UIConstants.FONT_BODY);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(UIConstants.PRIMARY_LIGHT);
        table.setSelectionForeground(UIConstants.TEXT_PRIMARY);

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
        table.getTableHeader().setDefaultRenderer(headerRenderer);

        // Custom Zebra Renderer with Alignment
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean foc, int row,
                    int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                lbl.setBackground(sel ? UIConstants.PRIMARY_LIGHT : (row % 2 == 0 ? Color.WHITE : new Color(0xFAFAFA)));
                lbl.setFont(UIConstants.FONT_BODY);
                lbl.setForeground(UIConstants.TEXT_PRIMARY);

                // Căn lề theo loại dữ liệu
                if (col == 6) {
                    lbl.setHorizontalAlignment(SwingConstants.RIGHT);
                } else if (col == 1) {
                    lbl.setHorizontalAlignment(SwingConstants.LEFT);
                } else {
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                }

                return lbl;
            }
        });

        table.getColumnModel().getColumn(8).setCellRenderer(new KhuyenMaiStatusRenderer());
        table.getColumnModel().getColumn(7).setCellRenderer(new UsageRenderer());
        table.getColumnModel().getColumn(2).setCellRenderer(new BadgeRenderer());

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        table.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);
    }

    private void loadData() {
        String keyword = txtSearch != null ? txtSearch.getText().trim() : "";
        String status = cbStatus != null ? (String) cbStatus.getSelectedItem() : "Tất cả trạng thái";
        if ("Tất cả trạng thái".equals(status)) status = "Tất cả";
        allKhuyenMai = kmDAO.search(keyword, status);

        if (pagination != null) {
            pagination.setCurrentPage(1);
        }
        displayCurrentPage();
    }

    private void displayCurrentPage() {
        model.setRowCount(0);
        if (allKhuyenMai == null)
            return;

        int pageSize = 12;
        int currentPage = pagination.getCurrentPage();
        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, allKhuyenMai.size());

        for (int i = start; i < end; i++) {
            KhuyenMai km = allKhuyenMai.get(i);
            Object[] row = {
                    km.getMaKhuyenMai(),
                    km.getTenKhuyenMai(),
                    km.getLoaiGiam() == LoaiGiam.PERCENT ? "PHẦN TRĂM (%)" : "TIỀN MẶT (₫)",
                    km.getLoaiGiam() == LoaiGiam.PERCENT ? km.getGiaTriGiam() + "%"
                            : String.format("%,.0f ₫", km.getGiaTriGiam()),
                    km.getNgayBatDau().format(df),
                    km.getNgayKetThuc().format(df),
                    String.format("%,.0f ₫", km.getDieuKienToiThieu()),
                    km.getDaDung() + "/" + km.getSoLuong(),
                    determineStatus(km)
            };
            model.addRow(row);
        }

        pagination.update(allKhuyenMai.size(), pageSize, currentPage);
    }

    private String determineStatus(KhuyenMai km) {
        if (!km.isTrangThai())
            return "TẠM DỪNG";
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(km.getNgayBatDau()))
            return "SẮP DIỄN RA";
        if (now.isAfter(km.getNgayKetThuc()))
            return "HẾT HẠN";
        if (km.getDaDung() >= km.getSoLuong())
            return "HẾT LƯỢT";
        return "HOẠT ĐỘNG";
    }

    private void viewSelected() {
        int row = table.getSelectedRow();
        if (row < 0)
            return;
        String id = (String) model.getValueAt(row, 0);
        KhuyenMai km = kmDAO.getByVoucherCode(id);
        if (km != null) {
            new KhuyenMaiDialog((Window) SwingUtilities.getWindowAncestor(this), km, false, true).setVisible(true);
            loadData();
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0)
            return;
        String id = (String) model.getValueAt(row, 0);
        KhuyenMai km = kmDAO.getByVoucherCode(id);
        if (km != null) {
            new KhuyenMaiDialog((Window) SwingUtilities.getWindowAncestor(this), km, true, false).setVisible(true);
            loadData();
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0)
            return;
        String id = (String) model.getValueAt(row, 0);
        KhuyenMai km = kmDAO.getByVoucherCode(id);

        if (km != null && km.getDaDung() > 0) {
            JOptionPane.showMessageDialog(this,
                    "Không thể xóa khuyến mãi đã được sử dụng! Hãy chuyển trạng thái sang Tạm dừng.", "Thông báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (JOptionPane.showConfirmDialog(this, "Xác nhận xóa mã " + id + "?", "Cảnh báo",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            if (kmDAO.delete(id)) {
                loadData();
            } else {
                JOptionPane.showMessageDialog(this, "Lỗi khi xóa!");
            }
        }
    }

    // --- Renderers ---

    private static class KhuyenMaiStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            String text = (value != null) ? value.toString() : "";
            Color bg, fg;
            switch (text) {
                case "HOẠT ĐỘNG":
                    bg = new Color(230, 250, 240);
                    fg = new Color(30, 150, 80);
                    break;
                case "HẾT HẠN":
                case "HẾT LƯỢT":
                    bg = new Color(255, 235, 235);
                    fg = new Color(220, 50, 50);
                    break;
                case "SẮP DIỄN RA":
                    bg = new Color(235, 243, 255);
                    fg = new Color(30, 100, 230);
                    break;
                default:
                    bg = new Color(245, 247, 250);
                    fg = UIConstants.TEXT_MUTED;
                    break;
            }
            StatusBadge badge = new StatusBadge(text, bg, fg);
            badge.setHorizontalAlignment(CENTER);
            JPanel w = new JPanel(new GridBagLayout());
            w.setOpaque(true);
            w.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
            w.add(badge);
            return w;
        }
    }

    private static class BadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            String text = (value != null) ? value.toString() : "";
            Color bg = text.contains("%") ? new Color(240, 235, 255) : new Color(230, 245, 255);
            Color fg = text.contains("%") ? new Color(100, 80, 200) : new Color(30, 120, 180);
            StatusBadge badge = new StatusBadge(text, bg, fg);
            badge.setHorizontalAlignment(CENTER);
            JPanel w = new JPanel(new GridBagLayout());
            w.setOpaque(true);
            w.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
            w.add(badge);
            return w;
        }
    }

    private static class UsageRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            String text = (value != null) ? value.toString() : "0/0";
            String[] parts = text.split("/");
            if (parts.length == 2) {
                try {
                    double used = Double.parseDouble(parts[0]);
                    double total = Double.parseDouble(parts[1]);
                    double pct = (total > 0) ? (used / total) : 0;
                    Color fg = (pct > 0.9) ? UIConstants.DANGER
                            : (pct > 0.7 ? UIConstants.WARNING : UIConstants.SUCCESS);
                    JLabel l = new JLabel(text);
                    l.setFont(UIConstants.FONT_SMALL_BOLD);
                    l.setForeground(fg);
                    l.setHorizontalAlignment(CENTER);
                    JPanel w = new JPanel(new GridBagLayout());
                    w.setOpaque(true);
                    w.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
                    w.add(l);
                    return w;
                } catch (Exception ignored) {
                }
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }
}
