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
@MultipartConfig(maxFileSize = 1024 * 1024 * 50) // massimo 50MB
public class CaricaBranoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;
    private Gson gson;

    // Path assoluto alla cartella Audio (usato anche per GET audio)
    private static final String UPLOAD_FOLDER = "C:\\Users\\emanu\\Audio";

    @Override
    public void init() throws ServletException {
        connection = ConnectionHandler.getConnection(getServletContext());
        gson = new Gson();

        // *** IMPORTANTE: forziamo java.io.tmpdir sulla tua cartella esistente
        // così Tomcat userà questa come temp dir per gli upload
        System.setProperty("java.io.tmpdir", UPLOAD_FOLDER);

        // Crea la cartella se non esiste
        File uploadDir = new File(UPLOAD_FOLDER);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        System.out.println("CaricaBranoServlet inizializzata correttamente");
        System.out.println("java.io.tmpdir impostato su: " + System.getProperty("java.io.tmpdir"));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Imposta risposta JSON
        response.setContentType("application/json;charset=UTF-8");

        System.out.println("CaricaBranoServlet.doPost() chiamato");

        HttpSession session = request.getSession(false);
        Utente utente = (Utente) session.getAttribute("utente");

        if (utente == null) {
            System.out.println("Utente non autenticato");
            sendErrorResponse(response, 401, "Sessione scaduta. Effettua di nuovo il login.");
            return;
        }

        System.out.println("Caricamento brano per utente: " + utente.getUsername());

        // Recupera parametri multipart
        String titolo = request.getParameter("titolo");
        String idAlbumStr = request.getParameter("id_album");
        Part filePart = request.getPart("file_musicale");

        System.out.println("Titolo: " + titolo);
        System.out.println("ID Album: " + idAlbumStr);
        System.out.println("File part: " + (filePart != null ? filePart.getSubmittedFileName() : "null"));

        // Validazione parametri
        if (titolo == null || titolo.trim().isEmpty()) {
            System.out.println("Errore: titolo mancante");
            sendErrorResponse(response, 400, "Il titolo del brano è obbligatorio");
            return;
        }

        if (idAlbumStr == null || idAlbumStr.trim().isEmpty()) {
            System.out.println("Errore: album non selezionato");
            sendErrorResponse(response, 400, "Devi selezionare un album");
            return;
        }

        if (filePart == null || filePart.getSize() == 0) {
            System.out.println("Errore: file non caricato");
            sendErrorResponse(response, 400, "Devi caricare un file audio");
            return;
        }

        try {
            int idAlbum = Integer.parseInt(idAlbumStr);

            // Recupera solo il nome del file (rimuovi percorso se presente)
            String submittedFileName = filePart.getSubmittedFileName();
            String fileName = submittedFileName.substring(
                    Math.max(
                            submittedFileName.lastIndexOf("\\"),
                            submittedFileName.lastIndexOf("/")
                    ) + 1
            );

            System.out.println("Nome file pulito: " + fileName);
            System.out.println("java.io.tmpdir = " + System.getProperty("java.io.tmpdir"));
            System.out.println("UPLOAD_FOLDER exists: " + new File(UPLOAD_FOLDER).exists());
            System.out.println("filePart size: " + filePart.getSize());

            // Verifica formato file
            String contentType = filePart.getContentType();
            if (contentType == null || !contentType.startsWith("audio/")) {
                System.out.println("Errore: formato file non valido - " + contentType);
                sendErrorResponse(response, 400, "Formato file non valido. Usa MP3, WAV o OGG");
                return;
            }

            // Salva il file usando solo il nome (senza percorso)
            // Tomcat userà java.io.tmpdir come base
            filePart.write(fileName);

            // Verifica che il file sia stato salvato
            File savedFile = new File(UPLOAD_FOLDER, fileName);
            if (!savedFile.exists()) {
                System.err.println("❌ ERRORE: File non salvato correttamente");
                sendErrorResponse(response, 500, "Errore nel salvataggio del file");
                return;
            }

            System.out.println("✅ File salvato con successo - Dimensione: " + savedFile.length() + " bytes");

            // Salva nel database
            BranoDAO branoDAO = new BranoDAO(connection);
            branoDAO.caricaBrano(titolo, fileName, utente.getId_utente(), idAlbum);

            System.out.println("✅ Brano salvato nel database con successo");

            // Risposta JSON di successo
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