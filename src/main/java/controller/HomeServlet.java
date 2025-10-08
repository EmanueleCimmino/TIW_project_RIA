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
import java.util.List;

@WebServlet("/home")
public class HomeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
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

        HttpSession session = request.getSession();
        Utente utente = (Utente) session.getAttribute("utente");

        if (utente == null) {
            response.sendRedirect(getServletContext().getContextPath() + "/");
            return;
        }

        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        AlbumDAO albumDAO = new AlbumDAO(connection);
        BranoDAO branoDAO = new BranoDAO(connection);

        try {
            // Recupera le playlist dell'utente
            List<Playlist> playlists = playlistDAO.getPlaylistDaUtente(utente.getId_utente());

            // Recupera tutti gli album disponibili per il form di caricamento
            List<Album> albums = albumDAO.getAlbums();

            // Recupera i brani dell'utente
            List<Brano> brani = branoDAO.getBraniDaUtenteInterpreteAnno(utente.getId_utente());

            // Prepara il contesto Thymeleaf
            String path = "/home.html";
            JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(getServletContext());
            WebContext context = new WebContext(application.buildExchange(request, response));

            context.setVariable("playlists", playlists);
            context.setVariable("brani", brani);
            context.setVariable("albums", albums);
            context.setVariable("username", utente.getUsername());

            templateEngine.process(path, context, response.getWriter());

        } catch (SQLException e) {
            System.err.println("Errore nel recupero dei dati: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel recupero dei dati");
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] braniSelezionati = request.getParameterValues("braniSelezionati");

        if (braniSelezionati != null) {
            for (String id : braniSelezionati) {
                try {
                    int id_brano = Integer.parseInt(id);
                    System.out.println("Brano selezionato: " + id_brano);
                } catch (NumberFormatException e) {
                    System.err.println("ID brano non valido: " + id);
                }
            }
        }

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