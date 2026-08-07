package ui.components;

import ui.components.RoundedComponents.RoundedButton;
import java.awt.*;
import javax.swing.*;

/**
 * Component phân trang hiện đại và đẹp mắt.
 */
public class PaginationPanel extends JPanel {

    public interface PageChangeListener {
        void onPageChanged(int newPage);
    }

    private int currentPage = 1;
    private int totalPages = 1;
    private int totalRecords = 0;
    private int pageSize = 12;

    private final RoundedButton btnPrev;
    private final RoundedButton btnNext;
    private final JLabel lblPage;
    private final JLabel lblStatus;
    
    private PageChangeListener listener;

    private static class ArrowIcon implements Icon {
        private final boolean left;
        private final int size;
        private Color color;

        public ArrowIcon(boolean left, int size, Color color) {
            this.left = left;
            this.size = size;
            this.color = color;
        }

        public void setColor(Color color) { this.color = color; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            int w = getIconWidth(), h = getIconHeight();
            int[] xPoints, yPoints;
            if (left) {
                xPoints = new int[]{x + w, x, x + w};
                yPoints = new int[]{y, y + h / 2, y + h};
            } else {
                xPoints = new int[]{x, x + w, x};
                yPoints = new int[]{y, y + h / 2, y + h};
            }
            g2.fillPolygon(xPoints, yPoints, 3);
            g2.dispose();
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    private ArrowIcon iconPrev, iconNext;

    public PaginationPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        // LEFT: Status label
        lblStatus = new JLabel("Hiển thị 0 - 0 trong số 0 bản ghi");
        lblStatus.setFont(UIConstants.FONT_SMALL);
        lblStatus.setForeground(UIConstants.TEXT_SECONDARY);

        // CENTER: Paging controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        controls.setOpaque(false);

        iconPrev = new ArrowIcon(true, 10, UIConstants.TEXT_PRIMARY);
        btnPrev = new RoundedButton("", Color.WHITE, UIConstants.TEXT_PRIMARY);
        btnPrev.setIcon(iconPrev);
        btnPrev.setPreferredSize(new Dimension(36, 32));
        
        iconNext = new ArrowIcon(false, 10, UIConstants.TEXT_PRIMARY);
        btnNext = new RoundedButton("", Color.WHITE, UIConstants.TEXT_PRIMARY);
        btnNext.setIcon(iconNext);
        btnNext.setPreferredSize(new Dimension(36, 32));

        lblPage = new JLabel("Trang 1 / 1");
        lblPage.setFont(UIConstants.FONT_BODY_BOLD);
        lblPage.setForeground(UIConstants.TEXT_PRIMARY);

        btnPrev.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                notifyListener();
                updateUIState();
            }
        });

        btnNext.addActionListener(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                notifyListener();
                updateUIState();
            }
        });

        controls.add(btnPrev);
        controls.add(lblPage);
        controls.add(btnNext);

        add(lblStatus, BorderLayout.WEST);
        add(controls, BorderLayout.CENTER);
        
        // RIGHT: Optional spacer or extra info
        add(Box.createHorizontalStrut(150), BorderLayout.EAST);
        
        updateUIState();
    }

    public void setPageChangeListener(PageChangeListener listener) {
        this.listener = listener;
    }

    public void update(int totalRecords, int pageSize, int currentPage) {
        this.totalRecords = totalRecords;
        this.pageSize = pageSize;
        this.currentPage = currentPage;
        this.totalPages = (int) Math.ceil((double) totalRecords / pageSize);
        if (this.totalPages == 0) this.totalPages = 1;
        
        updateUIState();
    }

    private void updateUIState() {
        lblPage.setText("Trang " + currentPage + " / " + totalPages);
        
        int start = totalRecords == 0 ? 0 : (currentPage - 1) * pageSize + 1;
        int end = Math.min(currentPage * pageSize, totalRecords);
        lblStatus.setText("Hiển thị " + start + " - " + end + " trong số " + totalRecords + " bản ghi");

        boolean canPrev = currentPage > 1;
        boolean canNext = currentPage < totalPages;

        btnPrev.setEnabled(canPrev);
        btnNext.setEnabled(canNext);
        
        // Fade effect for disabled buttons
        iconPrev.setColor(canPrev ? UIConstants.TEXT_PRIMARY : UIConstants.TEXT_MUTED);
        iconNext.setColor(canNext ? UIConstants.TEXT_PRIMARY : UIConstants.TEXT_MUTED);
        btnPrev.repaint();
        btnNext.repaint();
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onPageChanged(currentPage);
        }
    }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int page) { 
        this.currentPage = page; 
        updateUIState();
    }
}
