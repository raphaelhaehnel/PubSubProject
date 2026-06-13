package servlets;

import graph.GenericConfig;
import graph.Graph;
import server.dtos.HTTPRequest;
import server.dtos.HTTPResponse;
import server.enums.HTTPStatus;
import server.exceptions.HTTPException;
import view.HtmlGraphWriter;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

/**
 * POST /upload : accepts a JSON configuration in the "config" form
 * field, rebuilds the agent graph, and returns the new graph as JSON.
 * Cyclic configurations are rejected.
 */
public class ConfLoader extends BaseServlet {

    // GenericConfig reads from a File, so we write the upload to disk first.
    private static final String TEMP_FILE = "uploaded_config.json";

    @Override
    public HTTPResponse handle(HTTPRequest request) throws HTTPException {
        String configText = request.getParameters().get("config");

        if (configText == null || configText.isEmpty()) {
            throw new HTTPException(HTTPStatus.BAD_REQUEST, "No config provided");
        }

        String decodedText = URLDecoder.decode(configText, StandardCharsets.UTF_8);
        Path tempFilePath = null;

        try {
            // Create a completely unique temp file for this specific request
            tempFilePath = Files.createTempFile("graph_config_", ".txt");
            Files.writeString(tempFilePath, decodedText);

            GenericConfig config = new GenericConfig();
            // Pass the unique file path to your config object
            config.setConfFile(tempFilePath.toString()); 
            config.create();

            Graph graph = new Graph();
            graph.createFromTopics();

            if (graph.hasCycles()) {
                throw new HTTPException(
                    HTTPStatus.BAD_REQUEST, 
                    "The current configuration has cycles. Please provide a graph without cycles."
                );
            }

            // Using the overloaded helper we built in BaseServlet!
            return sendJsonResponse(HtmlGraphWriter.getGraphJSON(graph));

        } catch (IOException e) {
            // Catch disk errors and chain them into an HTTP 500
            throw new HTTPException(HTTPStatus.INTERNAL_SERVER_ERROR, "Failed to process configuration file", e);
            
        } finally {
            // ALWAYS delete temp files so the server's hard drive doesn't fill up
            if (tempFilePath != null) {
                try {
                    Files.deleteIfExists(tempFilePath);
                } catch (IOException ignored) {
                    // Safe to ignore during cleanup
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        Files.deleteIfExists(Paths.get(TEMP_FILE));
    }
}
