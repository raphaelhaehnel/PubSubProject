package servlets;

import server.RequestParser;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HtmlLoader implements Servlet {

    private final String baseDir;

    public HtmlLoader(String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public void handle(RequestParser.RequestInfo ri, OutputStream toClient) throws IOException {

        try {
            String uri = ri.getUri(); // e.g. /app/index.html
            String fileName = uri.replaceFirst("/app?", "");

            // Default file
            if (fileName.isEmpty()) {
                fileName = "index.html";
            }

            String fullPath = baseDir + "/" + fileName;

            if (!Files.exists(Paths.get(fullPath))) {
                sendResponse(toClient, 404,
                        "<html><body><h3>404 - File not found</h3></body></html>");
                return;
            }

            byte[] content = Files.readAllBytes(Paths.get(fullPath));

            String header =
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/html\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "\r\n";

            toClient.write(header.getBytes(StandardCharsets.UTF_8));
            toClient.write(content);
            toClient.flush();

        } catch (Exception e) {
            e.printStackTrace();

            sendResponse(toClient, 500,
                    "<html><body><h3>500 - Server Error</h3></body></html>");
        }
    }

    @Override
    public void close() throws IOException {
        // nothing to close
    }

    private void sendResponse(OutputStream out, int statusCode, String body) throws IOException {
        String statusText = (statusCode == 200) ? "OK" :
                (statusCode == 404) ? "Not Found" : "ERROR";

        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        String response =
                "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + bodyBytes.length + "\r\n" +
                        "\r\n";

        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.write(bodyBytes);
        out.flush();
    }
}