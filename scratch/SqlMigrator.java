import java.sql.*;

public class SqlMigrator {
    public static void main(String[] args) {
        String url = "jdbc:sqlserver://localhost:1433;databaseName=HotelMs;encrypt=false;trustServerCertificate=true";
        String user = "sa";
        String pass = "sapassword";
        
        String sql = "ALTER TABLE KhuyenMai ADD giaTriGiamToiDa FLOAT DEFAULT 0;";
        
        System.out.println("Connecting to database...");
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Executing: " + sql);
            stmt.execute(sql);
            System.out.println("Success! Column giaTriGiamToiDa added to KhuyenMai table.");
            
        } catch (SQLException e) {
            if (e.getMessage().contains("already exists") || e.getErrorCode() == 2705) {
                System.out.println("Note: Column giaTriGiamToiDa already exists. Skipping.");
            } else {
                System.err.println("Error executing SQL: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
