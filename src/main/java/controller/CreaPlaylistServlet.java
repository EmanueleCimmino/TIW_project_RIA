package controller;

import java.io.BufferedReader;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@WebServlet("/CreaPlaylist")
public class CreaPlaylistServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;
    private Gson gson;

    public void init() throws ServletException {
        try {
            ServletContext context = getServletContext();
            this.connection = ConnectionHandler.getConnection(context);
            this.gson = new Gson();

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
            throws IOException {

        response.setContentType("application/json;charset=UTF-8");

        HttpSession session = request.getSession(false);
        Utente utente = (session != null) ? (Utente) session.getAttribute("utente") : null;

        if (utente == null) {
            sendErrorResponse(response, 401, "Utente non autenticato");
            return;
        }

        String contentType = request.getContentType();
        boolean isJson = contentType != null && contentType.contains("application/json");

        String playlistTitolo;
        ArrayList<Integer> braniId = new ArrayList<>();

        if (isJson) {
            try (BufferedReader reader = request.getReader()) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                String jsonString = sb.toString();

                JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

                if (!jsonObject.has("playlist_titolo")) {
                    sendErrorResponse(response, 400, "Il titolo della playlist è obbligatorio");
                    return;
                }

                playlistTitolo = jsonObject.get("playlist_titolo").getAsString().trim();

                if (playlistTitolo.isEmpty()) {
                    sendErrorResponse(response, 400, "Il titolo della playlist è obbligatorio");
                    return;
                }

                if (playlistTitolo.length() > 50) {
                    sendErrorResponse(response, 400, "Il titolo della playlist non può superare i 50 caratteri");
                    return;
                }

                if (!jsonObject.has("braniSelezionati")) {
                    sendErrorResponse(response, 400, "Seleziona almeno un brano per creare la playlist");
                    return;
                }

                if (jsonObject.get("braniSelezionati").isJsonArray()) {
                    JsonArray braniArray = jsonObject.getAsJsonArray("braniSelezionati");
                    for (int i = 0; i < braniArray.size(); i++) {
                        String value = braniArray.get(i).getAsString();
                        braniId.add(Integer.parseInt(value));
                    }
                } else {
                    String value = jsonObject.get("braniSelezionati").getAsString();
                    braniId.add(Integer.parseInt(value));
                }

                if (braniId.isEmpty()) {
                    sendErrorResponse(response, 400, "Seleziona almeno un brano per creare la playlist");
                    return;
                }


            } catch (Exception e) {
                System.err.println("Errore parsing JSON: " + e.getMessage());
                e.printStackTrace();
                sendErrorResponse(response, 400, "Formato JSON non valido");
                return;
            }

        } else {
            playlistTitolo = request.getParameter("playlist_titolo");
            String[] braniSelezionatiIds = request.getParameterValues("braniSelezionati");

            if (playlistTitolo == null || playlistTitolo.trim().isEmpty()) {
                sendErrorResponse(response, 400, "Il titolo della playlist è obbligatorio");
                return;
            }

            playlistTitolo = playlistTitolo.trim();

            if (playlistTitolo.length() > 50) {
                sendErrorResponse(response, 400, "Il titolo della playlist non può superare i 50 caratteri");
                return;
            }

            if (braniSelezionatiIds == null || braniSelezionatiIds.length == 0) {
                sendErrorResponse(response, 400, "Seleziona almeno un brano per creare la playlist");
                return;
            }

            for (String idString : braniSelezionatiIds) {
                try {
                    braniId.add(Integer.parseInt(idString));
                } catch (NumberFormatException e) {
                    sendErrorResponse(response, 400, "ID brano non valido: " + idString);
                    return;
                }
            }
        }

        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        BranoDAO branoDAO = new BranoDAO(connection);

        try {
            List<Brano> braniSelezionati = new ArrayList<>();

            for (int branoId : braniId) {
                Brano brano = branoDAO.getBrano(branoId);

                if (brano == null) {
                    sendErrorResponse(response, 400, "Brano non trovato: " + branoId);
                    return;
                }

                if (brano.getId_utente() != utente.getId_utente()) {
                    sendErrorResponse(response, 403, "Non hai i permessi per aggiungere questo brano");
                    return;
                }

                braniSelezionati.add(brano);
            }

            playlistDAO.creaPlaylist(playlistTitolo, braniSelezionati, utente);


            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("success", true);
            jsonResponse.put("message", "Playlist \"" + playlistTitolo + "\" creata con successo");
            jsonResponse.put("playlistTitolo", playlistTitolo);
            jsonResponse.put("braniAggiunti", braniSelezionati.size());

            response.setStatus(200);
            response.getWriter().write(gson.toJson(jsonResponse));

        } catch (SQLException e) {
            System.err.println("Errore SQL nella creazione della playlist: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, 500, "Errore nel database durante la creazione della playlist");
        }
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String errorMessage)
            throws IOException {
        Map<String, Object> errorJson = new HashMap<>();
        errorJson.put("success", false);
        errorJson.put("error", errorMessage);

        response.setStatus(status);
        response.getWriter().write(gson.toJson(errorJson));

        System.out.println("Risposta errore: " + gson.toJson(errorJson));
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