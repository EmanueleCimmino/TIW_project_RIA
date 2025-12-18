package controller;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.SQLException;

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

@WebServlet("/GetAudio")
public class GetAudioServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String AUDIO_PATH = "C:\\Users\\emanu\\Audio\\";
    private static final int BUFFER_SIZE = 8192; // 8KB buffer
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
            throws IOException {

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
        String branoIdParam = request.getParameter("brano_id");

        if (branoIdParam == null || branoIdParam.isEmpty()) {
            response.sendError(400, "ID brano mancante");
            return;
        }

        int branoId;
        try {
            branoId = Integer.parseInt(branoIdParam);
        } catch (NumberFormatException e) {
            response.sendError(400, "ID brano non valido");
            return;
        }

        BranoDAO branoDAO = new BranoDAO(connection);

        try {
            // Recupera il brano
            Brano brano = branoDAO.getBrano(branoId);

            if (brano == null) {
                response.sendError(404, "Brano non trovato");
                return;
            }

            // Verifica che il brano appartenga all'utente (sicurezza)
            if (brano.getId_utente() != utente.getId_utente()) {
                response.sendError(403, "Accesso negato");
                return;
            }

            // Costruisci il path completo del file audio
            String nomeFile = brano.getFile_musicale();
            File audio = new File(AUDIO_PATH + nomeFile);

            // Verifica che il file esista
            if (!audio.exists() || !audio.isFile()) {
                System.err.println("Errore: file audio non trovato - " + audio.getAbsolutePath());
                response.sendError(404, "File audio non trovato");
                return;
            }

            // Verifica che il file sia all'interno della directory consentita (sicurezza)
            String path = audio.getCanonicalPath();
            String basePath = new File(AUDIO_PATH).getCanonicalPath();
            if (!path.startsWith(basePath)) {
                System.err.println("Errore: tentativo di accesso fuori dalla cartella consentita");
                response.sendError(403, "Accesso negato");
                return;
            }

            // Supporto per HTTP Range Requests (necessario per seek nel player)
            serveAudioWithRangeSupport(audio, request, response);

        } catch (SQLException e) {
            System.err.println("Errore nel recupero dei dati del brano: " + e.getMessage());
            e.printStackTrace();
            response.sendError(500,
                    "Errore nel recupero del file audio");
        }
    }

    /**
     * Serve il file audio con supporto per HTTP Range Requests.
     * Questo è necessario per permettere al player HTML5 di fare seek (spostarsi avanti/indietro)
     */
    private void serveAudioWithRangeSupport(File audioFile, HttpServletRequest request,
                                            HttpServletResponse response) {

        long lunghezzaFile = audioFile.length();
        long inizio = 0;
        long fine = lunghezzaFile - 1;

        // Imposta il content type
        String mimeType = getServletContext().getMimeType(audioFile.getName());
        if (mimeType == null) {
            mimeType = "audio/mpeg"; // Default per MP3
        }
        response.setContentType(mimeType);

        // Gestisci la richiesta Range (es. "Range: bytes=0-1023")
        String rangeHeader = request.getHeader("Range");

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            // Parsing del range header (formato: "bytes=start-end")
            String rangeValue = rangeHeader.substring(6); // Rimuovi "bytes="
            String[] ranges = rangeValue.split("-");

            try {
                // Parse start
                if (ranges.length > 0 && !ranges[0].trim().isEmpty()) {
                    inizio = Long.parseLong(ranges[0].trim());
                }

                // Parse end (opzionale)
                if (ranges.length > 1 && !ranges[1].trim().isEmpty()) {
                    fine = Long.parseLong(ranges[1].trim());
                }

                // Valida il range
                if (inizio < 0 || fine >= lunghezzaFile || inizio > fine) {
                    response.setStatus(416);
                    response.setHeader("Content-Range", "bytes */" + lunghezzaFile);
                    return;
                }

                // Imposta status 206 Partial Content
                response.setStatus(206);
                response.setHeader("Content-Range", "bytes " + inizio + "-" + fine + "/" + lunghezzaFile);
                response.setHeader("Accept-Ranges", "bytes");
                response.setContentLengthLong(fine - inizio + 1);


            } catch (NumberFormatException e) {
                System.err.println("Errore parsing range header: " + e.getMessage());
                // Range non valido, invia tutto il file
                inizio = 0;
                fine = lunghezzaFile - 1;
                response.setStatus(200);
                response.setHeader("Accept-Ranges", "bytes");
                response.setContentLengthLong(lunghezzaFile);
            }
        } else {
            // Nessun range specificato, invia tutto il file
            response.setStatus(200);
            response.setHeader("Accept-Ranges", "bytes");
            response.setContentLengthLong(lunghezzaFile);
        }

        // Invia i dati del file
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(audioFile, "r")) {
            // Posizionati all'offset di inizio
            randomAccessFile.seek(inizio);

            byte[] buffer = new byte[BUFFER_SIZE];
            long byteDaLeggere = fine - inizio + 1;

            while (byteDaLeggere > 0) {
                int lunghezza = randomAccessFile.read(buffer, 0, (int) Math.min(buffer.length, byteDaLeggere));

                if (lunghezza == -1) {
                    break; // Fine del file
                }

                response.getOutputStream().write(buffer, 0, lunghezza);
                byteDaLeggere -= lunghezza;
            }

            response.getOutputStream().flush();

        } catch (IOException e) {
            // Il client potrebbe aver chiuso la connessione (normale per il seeking)
            System.err.println("Connessione chiusa dal client (normale per seeking): " + e.getMessage());
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