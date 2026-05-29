package servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Shared HTTP response helpers so concrete servlets only contain
 * business logic.
 */
public abstract class BaseServlet implements Servlet {

    /** HTML response with the given status code. */
    protected void sendResponse(OutputStream out, int statusCode, String body) throws IOException {
        sendRaw(out, statusCode, "text/html", body);
    }

    /** JSON response with status 200. */
    protected void sendJsonResponse(OutputStream out, String body) throws IOException {
        sendRaw(out, 200, "application/json", body);
    }

    private static final Map<Integer, String> STATUS_TEXTS = Map.of(
            200, "OK",
            400, "Bad Request",
            404, "Not Found",
            500, "Internal Server Error"
    );

    private void sendRaw(OutputStream out, int statusCode, String contentType, String body)
            throws IOException {

        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String statusText = STATUS_TEXTS.getOrDefault(statusCode, "Unknown");

        String header =
                "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                "Content-Type: " + contentType + "; charset=UTF-8\r\n" +
                "Content-Length: " + bodyBytes.length + "\r\n" +
                "\r\n";

        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(bodyBytes);
        out.flush();
    }
}
