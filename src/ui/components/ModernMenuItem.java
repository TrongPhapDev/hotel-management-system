package ui.components;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.*;

/**
 * Custom menu item với thiết kế hiện đại
 */
public class ModernMenuItem extends JMenuItem implements MouseListener {

    private static final Color HOVER_BG = new Color(239, 246, 255);
    private static final Color HOVER_TEXT = new Color(37, 99, 235);
    private static final Color NORMAL_TEXT = new Color(15, 23, 42);
    private static final Color DISABLED_TEXT = new Color(148, 163, 184);
    private boolean isHovered = false;

    public ModernMenuItem(String text) {
        super(text);
        setOpaque(false);
        setFont(UIConstants.FONT_BODY);
        setBorderPainted(false);
        setFocusPainted(false);
        setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        SwingUtilities.invokeLater(() -> addMouseListener(ModernMenuItem.this));
        setUI(new javax.swing.plaf.basic.BasicMenuItemUI());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (isEnabled()) {
            // Draw background on hover
            if (isHovered || getModel().isArmed()) {
                g2.setColor(HOVER_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        }

        // Draw text
        g2.setFont(getFont());
        g2.setColor(isEnabled() ? (isHovered || getModel().isArmed() ? HOVER_TEXT : NORMAL_TEXT) : DISABLED_TEXT);
        FontMetrics fm = g2.getFontMetrics();
        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        String text = getText();
        if (text != null) {
            g2.drawString(text, 16, y);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (isEnabled()) {
            isHovered = true;
            repaint();
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        isHovered = false;
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}
}

