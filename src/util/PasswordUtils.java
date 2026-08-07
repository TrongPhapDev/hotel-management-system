package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * Tiện ích hash mật khẩu.
 * Sử dụng SHA-256 + Salt.
 * (Trong dự án thực tế nên dùng BCrypt, nhưng SHA-256 đủ cho đồ án học thuật
 *  và không cần thêm thư viện bên ngoài.)
 */
public class PasswordUtils {

    private static final Logger LOGGER = Logger.getLogger(PasswordUtils.class.getName());
    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;

    /**
     * Tạo salt ngẫu nhiên.
     */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hash mật khẩu với salt.
     * Kết quả: salt$hash (dùng $ làm separator)
     */
    public static String hashPassword(String password) {
        String salt = generateSalt();
        return hashPassword(password, salt);
    }

    /**
     * Hash mật khẩu với salt cho trước.
     */
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            String salted = salt + password;
            byte[] hash = md.digest(salted.getBytes(StandardCharsets.UTF_8));
            String hashStr = Base64.getEncoder().encodeToString(hash);
            return salt + "$" + hashStr;
        } catch (NoSuchAlgorithmException e) {
            LOGGER.severe("Lỗi hash mật khẩu: " + e.getMessage());
            throw new RuntimeException("Không hỗ trợ thuật toán " + ALGORITHM, e);
        }
    }

    /**
     * Kiểm tra mật khẩu có khớp với hash đã lưu không.
     * @param password  Mật khẩu người dùng nhập
     * @param storedHash  Hash đã lưu trong DB (dạng salt$hash)
     * @return true nếu khớp
     */
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null) return false;

        // Hỗ trợ legacy: nếu hash không chứa '$' thì đây là plaintext cũ → so sánh trực tiếp
        if (!storedHash.contains("$")) {
            return password.equals(storedHash);
        }

        String salt = storedHash.substring(0, storedHash.indexOf('$'));
        String newHash = hashPassword(password, salt);
        return newHash.equals(storedHash);
    }

    /**
     * Kiểm tra xem mật khẩu đã được hash chưa (có chứa '$' separator).
     */
    public static boolean isHashed(String password) {
        return password != null && password.contains("$") && password.length() > 30;
    }
}
