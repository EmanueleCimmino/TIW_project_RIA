package utils;

import jakarta.servlet.ServletContext;
import jakarta.servlet.UnavailableException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionHandler {

    public static Connection getConnection(ServletContext context) throws UnavailableException {
        Connection connection = null;
        try {
            String driver = context.getInitParameter("db.driver");
            String url = context.getInitParameter("db.url");
            String user = context.getInitParameter("db.user");
            String password = context.getInitParameter("db.password");

            if (driver == null) {
                throw new UnavailableException("Parametro db.driver non trovato nel web.xml");
            }
            if (url == null) {
                throw new UnavailableException("Parametro db.url non trovato nel web.xml");
            }
            if (user == null) {
                throw new UnavailableException("Parametro db.user non trovato nel web.xml");
            }
            if (password == null) {
                throw new UnavailableException("Parametro db.password non trovato nel web.xml");
            }

            Class.forName(driver);

            connection = DriverManager.getConnection(url, user, password);

        } catch (ClassNotFoundException e) {
            System.err.println("ERRORE ClassNotFoundException: " + e.getMessage());
            e.printStackTrace();
            throw new UnavailableException("Non riesco a caricare il database driver: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("ERRORE SQLException: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("ErrorCode: " + e.getErrorCode());
            e.printStackTrace();
            throw new UnavailableException("Non riesco a connettermi al database: " + e.getMessage());
        }
        return connection;
    }

    public static void closeConnection(Connection connection) throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}