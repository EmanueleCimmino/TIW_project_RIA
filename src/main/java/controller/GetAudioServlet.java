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

            System.out.println("GetAudioServlet inizializzata correttamente");

        } catch (UnavailableException | SQLException e) {
            System.err.println("Errore durante l'inizializzazione della servlet: " + e.getMessage());
            e.printStackTrace();
            throw new ServletException("Errore nell'inizializzazione della servlet", e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("GetAudioServlet.doGet() chiamato");

        // Verifica sessione utente
        HttpSession session = request.getSession(false);
        if (session == null) {
            System.out.println("Errore: sessione non valida");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Sessione non valida");
            return;
        }

        Utente utente = (Utente) session.getAttribute("utente");
        if (utente == null) {
            System.out.println("Errore: utente non autenticato");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Utente non autenticato");
            return;
        }

        // Recupera parametro brano_id
        String branoIdParam = request.getParameter("brano_id");

        if (branoIdParam == null || branoIdParam.isEmpty()) {
            System.out.println("Errore: ID brano mancante");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID brano mancante");
            return;
        }

        int branoId;
        try {
            branoId = Integer.parseInt(branoIdParam);
        } catch (NumberFormatException e) {
            System.out.println("Errore: ID brano non valido - " + branoIdParam);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID brano non valido");
            return;
        }

        System.out.println("Richiesta audio per brano ID: " + branoId);

        BranoDAO branoDAO = new BranoDAO(connection);

        try {
            // Recupera il brano
            Brano brano = branoDAO.getBrano(branoId);

            if (brano == null) {
                System.out.println("Errore: brano non trovato");
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Brano non trovato");
                return;
            }

            // Verifica che il brano appartenga all'utente (sicurezza)
            if (brano.getId_utente() != utente.getId_utente()) {
                System.out.println("Errore: accesso negato per brano ID " + branoId);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accesso negato");
                return;
            }

            // Costruisci il path completo del file audio
            String nomeFile = brano.getFile_musicale();
            File audio = new File(AUDIO_PATH + nomeFile);

            System.out.println("File audio: " + audio.getAbsolutePath());

            // Verifica che il file esista
            if (!audio.exists() || !audio.isFile()) {
                System.err.println("Errore: file audio non trovato - " + audio.getAbsolutePath());
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "File audio non trovato");
                return;
            }

            // Verifica che il file sia all'interno della directory consentita (sicurezza)
            String path = audio.getCanonicalPath();
            String basePath = new File(AUDIO_PATH).getCanonicalPath();
            if (!path.startsWith(basePath)) {
                System.err.println("Errore: tentativo di accesso fuori dalla cartella consentita");
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accesso negato");
                return;
            }

            System.out.println("File trovato - dimensione: " + audio.length() + " bytes");

            // Supporto per HTTP Range Requests (necessario per seek nel player)
            serveAudioWithRangeSupport(audio, request, response);

        } catch (SQLException e) {
            System.err.println("Errore nel recupero dei dati del brano: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Errore nel recupero del file audio");
        }
    }

    /**
     * Serve il file audio con supporto per HTTP Range Requests.
     * Questo è necessario per permettere al player HTML5 di fare seek (spostarsi avanti/indietro)
     */
    private void serveAudioWithRangeSupport(File audioFile, HttpServletRequest request,
                                            HttpServletResponse response) throws IOException {

        long fileLength = audioFile.length();
        long start = 0;
        long end = fileLength - 1;

        // Imposta il content type
        String mimeType = getServletContext().getMimeType(audioFile.getName());
        if (mimeType == null) {
            mimeType = "audio/mpeg"; // Default per MP3
        }
        response.setContentType(mimeType);

        // Gestisci la richiesta Range (es. "Range: bytes=0-1023")
        String rangeHeader = request.getHeader("Range");

        System.out.println("Range header: " + rangeHeader);

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            // Parsing del range header (formato: "bytes=start-end")
            String rangeValue = rangeHeader.substring(6); // Rimuovi "bytes="
            String[] ranges = rangeValue.split("-");

            try {
                // Parse start
                if (ranges.length > 0 && !ranges[0].trim().isEmpty()) {
                    start = Long.parseLong(ranges[0].trim());
                }

                // Parse end (opzionale)
                if (ranges.length > 1 && !ranges[1].trim().isEmpty()) {
                    end = Long.parseLong(ranges[1].trim());
                }

                // Valida il range
                if (start < 0 || start >= fileLength || end >= fileLength || start > end) {
                    System.out.println("Range non valido: " + start + "-" + end + " (file size: " + fileLength + ")");
                    response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                    response.setHeader("Content-Range", "bytes */" + fileLength);
                    return;
                }

                // Imposta status 206 Partial Content
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
                response.setHeader("Accept-Ranges", "bytes");
                response.setContentLengthLong(end - start + 1);

                System.out.println("Serving partial content: bytes " + start + "-" + end + "/" + fileLength);

            } catch (NumberFormatException e) {
                System.err.println("Errore parsing range header: " + e.getMessage());
                // Range non valido, invia tutto il file
                start = 0;
                end = fileLength - 1;
                response.setStatus(HttpServletResponse.SC_OK);
                response.setHeader("Accept-Ranges", "bytes");
                response.setContentLengthLong(fileLength);
                System.out.println("Range parsing fallito, serving full file");
            }
        } else {
            // Nessun range specificato, invia tutto il file
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("Accept-Ranges", "bytes");
            response.setContentLengthLong(fileLength);
            System.out.println("No range header, serving full file: " + fileLength + " bytes");
        }

        // Invia i dati del file
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(audioFile, "r")) {
            // Posizionati all'offset di inizio
            randomAccessFile.seek(start);

            byte[] buffer = new byte[BUFFER_SIZE];
            long bytesToRead = end - start + 1;
            long bytesRead = 0;

            while (bytesToRead > 0) {
                int len = randomAccessFile.read(buffer, 0, (int) Math.min(buffer.length, bytesToRead));

                if (len == -1) {
                    break; // Fine del file
                }

                response.getOutputStream().write(buffer, 0, len);
                bytesToRead -= len;
                bytesRead += len;
            }

            response.getOutputStream().flush();
            System.out.println("Audio servito con successo: " + bytesRead + " bytes inviati");

        } catch (IOException e) {
            // Il client potrebbe aver chiuso la connessione (normale per il seeking)
            System.out.println("Connessione chiusa dal client (normale per seeking): " + e.getMessage());
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