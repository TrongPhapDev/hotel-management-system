package ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * A modern, rounded status badge component for labels.
 */
public class StatusBadge extends JPanel {
    private final String text;
    private final Color bgColor;
    private final Color fgColor;
    private final Font font = new Font("Segoe UI", Font.BOLD, 12);

    public StatusBadge(String text, Color bgColor, Color fgColor) {
        this.text = text;
        this.bgColor = bgColor;
        this.fgColor = fgColor;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        FontMetrics fm = g2.getFontMetrics(font);
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent(); // Use ascent for vertical centering
        
        int badgeWidth = textWidth + 20; // 10px padding each side
        int badgeHeight = 24; // Standard height
        
        int x = (getWidth() - badgeWidth) / 2;
        int y = (getHeight() - badgeHeight) / 2;

        // Draw background
        g2.setColor(bgColor);
        g2.fillRoundRect(x, y, badgeWidth, badgeHeight, 10, 10);

        // Draw text
        g2.setColor(fgColor);
        g2.setFont(font);
        // Center text perfectly
        int tx = x + (badgeWidth - textWidth) / 2;
        int ty = y + (badgeHeight + textHeight) / 2 - 2; // Slight adjustment for Segoe UI
        g2.drawString(text, tx, ty);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(100, 32);
    }
}
