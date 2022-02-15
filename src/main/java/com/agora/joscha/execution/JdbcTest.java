package com.agora.joscha.execution;

import java.sql.*;

public class JdbcTest {
    private final String url = "jdbc:postgresql://postgres:5432/db1";
    private final String user = "odbc_user";
    private final String password = "password";

    public static void main(String[] args){
        JdbcTest app = new JdbcTest();
        app.selectHealthTest();
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
    public void selectHealthTest() {
        String SQL = "SELECT bundesland, population_total, vaccinated_firstshot FROM health";

        try {
            Connection conn = connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL);
            while(rs.next()){
                System.out.println(rs.getString(0) + " | " + rs.getInt(1) + " | " + rs.getInt(2));
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
