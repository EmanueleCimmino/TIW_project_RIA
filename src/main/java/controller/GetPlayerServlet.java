/*package controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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

import com.google.gson.Gson;

@WebServlet("/getPlayer")
public class GetPlayerServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    public void init() throws ServletException {
        try {
            ServletContext context = getServletContext();
            this.connection = ConnectionHandler.getConnection(context);

            if (this.connection == null || this.connection.isClosed()) {
                throw new ServletException("Impossibile stabilire connessione al database");
            }

            System.out.println("GetPlayerServlet inizializzata correttamente");

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
        Utente utente = (session != null) ? (Utente) session.getAttribute("utente") : null;

        Map<String, Object> jsonResponse = new HashMap<>();
        Gson gson = new Gson();

        if (utente == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            jsonResponse.put("success", false);
            jsonResponse.put("error", "Utente non autenticato");
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        // Recupera parametro brano_id
        String branoIdParam = request.getParameter("brano_id");

        // Validazione parametri
        if (branoIdParam == null || branoIdParam.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            jsonResponse.put("success", false);
            jsonResponse.put("error", "ID brano mancante");
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        int branoId;
        try {
            branoId = Integer.parseInt(branoIdParam);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            jsonResponse.put("success", false);
            jsonResponse.put("error", "ID brano non valido");
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        BranoDAO branoDAO = new BranoDAO(connection);
        AlbumDAO albumDAO = new AlbumDAO(connection);

        try {
            // Recupera il brano
            Brano brano = branoDAO.getBrano(branoId);

            if (brano == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                jsonResponse.put("success", false);
                jsonResponse.put("error", "Brano non trovato");
                response.getWriter().write(gson.toJson(jsonResponse));
                return;
            }

            // Verifica che il brano appartenga all'utente (sicurezza)
            if (brano.getId_utente() != utente.getId_utente()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                jsonResponse.put("success", false);
                jsonResponse.put("error", "Non hai i permessi per accedere a questo brano");
                response.getWriter().write(gson.toJson(jsonResponse));
                return;
            }

            // Recupera i dati dell'album
            Album album = albumDAO.getAlbum(brano.getId_album());

            if (album == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                jsonResponse.put("success", false);
                jsonResponse.put("error", "Album non trovato");
                response.getWriter().write(gson.toJson(jsonResponse));
                return;
            }

            // Prepara i dati del brano
            Map<String, Object> branoData = new HashMap<>();
            branoData.put("id_brano", brano.getId());
            branoData.put("titolo", brano.getTitolo());
            branoData.put("file_musicale", brano.getFile_musicale());
            branoData.put("id_album", brano.getId_album());

            // Prepara i dati dell'album
            Map<String, Object> albumData = new HashMap<>();
            albumData.put("id_album", album.getId_album());
            albumData.put("titolo_album", album.getTitolo_album());
            albumData.put("interprete", album.getInterprete());
            albumData.put("anno_pubblicazione", album.getAnno_pubblicazione());
            albumData.put("genere", album.getGenere());
            albumData.put("immagine_album", album.getImmagine_album());

            // Risposta di successo
            jsonResponse.put("success", true);
            jsonResponse.put("brano", branoData);
            jsonResponse.put("album", albumData);

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(gson.toJson(jsonResponse));

            System.out.println("Brano " + branoId + " caricato per player - utente: " + utente.getUsername());

        } catch (SQLException e) {
            System.err.println("Errore nel recupero dei dati del brano: " + e.getMessage());
            e.printStackTrace();

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            jsonResponse.put("success", false);
            jsonResponse.put("error", "Errore nel recupero dei dati del brano");
            response.getWriter().write(gson.toJson(jsonResponse));
        }
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
}*/