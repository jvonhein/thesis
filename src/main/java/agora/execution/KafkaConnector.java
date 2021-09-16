package agora.execution;

import java.sql.*;

public class KafkaConnector {
    final static String KAFKA_JDBC_CONNECTION_STRING = "jdbc:apachekafka:BootstrapServers=\"localhost:9092\";Topic=\"quickstart-events\"";

    public static void main(String[] args) throws SQLException {

        Connection conn = DriverManager.getConnection(KAFKA_JDBC_CONNECTION_STRING);

        Statement stat = conn.createStatement();
        boolean ret = stat.execute("SELECT * FROM quickstart-events");
        ResultSet rs = stat.getResultSet();

        System.out.println(rs);
    }
}
