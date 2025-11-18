package controller;

import beans.Utente;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import utils.ConnectionHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        super.init();
        gson = new Gson();
        System.out.println("LoginServlet inizializzata con supporto JSON");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Imposta il tipo di risposta come JSON
        response.setContentType("application/json;charset=UTF-8");

        System.out.println("LoginServlet.doPost() chiamato");
        System.out.println("Content-Type ricevuto: " + request.getContentType());

        String username = null;
        String password = null;

        // ✅ Leggi il JSON dal body
        try (BufferedReader reader = request.getReader()) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            String jsonString = sb.toString();
            System.out.println("JSON ricevuto: " + jsonString);

            // ✅ Parsing JSON con Gson
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

            if (jsonObject.has("username")) {
                username = jsonObject.get("username").getAsString();
            }
            if (jsonObject.has("password")) {
                password = jsonObject.get("password").getAsString();
            }

            System.out.println("Username estratto: " + username);
            System.out.println("Password estratta: " + password);

        } catch (Exception e) {
            System.err.println("Errore nel parsing JSON: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, 400, "Formato JSON non valido");
            return;
        }

        // Validazione parametri
        if (username == null || password == null ||
                username.trim().isEmpty() || password.trim().isEmpty()) {
            System.out.println("Errore: parametri mancanti");
            sendErrorResponse(response, 400, "Mancano username e password");
            return;
        }

        // ✅ Verifica credenziali nel database
        Connection connection;
        PreparedStatement statement = null;
        ResultSet result = null;

        try {
            try {
                connection = ConnectionHandler.getConnection(getServletContext());
            } catch (UnavailableException e) {
                System.err.println("ERRORE: impossibile ottenere la connessione dal ConnectionHandler");
                e.printStackTrace();
                sendErrorResponse(response, 500, "Errore del server: impossibile connettersi al database");
                return;
            }

            // Query per verificare le credenziali
            String query = "SELECT * FROM utente WHERE username = ? AND password = ?";
            statement = connection.prepareStatement(query);
            statement.setString(1, username);
            statement.setString(2, password);

            result = statement.executeQuery();

            if (result.next()) {
                // Crea l'oggetto utente con tutti i dati
                Utente utente = new Utente();
                utente.setId_utente(result.getInt("id_utente"));
                utente.setNome(result.getString("nome"));
                utente.setCognome(result.getString("cognome"));
                utente.setUsername(result.getString("username"));
                utente.setPassword(result.getString("password"));

                // Salva l'oggetto nella sessione
                HttpSession session = request.getSession(true);
                session.setAttribute("utente", utente);
                session.setMaxInactiveInterval(30 * 60); // 30 minuti

                System.out.println("Login riuscito per: " + utente.getUsername());

                JsonObject responseJson = new JsonObject();
                responseJson.addProperty("success", true);
                responseJson.addProperty("redirectUrl", "home.html");

                response.setStatus(200);
                response.getWriter().write(gson.toJson(responseJson));
            }
            else {
                // ❌ Credenziali errate
                System.out.println("Login fallito: credenziali errate");
                sendErrorResponse(response, 401, "Username o password errati");
            }

        } catch (SQLException e) {
            System.err.println("Errore database: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, 500, "Errore del server");

        } finally {
            // Chiudi le risorse
            try {
                if (result != null) result.close();
                if (statement != null) statement.close();
                // NON chiudere la connessione, è condivisa!
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Metodo helper per inviare risposte di errore in formato JSON
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String errorMessage)
            throws IOException {
        JsonObject errorJson = new JsonObject();
        errorJson.addProperty("success", false);
        errorJson.addProperty("error", errorMessage);

        response.setStatus(status);
        response.getWriter().write(gson.toJson(errorJson));

        System.out.println("Risposta errore: " + gson.toJson(errorJson));
    }
}