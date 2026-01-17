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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import utils.ConnectionHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private Connection connection;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        super.init();
        gson = new Gson();

        try {
            connection = ConnectionHandler.getConnection(getServletContext());

            if (connection == null || connection.isClosed()) {
                throw new ServletException("Impossibile stabilire connessione al database");
            }


        } catch (UnavailableException | SQLException e) {
            System.err.println("Errore durante l'inizializzazione della servlet: " + e.getMessage());
            e.printStackTrace();
            throw new ServletException("Errore nell'inizializzazione della servlet", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json;charset=UTF-8");

        String username = null;
        String password = null;

        try (BufferedReader reader = request.getReader()) {
            StringBuilder sb = new StringBuilder();
            String riga;
            while ((riga = reader.readLine()) != null) {
                sb.append(riga);
            }

            String jsonString = sb.toString();

            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

            if (jsonObject.has("username")) {
                username = jsonObject.get("username").getAsString();
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

        if (username == null || password == null ||
                username.trim().isEmpty() || password.trim().isEmpty()) {
            sendErrorResponse(response, 400, "Mancano username e password");
            return;
        }

        try {
            if (connection == null || connection.isClosed()) {
                connection = ConnectionHandler.getConnection(getServletContext());
            }

            UtenteDAO utenteDAO = new UtenteDAO(connection);
            Utente utente = utenteDAO.controlloAutenticazione(username, password);

            if (utente != null) {
                HttpSession session = request.getSession(true);
                session.setAttribute("utente", utente);
                session.setMaxInactiveInterval(30 * 60);


                JsonObject responseJson = new JsonObject();
                responseJson.addProperty("success", true);
                responseJson.addProperty("redirectUrl", "home.html");

                response.setStatus(200);
                response.getWriter().write(gson.toJson(responseJson));

            } else {
                sendErrorResponse(response, 401, "Username o password errati");
            }

        } catch (SQLException e) {
            System.err.println("Errore database: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, 500, "Errore del server durante l'autenticazione");

        } catch (UnavailableException e) {
            System.err.println("Errore di connessione: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, 500, "Servizio temporaneamente non disponibile");
        }
    }


    private void sendErrorResponse(HttpServletResponse response, int status, String errorMessage)
            throws IOException {
        JsonObject errorJson = new JsonObject();
        errorJson.addProperty("success", false);
        errorJson.addProperty("error", errorMessage);

        response.setStatus(status);
        response.getWriter().write(gson.toJson(errorJson));

    }

    @Override
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