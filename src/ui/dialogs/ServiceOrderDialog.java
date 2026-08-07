package ui.dialogs;

import dao.SuDungDichVuDAO;
import entity.ChiTietDatPhong;
import entity.DichVu;
import entity.SuDungDichVu;
import service.DichVuService;
import ui.components.UIConstants;
import ui.components.RoundedComponents.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ServiceOrderDialog extends JDialog {

    private final ChiTietDatPhong chiTietDatPhong;
    private final DichVuService dichVuService = new DichVuService();
    private final service.ThuePhongService thuePhongService = new service.ThuePhongService();

    private JPanel gridPanel;
    private JPanel cartItemsPanel;
    private JPanel categoryContainer;
    private JLabel lblTotal;
    private JTextField txtSearch;
    
    private final List<SuDungDichVu> cart = new ArrayList<>();
    private List<DichVu> allServices;
    private List<String> categories = new ArrayList<>();
    private String selectedCategory = "Tất cả";
    private boolean confirmed = false;

    public ServiceOrderDialog(Frame parent, ChiTietDatPhong ctdp) {
        super(parent, "Dịch vụ phòng " + (ctdp.getPhong() != null ? ctdp.getPhong().getSoPhong() : ""), true);
        this.chiTietDatPhong = ctdp;

        setSize(880, 640);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(0xF1F5F9));

        buildUI();
        loadData();
    }

    private void buildUI() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setPreferredSize(new Dimension(0, UIConstants.HEADER_HEIGHT));
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER));
        topBar.setBorder(BorderFactory.createCompoundBorder(topBar.getBorder(), new EmptyBorder(0, 20, 0, 20)));

        JLabel lblTitle = new JLabel("Thực đơn Dịch vụ");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(UIConstants.TEXT_PRIMARY);
        topBar.add(lblTitle, BorderLayout.WEST);

        JPanel searchBox = new JPanel(new BorderLayout(8, 0));
        searchBox.setOpaque(false);
        txtSearch = new JTextField();
        txtSearch.setPreferredSize(new Dimension(220, 36));
        txtSearch.setFont(UIConstants.FONT_BODY);
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm tên dịch vụ...");
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyReleased(java.awt.event.KeyEvent e) { filterGrid(); }
        });
        searchBox.add(new JLabel("🔍"), BorderLayout.WEST);
        searchBox.add(txtSearch, BorderLayout.CENTER);
        
        JPanel searchWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 12));
        searchWrapper.setOpaque(false);
        searchWrapper.add(searchBox);
        topBar.add(searchWrapper, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // --- Main Content (Grid + Cart) ---
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        
        // Left: Service Grid
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(false);
        leftPanel.setBorder(new EmptyBorder(15, 20, 15, 10));

        // Category Filter
        categoryContainer = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 10));
        categoryContainer.setOpaque(false);
        categoryContainer.setBorder(new EmptyBorder(0, 0, 5, 0));
        leftPanel.add(categoryContainer, BorderLayout.NORTH);

        gridPanel = new ScrollablePanel(new WrapLayout(FlowLayout.LEFT, 12, 12));
        gridPanel.setOpaque(false);
        JScrollPane scrollGrid = new JScrollPane(gridPanel);
        scrollGrid.setBorder(null);
        scrollGrid.setOpaque(false);
        scrollGrid.getViewport().setOpaque(false);
        scrollGrid.getVerticalScrollBar().setUnitIncrement(16);
        leftPanel.add(scrollGrid, BorderLayout.CENTER);

        content.add(leftPanel, BorderLayout.CENTER);

        // Right: Modern Sidebar Cart
        JPanel cartPanel = new JPanel(new BorderLayout());
        cartPanel.setBackground(Color.WHITE);
        cartPanel.setPreferredSize(new Dimension(260, 0));
        cartPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, UIConstants.BORDER));

        JPanel cartHeader = new JPanel(new BorderLayout());
        cartHeader.setOpaque(false);
        cartHeader.setBorder(new EmptyBorder(10, 20, 5, 20));
        JLabel lblCart = new JLabel("Giỏ hàng mới");
        lblCart.setFont(new Font("Segoe UI", Font.BOLD, 15));
        cartHeader.add(lblCart, BorderLayout.WEST);

        cartItemsPanel = new JPanel();
        cartItemsPanel.setBackground(Color.WHITE);
        cartItemsPanel.setLayout(new BoxLayout(cartItemsPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollSidebar = new JScrollPane(cartItemsPanel);
        scrollSidebar.setBorder(null);
        scrollSidebar.setOpaque(false);
        scrollSidebar.getViewport().setOpaque(false);
        
        cartPanel.add(cartHeader, BorderLayout.NORTH);
        cartPanel.add(scrollSidebar, BorderLayout.CENTER);

        JPanel cartFooter = new JPanel(new BorderLayout(0, 12));
        cartFooter.setBackground(Color.WHITE);
        cartFooter.setBorder(new EmptyBorder(10, 20, 15, 20));
        
        JPanel totalBox = new JPanel(new BorderLayout());
        totalBox.setOpaque(false);
        JLabel lblT = new JLabel("Tổng tiền tạm tính");
        lblT.setFont(UIConstants.FONT_BODY);
        lblT.setForeground(UIConstants.TEXT_SECONDARY);
        lblTotal = new JLabel("0đ");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTotal.setForeground(UIConstants.PRIMARY);
        totalBox.add(lblT, BorderLayout.NORTH);
        totalBox.add(lblTotal, BorderLayout.SOUTH);
        
        RoundedButton btnCheckout = new RoundedButton("XÁC NHẬN GHI DỊCH VỤ", UIConstants.PRIMARY, Color.WHITE);
        btnCheckout.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCheckout.setPreferredSize(new Dimension(0, 48));
        btnCheckout.addActionListener(e -> saveAll());

        cartFooter.add(totalBox, BorderLayout.NORTH);
        cartFooter.add(btnCheckout, BorderLayout.CENTER);
        cartPanel.add(cartFooter, BorderLayout.SOUTH);

        content.add(cartPanel, BorderLayout.EAST);
        add(content, BorderLayout.CENTER);
    }

    private void renderCategories() {
        categoryContainer.removeAll();
        JButton btnAll = createCategoryButton("Tất cả");
        categoryContainer.add(btnAll);
        for (String cat : categories) {
            categoryContainer.add(createCategoryButton(cat));
        }
        categoryContainer.revalidate();
        categoryContainer.repaint();
    }

    private JButton createCategoryButton(String cat) {
        boolean isSelected = cat.equals(selectedCategory);
        JButton btn = new JButton(cat);
        btn.setFont(UIConstants.FONT_SMALL_BOLD);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(isSelected ? UIConstants.PRIMARY : UIConstants.BORDER, isSelected ? 2 : 1, true),
            new EmptyBorder(8, 20, 8, 20)
        ));
        
        if (isSelected) {
            btn.setForeground(UIConstants.PRIMARY);
            btn.setBackground(new Color(0xEFF6FF));
            btn.setOpaque(true);
        }

        btn.addActionListener(e -> {
            selectedCategory = cat;
            renderCategories();
            filterGrid();
        });
        return btn;
    }

    private void loadData() {
        allServices = dichVuService.getAll();
        
        
        // Dynamically build category list from actual data
        categories = allServices.stream()
            .map(DichVu::getLoai)
            .filter(l -> l != null && !l.isBlank())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
            
        renderCategories();
        renderGrid(allServices);
    }

    private void filterGrid() {
        String kw = txtSearch.getText().toLowerCase().trim();
        List<DichVu> filtered = allServices.stream()
            .filter(dv -> {
                if ("Tất cả".equals(selectedCategory)) return true;
                return selectedCategory.equalsIgnoreCase(dv.getLoai());
            })
            .filter(dv -> {
                if (kw.isEmpty()) return true;
                String name = dv.getTenDichVu();
                return name != null && name.toLowerCase().contains(kw);
            })
            .collect(Collectors.toList());
        renderGrid(filtered);
        updateCartUI();
    }

    private void renderGrid(List<DichVu> services) {
        gridPanel.removeAll();
        for (DichVu dv : services) {
            gridPanel.add(new ServiceCard(dv));
        }
        // Add glue to prevent vertical stretching if items don't fill the page
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private void addToCart(DichVu dv) {
        // Kiểm tra xem đã có trong giỏ hàng chưa
        for (SuDungDichVu s : cart) {
            String mid = s.getDichVu() != null ? s.getDichVu().getMaDichVu() : "";
            if (mid.equals(dv.getMaDichVu())) {
                s.setSoLuong(s.getSoLuong() + 1);
                updateCartUI();
                return;
            }
        }
        
        SuDungDichVu su = new SuDungDichVu();
        su.setCtdp(chiTietDatPhong);
        su.setDichVu(dv);
        su.setSoLuong(1);
        su.setDonGiaLuu(dv.getDonGia());
        su.setThoiDiem(LocalDateTime.now());
        cart.add(su);
        updateCartUI();
    }

    private void updateCartUI() {
        cartItemsPanel.removeAll();
        double total = 0;
        
        for (SuDungDichVu s : cart) {
            cartItemsPanel.add(new CartItemRow(s));
            total += s.tinhThanhTien();
        }
        lblTotal.setText(String.format("%,.0fđ", total));
        cartItemsPanel.add(Box.createVerticalGlue());
        cartItemsPanel.revalidate();
        cartItemsPanel.repaint();
    }

    private JButton createPillBtn(String text, boolean plus) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(24, 24));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorder(null);
        btn.setBackground(new Color(0xF8FAFC));
        btn.setForeground(new Color(0x64748B));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(0xF1F5F9));
                btn.setForeground(UIConstants.PRIMARY);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(0xF8FAFC));
                btn.setForeground(new Color(0x64748B));
            }
        });
        return btn;
    }

    private void saveAll() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn ít nhất 1 dịch vụ!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        List<String> errors = new ArrayList<>();
        for (SuDungDichVu s : cart) {
            String err = thuePhongService.themDichVu(s);
            if (err != null) {
                errors.add(s.getDichVu().getTenDichVu() + ": " + err);
            }
        }

        if (!errors.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Một số dịch vụ không thể lưu lại:\n- " + String.join("\n- ", errors), 
                "Lỗi xử lý", JOptionPane.ERROR_MESSAGE);
            return;
        }

        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() { return confirmed; }

    // --- Inner Component: Service Card ---
    class ServiceCard extends JPanel {
        private final DichVu dv;
        public ServiceCard(DichVu dv) {
            this.dv = dv;
            setLayout(new BorderLayout(0, 6));
            setPreferredSize(new Dimension(185, 135));
            setBackground(Color.WHITE);
            setBorder(new EmptyBorder(12, 12, 12, 12));
            
            // Icon Placeholder
            JLabel icon = new JLabel(dv.getLoai().substring(0, 2).toUpperCase(), SwingConstants.CENTER);
            icon.setFont(new Font("Segoe UI", Font.BOLD, 15));
            icon.setOpaque(true);
            icon.setBackground(new Color(0xEFF6FF));
            icon.setForeground(UIConstants.PRIMARY);
            icon.setPreferredSize(new Dimension(34, 34));
            icon.setBorder(BorderFactory.createLineBorder(new Color(0xDBEAFE)));
            
            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            top.add(icon, BorderLayout.WEST);
            
            JLabel price = new JLabel(String.format("%,.0fđ", dv.getDonGia()));
            price.setFont(UIConstants.FONT_SMALL_BOLD);
            price.setForeground(UIConstants.SUCCESS);
            top.add(price, BorderLayout.SOUTH);
            
            JLabel name = new JLabel("<html><body style='width:200px'>" + dv.getTenDichVu() + "</body></html>");
            name.setFont(UIConstants.FONT_BODY_BOLD);
            
            JButton btnAdd = new JButton("+ Thêm");
            btnAdd.setFont(UIConstants.FONT_SMALL_BOLD);
            btnAdd.setForeground(UIConstants.PRIMARY);
            btnAdd.setFocusPainted(false);
            btnAdd.setContentAreaFilled(false);
            btnAdd.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnAdd.addActionListener(e -> addToCart(dv));

            add(top, BorderLayout.NORTH);
            add(name, BorderLayout.CENTER);
            add(btnAdd, BorderLayout.SOUTH);
            
            // Hover effect
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { setBackground(new Color(0xF8FAFC)); }
                @Override public void mouseExited(MouseEvent e) { setBackground(Color.WHITE); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g2.setColor(UIConstants.BORDER);
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
            g2.dispose();
        }
    }

    // --- Inner Component: Cart Item Row ---
    class CartItemRow extends JPanel {
        public CartItemRow(SuDungDichVu s) {
            setLayout(new BorderLayout(8, 0));
            setBackground(Color.WHITE);
            setBorder(new EmptyBorder(6, 20, 6, 12));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

            JPanel leftContent = new JPanel(new BorderLayout());
            leftContent.setOpaque(false);
            
            JLabel name = new JLabel(s.getDichVu().getTenDichVu());
            name.setFont(new Font("Segoe UI", Font.BOLD, 12));
            leftContent.add(name, BorderLayout.CENTER);

            JLabel price = new JLabel(String.format("%,.0fđ", s.tinhThanhTien()));
            price.setFont(UIConstants.FONT_SMALL);
            price.setForeground(UIConstants.PRIMARY);
            leftContent.add(price, BorderLayout.SOUTH);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            actions.setOpaque(false);
            actions.setBorder(new EmptyBorder(10, 0, 0, 0));

            // Pill Picker (+/-)
            JPanel pill = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            pill.setBackground(Color.WHITE);
            pill.setBorder(BorderFactory.createLineBorder(new Color(0xE2E8F0), 1));

            JButton btnMin = createPillBtn("-", false);
            JLabel lblQtyValue = new JLabel(String.valueOf(s.getSoLuong()), SwingConstants.CENTER);
            lblQtyValue.setPreferredSize(new Dimension(30, 24));
            lblQtyValue.setFont(new Font("Segoe UI", Font.BOLD, 12));
            JButton btnPlus = createPillBtn("+", true);

            btnMin.addActionListener(e -> {
                if (s.getSoLuong() > 1) {
                    s.setSoLuong(s.getSoLuong() - 1);
                } else {
                    cart.remove(s);
                }
                updateCartUI();
            });
            btnPlus.addActionListener(e -> {
                s.setSoLuong(s.getSoLuong() + 1);
                updateCartUI();
            });

            pill.add(btnMin);
            pill.add(lblQtyValue);
            pill.add(btnPlus);

            JButton btnDel = new JButton("\u00d7");
            btnDel.setFont(new Font("Segoe UI", Font.PLAIN, 20));
            btnDel.setForeground(new Color(0x94A3B8));
            btnDel.setBorder(null);
            btnDel.setOpaque(false);
            btnDel.setContentAreaFilled(false);
            btnDel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnDel.addActionListener(e -> { cart.remove(s); updateCartUI(); });
            btnDel.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) { btnDel.setForeground(Color.RED); }
                public void mouseExited(MouseEvent e) { btnDel.setForeground(new Color(0x94A3B8)); }
            });

            actions.add(pill);
            actions.add(btnDel);

            add(leftContent, BorderLayout.CENTER);
            add(actions, BorderLayout.EAST);
        }
    }

    // --- Inner Component: Scrollable Panel for Grid ---
    static class ScrollablePanel extends JPanel implements Scrollable {
        public ScrollablePanel(LayoutManager layout) { super(layout); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 20; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return 100; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // Revised WrapLayout to prevent overflow and ensure proper wrapping within Viewport
    static class WrapLayout extends FlowLayout {
        public WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }
        @Override public Dimension preferredLayoutSize(Container target) { return layoutSize(target, true); }
        @Override public Dimension minimumLayoutSize(Container target) { return layoutSize(target, false); }
        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getWidth();
                // If width is 0 (first render), use a sensible default
                if (targetWidth == 0) targetWidth = 800;
                
                int hgap = getHgap(); int vgap = getVgap();
                Insets insets = target.getInsets();
                int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
                int maxWidth = targetWidth - horizontalInsetsAndGap;
                
                int x = 0; int y = insets.top; int rowHeight = 0;
                int nmembers = target.getComponentCount();
                for (int i = 0; i < nmembers; i++) {
                    Component m = target.getComponent(i);
                    if (m.isVisible()) {
                        Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                        if (x == 0 || x + d.width <= maxWidth) {
                            if (x > 0) x += hgap;
                            x += d.width; rowHeight = Math.max(rowHeight, d.height);
                        } else {
                            x = d.width; y += vgap + rowHeight; rowHeight = d.height;
                        }
                    }
                }
                return new Dimension(targetWidth, y + rowHeight + insets.bottom);
            }
        }
    }
}
