import java.sql.*;

public class SqlRunner {
    public static void main(String[] args) {
        String url = "jdbc:sqlserver://localhost:1433;databaseName=HotelMs;encrypt=false;trustServerCertificate=true";
        String user = "sa";
        String pass = "sapassword";
        
        if (args.length == 0) {
            System.out.println("Usage: SqlRunner \"QUERY\"");
            return;
        }
        
        String sql = args[0];
        
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {
            
            if (sql.trim().toUpperCase().startsWith("SELECT")) {
                ResultSet rs = stmt.executeQuery(sql);
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                
                for (int i = 1; i <= cols; i++) {
                    System.out.print(md.getColumnName(i) + "\t");
                }
                System.out.println("\n--------------------------------------------------");
                
                while (rs.next()) {
                    for (int i = 1; i <= cols; i++) {
                        System.out.print(rs.getString(i) + "\t");
                    }
                    System.out.println();
                }
            } else {
                int rows = stmt.executeUpdate(sql);
                System.out.println("Rows affected: " + rows);
            }
            
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
