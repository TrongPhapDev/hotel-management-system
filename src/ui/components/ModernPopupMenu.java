package ui.components;

import java.awt.*;
import javax.swing.*;

/**
 * Custom popup menu với thiết kế hiện đại sử dụng Graphics2D
 */
public class ModernPopupMenu extends JPopupMenu {

    private static final int RADIUS = 12;
    private static final Color BACKGROUND = new Color(255, 255, 255);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color SHADOW_COLOR = new Color(0, 0, 0, 15);

    public ModernPopupMenu() {
        super();
        setOpaque(false);
        setBorderPainted(false);
        setBackground(new Color(0, 0, 0, 0));
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw shadow
        g2.setColor(SHADOW_COLOR);
        for (int i = 3; i >= 1; i--) {
            g2.drawRoundRect(i, i, getWidth() - i * 2 - 1, getHeight() - i * 2 - 1, RADIUS, RADIUS);
        }

        // Draw background with rounded corners
        g2.setColor(BACKGROUND);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RADIUS, RADIUS);

        // Draw border
        g2.setColor(BORDER_COLOR);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RADIUS, RADIUS);

        super.paint(g);
    }
}

