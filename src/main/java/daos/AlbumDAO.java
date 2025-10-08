package daos;

import beans.Album;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AlbumDAO {
    private Connection connection;

    public AlbumDAO(Connection connection) {
        this.connection = connection;
    }

    public void caricaAlbum(String titolo_album, String interprete,
                            String genere, int anno_pubblicazione, String immagine_album) throws SQLException {
        String query = "INSERT INTO album (titolo_album, interprete, genere, anno_pubblicazione, immagine_album) VALUES (?,?,?,?,?)";
        try (PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setString(1, titolo_album);
            pstatement.setString(2, interprete);
            pstatement.setString(3, genere);
            pstatement.setInt(4, anno_pubblicazione);
            pstatement.setString(5, immagine_album);
            pstatement.executeUpdate();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Album getAlbum(int id_album) throws SQLException {
        String query = "SELECT id_album, titolo_album, interprete, genere, anno_pubblicazione, immagine_album FROM album WHERE id_album = ?";
        Album album = new Album();
        try(PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setInt(1, id_album);
            try (ResultSet result = pstatement.executeQuery();) {
                if (result.next()) {
                    album.setTitolo_album(result.getString(2));
                    album.setInterprete(result.getString(3));
                    album.setGenere(result.getString(4));
                    album.setAnno_pubblicazione(result.getInt(5));
                    album.setImmagine_album(result.getString(6));
                    return album;
                }
            }
            catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
        }
        return null;
    }

    public List<Album> getAlbumInterpreteAnno(String interprete, int anno_pubblicazione) throws SQLException {
        String query = "SELECT * FROM album WHERE interprete = ? AND anno_pubblicazione";
        List<Album> albums = new ArrayList<>();
        try(PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setString(3, interprete);
            pstatement.setInt(5, anno_pubblicazione);
            try (ResultSet result = pstatement.executeQuery();) {
                if (result.next()) {
                    Album album = new Album();
                    album.setId_album(result.getInt(1));
                    album.setTitolo_album(result.getString(2));
                    album.setGenere(result.getString(4));
                    album.setImmagine_album(result.getString(6));
                    albums.add(album);
                }
            }
            catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
        }
        return albums;
    }

    public List<Album> getAlbums() throws SQLException {
        List<Album> albums = new ArrayList<>();
        String query = "SELECT * FROM album ORDER BY titolo_album";

        try (PreparedStatement pstatement = connection.prepareStatement(query);
             ResultSet result = pstatement.executeQuery()) {

            while (result.next()) {
                Album album = new Album();
                album.setId_album(result.getInt("id_album"));
                album.setTitolo_album(result.getString("titolo_album"));
                album.setInterprete(result.getString("interprete"));
                album.setGenere(result.getString("genere"));
                album.setAnno_pubblicazione(result.getInt("anno_pubblicazione"));
                album.setImmagine_album(result.getString("immagine_album"));
                albums.add(album);
            }
        }
        return albums;
    }
}
