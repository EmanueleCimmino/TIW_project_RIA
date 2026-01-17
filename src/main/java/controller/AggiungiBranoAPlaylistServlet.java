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

        HttpSession session = request.getSession(false);
        Utente utente = (session != null) ? (Utente) session.getAttribute("utente") : null;

        if (utente == null) {
            sendErrorResponse(response, 401, "Utente non autenticato");
            return;
        }

        String contentType = request.getContentType();
        boolean isJson = contentType != null && contentType.contains("application/json");

        int id_playlist;
        ArrayList<Integer> brani_ids = new ArrayList<>();

        if (isJson) {
            try (BufferedReader reader = request.getReader()) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                String jsonString = sb.toString();

                JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

                if (!jsonObject.has("id_playlist")) {
                    sendErrorResponse(response, 400, "ID playlist mancante");
                    return;
                }

                id_playlist = jsonObject.get("id_playlist").getAsInt();

                String arrayKey = jsonObject.has("braniSelezionati") ? "braniSelezionati" : "braniSelezionati_id";

                if (!jsonObject.has(arrayKey)) {
                    sendErrorResponse(response, 400, "Seleziona almeno un brano");
                    return;
                }

                if (jsonObject.get(arrayKey).isJsonArray()) {
                    JsonArray braniSelezionati = jsonObject.getAsJsonArray(arrayKey);
                    for (int i = 0; i < braniSelezionati.size(); i++) {
                        String value = braniSelezionati.get(i).getAsString();
                        brani_ids.add(Integer.parseInt(value));
                    }
                } else {
                    String value = jsonObject.get(arrayKey).getAsString();
                    brani_ids.add(Integer.parseInt(value));
                }

            } catch (Exception e) {
                System.err.println("Errore parsing JSON: " + e.getMessage());
                e.printStackTrace();
                sendErrorResponse(response, 400, "Formato JSON non valido");
                return;
            }

        } else {
            String playlistIdParam = request.getParameter("id_playlist");
            String[] braniSelezionati = request.getParameterValues("braniSelezionati_id");

            if (braniSelezionati == null) {
                braniSelezionati = request.getParameterValues("braniSelezionati");
            }

            if (playlistIdParam == null || playlistIdParam.trim().isEmpty()) {
                sendErrorResponse(response, 400, "ID playlist mancante");
                return;
            }

            try {
                id_playlist = Integer.parseInt(playlistIdParam);
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
                    brani_ids.add(Integer.parseInt(idString));
                } catch (NumberFormatException e) {
                    sendErrorResponse(response, 400, "ID brano non valido: " + idString);
                    return;
                }
            }
        }

        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        BranoDAO branoDAO = new BranoDAO(connection);

        try {
            playlistDAO.controllaUtentePlaylist(id_playlist, utente.getId_utente());

            Playlist playlist = playlistDAO.getPlaylist(id_playlist);
            if (playlist == null) {
                sendErrorResponse(response, 404, "Playlist non trovata");
                return;
            }

            ArrayList<Brano> braniDaAggiungere = new ArrayList<>();

            for (int id_brano : brani_ids) {
                Brano brano = branoDAO.getBrano(id_brano);

                if (brano == null) {
                    sendErrorResponse(response, 400, "Brano non trovato: " + id_brano);
                    return;
                }

                if (brano.getId_utente() != utente.getId_utente()) {
                    sendErrorResponse(response, 403, "Non hai i permessi per aggiungere questo brano");
                    return;
                }

                braniDaAggiungere.add(brano);
            }


            branoDAO.aggiungiBraniAllaPlaylist(braniDaAggiungere, playlist, utente);

            String checkQuery = "SELECT id_brano, ordinamento FROM playlist_brano WHERE id_playlist = ? ORDER BY ordinamento DESC";

            try (PreparedStatement checkPStatement = connection.prepareStatement(checkQuery)) {
                checkPStatement.setInt(1, id_playlist);

                try (ResultSet result = checkPStatement.executeQuery()) {
                    boolean haOrdinamentoPersonalizzato = false;
                    int maxOrdinamento = 0;

                    while (result.next()) {
                        int ordinamento = result.getInt("ordinamento");
                        if (ordinamento > 0) {
                            haOrdinamentoPersonalizzato = true;
                            if (ordinamento > maxOrdinamento) {
                                maxOrdinamento = ordinamento;
                            }
                        }
                    }

                    if (haOrdinamentoPersonalizzato) {
                        int nextOrdinamento = maxOrdinamento + 1;


                        String updateQuery = "UPDATE playlist_brano SET ordinamento = ? WHERE id_playlist = ? AND id_brano = ?";

                        try (PreparedStatement updatePStatement = connection.prepareStatement(updateQuery)) {
                            for (Brano brano : braniDaAggiungere) {
                                updatePStatement.setInt(1, nextOrdinamento);
                                updatePStatement.setInt(2, id_playlist);
                                updatePStatement.setInt(3, brano.getId());
                                updatePStatement.executeUpdate();

                                nextOrdinamento++;
                            }
                        }
                    }
                }
            }

            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("success", true);
            jsonResponse.put("message", "Brani aggiunti con successo alla playlist");
            jsonResponse.put("braniAggiunti", braniDaAggiungere.size());

            response.setStatus(200);
            response.getWriter().write(gson.toJson(jsonResponse));

        } catch (SQLException e) {
            System.err.println("Errore SQL: " + e.getMessage());
            e.printStackTrace();

            if (e.getMessage() != null && e.getMessage().contains("già nella playlist")) {
                sendErrorResponse(response, 400, "Uno o più brani sono già presenti nella playlist");
            } else {
                sendErrorResponse(response, 500, "Errore nel database durante l'aggiunta dei brani");
            }
        }
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String errorMessage)
            throws IOException {
        Map<String, Object> errorJson = new HashMap<>();
        errorJson.put("success", false);
        errorJson.put("error", errorMessage);

        response.setStatus(status);
        response.getWriter().write(gson.toJson(errorJson));

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