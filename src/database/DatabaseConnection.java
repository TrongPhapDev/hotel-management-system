package database;

import java.io.*;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quản lý kết nối SQL Server.
 * - Singleton pattern
 * - Đọc cấu hình từ resources/db.properties (không hard-code)
 * - Hỗ trợ Transaction management
 */
public class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

    private static DatabaseConnection instance;
    private Connection connection;

    // Cấu hình đọc từ file properties
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private String dbDriver;

    private DatabaseConnection() {
        loadProperties();
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /**
     * Đọc cấu hình từ resources/db.properties
     */
    private void loadProperties() {
        Properties props = new Properties();
        // Thử đọc từ nhiều vị trí
        String[] paths = {
            "resources/db.properties",
            "../resources/db.properties",
            "db.properties"
        };
        
        boolean loaded = false;
        for (String path : paths) {
            File file = new File(path);
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    props.load(fis);
                    loaded = true;
                    LOGGER.info("Đọc cấu hình DB từ: " + file.getAbsolutePath());
                    break;
                } catch (IOException e) {
                    LOGGER.warning("Không đọc được file: " + path);
                }
            }
        }

        // Fallback: thử đọc từ classpath
        if (!loaded) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("db.properties")) {
                if (is != null) {
                    props.load(is);
                    loaded = true;
                    LOGGER.info("Đọc cấu hình DB từ classpath");
                }
            } catch (IOException e) {
                LOGGER.warning("Không đọc được từ classpath");
            }
        }

        if (!loaded) {
            LOGGER.warning("Không tìm thấy db.properties — dùng giá trị mặc định");
        }

        // Đọc giá trị với fallback
        this.dbUrl = props.getProperty("db.url",
            "jdbc:sqlserver://localhost:1433;databaseName=HotelMs;encrypt=false;trustServerCertificate=true");
        this.dbUser = props.getProperty("db.user", "sa");
        this.dbPassword = props.getProperty("db.password", "sapassword");
        this.dbDriver = props.getProperty("db.driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
    }

    public synchronized Connection getConnection() throws SQLException {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName(dbDriver);
                DriverManager.setLoginTimeout(5);
                connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                LOGGER.info("Kết nối SQL Server thành công");
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQL Server JDBC Driver không tìm thấy: " + e.getMessage());
        }
        return connection;
    }

    /**
     * Bắt đầu transaction (tắt auto-commit).
     * Dùng cho các thao tác cần đảm bảo tính nhất quán (check-out, tạo hóa đơn, v.v.)
     */
    public void beginTransaction() throws SQLException {
        getConnection().setAutoCommit(false);
    }

    /**
     * Commit transaction.
     */
    public void commitTransaction() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.commit();
            connection.setAutoCommit(true);
        }
    }

    /**
     * Rollback transaction khi có lỗi.
     */
    public void rollbackTransaction() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi rollback transaction", e);
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Lỗi đóng kết nối", e);
        }
    }

    public boolean testConnection() {
        try {
            getConnection();
            LOGGER.info("✓ Kết nối SQL Server thành công!");
            LOGGER.info("  URL: " + dbUrl);
            LOGGER.info("  User: " + dbUser);
            return true;
        } catch (SQLException e) {
            LOGGER.severe("✗ Kết nối thất bại: " + e.getMessage());
            return false;
        }
    }
}