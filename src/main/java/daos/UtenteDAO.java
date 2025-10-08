package daos;

import beans.Playlist;
import beans.Utente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;


public class UtenteDAO {
    private Connection connection;

    public UtenteDAO(Connection connection) {
        this.connection = connection;
    }


    public void creaUtente(String username, String nome, String cognome, String password) throws SQLException {
        String query = "SELECT COUNT(*) AS total FROM utente WHERE username = ?";
        int count;
        try (PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setString(1, username);
            try (ResultSet result = pstatement.executeQuery();) {
                result.next();
                count = result.getInt("total");
            }
        }
        if(count > 0) {
            throw new SQLException("Username già occupato");
        }

        query = "INSERT INTO utente (username, nome, cognome, password) VALUES (?, ?, ?, ?)";
        try(PreparedStatement pstatement = connection.prepareStatement(query);){
            pstatement.setString(1, username);
            pstatement.setString(2, nome);
            pstatement.setString(3, cognome);
            pstatement.setString(4, password);

            // QUESTA RIGA MANCAVA - executeUpdate() per eseguire l'INSERT!
            int rowsAffected = pstatement.executeUpdate();

            // Opzionale: verifica che l'inserimento sia andato a buon fine
            if (rowsAffected == 0) {
                throw new SQLException("Creazione utente fallita, nessuna riga inserita.");
            }
        }
    }

    public Utente controlloAutenticazione(String username, String password) throws SQLException {
        String query = "SELECT * FROM utente WHERE username = ? AND password = ?";
        try (PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setString(1, username);
            pstatement.setString(2, password);
            try (ResultSet result = pstatement.executeQuery();) {
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

    //DA ELIMINARE PROBABILMENTE
    /*public void eliminaUtente(String username) throws SQLException {
        int userId = -1;
        String query = "DELETE FROM utente WHERE username = ?";
        try (PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setString(1, username);
            try (ResultSet rs = pstatement.executeQuery()) {
                if (rs.next()) {
                    userId = rs.getInt("id");
                }
            }
        }

        if (userId == -1) {
            throw new SQLException("Utente non trovato");
        }

        query = "DELETE FROM brano WHERE id_utente = ?";
        try (PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setInt(1, userId);
            pstatement.executeUpdate();
        }

        query = "DELETE FROM playlist WHERE id_utente = ?";
        try (PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setInt(1, userId);
            pstatement.executeUpdate();
        }

        query = "DELETE FROM utente WHERE id_utente = ?";
        try (PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setInt(1, userId);
            pstatement.executeUpdate();
        }
    }*/

    public Utente getUtente(int id_utente) throws SQLException {
        String query = "SELECT * FROM utente WHERE id_utente = ?";
        try (PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setInt(1, id_utente);
            try (ResultSet result = pstatement.executeQuery();) {
                if (result.next()) {
                    Utente utente = new Utente();
                    utente.setId_utente(id_utente);
                    utente.setUsername(result.getString("username"));
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




    public ArrayList<Playlist> playlistUtente(Utente utente) throws SQLException {
        int userId = -1;
        String query = "SELECT id_utente FROM utente WHERE username = ?";
        try (PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setString(1, utente.getUsername());
            try (ResultSet rs = pstatement.executeQuery()) {
                if (rs.next()) {
                    userId = rs.getInt("id_utente");
                }
            }
        }

        if (userId == -1) {
            throw new SQLException("Utente non trovato");
        }
        ArrayList<Playlist> playlists = new ArrayList<>();
        query = "SELECT id_playlist, titolo, data_creazione FROM playlist WHERE id_utente = ? ORDER BY playlist.data_creazione DESC";
        try (PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setInt(1, userId);
            try (ResultSet result = pstatement.executeQuery();) {
                while (result.next()) {
                    Playlist playlist = new Playlist();
                    playlist.setId(result.getInt("id_playlist"));
                    playlist.setId_utente(userId);
                    playlist.setTitolo(result.getString("titolo"));
                    playlist.setData_creazione(result.getTimestamp("data_creazione").toLocalDateTime());
                    playlists.add(playlist);
                }
            }
        }
        return playlists;
    }

    //DA RIFARE
    /*public ArrayList<Brano> getBraniUtente(int id_utente) throws SQLException {
        String query = "SELECT id_brano, titolo FROM brano WHERE id_utente = ? ORDER BY brano.anno_pubblicazione DESC";
        ArrayList<Brano> brani = new ArrayList<>();
        try(PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setInt(1, id_utente);
            try (ResultSet result = pstatement.executeQuery();) {
                while (result.next()) {
                    Brano brano = new Brano();
                    brano.setId(result.getInt("id_brano"));
                    brano.setTitolo(result.getString("titolo"));
                    brano.setId_utente(id_utente);
                    brano.setImmagine_album(Base64.getEncoder().encodeToString(result.getBytes("immagine_album")));

                    brani.add(brano);
                }
            }
        }
        return brani;
    }*/
}