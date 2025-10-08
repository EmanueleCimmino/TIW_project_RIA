package daos;

import beans.Brano;
import beans.Playlist;
import beans.Utente;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PlaylistDAO {
    private Connection connection;
    public PlaylistDAO(Connection connection) {
        this.connection = connection;
    }
    public void creaPlaylist(String titolo, List<Brano> brani, Utente utente) throws SQLException {
        connection.setAutoCommit(false);
        String query = "INSERT INTO playlist (titolo, id_utente, data_creazione) VALUES (?,?,?)";
        int id_playlist;
        try (PreparedStatement pstatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);) {
            pstatement.setString(1, titolo);
            pstatement.setInt(2, utente.getId_utente());
            pstatement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            pstatement.executeUpdate();
            ResultSet result = pstatement.getGeneratedKeys();
            result.next();
            id_playlist = result.getInt(1);
            result.close();
        }

        if(!brani.isEmpty()) {
            Playlist playlist = getPlaylistDalTitolo(titolo);
            BranoDAO branoDAO = new BranoDAO(this.connection);
            try {
                for(Brano brano : brani) {
                    branoDAO.aggiungiBranoAllaPlaylist(brano, playlist, utente);
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
        else {
            connection.commit();
            connection.setAutoCommit(true);
        }
    }

    public void rimuoviBranoDaPlaylist(int id_brano, int id_playlist, int id_utente) throws SQLException {
        this.controllaUtentePlaylist(id_playlist, id_utente);
        BranoDAO branoDAO = new BranoDAO(this.connection);
        branoDAO.controllaUtenteBrano(id_brano, id_utente);

        String query = "DELETE FROM playlist_brano WHERE id_brano = ? AND id_playlist = ? AND id_utente = ?";
        try (PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setInt(1, id_brano);
            pstatement.setInt(2, id_playlist);
            pstatement.setInt(3, id_utente);
            pstatement.executeUpdate();
        }
        catch (SQLException e) {
            connection.rollback();
            throw e;
        }

    }

    public String getTitoloDallaPlaylist(int id_playlist, int id_utente) throws SQLException {
        this.controllaUtentePlaylist(id_playlist, id_utente);

        String query = "SELECT titolo FROM playlist WHERE id_playlist = ? AND id_utente = ?";
        String titolo = null;
        try (PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setInt(1, id_playlist);
            pstatement.setInt(2, id_utente);
            try (ResultSet result = pstatement.executeQuery();) {
                if (result.next()) {
                    titolo = result.getString("name");
                }
            }
        }
        catch (SQLException e) {
            connection.rollback();
            throw e;
        }
        return titolo;
    }

    /* NON NECESSARIO
    public void eliminaPlaylist(int id_playlist, int id_utente) throws SQLException {
        this.controllaUtentePlaylist(id_playlist, id_utente);
        String query = "DELETE FROM playlist_brano WHERE id_playlist = ? AND id_utente = ?";
        try (PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setInt(1, id_playlist);
            pstatement.setInt(2, id_utente);
            pstatement.executeUpdate();
        }
        catch (SQLException e){
            connection.rollback();
            throw e;
        }

        query = "DELETE FROM playlist WHERE id_playlist = ? AND id_utente = ?";
        try (PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setInt(1, id_playlist);
            pstatement.setInt(2, id_utente);
            pstatement.executeUpdate();
        }
        catch (SQLException e){
            connection.rollback();
            throw e;
        }
    }
    */

    public ArrayList<Brano> getBraniNonInPlaylist(int id_utente, int id_playlist) throws SQLException {
        this.controllaUtentePlaylist(id_playlist, id_utente);

        String query = "(SELECT id_brano, titolo FROM brano WHERE id_utente = ?) EXCEPT (SELECT id_brano, titolo FROM brano WHERE brano.id_brano IN (SELECT id_brano FROM playlist_brano WHERE id_playlist = ? AND brano.id_utente = ?))";
        ArrayList<Brano> brani = new ArrayList<>();
        try(PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setInt(1, id_utente);
            pstatement.setInt(2, id_playlist);
            pstatement.setInt(3, id_utente);
            try (ResultSet result = pstatement.executeQuery();) {
                while (result.next()) {
                    Brano brano = new Brano();
                    brano.setId(result.getInt("id_brano"));
                    brano.setTitolo(result.getString("titolo"));
                    brano.setId_utente(id_utente);
                    brani.add(brano);
                }
            }
        }
        catch (SQLException e){
            connection.rollback();
            throw e;
        }
        return brani;
    }

    public void controllaUtentePlaylist(int id_playlist, int id_utente) throws SQLException {
        String query = "SELECT COUNT(*) AS total FROM playlist WHERE id_playlist = ? AND id_utente = ?";
        int count;
        try (PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setInt(1, id_playlist);
            pstatement.setInt(2, id_utente);
            try (ResultSet result = pstatement.executeQuery();) {
                result.next();
                count = result.getInt("total");
            }
        }
        if(count <= 0) {
            throw new SQLException("Errore sulla playlist richiesta");
        }
    }

    public Playlist getPlaylistDalTitolo(String titolo) throws SQLException {
        String query = "SELECT id_playlist, id_utente, titolo, data_creazione FROM playlist WHERE titolo = ?";
        Playlist playlist = null;
        try (PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setString(1, titolo);
            try (ResultSet result = pstatement.executeQuery();) {
                if (result.next()) {
                    playlist=new Playlist();
                    playlist.setId(result.getInt("id_playlist"));
                    playlist.setId_utente(result.getInt("id_utente"));
                    playlist.setTitolo(result.getString("titolo"));
                    playlist.setData_creazione(result.getTimestamp("data_creazione").toLocalDateTime());
                }
            }
        }
        catch (SQLException e) {
            connection.rollback();
            throw e;
        }
        return playlist;
    }

    public List<Playlist> getPlaylistDaUtente(int id_utente) throws SQLException {
        String query = "SELECT id_playlist, id_utente, titolo, data_creazione FROM playlist " +
                "WHERE id_utente = ? ORDER BY data_creazione DESC";
        List<Playlist> playlists = new ArrayList<>();

        try (PreparedStatement pstatement = connection.prepareStatement(query)) {
            pstatement.setInt(1, id_utente);

            try (ResultSet result = pstatement.executeQuery()) {
                while (result.next()) {
                    Playlist playlist = new Playlist();
                    playlist.setId(result.getInt("id_playlist"));
                    playlist.setId_utente(result.getInt("id_utente"));
                    playlist.setTitolo(result.getString("titolo"));
                    playlist.setData_creazione(result.getTimestamp("data_creazione").toLocalDateTime());
                    playlists.add(playlist);
                }
            }
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }

        return playlists;
    }

    public Playlist getPlaylist(int id_playlist) throws SQLException {
        String query = "SELECT * FROM playlist WHERE id_playlist = ?";
        Playlist playlist = new Playlist();
        try (PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setInt(1, id_playlist);
            try (ResultSet result = pstatement.executeQuery();) {
                if (result.next()) {
                    playlist.setId(id_playlist);
                    playlist.setId_utente(result.getInt("id_utente"));
                    playlist.setTitolo(result.getString("titolo"));
                    playlist.setData_creazione(result.getTimestamp("data_creazione").toLocalDateTime());
                }
            }
        }
        catch (SQLException e) {
            connection.rollback();
            throw e;
        }
        return playlist;
    }
}
