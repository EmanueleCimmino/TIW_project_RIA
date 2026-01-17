package controller;

import beans.Album;
import beans.Brano;
import beans.Playlist;
import beans.Utente;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import daos.AlbumDAO;
import daos.BranoDAO;
import daos.PlaylistDAO;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import utils.ConnectionHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/GetPlaylist")
public class GetPlaylistServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        try {
            ServletContext context = getServletContext();
            this.connection = ConnectionHandler.getConnection(context);

            if (this.connection == null || this.connection.isClosed()) {
                throw new ServletException("Impossibile stabilire connessione al database");
            }

            this.gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class,
                            (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context1) ->
                                    context1.serialize(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                    .create();


        } catch (UnavailableException | SQLException e) {
            System.err.println("Errore durante l'inizializzazione della servlet: " + e.getMessage());
            e.printStackTrace();
            throw new ServletException("Errore nell'inizializzazione della servlet", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {


        HttpSession session = request.getSession(false);
        Utente utente = (session != null) ? (Utente) session.getAttribute("utente") : null;

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (utente == null) {
            response.setStatus(401);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Utente non autenticato");

            response.getWriter().write(gson.toJson(errorResponse));
            return;
        }

        String idPlaylistParam = request.getParameter("id_playlist");

        if (idPlaylistParam == null || idPlaylistParam.isEmpty()) {
            response.setStatus(400);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "ID playlist mancante");

            response.getWriter().write(gson.toJson(errorResponse));
            return;
        }

        int idPlaylist;
        try {
            idPlaylist = Integer.parseInt(idPlaylistParam);
        } catch (NumberFormatException e) {
            response.setStatus(400);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "ID playlist non valido");

            response.getWriter().write(gson.toJson(errorResponse));
            return;
        }

        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        BranoDAO branoDAO = new BranoDAO(connection);
        AlbumDAO albumDAO = new AlbumDAO(connection);

        try {
            Playlist playlist = playlistDAO.getPlaylist(idPlaylist);

            if (playlist == null) {
                response.setStatus(404);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Playlist non trovata");

                response.getWriter().write(gson.toJson(errorResponse));
                return;
            }

            if (playlist.getId_utente() != utente.getId_utente()) {
                response.setStatus(403);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Non hai i permessi per accedere a questa playlist");

                response.getWriter().write(gson.toJson(errorResponse));
                return;
            }

            List<Brano> brani = branoDAO.getBraniDaPlaylist(idPlaylist);

            List<Brano> braniDisponibili = branoDAO.getBraniDisponibiliPlaylist(utente.getId_utente(),
                    idPlaylist);

            List<Album> albums = albumDAO.getAlbums();

            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("success", true);
            jsonResponse.put("playlist", playlist);
            jsonResponse.put("brani", brani);
            jsonResponse.put("braniDisponibili", braniDisponibili);
            jsonResponse.put("albums", albums);

            response.setStatus(200);
            response.getWriter().write(gson.toJson(jsonResponse));


        } catch (SQLException e) {
            System.err.println("Errore SQL nel recupero della playlist: " + e.getMessage());
            e.printStackTrace();

            response.setStatus(500);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Errore nel recupero dei dati dal database");

            response.getWriter().write(gson.toJson(errorResponse));
        }
    }

    @Override
    public void destroy() {
        try {
            if (connection != null && !connection.isClosed()) {
                ConnectionHandler.closeConnection(connection);
            }
        } catch (SQLException e) {
            System.err.println("Errore durante la chiusura della connessione: " + e.getMessage());
            e.printStackTrace();
        }
    }
}