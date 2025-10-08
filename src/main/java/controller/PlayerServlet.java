package controller;

import beans.Album;
import beans.Brano;
import beans.Utente;
import daos.AlbumDAO;
import daos.BranoDAO;
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

@WebServlet("/player")
public class PlayerServlet extends HttpServlet {
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

        // Verifica sessione utente
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect(getServletContext().getContextPath() + "/");
            return;
        }

        Utente utente = (Utente) session.getAttribute("utente");
        if (utente == null) {
            response.sendRedirect(getServletContext().getContextPath() + "/");
            return;
        }

        // Recupera parametro brano_id
        String branoIdParam = request.getParameter("brano_id");

        // Validazione parametri
        if (branoIdParam == null || branoIdParam.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID brano mancante");
            return;
        }

        int branoId;
        try {
            branoId = Integer.parseInt(branoIdParam);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID brano non valido");
            return;
        }

        BranoDAO branoDAO = new BranoDAO(connection);
        AlbumDAO albumDAO = new AlbumDAO(connection);

        try {
            // Recupera il brano
            Brano brano = branoDAO.getBrano(branoId);

            if (brano == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Brano non trovato");
                return;
            }

            // Verifica che il brano appartenga all'utente (sicurezza)
            if (brano.getId_utente() != utente.getId_utente()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accesso negato");
                return;
            }

            // Recupera i dati dell'album
            Album album = albumDAO.getAlbum(brano.getId_album());

            if (album == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Album non trovato");
                return;
            }

            // Prepara il contesto Thymeleaf
            JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(getServletContext());
            WebContext context = new WebContext(application.buildExchange(request, response));

            context.setVariable("brano", brano);
            context.setVariable("album", album);

            templateEngine.process("/player.html", context, response.getWriter());

        } catch (SQLException e) {
            System.err.println("Errore nel recupero dei dati del brano: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Errore nel recupero dei dati del brano");
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