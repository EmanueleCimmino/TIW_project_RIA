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

            System.out.println("CreaPlaylistServlet inizializzata correttamente");

        } catch (UnavailableException | SQLException e) {
            System.err.println("Errore durante l'inizializzazione della servlet: " + e.getMessage());
            e.printStackTrace();
            throw new ServletException("Errore nell'inizializzazione della servlet", e);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");

        System.out.println("CreaPlaylistServlet.doPost() chiamato");
        System.out.println("Content-Type: " + request.getContentType());

        // Verifica sessione utente
        HttpSession session = request.getSession(false);
        Utente utente = (session != null) ? (Utente) session.getAttribute("utente") : null;

        if (utente == null) {
            System.out.println("Errore: utente non autenticato");
            sendErrorResponse(response, 401, "Utente non autenticato");
            return;
        }

        System.out.println("Utente autenticato: " + utente.getUsername());

        // Determina se è JSON o form-data
        String contentType = request.getContentType();
        boolean isJson = contentType != null && contentType.contains("application/json");

        String playlistTitolo;
        ArrayList<Integer> braniIds = new ArrayList<>();

        if (isJson) {
            // Parsing JSON
            try (BufferedReader reader = request.getReader()) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                String jsonString = sb.toString();
                System.out.println("JSON ricevuto: " + jsonString);

                JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

                if (!jsonObject.has("playlist_titolo")) {
                    System.out.println("Errore: playlist_titolo mancante nel JSON");
                    sendErrorResponse(response, 400, "Il titolo della playlist è obbligatorio");
                    return;
                }

                playlistTitolo = jsonObject.get("playlist_titolo").getAsString().trim();

                if (playlistTitolo.isEmpty()) {
                    System.out.println("Errore: playlist_titolo vuoto");
                    sendErrorResponse(response, 400, "Il titolo della playlist è obbligatorio");
                    return;
                }

                if (playlistTitolo.length() > 50) {
                    System.out.println("Errore: playlist_titolo troppo lungo");
                    sendErrorResponse(response, 400, "Il titolo della playlist non può superare i 50 caratteri");
                    return;
                }

                if (!jsonObject.has("braniSelezionati")) {
                    System.out.println("Errore: braniSelezionati mancante nel JSON");
                    sendErrorResponse(response, 400, "Seleziona almeno un brano per creare la playlist");
                    return;
                }

                // braniSelezionati può essere un array o un singolo valore
                if (jsonObject.get("braniSelezionati").isJsonArray()) {
                    JsonArray braniArray = jsonObject.getAsJsonArray("braniSelezionati");
                    for (int i = 0; i < braniArray.size(); i++) {
                        String value = braniArray.get(i).getAsString();
                        braniIds.add(Integer.parseInt(value));
                    }
                } else {
                    String value = jsonObject.get("braniSelezionati").getAsString();
                    braniIds.add(Integer.parseInt(value));
                }

                if (braniIds.isEmpty()) {
                    System.out.println("Errore: nessun brano selezionato");
                    sendErrorResponse(response, 400, "Seleziona almeno un brano per creare la playlist");
                    return;
                }

                System.out.println("Playlist titolo: " + playlistTitolo);
                System.out.println("Brani selezionati: " + braniIds.size());

            } catch (Exception e) {
                System.err.println("Errore parsing JSON: " + e.getMessage());
                e.printStackTrace();
                sendErrorResponse(response, 400, "Formato JSON non valido");
                return;
            }

        } else {
            // Parsing form-data (fallback per compatibilità)
            playlistTitolo = request.getParameter("playlist_titolo");
            String[] braniSelezionatiIds = request.getParameterValues("braniSelezionati");

            System.out.println("Form-data ricevuto");
            System.out.println("playlist_titolo: " + playlistTitolo);
            System.out.println("braniSelezionati: " + (braniSelezionatiIds != null ? braniSelezionatiIds.length : "null"));

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
                    braniIds.add(Integer.parseInt(idString));
                } catch (NumberFormatException e) {
                    sendErrorResponse(response, 400, "ID brano non valido: " + idString);
                    return;
                }
            }
        }

        // Logica di creazione playlist (identica al vecchio progetto)
        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        BranoDAO branoDAO = new BranoDAO(connection);

        try {
            // Converti gli ID dei brani e recupera gli oggetti Brano
            List<Brano> braniSelezionati = new ArrayList<>();

            for (int branoId : braniIds) {
                Brano brano = branoDAO.getBrano(branoId);

                if (brano == null) {
                    System.out.println("Errore: brano non trovato - ID: " + branoId);
                    sendErrorResponse(response, 400, "Brano non trovato: " + branoId);
                    return;
                }

                // Verifica che il brano appartenga all'utente (sicurezza)
                if (brano.getId_utente() != utente.getId_utente()) {
                    System.out.println("Errore: permessi insufficienti per brano ID: " + branoId);
                    sendErrorResponse(response, 403, "Non hai i permessi per aggiungere questo brano");
                    return;
                }

                braniSelezionati.add(brano);
            }

            // Crea la playlist utilizzando il metodo esistente del DAO
            playlistDAO.creaPlaylist(playlistTitolo, braniSelezionati, utente);

            System.out.println("✅ Playlist creata con successo: " + playlistTitolo);

            // Risposta di successo (JSON per RIA)
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

    /**
     * Metodo helper per inviare risposte di errore in formato JSON
     */
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