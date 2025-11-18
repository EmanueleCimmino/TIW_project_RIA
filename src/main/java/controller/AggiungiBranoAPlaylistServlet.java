package controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

import com.google.gson.Gson;

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
        String playlist_id_param = request.getParameter("playlist_id");
        String[] braniSelezionati_id = request.getParameterValues("selectedSongs");

        // Validazione lato server
        if (playlist_id_param == null || playlist_id_param.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            jsonResponse.put("success", false);
            jsonResponse.put("error", "ID playlist mancante");
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        int playlist_id;
        try {
            playlist_id = Integer.parseInt(playlist_id_param);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            jsonResponse.put("success", false);
            jsonResponse.put("error", "ID playlist non valido");
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        if (braniSelezionati_id == null || braniSelezionati_id.length == 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            jsonResponse.put("success", false);
            jsonResponse.put("error", "Seleziona almeno un brano da aggiungere");
            response.getWriter().write(gson.toJson(jsonResponse));
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
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                jsonResponse.put("success", false);
                jsonResponse.put("error", "Playlist non trovata");
                response.getWriter().write(gson.toJson(jsonResponse));
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

                    braniDaAggiungere.add(brano);

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

            // Aggiungi i brani alla playlist usando il metodo esistente del DAO
            branoDAO.aggiungiBraniAllaPlaylist(braniDaAggiungere, playlist, utente);

            // Risposta di successo
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            jsonResponse.put("success", true);
            jsonResponse.put("message", "Brani aggiunti con successo alla playlist");
            jsonResponse.put("braniAggiunti", braniDaAggiungere.size());
            response.getWriter().write(gson.toJson(jsonResponse));

        } catch (SQLException e) {
            // Se il brano è già nella playlist, viene lanciata un'eccezione
            if (e.getMessage() != null && e.getMessage().contains("already in playlist")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                jsonResponse.put("success", false);
                jsonResponse.put("error", "Uno o più brani selezionati sono già presenti nella playlist");
                response.getWriter().write(gson.toJson(jsonResponse));
            } else {
                System.err.println("Errore SQL nell'aggiunta dei brani alla playlist: " + e.getMessage());
                e.printStackTrace();

                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                jsonResponse.put("success", false);
                jsonResponse.put("error", "Errore nel database durante l'aggiunta dei brani");
                response.getWriter().write(gson.toJson(jsonResponse));
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