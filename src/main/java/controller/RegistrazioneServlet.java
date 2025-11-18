/*package controller;

import beans.Utente;
import daos.UtenteDAO;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import utils.ConnectionHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.gson.Gson;

@WebServlet("/registrazione")
public class RegistrazioneServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private Connection connection;

    public RegistrazioneServlet() {
        super();
    }

    @Override
    public void init() throws ServletException {
        try {
            ServletContext context = getServletContext();
            this.connection = ConnectionHandler.getConnection(context);

            if (this.connection == null || this.connection.isClosed()) {
                throw new ServletException("Impossibile stabilire connessione al database");
            }

            System.out.println("RegistrazioneServlet inizializzata correttamente con connessione al DB");

        } catch (UnavailableException | SQLException e) {
            System.err.println("Errore durante l'inizializzazione della servlet: " + e.getMessage());
            e.printStackTrace();
            throw new ServletException("Errore nell'inizializzazione della servlet", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("RegistrazioneServlet.doPost() chiamato");

        String username = request.getParameter("username");
        String nome = request.getParameter("nome");
        String cognome = request.getParameter("cognome");
        String password = request.getParameter("password");

        Map<String, Object> jsonResponse = new HashMap<>();
        Gson gson = new Gson();
        String error = null;

        // Log parametri ricevuti
        System.out.println("Username: " + username);
        System.out.println("Nome: " + nome);
        System.out.println("Cognome: " + cognome);
        System.out.println("Password: " + (password != null ? "***" : "null"));

        // Validazione campi obbligatori
        if (username == null || password == null || username.isEmpty() || password.isEmpty()
                || nome == null || nome.isEmpty() || cognome == null || cognome.isEmpty()) {
            error = "Tutti i campi sono obbligatori";
        }
        // Validazione formato username
        else if (!Pattern.compile("^[a-zA-Z0-9_]{3,}$").matcher(username).matches()) {
            error = "Username non valido. Deve contenere almeno 3 caratteri alfanumerici o underscore";
        }
        // Validazione formato nome
        else if (!Pattern.compile("^[a-zA-Z0-9_]{3,}$").matcher(nome).matches()) {
            error = "Nome non valido. Deve contenere almeno 3 caratteri alfanumerici o underscore";
        }
        // Validazione formato cognome
        else if (!Pattern.compile("^[a-zA-Z0-9_]{3,}$").matcher(cognome).matches()) {
            error = "Cognome non valido. Deve contenere almeno 3 caratteri alfanumerici o underscore";
        }
        // Validazione formato password
        else if (!Pattern.compile("^[a-zA-Z0-9_]{3,}$").matcher(password).matches()) {
            error = "Password non valida. Deve contenere almeno 3 caratteri alfanumerici o underscore";
        }

        // Se ci sono errori di validazione, ritorna subito
        if (error != null) {
            System.out.println("Errore validazione: " + error);

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            jsonResponse.put("success", false);
            jsonResponse.put("error", error);

            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        // Creazione utente nel database
        Connection conn = null;
        try {
            // Ottieni connessione fresca
            conn = ConnectionHandler.getConnection(getServletContext());
            UtenteDAO utenteDAO = new UtenteDAO(conn);

            System.out.println("Tentativo creazione utente: " + username);

            // Crea l'utente
            utenteDAO.creaUtente(username, nome, cognome, password);

            System.out.println("Utente creato con successo: " + username);

            // Recupera l'utente appena creato per metterlo in sessione
            Utente nuovoUtente = utenteDAO.controlloAutenticazione(username, password);

            if (nuovoUtente != null) {
                // Crea sessione e salva utente
                HttpSession session = request.getSession(true);
                session.setAttribute("utente", nuovoUtente);

                System.out.println("Utente inserito in sessione");
            }

            // Risposta di successo
            response.setStatus(HttpServletResponse.SC_OK); // 200
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            jsonResponse.put("success", true);
            jsonResponse.put("message", "Registrazione completata con successo!");
            jsonResponse.put("redirectUrl", getServletContext().getContextPath() + "/home.html");
            jsonResponse.put("username", username);

            String jsonSuccess = gson.toJson(jsonResponse);
            System.out.println("Risposta successo: " + jsonSuccess);
            response.getWriter().write(jsonSuccess);

        } catch (SQLException e) {
            // Errore nella creazione dell'utente
            System.err.println("Errore SQL durante registrazione: " + e.getMessage());
            e.printStackTrace();

            response.setStatus(HttpServletResponse.SC_CONFLICT); // 409
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            jsonResponse.put("success", false);
            jsonResponse.put("error", "Impossibile creare un nuovo utente. L'username potrebbe essere già esistente.");

            response.getWriter().write(gson.toJson(jsonResponse));

        } catch (UnavailableException e) {
            System.err.println("Errore di connessione durante registrazione: " + e.getMessage());
            e.printStackTrace();

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            jsonResponse.put("success", false);
            jsonResponse.put("error", "Servizio temporaneamente non disponibile");

            response.getWriter().write(gson.toJson(jsonResponse));

        } finally {
            // Chiudi la connessione se è stata aperta
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    @Override
    public void destroy() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Connessione DB chiusa");
            } catch (SQLException e) {
                System.err.println("Errore nella chiusura della connessione: " + e.getMessage());
            }
        }
    }
}*/