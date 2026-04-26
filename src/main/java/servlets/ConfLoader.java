package servlets;

import graph.GenericConfig;
import graph.Graph;
import server.RequestParser;
import view.HtmlGraphWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfLoader implements Servlet {

    private static final String TEMP_FILE = "uploaded_config.txt";

    @Override
    public void handle(RequestParser.RequestInfo ri, OutputStream toClient) throws IOException {

        try {
            String configText = ri.getParameters().get("config");

            if (configText == null || configText.isEmpty()) {
                sendResponse(toClient, 400, "<html><body>No config provided</body></html>");
                return;
            }

            String decodedText = URLDecoder.decode(configText, StandardCharsets.UTF_8);

            Files.writeString(Paths.get(TEMP_FILE), decodedText);

            GenericConfig config = new GenericConfig();
            config.setConfFile(TEMP_FILE);
            config.create();

            Graph graph = new Graph();
            graph.createFromTopics();

            if (graph.hasCycles()) {
                throw new RuntimeException("The current configuration has cycles. Please provide a graph without cycles.");
            }

            String json = HtmlGraphWriter.getGraphJSON(graph);
            sendJsonResponse(toClient, 200, json);

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(toClient, 500, "<html><body>Error processing config</body></html>");
        }
    }

    private void sendJsonResponse(OutputStream out, int statusCode, String body) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        String response =
                "HTTP/1.1 " + statusCode + " OK\r\n" +
                        "Content-Type: application/json; charset=UTF-8\r\n" +
                        "Content-Length: " + bodyBytes.length + "\r\n\r\n";

        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.write(bodyBytes);
        out.flush();
    }

    @Override
    public void close() throws IOException {
        // Nothing to close for now
    }

    // helper method to send HTTP response
    private void sendResponse(OutputStream out, int statusCode, String body) throws IOException {
        String statusText = (statusCode == 200) ? "OK" : "ERROR";

        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        String response =
                "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                        "Content-Type: text/html; charset=UTF-8\r\n" +
                        "Content-Length: " + bodyBytes.length + "\r\n" +
                        "\r\n";

        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.write(bodyBytes);
        out.flush();
    }
}