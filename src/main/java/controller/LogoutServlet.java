package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/Logout")
public class LogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public LogoutServlet() {
        super();
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException{


        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        String ajaxHeader = request.getHeader("X-Requested-With");
        boolean isAjax = "XMLHttpRequest".equals(ajaxHeader);

        String acceptHeader = request.getHeader("Accept");
        boolean expectsJson = acceptHeader != null && acceptHeader.contains("application/json");

        if (isAjax || expectsJson) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("success", true);
            jsonResponse.put("message", "Logout effettuato con successo");
            jsonResponse.put("redirectUrl", getServletContext().getContextPath() + "/");

            Gson gson = new Gson();
            response.getWriter().write(gson.toJson(jsonResponse));
        } else {
            String path = getServletContext().getContextPath();
            response.sendRedirect(path);
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException{
        doGet(request, response);
    }
}