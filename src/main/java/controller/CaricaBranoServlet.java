package controller;

import beans.Utente;
import daos.BranoDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import utils.ConnectionHandler;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/caricaBrano")
@MultipartConfig(maxFileSize = 1024 * 1024 * 50) // massimo 50MB
public class CaricaBranoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    // Path assoluto alla cartella Audio
    private static final String UPLOAD_FOLDER = "C:\\Users\\emanu\\Audio";

    @Override
    public void init() throws ServletException {
        connection = ConnectionHandler.getConnection(getServletContext());

        // Crea la cartella se non esiste
        File uploadDir = new File(UPLOAD_FOLDER);
        if (!uploadDir.exists()) uploadDir.mkdirs();

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        Utente utente = (Utente) session.getAttribute("utente");

        if (utente == null) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        String titolo = request.getParameter("titolo");
        String idAlbumStr = request.getParameter("id_album");
        Part filePart = request.getPart("file_musicale");

        if (titolo == null || titolo.trim().isEmpty() || idAlbumStr == null ||
                idAlbumStr.trim().isEmpty() || filePart == null || filePart.getSize() == 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Dati mancanti o file non caricato");
            return;
        }

        try {
            int idAlbum = Integer.parseInt(idAlbumStr);

            // Recupera solo il nome del file
            String submittedFileName = filePart.getSubmittedFileName();
            String fileName = submittedFileName.substring(Math.max(submittedFileName.lastIndexOf("\\"), submittedFileName.lastIndexOf("/")) + 1);

            // Salva il file nella cartella esterna
            String filePath = UPLOAD_FOLDER + File.separator + fileName;
            filePart.write(filePath);

            BranoDAO branoDAO = new BranoDAO(connection);
            branoDAO.caricaBrano(titolo, fileName, utente.getId_utente(), idAlbum);

            response.sendRedirect(request.getContextPath() + "/home");

        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID album non valido");
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore durante il caricamento del brano");
        }
    }

    @Override
    public void destroy() {
        try {
            if (connection != null && !connection.isClosed()) {
                ConnectionHandler.closeConnection(connection);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
