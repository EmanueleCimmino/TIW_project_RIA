package daos;

import beans.Brano;
import beans.Playlist;
import beans.Utente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BranoDAO {
    private Connection connection;

    public BranoDAO(Connection connection) {
        this.connection = connection;
    }

    public void caricaBrano(String titolo, String file_musicale, int id_album, int id_utente) throws SQLException{
        String query = "INSERT INTO brano (titolo, file_musicale, id_album, id_utente) VALUES (?,?,?,?)";

        try (PreparedStatement pstatement = connection.prepareStatement(query)) {
            pstatement.setString(1, titolo);
            pstatement.setString(2, file_musicale);
            pstatement.setInt(3, id_album);
            pstatement.setInt(4, id_utente);
            pstatement.executeUpdate();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Brano getBrano(int id_brano) throws SQLException {
        String query = "SELECT * FROM brano WHERE id_brano = ?";

        try(PreparedStatement pstatement = connection.prepareStatement(query)) {
            pstatement.setInt(1, id_brano);
            try (ResultSet result = pstatement.executeQuery()) {
                if (result.next()) {
                    Brano brano = new Brano();
                    brano.setId(id_brano);
                    brano.setTitolo(result.getString("titolo"));
                    brano.setFile_musicale(result.getString("file_musicale"));
                    brano.setId_utente(result.getInt("id_utente"));
                    brano.setId_album(result.getInt("id_album"));
                    return brano;
                }
            }
        }
        catch (SQLException e){
            e.printStackTrace();
            throw e;
        }
        return null;
    }

    public void aggiungiBranoAllaPlaylist(Brano brano, Playlist playlist, Utente utente) throws SQLException{
        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        playlistDAO.controllaUtentePlaylist(playlist.getId(), utente.getId_utente());
        this.controllaUtenteBrano(brano.getId(), utente.getId_utente());

        String query = "SELECT COUNT(*) AS total FROM playlist_brano WHERE id_playlist = ? AND id_brano = ? AND id_utente = ?";
        int count;
        try (PreparedStatement pstatement = connection.prepareStatement(query)) {
            pstatement.setInt(1, playlist.getId());
            pstatement.setInt(2, brano.getId());
            pstatement.setInt(3, utente.getId_utente());
            try (ResultSet result = pstatement.executeQuery()) {
                result.next();
                count = result.getInt("total");
            }
        }
        if(count > 0) {
            throw new SQLException("Brani già in playlist");
        }

        query = "INSERT INTO playlist_brano (id_brano, id_playlist, id_utente) VALUES (?,?,?)";

        try(PreparedStatement pstatement = connection.prepareStatement(query)) {
            pstatement.setInt(1, brano.getId());
            pstatement.setInt(2, playlist.getId());
            pstatement.setInt(3, utente.getId_utente());
            pstatement.executeUpdate();
        }
    }

    public void aggiungiBraniAllaPlaylist(ArrayList<Brano> brani, Playlist playlist, Utente utente) throws SQLException {
        connection.setAutoCommit(false);

        if(!brani.isEmpty()) {
            try {
                for(Brano brano : brani) {
                    this.aggiungiBranoAllaPlaylist(brano, playlist, utente);
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
            connection.setAutoCommit(true);
        }
    }

    public void controllaUtenteBrano(int id_brano, int id_utente) throws SQLException {
        String query = "SELECT COUNT(*) AS total FROM brano WHERE id_brano = ? AND id_utente = ?";

        int count = 0;
        try (PreparedStatement pstatement = connection.prepareStatement(query)) {
            pstatement.setInt(1, id_brano);
            pstatement.setInt(2, id_utente);
            try (ResultSet result = pstatement.executeQuery()) {
                result.next();
                count = result.getInt("total");
            }
        }
        if(count <= 0) {
            throw new SQLException("Brano non valido");
        }
    }

    public List<Brano> getBraniDaUtenteInterpreteAnno(int id_utente) throws SQLException {
        String query = "SELECT b.id_brano, b.titolo, b.file_musicale, b.id_album, b.id_utente, " +
                "a.titolo_album, a.interprete, a.anno_pubblicazione, a.genere " +
                "FROM brano b JOIN album a ON b.id_album = a.id_album " +
                "WHERE b.id_utente = ? ORDER BY a.interprete ASC, a.anno_pubblicazione ASC";

        List<Brano> brani = new ArrayList<>();
        try (PreparedStatement pstatement = connection.prepareStatement(query)) {
            pstatement.setInt(1, id_utente);
            try (ResultSet result = pstatement.executeQuery()) {
                while (result.next()) {
                    Brano brano = new Brano();
                    brano.setId(result.getInt("id_brano"));
                    brano.setTitolo(result.getString("titolo"));
                    brano.setFile_musicale(result.getString("file_musicale"));
                    brano.setId_utente(id_utente);
                    brano.setId_album(result.getInt("id_album"));
                    brani.add(brano);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        return brani;
    }

    public List<Brano> getBraniDaPlaylist(int id_playlist) throws SQLException {
        String query = "SELECT b.*, pb.ordinamento FROM brano b " +
                "INNER JOIN playlist_brano pb ON b.id_brano = pb.id_brano " +
                "INNER JOIN album a ON b.id_album = a.id_album " +
                "WHERE pb.id_playlist = ? " +
                "ORDER BY a.interprete ASC, a.anno_pubblicazione ASC";

        List<Brano> brani = new ArrayList<>();
        try (PreparedStatement pstatement = connection.prepareStatement(query)) {
            pstatement.setInt(1, id_playlist);
            try (ResultSet result = pstatement.executeQuery()) {
                while (result.next()) {
                    Brano brano = new Brano();
                    brano.setId(result.getInt("id_brano"));
                    brano.setTitolo(result.getString("titolo"));
                    brano.setFile_musicale(result.getString("file_musicale"));
                    brano.setId_utente(result.getInt("id_utente"));
                    brano.setId_album(result.getInt("id_album"));
                    brano.setOrdinamento(result.getInt("ordinamento")); // NUOVO
                    brani.add(brano);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        return brani;
    }

    public List<Brano> getBraniDisponibiliPlaylist(int id_utente, int id_playlist) throws SQLException {
        String query = "SELECT b.* FROM brano b JOIN album a ON b.id_album = a.id_album " +
                "WHERE b.id_utente = ? AND b.id_brano NOT IN " +
                "(SELECT pb.id_brano FROM playlist_brano pb " +
                "WHERE pb.id_playlist = ? AND pb.id_utente = ?) " +
                "ORDER BY a.interprete ASC, a.anno_pubblicazione ASC";

        List<Brano> brani = new ArrayList<>();
        try (PreparedStatement pstatement = connection.prepareStatement(query)) {
            pstatement.setInt(1, id_utente);
            pstatement.setInt(2, id_playlist);
            pstatement.setInt(3, id_utente);
            try (ResultSet result = pstatement.executeQuery()) {
                while (result.next()) {
                    Brano brano = new Brano();
                    brano.setId(result.getInt("id_brano"));
                    brano.setTitolo(result.getString("titolo"));
                    brano.setFile_musicale(result.getString("file_musicale"));
                    brano.setId_album(result.getInt("id_album"));
                    brano.setId_utente(result.getInt("id_utente"));
                    brani.add(brano);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        return brani;
    }
}