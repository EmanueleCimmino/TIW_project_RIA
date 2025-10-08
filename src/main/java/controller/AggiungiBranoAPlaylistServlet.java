package controller;

import beans.Brano;
import beans.Playlist;
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

@WebServlet("/AggiungiBranoAPlaylist")
public class AggiungiBranoAPlaylistServlet extends HttpServlet {
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
        String playlist_id_param = request.getParameter("playlist_id");
        String[] braniSelezionati_id = request.getParameterValues("braniSelezionati_id");

        // Validazione lato server
        if (playlist_id_param == null || playlist_id_param.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "ID playlist mancante");
            return;
        }

        int playlist_id;
        try {
            playlist_id = Integer.parseInt(playlist_id_param);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "ID playlist non valido");
            return;
        }

        if (braniSelezionati_id == null || braniSelezionati_id.length == 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Seleziona almeno un brano da aggiungere");
            return;
        }

        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        BranoDAO branoDAO = new BranoDAO(connection);

        try {
            // Verifica che la playlist appartenga all'utente
            playlistDAO.controllaUtentePlaylist(playlist_id, utente.getId_utente());

            // Recupera l'oggetto Playlist
            Playlist playlist = playlistDAO.getPlaylist(playlist_id);
            if (playlist == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Playlist non trovata");
                return;
            }

            // Converti gli ID dei brani e recupera gli oggetti Brano
            ArrayList<Brano> braniDaAggiungere = new ArrayList<>();

            for (String idString : braniSelezionati_id) {
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

                    braniDaAggiungere.add(brano);

                } catch (NumberFormatException e) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "ID brano non valido: " + idString);
                    return;
                }
            }

            // Aggiungi i brani alla playlist usando il metodo esistente del DAO
            branoDAO.aggiungiBraniAllaPlaylist(braniDaAggiungere, playlist, utente);

            // Redirect alla pagina della playlist (dal primo blocco come da specifica)
            response.sendRedirect(getServletContext().getContextPath() +
                    "/playlist?id_playlist=" + playlist_id);

        } catch (SQLException e) {
            // Se il brano è già nella playlist, viene lanciata un'eccezione
            if (e.getMessage() != null && e.getMessage().contains("already in playlist")) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Uno o più brani selezionati sono già presenti nella playlist");
            } else {
                System.err.println("Errore SQL nell'aggiunta dei brani alla playlist: " + e.getMessage());
                e.printStackTrace();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Errore nel database durante l'aggiunta dei brani");
            }
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