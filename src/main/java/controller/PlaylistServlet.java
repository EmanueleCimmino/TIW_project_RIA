package controller;

import beans.Album;
import beans.Brano;
import beans.Playlist;
import beans.Utente;
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
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import utils.ConnectionHandler;
import utils.TemplateHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@WebServlet("/playlist")
public class PlaylistServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final int MaxBraniPerPagina = 5;
    private Connection connection = null;
    private TemplateEngine templateEngine;

    public void init() throws ServletException {
        try {
            ServletContext context = getServletContext();
            this.connection = ConnectionHandler.getConnection(context);
            this.templateEngine = TemplateHandler.getTemplateEngine(getServletContext());

            if (this.connection == null || this.connection.isClosed()) {
                throw new ServletException("Impossibile stabilire connessione al database");
            }

        } catch (UnavailableException | SQLException e) {
            System.err.println("Errore durante l'inizializzazione della servlet: " + e.getMessage());
            e.printStackTrace();
            throw new ServletException("Errore nell'inizializzazione della servlet", e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        Utente utente = (Utente) session.getAttribute("utente");

        if (utente == null) {
            response.sendRedirect(getServletContext().getContextPath() + "/");
            return;
        }

        int id_utente = utente.getId_utente();
        String id_playlist_param = request.getParameter("id_playlist");
        String offset_param = request.getParameter("offset");

        // Validazione parametri
        if (id_playlist_param == null || id_playlist_param.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Playlist ID mancante");
            return;
        }

        int id_playlist;
        int offset = 0;

        try {
            id_playlist = Integer.parseInt(id_playlist_param);
            if (offset_param != null && !offset_param.isEmpty()) {
                offset = Integer.parseInt(offset_param);
                if (offset < 0) {
                    offset = 0;
                }
            }
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Id playlist non valido");
            return;
        }

        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        BranoDAO branoDAO = new BranoDAO(connection);
        AlbumDAO albumDAO = new AlbumDAO(connection);

        try {
            // Verifica che la playlist appartenga all'utente
            Playlist playlist = playlistDAO.getPlaylist(id_playlist);
            if (playlist == null || playlist.getId_utente() != id_utente) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Playlist non trovata");
                return;
            }

            // Recupera tutti i brani della playlist ordinati
            List<Brano> brani = branoDAO.getBraniDaPlaylist(id_playlist);

            // Calcola paginazione
            int totaleBrani = brani.size();

            // Correggi offset se fuori range
            if (offset < 0) {
                offset = 0;
            }
            if (offset >= totaleBrani) {
                offset = Math.max(0, totaleBrani - MaxBraniPerPagina);
            }

            // Estrai il sottoinsieme di brani da visualizzare
            int fineIndice = Math.min(offset + MaxBraniPerPagina, totaleBrani);
            List<Brano> braniDaVisualizzare = totaleBrani > 0 ? brani.subList(offset, fineIndice) : new ArrayList<>();

            // Determina visibilità bottoni
            boolean Precedente = offset > 0;
            boolean Successivo = fineIndice < totaleBrani;

            // Calcola offset per i bottoni
            int PrecedenteOffset = Math.max(0, offset - MaxBraniPerPagina);
            int SuccessivoOffset = offset + MaxBraniPerPagina;

            // Recupera brani disponibili da aggiungere (non già nella playlist)
            List<Brano> braniDisponibili = branoDAO.getBraniDisponibiliPlaylist(id_utente, id_playlist);

            // Recupera tutti gli album per la mappa
            List<Album> albums = albumDAO.getAlbums();

            // Crea una mappa per associare id_album -> Album
            Map<Integer, Album> albumMap = new HashMap<>();
            for (Album album : albums) {
                albumMap.put(album.getId_album(), album);
            }

            // Prepara il contesto Thymeleaf
            JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(getServletContext());
            WebContext context = new WebContext(application.buildExchange(request, response));

            context.setVariable("playlist", playlist);
            context.setVariable("braniDaVisualizzare", braniDaVisualizzare);
            context.setVariable("Precedente", Precedente);
            context.setVariable("Successivo", Successivo);
            context.setVariable("PrecedenteOffset", PrecedenteOffset);
            context.setVariable("SuccessivoOffset", SuccessivoOffset);
            context.setVariable("braniDisponibili", braniDisponibili);
            context.setVariable("albums", albums);
            context.setVariable("albumMap", albumMap);

            templateEngine.process("/playlist.html", context, response.getWriter());

        } catch (SQLException e) {
            System.err.println("Errore nel recupero dei dati della playlist: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Errore nel recupero dei dati della playlist");
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

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