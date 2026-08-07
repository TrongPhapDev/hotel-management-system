package ui.components;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý thông báo đẩy (Push Notifications/Toasts) trong ứng dụng.
 * Được thiết kế với phong cách premium, hiển thị ở góc trên bên phải.
 */
public class NotificationManager {

    private static final int TOAST_WIDTH = 340;
    private static final int TOAST_HEIGHT = 70;
    private static final int PADDING_X = 25;
    private static final int PADDING_Y = 25;
    private static final int DISPLAY_TIME_MS = 4500;

    private static final List<NotificationToast> activeToasts = new ArrayList<>();
    private static JLayeredPane rootPane;

    public static void init(JFrame frame) {
        rootPane = frame.getLayeredPane();
    }

    public static void showNotification(String title, String message, Color typeColor, String icon) {
        if (rootPane == null) return;

        NotificationToast toast = new NotificationToast(title, message, typeColor, icon != null ? icon : "ⓘ");
        activeToasts.add(0, toast); // Thêm vào đầu danh sách để đẩy các thông báo cũ xuống
        rootPane.add(toast, JLayeredPane.POPUP_LAYER);
        
        updateToastPositions();
        
        // Timer to fade out and remove
        Timer timer = new Timer(DISPLAY_TIME_MS, e -> {
            fadeOutToast(toast);
        });
        timer.setRepeats(false);
        timer.start();
    }

    public static void showInfo(String title, String message) {
        showNotification(title, message, UIConstants.INFO, "ℹ");
    }

    public static void showWarning(String title, String message) {
        showNotification(title, message, UIConstants.WARNING, "⚠");
    }

    public static void showError(String title, String message) {
        showNotification(title, message, UIConstants.DANGER, "✖");
    }

    public static void showSuccess(String title, String message) {
        showNotification(title, message, UIConstants.SUCCESS, "✔");
    }

    private static void updateToastPositions() {
        if (rootPane == null) return;
        
        int x = rootPane.getWidth() - TOAST_WIDTH - PADDING_X;
        int y = PADDING_Y;

        for (NotificationToast toast : activeToasts) {
            toast.setBounds(x, y, TOAST_WIDTH, TOAST_HEIGHT);
            y += TOAST_HEIGHT + 15; // Khoảng cách giữa các toast
        }
    }

    private static void fadeOutToast(NotificationToast toast) {
        Timer timer = new Timer(15, null);
        timer.addActionListener(e -> {
            float alpha = toast.getAlpha() - 0.05f;
            if (alpha <= 0) {
                timer.stop();
                rootPane.remove(toast);
                activeToasts.remove(toast);
                rootPane.repaint();
                updateToastPositions();
            } else {
                toast.setAlpha(alpha);
                toast.repaint();
            }
        });
        timer.start();
    }

    private static class NotificationToast extends JPanel {
        private final String title;
        private final String message;
        private final Color accentColor;
        private final String icon;
        private float alpha = 1.0f;

        public NotificationToast(String title, String message, Color accentColor, String icon) {
            this.title = title;
            this.message = message;
            this.accentColor = accentColor;
            this.icon = icon;
            setOpaque(false);
            setLayout(null); // Sử dụng null layout để vẽ tự do
        }

        public float getAlpha() { return alpha; }
        public void setAlpha(float alpha) { this.alpha = alpha; }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            // Shadow effect
            g2.setColor(new Color(0, 0, 0, 30));
            g2.fillRoundRect(3, 3, getWidth() - 6, getHeight() - 6, 15, 15);
            g2.setColor(new Color(0, 0, 0, 15));
            g2.fillRoundRect(5, 5, getWidth() - 10, getHeight() - 10, 15, 15);

            // Background
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 6, getHeight() - 6, 15, 15);

            // Border
            g2.setColor(new Color(240, 240, 240));
            g2.drawRoundRect(0, 0, getWidth() - 6, getHeight() - 6, 15, 15);

            // Left Color Bar
            g2.setColor(accentColor);
            g2.fillRoundRect(0, 0, 8, getHeight() - 6, 15, 15);
            g2.fillRect(4, 0, 4, getHeight() - 6);

            // Icon Circle
            g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 30));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.8f));
            g2.fillOval(20, (getHeight() - 6 - 36) / 2, 36, 36);
            
            // Icon Text
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(accentColor);
            g2.setFont(new Font("Segoe UI Emoji", Font.BOLD, 18));
            FontMetrics fmIcon = g2.getFontMetrics();
            int iconX = 20 + (36 - fmIcon.stringWidth(icon)) / 2;
            int iconY = (getHeight() - 6 - 36) / 2 + (36 + fmIcon.getAscent() - fmIcon.getDescent()) / 2 - 2;
            g2.drawString(icon, iconX, iconY);

            // Title
            g2.setColor(UIConstants.TEXT_PRIMARY);
            g2.setFont(UIConstants.FONT_HEADER);
            g2.drawString(title, 70, 28);

            // Message
            g2.setColor(UIConstants.TEXT_SECONDARY);
            g2.setFont(UIConstants.FONT_SMALL);
            // Cắt ngắn message nếu quá dài
            String displayMsg = message;
            if (message.length() > 50) displayMsg = message.substring(0, 47) + "...";
            g2.drawString(displayMsg, 70, 48);

            g2.dispose();
        }
    }
}

