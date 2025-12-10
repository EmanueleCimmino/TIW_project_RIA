package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@WebServlet("/AggiungiBranoAPlaylist")
public class AggiungiBranoAPlaylistServlet extends HttpServlet {
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

        int playlistId;
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

                if (!jsonObject.has("playlist_id")) {
                    System.out.println("Errore: playlist_id mancante nel JSON");
                    sendErrorResponse(response, 400, "ID playlist mancante");
                    return;
                }

                playlistId = jsonObject.get("playlist_id").getAsInt();

                // Controlla sia selectedSongs (nuovo nome) che braniSelezionati_id (vecchio nome)
                String arrayKey = jsonObject.has("selectedSongs") ? "selectedSongs" : "braniSelezionati_id";

                if (!jsonObject.has(arrayKey)) {
                    System.out.println("Errore: array brani mancante nel JSON");
                    sendErrorResponse(response, 400, "Seleziona almeno un brano");
                    return;
                }

                // selectedSongs/braniSelezionati_id può essere un array o un singolo valore
                if (jsonObject.get(arrayKey).isJsonArray()) {
                    JsonArray selectedSongs = jsonObject.getAsJsonArray(arrayKey);
                    for (int i = 0; i < selectedSongs.size(); i++) {
                        String value = selectedSongs.get(i).getAsString();
                        braniIds.add(Integer.parseInt(value));
                    }
                } else {
                    String value = jsonObject.get(arrayKey).getAsString();
                    braniIds.add(Integer.parseInt(value));
                }

                System.out.println("Playlist ID: " + playlistId);
                System.out.println("Brani da aggiungere: " + braniIds.size());

            } catch (Exception e) {
                System.err.println("Errore parsing JSON: " + e.getMessage());
                e.printStackTrace();
                sendErrorResponse(response, 400, "Formato JSON non valido");
                return;
            }

        } else {
            // Parsing form-data (fallback per compatibilità con vecchio progetto)
            String playlistIdParam = request.getParameter("playlist_id");
            String[] braniSelezionati = request.getParameterValues("braniSelezionati_id");

            // Se non trova braniSelezionati_id, prova con selectedSongs
            if (braniSelezionati == null) {
                braniSelezionati = request.getParameterValues("selectedSongs");
            }

            System.out.println("Form-data ricevuto");
            System.out.println("playlist_id: " + playlistIdParam);
            System.out.println("brani: " + (braniSelezionati != null ? braniSelezionati.length : "null"));

            if (playlistIdParam == null || playlistIdParam.trim().isEmpty()) {
                sendErrorResponse(response, 400, "ID playlist mancante");
                return;
            }

            try {
                playlistId = Integer.parseInt(playlistIdParam);
            } catch (NumberFormatException e) {
                sendErrorResponse(response, 400, "ID playlist non valido");
                return;
            }

            if (braniSelezionati == null || braniSelezionati.length == 0) {
                sendErrorResponse(response, 400, "Seleziona almeno un brano");
                return;
            }

            for (String idString : braniSelezionati) {
                try {
                    braniIds.add(Integer.parseInt(idString));
                } catch (NumberFormatException e) {
                    sendErrorResponse(response, 400, "ID brano non valido: " + idString);
                    return;
                }
            }
        }

        // Logica di aggiunta brani (identica al vecchio progetto)
        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        BranoDAO branoDAO = new BranoDAO(connection);

        try {
            // Verifica che la playlist appartenga all'utente
            playlistDAO.controllaUtentePlaylist(playlistId, utente.getId_utente());

            // Recupera l'oggetto Playlist
            Playlist playlist = playlistDAO.getPlaylist(playlistId);
            if (playlist == null) {
                System.out.println("Errore: playlist non trovata");
                sendErrorResponse(response, 404, "Playlist non trovata");
                return;
            }

            // Converti gli ID dei brani e recupera gli oggetti Brano
            ArrayList<Brano> braniDaAggiungere = new ArrayList<>();

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

                braniDaAggiungere.add(brano);
            }

            //Gestione ordinamento personalizzato

            // Prima aggiungi i brani alla playlist normalmente
            branoDAO.aggiungiBraniAllaPlaylist(braniDaAggiungere, playlist, utente);

            // Ora recupera TUTTI i brani della playlist per verificare se c'è ordinamento personalizzato
            String checkQuery = "SELECT id_brano, ordinamento FROM playlist_brano WHERE id_playlist = ? ORDER BY ordinamento DESC";

            try (PreparedStatement checkPStatement = connection.prepareStatement(checkQuery)) {
                checkPStatement.setInt(1, playlistId);

                try (ResultSet result = checkPStatement.executeQuery()) {
                    boolean haOrdinamentoPersonalizzato = false;
                    int maxOrdinamento = 0;

                    // Controlla se esiste ordinamento personalizzato
                    while (result.next()) {
                        int ordinamento = result.getInt("ordinamento");
                        if (ordinamento > 0) {
                            haOrdinamentoPersonalizzato = true;
                            if (ordinamento > maxOrdinamento) {
                                maxOrdinamento = ordinamento;
                            }
                        }
                    }

                    System.out.println("Ordinamento personalizzato rilevato: " + haOrdinamentoPersonalizzato);
                    System.out.println("Max ordinamento esistente: " + maxOrdinamento);

                    // Se c'è ordinamento personalizzato, aggiorna i brani appena aggiunti
                    if (haOrdinamentoPersonalizzato) {
                        int nextOrdinamento = maxOrdinamento + 1;

                        System.out.println("Assegnazione ordinamento ai nuovi brani a partire da: " + nextOrdinamento);

                        String updateQuery = "UPDATE playlist_brano SET ordinamento = ? WHERE id_playlist = ? AND id_brano = ?";

                        try (PreparedStatement updatePStatement = connection.prepareStatement(updateQuery)) {
                            for (Brano brano : braniDaAggiungere) {
                                updatePStatement.setInt(1, nextOrdinamento);
                                updatePStatement.setInt(2, playlistId);
                                updatePStatement.setInt(3, brano.getId());
                                updatePStatement.executeUpdate();

                                System.out.println("Brano \"" + brano.getTitolo() + "\" - ordinamento impostato a: " + nextOrdinamento);
                                nextOrdinamento++;
                            }
                        }
                    }
                }
            }

            // Risposta di successo (JSON per RIA)
            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("success", true);
            jsonResponse.put("message", "Brani aggiunti con successo alla playlist");
            jsonResponse.put("braniAggiunti", braniDaAggiungere.size());

            response.setStatus(200);
            response.getWriter().write(gson.toJson(jsonResponse));

        } catch (SQLException e) {
            System.err.println("Errore SQL: " + e.getMessage());
            e.printStackTrace();

            // Se il brano è già nella playlist
            if (e.getMessage() != null && e.getMessage().contains("già nella playlist")) {
                sendErrorResponse(response, 400, "Uno o più brani sono già presenti nella playlist");
            } else {
                sendErrorResponse(response, 500, "Errore nel database durante l'aggiunta dei brani");
            }
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