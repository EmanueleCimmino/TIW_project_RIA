package controller;

import daos.UtenteDAO;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import utils.ConnectionHandler;
import utils.TemplateHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Pattern;

@WebServlet("/registrazione")
public class RegistrazioneServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private Connection connection;
    private TemplateEngine templateEngine;

    public RegistrazioneServlet() {
        super();
    }

    @Override
    public void init() throws ServletException{
        try {
            ServletContext context = getServletContext();
            this.connection = ConnectionHandler.getConnection(context);
            this.templateEngine = TemplateHandler.getTemplateEngine(getServletContext());

            if (this.connection == null || this.connection.isClosed()) {
                throw new ServletException("Impossibile stabilire connessione al database");
            }

        }
        catch (UnavailableException | SQLException e) {
            System.err.println("Errore durante l'inizializzazione della servlet: " + e.getMessage());
            e.printStackTrace();
            throw new ServletException("Errore nell'inizializzazione della servlet", e);
        }

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(getServletContext());
        WebContext context = new WebContext(application.buildExchange(request, response));

        context.setVariable("pageTitle", "PoliMusic - Registrazione");


        templateEngine.process("registrazione", context, response.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        String nome = request.getParameter("nome");
        String cognome = request.getParameter("cognome");
        String password = request.getParameter("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()
                || nome == null || nome.isEmpty() || cognome == null || cognome.isEmpty()) {

            response.setContentType("text/html;charset=UTF-8");
            JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(getServletContext());
            WebContext context = new WebContext(application.buildExchange(request, response));            context.setVariable("pageTitle", "PoliMusic - Registrazione");
            context.setVariable("errorMessage", "Tutti i campi sono obbligatori");
            context.setVariable("username", username);
            context.setVariable("nome", nome);
            context.setVariable("cognome", cognome);

            templateEngine.process("registrazione", context, response.getWriter());
            return;
        }

        // Validazione formato username
        Pattern userPattern = Pattern.compile("^[a-zA-Z0-9_]{3,}$");
        if (!userPattern.matcher(username).matches()) {
            response.setContentType("text/html;charset=UTF-8");
            JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(getServletContext());
            WebContext context = new WebContext(application.buildExchange(request, response));            context.setVariable("pageTitle", "PoliMusic - Registrazione");
            context.setVariable("errorMessage", "Username non valido. Deve contenere almeno 3 caratteri alfanumerici o underscore");
            context.setVariable("nome", nome);
            context.setVariable("cognome", cognome);

            templateEngine.process("registrazione", context, response.getWriter());
            return;
        }

        // Validazione formato nome
        Pattern nomePattern = Pattern.compile("^[a-zA-Z0-9_]{3,}$");
        if (!nomePattern.matcher(nome).matches()) {
            response.setContentType("text/html;charset=UTF-8");
            JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(getServletContext());
            WebContext context = new WebContext(application.buildExchange(request, response));            context.setVariable("pageTitle", "PoliMusic - Registrazione");
            context.setVariable("errorMessage", "Nome non valido. Deve contenere almeno 3 caratteri alfanumerici o underscore");
            context.setVariable("username", username);
            context.setVariable("cognome", cognome);

            templateEngine.process("registrazione", context, response.getWriter());
            return;
        }

        // Validazione formato cognome
        Pattern cognomePattern = Pattern.compile("^[a-zA-Z0-9_]{3,}$");
        if (!cognomePattern.matcher(cognome).matches()) {
            response.setContentType("text/html;charset=UTF-8");
            JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(getServletContext());
            WebContext context = new WebContext(application.buildExchange(request, response));            context.setVariable("pageTitle", "PoliMusic - Registrazione");
            context.setVariable("errorMessage", "Cognome non valido. Deve contenere almeno 3 caratteri alfanumerici o underscore");
            context.setVariable("username", username);
            context.setVariable("nome", nome);

            templateEngine.process("registrazione", context, response.getWriter());
            return;
        }

        // Validazione formato password
        Pattern passwordPattern = Pattern.compile("^[a-zA-Z0-9_]{3,}$");
        if (!passwordPattern.matcher(password).matches()) {
            response.setContentType("text/html;charset=UTF-8");
            JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(getServletContext());
            WebContext context = new WebContext(application.buildExchange(request, response));            context.setVariable("pageTitle", "PoliMusic - Registrazione");
            context.setVariable("errorMessage", "Password non valida. Deve contenere almeno 3 caratteri alfanumerici o underscore");
            context.setVariable("username", username);
            context.setVariable("nome", nome);
            context.setVariable("cognome", cognome);

            templateEngine.process("registrazione", context, response.getWriter());
            return;
        }

        // Creazione utente nel database - ottieni la connessione al momento dell'uso
        Connection connection = null;
        try {
            connection = ConnectionHandler.getConnection(getServletContext());
            UtenteDAO utenteDAO = new UtenteDAO(connection);
            utenteDAO.creaUtente(username, nome, cognome, password);

            String path = getServletContext().getContextPath();
            String target = "/home";
            path = path + target;
            response.sendRedirect(path);

        } catch (SQLException e) {
            // Errore nella creazione dell'utente
            response.setContentType("text/html;charset=UTF-8");
            JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(getServletContext());
            WebContext context = new WebContext(application.buildExchange(request, response));            context.setVariable("pageTitle", "PoliMusic - Registrazione");
            context.setVariable("errorMessage", "Impossibile creare un nuovo utente. L'username potrebbe essere già esistente.");
            context.setVariable("nome", nome);
            context.setVariable("cognome", cognome);

            templateEngine.process("registrazione", context, response.getWriter());
            e.printStackTrace();
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
}