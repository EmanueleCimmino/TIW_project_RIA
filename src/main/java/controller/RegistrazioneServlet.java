package controller;

import beans.Utente;
import daos.UtenteDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import utils.ConnectionHandler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Pattern;

@WebServlet("/registrazione")
public class RegistrazioneServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        super.init();
        gson = new Gson();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // Imposta il tipo di risposta come JSON
        response.setContentType("application/json;charset=UTF-8");

        String username = null;
        String nome = null;
        String cognome = null;
        String password = null;

        // Leggi il JSON dal body
        try (BufferedReader reader = request.getReader()) {
            StringBuilder sb = new StringBuilder();
            String riga;
            while ((riga = reader.readLine()) != null) {
                sb.append(riga);
            }

            String jsonString = sb.toString();

            // Parsing JSON con Gson
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

            if (jsonObject.has("username")) {
                username = jsonObject.get("username").getAsString();
            }
            if (jsonObject.has("nome")) {
                nome = jsonObject.get("nome").getAsString();
            }
            if (jsonObject.has("cognome")) {
                cognome = jsonObject.get("cognome").getAsString();
            }
            if (jsonObject.has("password")) {
                password = jsonObject.get("password").getAsString();
            }


        } catch (Exception e) {
            System.err.println("Errore nel parsing JSON: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, 400, "Formato JSON non valido");
            return;
        }

        // Validazione parametri
        if (username == null || password == null || nome == null || cognome == null ||
                username.trim().isEmpty() || password.trim().isEmpty() ||
                nome.trim().isEmpty() || cognome.trim().isEmpty()) {
            sendErrorResponse(response, 400, "Tutti i campi sono obbligatori");
            return;
        }

        // Validazione formato username
        Pattern userPattern = Pattern.compile("^[a-zA-Z0-9_]{3,}$");
        if (!userPattern.matcher(username).matches()) {
            sendErrorResponse(response, 400, "Username non valido. Deve contenere almeno 3 caratteri alfanumerici o underscore");
            return;
        }

        // Validazione formato nome
        Pattern nomePattern = Pattern.compile("^[a-zA-Z0-9_]{3,}$");
        if (!nomePattern.matcher(nome).matches()) {
            sendErrorResponse(response, 400, "Nome non valido. Deve contenere almeno 3 caratteri alfanumerici o underscore");
            return;
        }

        // Validazione formato cognome
        Pattern cognomePattern = Pattern.compile("^[a-zA-Z0-9_]{3,}$");
        if (!cognomePattern.matcher(cognome).matches()) {
            sendErrorResponse(response, 400, "Cognome non valido. Deve contenere almeno 3 caratteri alfanumerici o underscore");
            return;
        }

        // Validazione formato password
        Pattern passwordPattern = Pattern.compile("^[a-zA-Z0-9_]{3,}$");
        if (!passwordPattern.matcher(password).matches()) {
            sendErrorResponse(response, 400, "Password non valida. Deve contenere almeno 3 caratteri alfanumerici o underscore");
            return;
        }

        // Creazione utente nel database
        Connection connection = null;
        try {
            connection = ConnectionHandler.getConnection(getServletContext());
            UtenteDAO utenteDAO = new UtenteDAO(connection);

            // Crea l'utente
            utenteDAO.creaUtente(username, nome, cognome, password);

            // Recupera l'utente creato per ottenere l'ID
            Utente nuovoUtente = utenteDAO.getUtenteDaUsername(username);

            if (nuovoUtente != null) {
                // Salva l'utente nella sessione (auto-login dopo registrazione)
                HttpSession session = request.getSession(true);
                session.setAttribute("utente", nuovoUtente);
                session.setMaxInactiveInterval(30 * 60); // 30 minuti

                // Risposta JSON di successo
                JsonObject responseJson = new JsonObject();
                responseJson.addProperty("success", true);
                responseJson.addProperty("redirectUrl", "index.html");

                response.setStatus(200);
                response.getWriter().write(gson.toJson(responseJson));
            } else {
                sendErrorResponse(response, 500, "Errore durante la creazione dell'utente");
            }

        } catch (SQLException | UnavailableException e) {
            System.err.println("Errore database: " + e.getMessage());
            e.printStackTrace();

            // Verifica se è un errore di duplicazione username
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate")) {
                sendErrorResponse(response, 409, "Username già esistente. Scegline un altro.");
            } else {
                sendErrorResponse(response, 500, "Impossibile creare un nuovo utente. Riprova più tardi.");
            }

        } finally {
            // Chiudi la connessione se è stata aperta
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                }
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

    }
}