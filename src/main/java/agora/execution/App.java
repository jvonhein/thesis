package agora.execution;

import java.sql.*;

public class App {
    private final String url = "jdbc:postgresql://localhost:5433/dvdrental";
    private final String user = "postgres";
    private final String password = "agora";

    public static void main(String[] args){
        App app = new App();
        app.connect();
        System.out.println(app.getActorCount());
    }

    public Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to the PostgreSQL server successfully.");
        } catch (SQLException e){
            System.out.println(e.getMessage());
        }

        return conn;
    }

    /**
     * Get actors count
     * @return
     */
    public int getActorCount() {
        String SQL = "SELECT count(*) FROM actor";
        int count = 0;

        try {
            Connection conn = connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL);
            rs.next();
            count = rs.getInt(1);
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

        return count;
    }
}
