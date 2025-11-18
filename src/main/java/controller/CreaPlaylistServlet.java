/*package controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.google.gson.Gson;

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

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Verifica sessione utente
        HttpSession session = request.getSession(false);
        Utente utente = (session != null) ? (Utente) session.getAttribute("utente") : null;

        Map<String, Object> jsonResponse = new HashMap<>();
        Gson gson = new Gson();

        if (utente == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            jsonResponse.put("success", false);
            jsonResponse.put("error", "Utente non autenticato");
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        // Recupera parametri dal form
        String playlist_titolo = request.getParameter("playlist_titolo");
        String[] braniSelezionatiIds = request.getParameterValues("braniSelezionati");

        // Validazione lato server
        if (playlist_titolo == null || playlist_titolo.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            jsonResponse.put("success", false);
            jsonResponse.put("error", "Il titolo della playlist è obbligatorio");
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        if (playlist_titolo.length() > 50) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            jsonResponse.put("success", false);
            jsonResponse.put("error", "Il titolo della playlist non può superare i 50 caratteri");
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        if (braniSelezionatiIds == null || braniSelezionatiIds.length == 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            jsonResponse.put("success", false);
            jsonResponse.put("error", "Seleziona almeno un brano per creare la playlist");
            response.getWriter().write(gson.toJson(jsonResponse));
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
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");

                        jsonResponse.put("success", false);
                        jsonResponse.put("error", "Brano non trovato: " + brano_id);
                        response.getWriter().write(gson.toJson(jsonResponse));
                        return;
                    }

                    // Verifica che il brano appartenga all'utente (sicurezza)
                    if (brano.getId_utente() != utente.getId_utente()) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");

                        jsonResponse.put("success", false);
                        jsonResponse.put("error", "Non hai i permessi per aggiungere questo brano");
                        response.getWriter().write(gson.toJson(jsonResponse));
                        return;
                    }

                    braniSelezionati.add(brano);

                } catch (NumberFormatException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");

                    jsonResponse.put("success", false);
                    jsonResponse.put("error", "ID brano non valido: " + idString);
                    response.getWriter().write(gson.toJson(jsonResponse));
                    return;
                }
            }

            // Crea la playlist utilizzando il metodo esistente del DAO
            playlistDAO.creaPlaylist(playlist_titolo.trim(), braniSelezionati, utente);

            // Risposta di successo
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            jsonResponse.put("success", true);
            jsonResponse.put("message", "Playlist creata con successo");
            response.getWriter().write(gson.toJson(jsonResponse));

        } catch (SQLException e) {
            System.err.println("Errore SQL nella creazione della playlist: " + e.getMessage());
            e.printStackTrace();

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            jsonResponse.put("success", false);
            jsonResponse.put("error", "Errore nel database durante la creazione della playlist");
            response.getWriter().write(gson.toJson(jsonResponse));
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
}*/