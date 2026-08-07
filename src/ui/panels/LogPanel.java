package ui.panels;

import service.LogService;
import entity.NhatKyHeThong;
import ui.components.UIConstants;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import ui.components.RoundedComponents.*;
import ui.components.PaginationPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import ui.dialogs.LogDialog;
import ui.components.ModernPopupMenu;
import javax.swing.table.TableColumn;

public class LogPanel extends JPanel {
    private final LogService service = new LogService();
    private JTable table;
    private DefaultTableModel model;
    private ModernTextField txtSearch;
    private ModernComboBox<String> cbActionType;

    private PaginationPanel pagination;
    private List<NhatKyHeThong> allLogs;
    private Timer autoRefreshTimer;

    public LogPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();
        setupAutoRefresh();
    }

    private void initData() {
        refresh();
    }

    private void setupAutoRefresh() {
        autoRefreshTimer = new Timer(5000, e -> refreshLogsOnly());
        autoRefreshTimer.setInitialDelay(5000);
        addHierarchyListener((e) -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    autoRefreshTimer.start();
                } else {
                    autoRefreshTimer.stop();
                }
            }
        });

        if (isShowing())
            autoRefreshTimer.start();
    }

    /**
     * Refresh but preserves current pagination if it hasn't changed.
     * For periodic background refresh.
     */
    private void refreshLogsOnly() {
        String keyword = txtSearch.getText();
        String actionType = (String) cbActionType.getSelectedItem();
        List<NhatKyHeThong> newLogs = service.searchLogs(keyword, actionType);

        // Only update if data changed (simple size check or content check)
        if (allLogs == null || newLogs.size() != allLogs.size()) {
            allLogs = newLogs;
            displayCurrentPage();
        }
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

        String[] cols = { "_id", "Thời gian", "Tài khoản", "Hành động", "Đối tượng", "Chi tiết" };
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(model);

        // Hide _id column
        TableColumn idCol = table.getColumn("_id");
        table.removeColumn(idCol);

        styleTable();

        // Setup Interactions
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    showDetail();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r >= 0)
                    table.setRowSelectionInterval(r, r);
            }
        });
        table.setComponentPopupMenu(buildContextMenu());

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(160);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(450);

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
        
        refresh();
    }

    private JPanel buildStats() {
        List<NhatKyHeThong> all = service.getAllLogs();
        int total = all.size();
        
        java.time.LocalDate today = java.time.LocalDate.now();
        long todayCount = all.stream().filter(l -> l.getThoiGian().toLocalDate().equals(today)).count();
        
        long alerts = all.stream().filter(l -> {
            String act = l.getHanhDong().toLowerCase();
            return act.contains("xóa") || act.contains("hủy") || act.contains("lỗi");
        }).count();

        JPanel row = new JPanel(new GridLayout(1, 3, 16, 0));
        row.setOpaque(false);
        row.add(statCard("Tổng số nhật ký", total + " bản ghi", UIConstants.PRIMARY));
        row.add(statCard("Nhật ký hôm nay", todayCount + " hoạt động", UIConstants.SUCCESS));
        row.add(statCard("Cảnh báo hệ thống", alerts + " sự kiện", UIConstants.DANGER));
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

        JLabel lblTitle = new JLabel("Nhật ký hệ thống");
        lblTitle.setFont(UIConstants.FONT_TITLE);
        lblTitle.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel lblSubTitle = new JLabel("Theo dõi các hoạt động vận hành thời gian thực");
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

        txtSearch = new ModernTextField("Tìm tài khoản, đối tượng...");
        txtSearch.setPreferredSize(new Dimension(300, 40));
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { refresh(); }
            @Override
            public void removeUpdate(DocumentEvent e) { refresh(); }
            @Override
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });

        cbActionType = new ModernComboBox<>();
        cbActionType.setPreferredSize(new Dimension(200, 40));
        updateActionTypes();
        cbActionType.addActionListener(e -> refresh());

        filterPanel.add(txtSearch);
        filterPanel.add(cbActionType);
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

        // Custom Zebra Renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean foc, int row,
                    int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                lbl.setBackground(sel ? UIConstants.PRIMARY_LIGHT : (row % 2 == 0 ? Color.WHITE : new Color(0xFAFAFA)));
                lbl.setFont(UIConstants.FONT_BODY);
                lbl.setForeground(UIConstants.TEXT_PRIMARY);

                // Căn lề
                if (col == 0 || col == 1 || col == 3) {
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    lbl.setHorizontalAlignment(SwingConstants.LEFT);
                }

                return lbl;
            }
        });

        table.getColumnModel().getColumn(2).setCellRenderer(new LogActionRenderer());
    }

    private void updateActionTypes() {
        List<String> types = service.getActionTypes();
        String current = (String) cbActionType.getSelectedItem();
        cbActionType.setModel(new DefaultComboBoxModel<String>(types.toArray(new String[0])));
        if (current != null && types.contains(current))
            cbActionType.setSelectedItem(current);
    }

    public void refresh() {
        String keyword = txtSearch.getText();
        String actionType = (String) cbActionType.getSelectedItem();
        allLogs = service.searchLogs(keyword, actionType);

        if (pagination != null) {
            pagination.setCurrentPage(1);
        }
        displayCurrentPage();
    }

    private void displayCurrentPage() {
        model.setRowCount(0);
        if (allLogs == null)
            return;

        int pageSize = 12;
        int currentPage = pagination.getCurrentPage();
        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, allLogs.size());

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
        for (int i = start; i < end; i++) {
            NhatKyHeThong log = allLogs.get(i);
            model.addRow(new Object[] {
                    log.getMaLog(),
                    log.getThoiGian().format(fmt),
                    log.getTenDangNhap(),
                    log.getHanhDong(),
                    log.getDoiTuong(),
                    log.getChiTiet()
            });
        }

        pagination.update(allLogs.size(), pageSize, currentPage);
    }

    private JPopupMenu buildContextMenu() {
        ModernPopupMenu menu = new ModernPopupMenu();

        JMenuItem miView = new JMenuItem("Xem chi tiết");
        miView.setFont(UIConstants.FONT_BODY);
        miView.addActionListener(e -> showDetail());

        menu.add(miView);
        return menu;
    }

    private NhatKyHeThong getSelected() {
        int row = table.getSelectedRow();
        if (row < 0)
            return null;
        int modelRow = table.convertRowIndexToModel(row);
        int id = (int) model.getValueAt(modelRow, 0);
        return allLogs.stream().filter(l -> l.getMaLog() == id).findFirst().orElse(null);
    }

    private void showDetail() {
        NhatKyHeThong log = getSelected();
        if (log == null)
            return;
        new LogDialog((Frame) SwingUtilities.getWindowAncestor(this), log).setVisible(true);
    }

    // --- Custom Renderer for Action Badge ---
    private static class LogActionRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            String text = (value != null) ? value.toString() : "";
            Color bg, fg;

            if (text.contains("Đăng nhập") || text.contains("Đăng xuất")) {
                bg = new Color(235, 243, 255);
                fg = new Color(30, 100, 230);
            } else if (text.contains("Check-") || text.contains("đặt") || text.contains("thanh toán")) {
                bg = new Color(230, 250, 240);
                fg = new Color(30, 150, 80);
            } else if (text.contains("Sửa") || text.contains("Cập nhật")) {
                bg = new Color(255, 245, 230);
                fg = new Color(200, 110, 30);
            } else if (text.contains("Xóa") || text.contains("Hủy")) {
                bg = new Color(255, 235, 235);
                fg = new Color(220, 50, 50);
            } else {
                bg = new Color(240, 242, 245);
                fg = UIConstants.TEXT_SECONDARY;
            }

            StatusBadge badge = new StatusBadge(text, bg, fg);
            badge.setHorizontalAlignment(CENTER);
            badge.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE); // Handle table selection
                                                                                            // background

            // Wrapper to hold the badge centrally
            JPanel wrapper = new JPanel(new GridBagLayout());
            wrapper.setOpaque(true);
            wrapper.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
            wrapper.add(badge);

            return wrapper;
        }
    }
}
