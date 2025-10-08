package controller;

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
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import utils.ConnectionHandler;
import utils.TemplateHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private Connection connection;
    private TemplateEngine templateEngine;

    public LoginServlet() {
        super();
    }

    public void init() throws ServletException {
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

        try {
            JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(getServletContext());
            WebContext context = new WebContext(application.buildExchange(request, response));

            context.setVariable("pageTitle", "Login");
            context.setVariable("message", "Inserisci le tue credenziali");

            String html = templateEngine.process("login", context);

            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(html);

        } catch (Exception e) {
            System.err.println("Errore in doGet: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nella visualizzazione della pagina");
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(getServletContext());
        WebContext context = new WebContext(application.buildExchange(request, response));

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String error = null;

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            error = "Mancano username e password";
        }
        else {
            try {
                if (this.connection == null || this.connection.isClosed()) {
                    this.connection = ConnectionHandler.getConnection(getServletContext());
                }

                UtenteDAO dao = new UtenteDAO(this.connection);
                Utente utente = dao.controlloAutenticazione(username, password);

                if(utente != null) {
                    HttpSession session = request.getSession(true);
                    session.setAttribute("utente", utente);


                } else {
                    error = "Credenziali non valide";
                }
            } catch (SQLException e) {
                e.printStackTrace();
                error = "Errore DB: " + e.getMessage();
            } catch (UnavailableException e) {
                System.err.println("Errore di connessione: " + e.getMessage());
                e.printStackTrace();
                error = "Servizio temporaneamente non disponibile";
            }
        }

        if(error != null) {
            context.setVariable("pageTitle", "Login");
            context.setVariable("error", error);
            context.setVariable("username", username);

            String html = templateEngine.process("login", context);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(html);
            return;
        }

        String path = getServletContext().getContextPath() + "/home";
        response.sendRedirect(path);
    }

    public void destroy() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Errore nella chiusura della connessione: " + e.getMessage());
            }
        }
    }
}