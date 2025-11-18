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

@WebServlet("/home")
public class HomeServlet extends HttpServlet {
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

            // Configura Gson per gestire LocalDateTime
            this.gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class,
                            (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context1) ->
                                    context1.serialize(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                    .create();

            System.out.println("HomeServlet inizializzata correttamente");

        } catch (UnavailableException | SQLException e) {
            System.err.println("Errore durante l'inizializzazione della servlet: " + e.getMessage());
            e.printStackTrace();
            throw new ServletException("Errore nell'inizializzazione della servlet", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("HomeServlet.doGet() chiamato");

        HttpSession session = request.getSession(false);
        Utente utente = (session != null) ? (Utente) session.getAttribute("utente") : null;

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (utente == null) {
            System.out.println("Utente non autenticato");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Utente non autenticato");
            errorResponse.put("redirectUrl", getServletContext().getContextPath() + "/index.html");

            response.getWriter().write(gson.toJson(errorResponse));
            return;
        }

        System.out.println("Utente autenticato: " + utente.getUsername());

        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        AlbumDAO albumDAO = new AlbumDAO(connection);
        BranoDAO branoDAO = new BranoDAO(connection);

        try {
            // Recupera le playlist dell'utente (ordinate per data decrescente)
            List<Playlist> playlists = playlistDAO.getPlaylistDaUtente(utente.getId_utente());
            System.out.println("Playlists trovate: " + playlists.size());

            // Recupera tutti gli album disponibili
            List<Album> albums = albumDAO.getAlbums();
            System.out.println("Albums trovati: " + albums.size());

            // Recupera i brani dell'utente (ordinati per interprete e anno)
            List<Brano> brani = branoDAO.getBraniDaUtenteInterpreteAnno(utente.getId_utente());
            System.out.println("Brani trovati: " + brani.size());

            // Prepara la risposta JSON
            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("success", true);
            jsonResponse.put("playlists", playlists);
            jsonResponse.put("brani", brani);
            jsonResponse.put("albums", albums);
            jsonResponse.put("username", utente.getUsername());
            jsonResponse.put("userId", utente.getId_utente());

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(jsonResponse));

            System.out.println("Dati inviati con successo");

        } catch (SQLException e) {
            System.err.println("Errore SQL nel recupero dei dati: " + e.getMessage());
            e.printStackTrace();

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

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
                System.out.println("Connessione DB chiusa");
            }
        } catch (SQLException e) {
            System.err.println("Errore durante la chiusura della connessione: " + e.getMessage());
            e.printStackTrace();
        }
    }
}