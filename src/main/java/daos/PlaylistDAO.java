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

        try (PreparedStatement pstatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstatement.setString(1, titolo);
            pstatement.setInt(2, utente.getId_utente());
            pstatement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            pstatement.executeUpdate();
            ResultSet result = pstatement.getGeneratedKeys();
            result.next();
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


    public void controllaUtentePlaylist(int id_playlist, int id_utente) throws SQLException {
        String query = "SELECT COUNT(*) AS total FROM playlist WHERE id_playlist = ? AND id_utente = ?";

        int count;
        try (PreparedStatement pstatement = connection.prepareStatement(query)) {
            pstatement.setInt(1, id_playlist);
            pstatement.setInt(2, id_utente);
            try (ResultSet result = pstatement.executeQuery()) {
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
        try (PreparedStatement pstatement = connection.prepareStatement(query)) {
            pstatement.setString(1, titolo);
            try (ResultSet result = pstatement.executeQuery()) {
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
        try (PreparedStatement pstatement = connection.prepareStatement(query)) {
            pstatement.setInt(1, id_playlist);
            try (ResultSet result = pstatement.executeQuery()) {
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

    public void salvaOrdinamentoPlaylist(int id_playlist, int id_utente, int[] idBraniOrdinati)
            throws SQLException {
        String query = "UPDATE playlist_brano SET ordinamento = ? " +
                "WHERE id_brano = ? AND id_playlist = ? AND id_utente = ?";

        connection.setAutoCommit(false);
        try (PreparedStatement pstatement = connection.prepareStatement(query)) {
            for (int i = 0; i < idBraniOrdinati.length; i++) {
                pstatement.setInt(1, i + 1);
                pstatement.setInt(2, idBraniOrdinati[i]);
                pstatement.setInt(3, id_playlist);
                pstatement.setInt(4, id_utente);
                pstatement.addBatch();
            }

            pstatement.executeBatch();
            connection.commit();

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }
}