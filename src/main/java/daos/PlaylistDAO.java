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
        try (PreparedStatement pstatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);) {
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

//    public void rimuoviBranoDaPlaylist(int id_brano, int id_playlist, int id_utente) throws SQLException {
//        this.controllaUtentePlaylist(id_playlist, id_utente);
//        BranoDAO branoDAO = new BranoDAO(this.connection);
//        branoDAO.controllaUtenteBrano(id_brano, id_utente);
//
//        String query = "DELETE FROM playlist_brano WHERE id_brano = ? AND id_playlist = ? AND id_utente = ?";
//        try (PreparedStatement pstatement = connection.prepareStatement(query);) {
//            pstatement.setInt(1, id_brano);
//            pstatement.setInt(2, id_playlist);
//            pstatement.setInt(3, id_utente);
//            pstatement.executeUpdate();
//        }
//        catch (SQLException e) {
//            connection.rollback();
//            throw e;
//        }
//
//    }

//    public String getTitoloDallaPlaylist(int id_playlist, int id_utente) throws SQLException {
//        this.controllaUtentePlaylist(id_playlist, id_utente);
//
//        String query = "SELECT titolo FROM playlist WHERE id_playlist = ? AND id_utente = ?";
//        String titolo = null;
//        try (PreparedStatement pstatement = connection.prepareStatement(query);) {
//            pstatement.setInt(1, id_playlist);
//            pstatement.setInt(2, id_utente);
//            try (ResultSet result = pstatement.executeQuery();) {
//                if (result.next()) {
//                    titolo = result.getString("name");
//                }
//            }
//        }
//        catch (SQLException e) {
//            connection.rollback();
//            throw e;
//        }
//        return titolo;
//    }

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

//    public ArrayList<Brano> getBraniNonInPlaylist(int id_utente, int id_playlist) throws SQLException {
//        this.controllaUtentePlaylist(id_playlist, id_utente);
//
//        String query = "(SELECT id_brano, titolo FROM brano WHERE id_utente = ?) EXCEPT (SELECT id_brano, titolo FROM brano WHERE brano.id_brano IN (SELECT id_brano FROM playlist_brano WHERE id_playlist = ? AND brano.id_utente = ?))";
//        ArrayList<Brano> brani = new ArrayList<>();
//        try(PreparedStatement pstatement = connection.prepareStatement(query);) {
//            pstatement.setInt(1, id_utente);
//            pstatement.setInt(2, id_playlist);
//            pstatement.setInt(3, id_utente);
//            try (ResultSet result = pstatement.executeQuery();) {
//                while (result.next()) {
//                    Brano brano = new Brano();
//                    brano.setId(result.getInt("id_brano"));
//                    brano.setTitolo(result.getString("titolo"));
//                    brano.setId_utente(id_utente);
//                    brani.add(brano);
//                }
//            }
//        }
//        catch (SQLException e){
//            connection.rollback();
//            throw e;
//        }
//        return brani;
//    }

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

//METODI AGGIUNTI DA CONTROLLARE


//    public List<Brano> getBraniPlaylistOrdinati(int id_playlist, int id_utente) throws SQLException {
//        List<Brano> brani = new ArrayList<>();
//
//        String query = "SELECT b.* FROM Brano b " +
//                "JOIN playlist_brano pb ON b.id_brano = pb.id_brano " +
//                "WHERE pb.id_playlist = ? AND pb.id_utente = ? " +
//                "ORDER BY pb.ordinamento ASC";
//
//        try (PreparedStatement stmt = connection.prepareStatement(query)) {
//            stmt.setInt(1, id_playlist);
//            stmt.setInt(2, id_utente);
//
//            try (ResultSet rs = stmt.executeQuery()) {
//                while (rs.next()) {
//                    Brano brano = new Brano();
//                    brano.setId(rs.getInt("id_brano"));
//                    brano.setTitolo(rs.getString("titolo"));
//                    brano.setId_album(rs.getInt("id_album"));
//                    brano.setId_utente(rs.getInt("id_utente"));
//                    brano.setFile_musicale(rs.getString("file_musicale"));
//                    brani.add(brano);
//                }
//            }
//        }
//
//        return brani;
//    }


//    public void aggiungiBranoAPlaylist(int id_brano, int id_playlist, int id_utente) throws SQLException {
//        // Prima trova la posizione massima attuale
//        int prossimoOrdinamento = 1;
//
//        String queryMax = "SELECT COALESCE(MAX(ordinamento), 0) + 1 AS prossimo " +
//                "FROM playlist_brano WHERE id_playlist = ? AND id_utente = ?";
//
//        try (PreparedStatement stmt = connection.prepareStatement(queryMax)) {
//            stmt.setInt(1, id_playlist);
//            stmt.setInt(2, id_utente);
//
//            try (ResultSet rs = stmt.executeQuery()) {
//                if (rs.next()) {
//                    prossimoOrdinamento = rs.getInt("prossimo");
//                }
//            }
//        }
//
//        // Poi inserisci il brano con il nuovo ordinamento
//        String queryInsert = "INSERT INTO playlist_brano (id_brano, id_playlist, id_utente, ordinamento) " +
//                "VALUES (?, ?, ?, ?)";
//
//        try (PreparedStatement stmt = connection.prepareStatement(queryInsert)) {
//            stmt.setInt(1, id_brano);
//            stmt.setInt(2, id_playlist);
//            stmt.setInt(3, id_utente);
//            stmt.setInt(4, prossimoOrdinamento);
//            stmt.executeUpdate();
//        }
//    }


    public void salvaOrdinamentoPlaylist(int id_playlist, int id_utente, int[] idBraniOrdinati)
            throws SQLException {

        String query = "UPDATE playlist_brano SET ordinamento = ? " +
                "WHERE id_brano = ? AND id_playlist = ? AND id_utente = ?";

        connection.setAutoCommit(false);

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            for (int i = 0; i < idBraniOrdinati.length; i++) {
                stmt.setInt(1, i + 1); // ordinamento parte da 1
                stmt.setInt(2, idBraniOrdinati[i]);
                stmt.setInt(3, id_playlist);
                stmt.setInt(4, id_utente);
                stmt.addBatch();
            }

            stmt.executeBatch();
            connection.commit();

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }


//    public boolean haOrdinamentoPersonalizzato(int id_playlist, int id_utente) throws SQLException {
//        String query = "SELECT COUNT(*) AS totale, " +
//                "SUM(CASE WHEN ordinamento > 0 THEN 1 ELSE 0 END) AS con_ordine " +
//                "FROM playlist_brano " +
//                "WHERE id_playlist = ? AND id_utente = ?";
//
//        try (PreparedStatement stmt = connection.prepareStatement(query)) {
//            stmt.setInt(1, id_playlist);
//            stmt.setInt(2, id_utente);
//
//            try (ResultSet rs = stmt.executeQuery()) {
//                if (rs.next()) {
//                    int totale = rs.getInt("totale");
//                    int conOrdine = rs.getInt("con_ordine");
//                    return totale > 0 && totale == conOrdine;
//                }
//            }
//        }
//
//        return false;
//    }


//    public void inizializzaOrdinamentoDefault(int id_playlist, int id_utente) throws SQLException {
//        String query = "UPDATE playlist_brano pb " +
//                "JOIN ( " +
//                "    SELECT pb2.id_brano, " +
//                "           ROW_NUMBER() OVER (ORDER BY a.interprete ASC, a.anno_pubblicazione ASC) AS nuovo_ordine " +
//                "    FROM playlist_brano pb2 " +
//                "    JOIN Brano b ON pb2.id_brano = b.id_brano " +
//                "    JOIN Album a ON b.id_album = a.id_album " +
//                "    WHERE pb2.id_playlist = ? AND pb2.id_utente = ? " +
//                ") AS ordinati ON pb.id_brano = ordinati.id_brano " +
//                "SET pb.ordinamento = ordinati.nuovo_ordine " +
//                "WHERE pb.id_playlist = ? AND pb.id_utente = ?";
//
//        try (PreparedStatement stmt = connection.prepareStatement(query)) {
//            stmt.setInt(1, id_playlist);
//            stmt.setInt(2, id_utente);
//            stmt.setInt(3, id_playlist);
//            stmt.setInt(4, id_utente);
//            stmt.executeUpdate();
//        }
//    }
}
