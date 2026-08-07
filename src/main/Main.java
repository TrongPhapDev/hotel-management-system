package main;

import ui.LoginFrame;
import database.DatabaseConnection;
import javax.swing.*;

/**
 * ỨNG DỤNG QUẢN LÝ KHÁCH SẠN
 * Đây là file chạy chính (GUI Entry Point)
 * Hướng dẫn: Chạy file này để mở giao diện Đăng nhập.
 */
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("Button.focus", new java.awt.Color(0, 0, 0, 0));
        } catch (Exception e) {
            // fallback to default
        }

        SwingUtilities.invokeLater(() -> {
            if (!DatabaseConnection.getInstance().testConnection()) {
                int choice = JOptionPane.showConfirmDialog(
                        null,
                        "Không thể kết nối SQL Server!\n\n" +
                                "Vui lòng kiểm tra:\n" +
                                "  • SQL Server đang chạy\n" +
                                "  • Database 'HotelMS' đã được tạo\n" +
                                "  • Cấu hình trong DatabaseConnection.java\n\n" +
                                "Tiếp tục ở chế độ demo (không có DB)?",
                        "Lỗi kết nối",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (choice == JOptionPane.NO_OPTION) {
                    System.exit(0);
                    return;
                }
            }
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });
    }
}
