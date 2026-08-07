package ui.components;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * Các component tùy chỉnh: nút bo góc, panel bo góc, text field đẹp.
 */
public class RoundedComponents {

    // ============================================================
    // RoundedButton
    // ============================================================
    public static class RoundedButton extends JButton {
        private Color normalBg, hoverBg, textColor;
        private int radius;
        private boolean isOutline;

        public RoundedButton(String text, Color bg, Color fg) {
            super(text);
            this.normalBg  = bg;
            this.hoverBg   = bg.darker();
            this.textColor = fg;
            this.radius    = UIConstants.BTN_RADIUS;
            setup();
        }

        /** Outline (viền) button */
        public static RoundedButton outline(String text, Color borderColor) {
            RoundedButton btn = new RoundedButton(text, Color.WHITE, borderColor);
            btn.isOutline = true;
            btn.normalBg  = Color.WHITE;
            btn.hoverBg   = new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), 20);
            return btn;
        }

        private void setup() {
            setFont(UIConstants.FONT_BODY_BOLD);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) { repaint(); }
                @Override
                public void mouseExited(MouseEvent e)  { repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean hover = getModel().isRollover();
            Color bg = hover ? hoverBg : normalBg;
            if (isOutline) {
                g2.setColor(hover ? hoverBg : Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
                g2.setColor(textColor);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0.75f, 0.75f, getWidth()-1.5f, getHeight()-1.5f, radius, radius));
            } else {
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            }
            // Text & Icon
            String text = getText() != null ? getText() : "";
            Icon icon = getIcon();
            
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getAscent() - fm.getDescent();
            int iconWidth = icon != null ? icon.getIconWidth() : 0;
            int iconHeight = icon != null ? icon.getIconHeight() : 0;
            int gap = (icon != null && !text.isEmpty()) ? 6 : 0;
            
            int totalWidth = textWidth + iconWidth + gap;
            int startX = (getWidth() - totalWidth) / 2;
            
            if (icon != null) {
                int iconY = (getHeight() - iconHeight) / 2;
                icon.paintIcon(this, g2, startX, iconY);
                startX += iconWidth + gap;
            }
            
            g2.setColor(textColor);
            if (!text.isEmpty()) {
                int textY = (getHeight() + textHeight) / 2;
                g2.drawString(text, startX, textY);
            }
            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width + 24, Math.max(d.height, 36));
        }
    }

    // ============================================================
    // RoundedPanel
    // ============================================================
    public static class RoundedPanel extends JPanel {
        private final int radius;
        private Color shadowColor;
        private boolean hasShadow;

        public RoundedPanel(int radius) {
            this.radius = radius;
            setOpaque(false);
        }

        public void setShadow(boolean hasShadow) {
            this.hasShadow   = hasShadow;
            this.shadowColor = new Color(0, 0, 0, 18);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (hasShadow) {
                g2.setColor(shadowColor);
                g2.fill(new RoundRectangle2D.Float(2, 3, getWidth()-2, getHeight()-2, radius, radius));
            }
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-2, getHeight()-3, radius, radius));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ============================================================
    // ModernTextField - text field với viền bo góc
    // ============================================================
    public static class ModernTextField extends JTextField {
        private String placeholder;

        public ModernTextField(String placeholder) {
            this.placeholder = placeholder;
            setFont(UIConstants.FONT_BODY);
            setForeground(UIConstants.TEXT_PRIMARY);
            setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(7, 12, 7, 12)));
            setBackground(Color.WHITE);
            setOpaque(true);
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER_FOCUS),
                        BorderFactory.createEmptyBorder(7, 12, 7, 12)));
                }
                @Override
                public void focusLost(FocusEvent e) {
                    setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
                        BorderFactory.createEmptyBorder(7, 12, 7, 12)));
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && placeholder != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.TEXT_MUTED);
                g2.setFont(getFont());
                g2.drawString(placeholder, 12, getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 2);
                g2.dispose();
            }
        }
    }

    // ============================================================
    // ModernSpinner - spinner với UI đồng bộ
    // ============================================================
    public static class ModernSpinner extends JSpinner {
        public ModernSpinner(SpinnerModel model) {
            super(model);
            setFont(UIConstants.FONT_BODY);
            setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
            setBackground(Color.WHITE);
            setUI(new javax.swing.plaf.basic.BasicSpinnerUI() {
                @Override
                protected Component createNextButton() {
                    Component c = createArrow(SwingConstants.NORTH);
                    c.setName("Spinner.nextButton");
                    installNextButtonListeners(c);
                    return c;
                }
                @Override
                protected Component createPreviousButton() {
                    Component c = createArrow(SwingConstants.SOUTH);
                    c.setName("Spinner.previousButton");
                    installPreviousButtonListeners(c);
                    return c;
                }
                
                private Component createArrow(int direction) {
                    JButton btn = new JButton();
                    btn.setBorder(BorderFactory.createEmptyBorder());
                    btn.setContentAreaFilled(false);
                    btn.setPreferredSize(new Dimension(24, 16));
                    btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    btn.setIcon(new Icon() {
                        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(UIConstants.TEXT_MUTED);
                            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            int w = 8, h = 5;
                            int sx = (c.getWidth() - w) / 2;
                            int sy = (c.getHeight() - h) / 2;
                            if (direction == SwingConstants.NORTH) { 
                                g2.drawLine(sx, sy + h, sx + w/2, sy); g2.drawLine(sx + w/2, sy, sx + w, sy + h);
                            } else {
                                g2.drawLine(sx, sy, sx + w/2, sy + h); g2.drawLine(sx + w/2, sy + h, sx + w, sy);
                            }
                            g2.dispose();
                        }
                        @Override public int getIconWidth() { return 12; }
                        @Override public int getIconHeight() { return 12; }
                    });
                    return btn;
                }
            });
            
            JComponent editor = getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                JTextField txt = ((JSpinner.DefaultEditor) editor).getTextField();
                txt.setBackground(Color.WHITE);
                txt.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 0));
                txt.addFocusListener(new FocusAdapter() {
                    @Override public void focusGained(FocusEvent e) {
                        setBorder(BorderFactory.createCompoundBorder(
                            new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER_FOCUS),
                            BorderFactory.createEmptyBorder(2, 2, 2, 2)));
                    }
                    @Override public void focusLost(FocusEvent e) {
                        setBorder(BorderFactory.createCompoundBorder(
                            new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
                            BorderFactory.createEmptyBorder(2, 2, 2, 2)));
                    }
                });
            }
        }
    }

    // ============================================================
    // ModernComboBox - dropdown đẹp mắt
    // ============================================================
    public static class ModernComboBox<T> extends JComboBox<T> {
        public ModernComboBox(T[] items) {
            super(items);
            setFont(UIConstants.FONT_BODY);
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(2, 12, 2, 12)));
            setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
                @Override
                protected JButton createArrowButton() {
                    JButton btn = new JButton();
                    btn.setBorder(BorderFactory.createEmptyBorder());
                    btn.setContentAreaFilled(false);
                    btn.setIcon(new Icon() {
                        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(UIConstants.TEXT_MUTED);
                            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            int w = 8;
                            int h = 4;
                            int sx = x + (getIconWidth() - w) / 2;
                            int sy = y + (getIconHeight() - h) / 2;
                            g2.drawLine(sx, sy, sx + w/2, sy + h);
                            g2.drawLine(sx + w/2, sy + h, sx + w, sy);
                            g2.dispose();
                        }
                        @Override public int getIconWidth() { return 14; }
                        @Override public int getIconHeight() { return 14; }
                    });
                    return btn;
                }
            });
            
            // Add focus effect
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER_FOCUS),
                        BorderFactory.createEmptyBorder(2, 12, 2, 12)));
                }
                @Override
                public void focusLost(FocusEvent e) {
                    setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
                        BorderFactory.createEmptyBorder(2, 12, 2, 12)));
                }
            });
        }

        // Empty constructor
        public ModernComboBox() {
            this((T[]) new Object[0]);
        }
    }

    // ============================================================
    // StatusBadge - pill badge cho trạng thái
    // ============================================================
    public static class StatusBadge extends JLabel {
        public StatusBadge(String text, Color bg, Color fg) {
            super(text);
            setFont(UIConstants.FONT_SMALL_BOLD);
            setForeground(fg);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ============================================================
    // RoundedBorder helper
    // ============================================================
    public static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;

        public RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color  = color;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new RoundRectangle2D.Float(x + 0.6f, y + 0.6f, w - 1.2f, h - 1.2f, radius, radius));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) { return new Insets(radius/2, radius, radius/2, radius); }
    }

    // ============================================================
    // Stat Card (cho dashboard)
    // ============================================================
    public static RoundedPanel createStatCard(String label, String value, Color accentColor) {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(UIConstants.BG_CARD);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(UIConstants.FONT_SMALL);
        lblLabel.setForeground(UIConstants.TEXT_SECONDARY);

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(UIConstants.FONT_CARD_NUM);
        lblValue.setForeground(UIConstants.TEXT_PRIMARY);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(lblLabel, BorderLayout.NORTH);
        top.add(lblValue, BorderLayout.CENTER);

        card.add(top, BorderLayout.CENTER);
        return card;
    }

    // ---- Factory helpers ----
    public static RoundedButton primaryButton(String text) {
        return new RoundedButton(text, UIConstants.PRIMARY, Color.WHITE);
    }

    public static RoundedButton successButton(String text) {
        return new RoundedButton(text, UIConstants.SUCCESS, Color.WHITE);
    }

    public static RoundedButton dangerButton(String text) {
        return new RoundedButton(text, UIConstants.DANGER, Color.WHITE);
    }

    public static RoundedButton grayButton(String text) {
        return new RoundedButton(text, new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
    }
}

