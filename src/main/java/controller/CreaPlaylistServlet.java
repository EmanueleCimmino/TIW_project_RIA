package controller;

import beans.Brano;
import beans.Utente;
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
import java.util.ArrayList;
import java.util.List;

@WebServlet("/CreaPlaylist")
public class CreaPlaylistServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    public void init() throws ServletException {
        try {
            ServletContext context = getServletContext();
            this.connection = ConnectionHandler.getConnection(context);

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
        // Redirect alla home se si accede via GET
        response.sendRedirect(getServletContext().getContextPath() + "/home");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
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

        // Recupera parametri dal form
        String playlist_titolo = request.getParameter("playlist_titolo");
        String[] braniSelezionatiIds = request.getParameterValues("braniSelezionati");

        // Validazione lato server
        if (playlist_titolo == null || playlist_titolo.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Il titolo della playlist è obbligatorio");
            return;
        }

        if (playlist_titolo.length() > 50) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Il titolo della playlist non può superare i 50 caratteri");
            return;
        }

        if (braniSelezionatiIds == null || braniSelezionatiIds.length == 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Seleziona almeno un brano per creare la playlist");
            return;
        }

        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        BranoDAO branoDAO = new BranoDAO(connection);

        try {
            // Converti gli ID dei brani e recupera gli oggetti Brano
            List<Brano> braniSelezionati = new ArrayList<>();

            for (String idString : braniSelezionatiIds) {
                try {
                    int brano_id = Integer.parseInt(idString);

                    // Recupera il brano dal database
                    Brano brano = branoDAO.getBrano(brano_id);

                    if (brano == null) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                                "Brano non trovato: " + brano_id);
                        return;
                    }

                    // Verifica che il brano appartenga all'utente (sicurezza)
                    if (brano.getId_utente() != utente.getId_utente()) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN,
                                "Non hai i permessi per aggiungere questo brano");
                        return;
                    }

                    braniSelezionati.add(brano);

                } catch (NumberFormatException e) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "ID brano non valido: " + idString);
                    return;
                }
            }

            // Crea la playlist utilizzando il metodo esistente del DAO
            playlistDAO.creaPlaylist(playlist_titolo.trim(), braniSelezionati, utente);

            // Redirect alla home dopo la creazione con successo
            response.sendRedirect(getServletContext().getContextPath() + "/home");

        } catch (SQLException e) {
            System.err.println("Errore SQL nella creazione della playlist: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Errore nel database durante la creazione della playlist");
        }
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