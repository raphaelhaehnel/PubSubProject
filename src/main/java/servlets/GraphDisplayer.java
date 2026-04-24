package servlets;

import graph.Graph;
import server.RequestParser;
import view.HtmlGraphWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class GraphDisplayer implements Servlet {

    @Override
    public void handle(RequestParser.RequestInfo ri, OutputStream toClient) throws IOException {
        try {
            Graph graph = new Graph();
            graph.createFromTopics();

            String json = HtmlGraphWriter.getGraphJSON(graph);
            sendJsonResponse(toClient, 200, json);

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(toClient, 500, "<html><body>Error loading graph</body></html>");
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

    private void sendResponse(OutputStream out, int statusCode, String body) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        String response =
                "HTTP/1.1 " + statusCode + " OK\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + bodyBytes.length + "\r\n\r\n";

        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.write(bodyBytes);
        out.flush();
    }

    @Override
    public void close() {}
}