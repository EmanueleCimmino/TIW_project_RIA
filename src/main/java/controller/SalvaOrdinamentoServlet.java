package controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import beans.Utente;
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

@WebServlet("/SalvaOrdinamento")
public class SalvaOrdinamentoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;
    private Gson gson = new Gson();

    public void init() throws ServletException {
        try {
            ServletContext context = getServletContext();
            this.connection = ConnectionHandler.getConnection(context);

            if (this.connection == null || this.connection.isClosed()) {
                throw new ServletException("Impossibile stabilire connessione al database");
            }

        } catch (UnavailableException | SQLException e) {
            System.err.println("Errore init servlet: " + e.getMessage());
            e.printStackTrace();
            throw new ServletException("Errore nell'inizializzazione della servlet", e);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> jsonResponse = new HashMap<>();

        // Verifica sessione
        HttpSession session = request.getSession(false);
        if (session == null) {
            jsonResponse.put("success", false);
            jsonResponse.put("error", "Sessione non valida");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        Utente utente = (Utente) session.getAttribute("utente");
        if (utente == null) {
            jsonResponse.put("success", false);
            jsonResponse.put("error", "Utente non autenticato");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        try {
            // Leggi il body JSON
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                buffer.append(line);
            }

            // Parsing JSON in Map
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> jsonRequest = gson.fromJson(buffer.toString(), type);

            // Validazione parametri
            if (!jsonRequest.containsKey("playlist_id") || !jsonRequest.containsKey("brani_ordinati")) {
                jsonResponse.put("success", false);
                jsonResponse.put("error", "Parametri mancanti");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(jsonResponse));
                return;
            }

            int id_playlist = ((Number) jsonRequest.get("playlist_id")).intValue();
            @SuppressWarnings("unchecked")
            List<Object> braniOrdinatiList = (List<Object>) jsonRequest.get("brani_ordinati");

            if (braniOrdinatiList == null || braniOrdinatiList.isEmpty()) {
                jsonResponse.put("success", false);
                jsonResponse.put("error", "Lista brani vuota");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(jsonResponse));
                return;
            }

            // Converti List in array di int
            int[] idBraniOrdinati = new int[braniOrdinatiList.size()];
            for (int i = 0; i < braniOrdinatiList.size(); i++) {
                idBraniOrdinati[i] = ((Number) braniOrdinatiList.get(i)).intValue();
            }

            // Salva l'ordinamento
            PlaylistDAO playlistDAO = new PlaylistDAO(connection);
            playlistDAO.salvaOrdinamentoPlaylist(id_playlist, utente.getId_utente(), idBraniOrdinati);

            jsonResponse.put("success", true);
            jsonResponse.put("message", "Ordinamento salvato con successo");
            response.getWriter().write(gson.toJson(jsonResponse));

        } catch (SQLException e) {
            System.err.println("Errore SQL nel salvataggio ordinamento: " + e.getMessage());
            e.printStackTrace();

            jsonResponse.put("success", false);
            jsonResponse.put("error", "Errore nel salvataggio dell'ordinamento");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(jsonResponse));

        } catch (Exception e) {
            System.err.println("Errore generico: " + e.getMessage());
            e.printStackTrace();

            jsonResponse.put("success", false);
            jsonResponse.put("error", "Errore nel processamento della richiesta");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(jsonResponse));
        }
    }

    public void destroy() {
        try {
            if (connection != null && !connection.isClosed()) {
                ConnectionHandler.closeConnection(connection);
            }
        } catch (SQLException e) {
            System.err.println("Errore chiusura connessione: " + e.getMessage());
            e.printStackTrace();
        }
    }
}