package ui.panels;

import service.GiaoCaService;
import entity.GiaoCa;
import ui.components.UIConstants;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import ui.components.RoundedComponents.*;
import ui.components.PaginationPanel;
import javax.swing.table.DefaultTableCellRenderer;
import ui.components.ModernPopupMenu;
import ui.dialogs.ShiftDetailDialog;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.stream.Collectors;

public class ShiftHistoryPanel extends JPanel {
    private final GiaoCaService service = new GiaoCaService();
    private JTable table;
    private DefaultTableModel model;
    private ModernTextField txtSearch;
    private ModernComboBox<String> cbStatus;

    private PaginationPanel pagination;
    private List<GiaoCa> allHistory;
    private Timer refreshTimer;

    public ShiftHistoryPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();
        refresh();
        setupAutoRefresh();
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

        String[] cols = {"Mã ca", "Nhân viên", "Bắt đầu", "Kết thúc", "Vốn đầu ca", "Thu tiền mặt", "Bàn giao", "Chênh lệch", "Trạng thái"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        styleTable();

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(8).setPreferredWidth(120);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        
        // Pagination
        pagination = new PaginationPanel();
        pagination.setPageChangeListener(page -> displayCurrentPage());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showDetail();
            }
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup(e);
            }
            private void showPopup(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && row < table.getRowCount()) {
                    table.setRowSelectionInterval(row, row);
                    buildContextMenu().show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        
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
        List<GiaoCa> all = service.getHistory();
        double totalRevenue = all.stream().mapToDouble(GiaoCa::getTienMatThuTrongCa).sum();
        double totalDiscrepancy = all.stream().mapToDouble(GiaoCa::getTienMatChenhLech).sum();
        long openShifts = all.stream().filter(gc -> "OPEN".equals(gc.getTrangThai())).count();

        JPanel row = new JPanel(new GridLayout(1, 3, 16, 0));
        row.setOpaque(false);
        row.add(statCard("Tổng thu tiền mặt", String.format("%,.0f VNĐ", totalRevenue), UIConstants.PRIMARY));
        row.add(statCard("Tổng chênh lệch", String.format("%,.0f VNĐ", totalDiscrepancy), UIConstants.WARNING));
        row.add(statCard("Ca đang hoạt động", openShifts + " ca trực", UIConstants.SUCCESS));
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
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 5));

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("Lịch sử giao ca");
        lblTitle.setFont(UIConstants.FONT_TITLE);
        lblTitle.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel lblSubTitle = new JLabel("Theo dõi và đối soát dữ liệu bàn giao ca trực");
        lblSubTitle.setFont(UIConstants.FONT_BODY);
        lblSubTitle.setForeground(UIConstants.TEXT_SECONDARY);

        titlePanel.add(lblTitle);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(lblSubTitle);
        p.add(titlePanel, BorderLayout.WEST);

        return p;
    }

    private JPanel buildFilterBar() {
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        filterPanel.setOpaque(false);

        txtSearch = new ModernTextField("Tìm nhân viên, mã ca...");
        txtSearch.setPreferredSize(new Dimension(300, 40));
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); }
            public void removeUpdate(DocumentEvent e) { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });

        cbStatus = new ModernComboBox<>(new String[]{"Tất cả trạng thái", "OPEN", "CLOSED"});
        cbStatus.setPreferredSize(new Dimension(180, 40));
        cbStatus.addActionListener(e -> refresh());

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
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                lbl.setBackground(sel ? UIConstants.PRIMARY_LIGHT : (row % 2 == 0 ? Color.WHITE : new Color(0xFAFAFA)));
                lbl.setFont(UIConstants.FONT_BODY);
                lbl.setForeground(UIConstants.TEXT_PRIMARY);

                // Căn lề theo loại dữ liệu
                if (col >= 4 && col <= 7) {
                    lbl.setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                }

                return lbl;
            }
        });

        table.getColumnModel().getColumn(8).setCellRenderer(new ShiftStatusRenderer());
    }

    public void refresh() {
        String keyword = txtSearch.getText().trim();
        String status = (String) cbStatus.getSelectedItem();
        if ("Tất cả trạng thái".equals(status)) status = null;
        allHistory = service.searchHistory(keyword, status);
        
        if (pagination != null) {
            pagination.setCurrentPage(1);
        }
        displayCurrentPage();
    }

    private void displayCurrentPage() {
        model.setRowCount(0);
        if (allHistory == null) return;

        int pageSize = 12;
        int currentPage = pagination.getCurrentPage();
        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, allHistory.size());

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM");
        for (int i = start; i < end; i++) {
            GiaoCa gc = allHistory.get(i);
            model.addRow(new Object[]{
                gc.getMaGiaoCa(),
                gc.getNhanVien().getHoTen(),
                gc.getThoiGianBatDau().format(fmt),
                gc.getThoiGianKetThuc() != null ? gc.getThoiGianKetThuc().format(fmt) : "--",
                String.format("%,.0f", gc.getTienMatDauCa()),
                String.format("%,.0f", gc.getTienMatThuTrongCa()),
                String.format("%,.0f", gc.getTienMatBanGiao()),
                String.format("%,.0f", gc.getTienMatChenhLech()),
                gc.getTrangThai()
            });
        }
        
        pagination.update(allHistory.size(), pageSize, currentPage);
    }

    private void setupAutoRefresh() {
        refreshTimer = new Timer(5000, e -> refreshLogsOnly());
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) refreshTimer.start();
                else refreshTimer.stop();
            }
        });
        if (isShowing()) refreshTimer.start();
    }

    private void refreshLogsOnly() {
        String keyword = txtSearch.getText().trim();
        String status = (String) cbStatus.getSelectedItem();
        if ("Tất cả trạng thái".equals(status)) status = null;
        List<GiaoCa> newHistory = service.searchHistory(keyword, status);
        
        if (allHistory == null || newHistory.size() != allHistory.size()) {
            allHistory = newHistory;
            displayCurrentPage();
        }
    }

    private JPopupMenu buildContextMenu() {
        ModernPopupMenu menu = new ModernPopupMenu();
        JMenuItem miView = new JMenuItem("Xem chi tiết");
        miView.setFont(UIConstants.FONT_BODY);
        miView.addActionListener(e -> showDetail());
        menu.add(miView);
        return menu;
    }

    private GiaoCa getSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        int modelRow = table.convertRowIndexToModel(row);
        String id = (String) model.getValueAt(modelRow, 0);
        return allHistory.stream().filter(gc -> gc.getMaGiaoCa().equals(id)).findFirst().orElse(null);
    }

    private void showDetail() {
        GiaoCa gc = getSelected();
        if (gc == null) return;
        new ShiftDetailDialog((Frame) SwingUtilities.getWindowAncestor(this), gc).setVisible(true);
    }

    // --- Custom Renderer for Shift Status ---
    private static class ShiftStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String text = (value != null) ? value.toString() : "";
            Color bg, fg;

            if ("OPEN".equals(text)) {
                bg = new Color(230, 250, 240); fg = new Color(30, 150, 80);
            } else {
                bg = new Color(235, 243, 255); fg = new Color(30, 100, 230);
            }

            StatusBadge badge = new StatusBadge(text, bg, fg);
            badge.setHorizontalAlignment(CENTER);
            badge.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
            
            JPanel wrapper = new JPanel(new GridBagLayout());
            wrapper.setOpaque(true);
            wrapper.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
            wrapper.add(badge);
            
            return wrapper;
        }
    }
}
