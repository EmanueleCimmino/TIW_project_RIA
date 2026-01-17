package controller;

import beans.Utente;
import daos.BranoDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import utils.ConnectionHandler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/caricaBrano")
@MultipartConfig(maxFileSize = 1024 * 1024 * 50)
public class CaricaBranoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;
    private Gson gson;

    private static final String UPLOAD_FOLDER = "C:\\Users\\emanu\\Audio";

    @Override
    public void init() throws ServletException {
        connection = ConnectionHandler.getConnection(getServletContext());
        gson = new Gson();

        System.setProperty("java.io.tmpdir", UPLOAD_FOLDER);

        File uploadDir = new File(UPLOAD_FOLDER);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");

        HttpSession session = request.getSession(false);
        Utente utente = (Utente) session.getAttribute("utente");

        if (utente == null) {
            sendErrorResponse(response, 401, "Sessione scaduta. Effettua di nuovo il login.");
            return;
        }

        String titolo = request.getParameter("titolo");
        String idAlbumStr = request.getParameter("id_album");
        Part filePart = request.getPart("file_musicale");

        if (titolo == null || titolo.trim().isEmpty()) {
            sendErrorResponse(response, 400, "Il titolo del brano è obbligatorio");
            return;
        }

        if (idAlbumStr == null || idAlbumStr.trim().isEmpty()) {
            sendErrorResponse(response, 400, "Devi selezionare un album");
            return;
        }

        if (filePart == null || filePart.getSize() == 0) {
            sendErrorResponse(response, 400, "Devi caricare un file audio");
            return;
        }

        try {
            int idAlbum = Integer.parseInt(idAlbumStr);

            String submittedFileName = filePart.getSubmittedFileName();
            String fileName = submittedFileName.substring(
                    Math.max(
                            submittedFileName.lastIndexOf("\\"),
                            submittedFileName.lastIndexOf("/")
                    ) + 1
            );

            String contentType = filePart.getContentType();
            if (contentType == null || !contentType.startsWith("audio/")) {
                sendErrorResponse(response, 400, "Formato file non valido. Usa MP3, WAV o OGG");
                return;
            }

            filePart.write(fileName);

            File savedFile = new File(UPLOAD_FOLDER, fileName);
            if (!savedFile.exists()) {
                System.err.println("❌ ERRORE: File non salvato correttamente");
                sendErrorResponse(response, 500, "Errore nel salvataggio del file");
                return;
            }

            BranoDAO branoDAO = new BranoDAO(connection);
            branoDAO.caricaBrano(titolo, fileName, utente.getId_utente(), idAlbum);

            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("success", true);
            responseJson.addProperty("message", "Brano \"" + titolo + "\" caricato con successo!");

            response.setStatus(200);
            response.getWriter().write(gson.toJson(responseJson));

        } catch (NumberFormatException e) {
            System.err.println("Errore: ID album non valido - " + e.getMessage());
            sendErrorResponse(response, 400, "ID album non valido");
        } catch (SQLException e) {
            System.err.println("Errore database: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, 500, "Errore durante il caricamento del brano nel database");
        } catch (Exception e) {
            System.err.println("Errore generico: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, 500, "Errore durante il caricamento del brano");
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
            e.printStackTrace();
        }
    }
}