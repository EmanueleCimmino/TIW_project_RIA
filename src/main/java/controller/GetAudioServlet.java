package controller;

import beans.Brano;
import beans.Utente;
import daos.BranoDAO;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import utils.ConnectionHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/GetAudio")
public class GetAudioServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String AUDIO_PATH = "C:\\Users\\emanu\\Audio\\";
    private Connection connection = null;

    public void init() throws ServletException {
        try {
            ServletContext context = getServletContext();
            this.connection = ConnectionHandler.getConnection(context);

            if (this.connection == null || this.connection.isClosed()) {
                throw new ServletException("Impossibile stabilire connessione al database");
            }

        } catch (UnavailableException | SQLException e) {
            System.err.println("Errore durante l'inizializzazione della servlet: " + e.getMessage());
            e.printStackTrace();
            throw new ServletException("Errore nell'inizializzazione della servlet", e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Verifica sessione utente
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Sessione non valida");
            return;
        }

        Utente utente = (Utente) session.getAttribute("utente");
        if (utente == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Utente non autenticato");
            return;
        }

        // Recupera parametro brano_id
        String brano_id_param = request.getParameter("brano_id");

        if (brano_id_param == null || brano_id_param.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID brano mancante");
            return;
        }

        int brano_id;
        try {
            brano_id = Integer.parseInt(brano_id_param);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID brano non valido");
            return;
        }

        BranoDAO branoDAO = new BranoDAO(connection);

        try {
            // Recupera il brano
            Brano brano = branoDAO.getBrano(brano_id);

            if (brano == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Brano non trovato");
                return;
            }

            // Verifica che il brano appartenga all'utente (sicurezza)
            if (brano.getId_utente() != utente.getId_utente()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accesso negato");
                return;
            }

            // Costruisci il path completo del file audio
            String nome_file = brano.getFile_musicale();
            File audio = new File(AUDIO_PATH + nome_file);

            // Verifica che il file esista
            if (!audio.exists() || !audio.isFile()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "File audio non trovato");
                return;
            }

            // Verifica che il file sia all'interno della directory consentita (sicurezza)
            String path = audio.getCanonicalPath();
            if (!path.startsWith(new File(AUDIO_PATH).getCanonicalPath())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accesso negato");
                return;
            }

            // Imposta il content type appropriato
            String mimeType = getServletContext().getMimeType(audio.getName());
            if (mimeType == null) {
                mimeType = "audio/mpeg"; // Default per MP3
            }
            response.setContentType(mimeType);
            response.setContentLengthLong(audio.length());

            // Invia il file
            try (FileInputStream fileInputStream = new FileInputStream(audio);
                 OutputStream outputStream = response.getOutputStream()) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

        } catch (SQLException e) {
            System.err.println("Errore nel recupero dei dati del brano: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Errore nel recupero del file audio");
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
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