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

public class ConfLoader extends BaseServlet {

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
                throw new RuntimeException(
                        "The current configuration has cycles. Please provide a graph without cycles.");
            }

            sendJsonResponse(toClient, HtmlGraphWriter.getGraphJSON(graph));

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(toClient, 500, "<html><body>Error processing config</body></html>");
        }
    }

    @Override
    public void close() throws IOException {
        Files.deleteIfExists(Paths.get(TEMP_FILE));
    }
}
