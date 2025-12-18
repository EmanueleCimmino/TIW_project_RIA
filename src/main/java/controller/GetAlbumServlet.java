package controller;

import beans.Album;
import beans.Brano;
import beans.Utente;
import daos.AlbumDAO;
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

@WebServlet("/GetAlbum")
public class GetAlbumServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String IMAGE_PATH = "C:\\Users\\emanu\\Immagini\\";
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
            throws  IOException {

        // Verifica sessione utente
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendError(401, "Sessione non valida");
            return;
        }

        Utente utente = (Utente) session.getAttribute("utente");
        if (utente == null) {
            response.sendError(401, "Utente non autenticato");
            return;
        }

        // Recupera parametro brano_id
        String brano_id_param = request.getParameter("brano_id");

        if (brano_id_param == null || brano_id_param.isEmpty()) {
            response.sendError(400, "ID brano mancante");
            return;
        }

        int brano_id;
        try {
            brano_id = Integer.parseInt(brano_id_param);
        } catch (NumberFormatException e) {
            response.sendError(400, "ID brano non valido");
            return;
        }

        BranoDAO branoDAO = new BranoDAO(connection);
        AlbumDAO albumDAO = new AlbumDAO(connection);

        try {
            // Recupera il brano per verificare che appartenga all'utente
            Brano brano = branoDAO.getBrano(brano_id);

            if (brano == null) {
                response.sendError(404, "Brano non trovato");
                return;
            }

            // Verifica che il brano appartenga all'utente (sicurezza)
            if (brano.getId_utente() != utente.getId_utente()) {
                response.sendError(403, "Accesso negato");
                return;
            }

            // Recupera l'album associato al brano
            Album album = albumDAO.getAlbum(brano.getId_album());

            if (album == null) {
                response.sendError(404, "Album non trovato");
                return;
            }

            // Costruisci il path completo del file immagine
            String nome_file = album.getImmagine_album();
            File immagine = new File(IMAGE_PATH + nome_file);

            // Verifica che il file esista
            if (!immagine.exists() || !immagine.isFile()) {
                response.sendError(404, "Immagine album non trovata");
                return;
            }

            // Verifica che il file sia all'interno della directory consentita (sicurezza)
            String path = immagine.getCanonicalPath();
            if (!path.startsWith(new File(IMAGE_PATH).getCanonicalPath())) {
                response.sendError(403, "Accesso negato");
                return;
            }

            // Imposta il content type appropriato
            String mimeType = getServletContext().getMimeType(immagine.getName());
            if (mimeType == null) {
                mimeType = "image/jpeg"; // Default per JPG
            }
            response.setContentType(mimeType);
            response.setContentLengthLong(immagine.length());

            // Invia il file
            try (FileInputStream fileInputStream = new FileInputStream(immagine);
                 OutputStream outputStream = response.getOutputStream()) {

                byte[] buffer = new byte[8192];
                int byteLetti;
                while ((byteLetti = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, byteLetti);
                }
            }

        } catch (SQLException e) {
            System.err.println("Errore nel recupero dei dati dell'album: " + e.getMessage());
            e.printStackTrace();
            response.sendError(500,
                    "Errore nel recupero dell'immagine");
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
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