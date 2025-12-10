package daos;

import beans.Utente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UtenteDAO {
    private Connection connection;

    public UtenteDAO(Connection connection) {
        this.connection = connection;
    }

    public void creaUtente(String username, String nome, String cognome, String password) throws SQLException {
        String query = "SELECT COUNT(*) AS total FROM utente WHERE username = ?";
        int count;
        try (PreparedStatement pstatement = connection.prepareStatement(query)) {
            pstatement.setString(1, username);
            try (ResultSet result = pstatement.executeQuery()) {
                result.next();
                count = result.getInt("total");
            }
        }
        if(count > 0) {
            throw new SQLException("Username già occupato");
        }

        query = "INSERT INTO utente (username, nome, cognome, password) VALUES (?, ?, ?, ?)";
        try(PreparedStatement pstatement = connection.prepareStatement(query)){
            pstatement.setString(1, username);
            pstatement.setString(2, nome);
            pstatement.setString(3, cognome);
            pstatement.setString(4, password);

            int righe = pstatement.executeUpdate();

            if (righe == 0) {
                throw new SQLException("Creazione utente fallita, nessuna riga inserita.");
            }
        }
    }

    public Utente controlloAutenticazione(String username, String password) throws SQLException {
        String query = "SELECT * FROM utente WHERE username = ? AND password = ?";
        try (PreparedStatement pstatement = connection.prepareStatement(query)) {
            pstatement.setString(1, username);
            pstatement.setString(2, password);
            try (ResultSet result = pstatement.executeQuery()) {
                if (result.next()) {
                    Utente utente = new Utente();
                    utente.setId_utente(result.getInt("id_utente"));
                    utente.setUsername(username);
                    utente.setNome(result.getString("nome"));
                    utente.setCognome(result.getString("cognome"));
                    utente.setPassword(result.getString("password"));
                    return utente;
                }
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    return null;
}

    public Utente getUtenteDaUsername(String username) throws SQLException {
        String query = "SELECT id_utente, username, nome, cognome FROM utente WHERE username = ?";
        try (PreparedStatement pstatement = connection.prepareStatement(query)) {
            pstatement.setString(1, username);
            try (ResultSet result = pstatement.executeQuery()) {
                if (result.next()) {
                    Utente utente = new Utente();
                    utente.setId_utente(result.getInt("id_utente"));
                    utente.setUsername(result.getString("username"));
                    utente.setNome(result.getString("nome"));
                    utente.setCognome(result.getString("cognome"));
                    return utente;
                }
                return null;
            }
        }
    }
}