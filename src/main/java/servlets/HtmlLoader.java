package servlets;

import server.RequestParser;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class HtmlLoader extends BaseServlet {

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            ".html", "text/html",
            ".css",  "text/css",
            ".js",   "application/javascript"
    );

    private final String baseDir;

    public HtmlLoader(String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public void handle(RequestParser.RequestInfo ri, OutputStream toClient) throws IOException {
        try {
            String uri = ri.getUri();
            String fileName = uri.replaceFirst("/app?", "");

            if (fileName.isEmpty()) {
                fileName = "index.html";
            }

            String fullPath = baseDir + "/" + fileName;

            Path path = Paths.get(fullPath);
            if (!Files.exists(path)) {
                sendResponse(toClient, 404,
                        "<html><body><h3>404 - File not found</h3></body></html>");
                return;
            }

            byte[] content = Files.readAllBytes(path);
            String contentType = getContentType(fileName);

            String header =
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + contentType + "; charset=UTF-8\r\n" +
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

    private String getContentType(String fileName) {
        return CONTENT_TYPES.entrySet().stream()
                .filter(e -> fileName.endsWith(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("text/plain");
    }

    @Override
    public void close() {}
}
