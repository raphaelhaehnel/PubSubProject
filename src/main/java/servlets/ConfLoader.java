package servlets;

import graph.GenericConfig;
import graph.Graph;
import graph.TopicManagerSingleton;
import server.dtos.HTTPRequest;
import server.dtos.HTTPResponse;
import server.enums.HTTPStatus;
import server.exceptions.HTTPException;
import view.JsonGraphWriter;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

/**
 * POST /upload : accepts a JSON configuration in the "config" form
 * field, rebuilds the agent graph, and returns the new graph as JSON.
 * Cyclic configurations are rejected.
 */
public class ConfLoader extends BaseServlet {

    @Override
    public HTTPResponse handle(HTTPRequest request) throws HTTPException {
        String configText = request.getParameters().get("config");

        if (configText == null || configText.isEmpty()) {
            throw new HTTPException(HTTPStatus.BAD_REQUEST, "No config provided");
        }

        String decodedText = URLDecoder.decode(configText, StandardCharsets.UTF_8);
        Path tempFilePath = null;

        try {
            tempFilePath = Files.createTempFile("graph_config_", ".json");
            Files.writeString(tempFilePath, decodedText);

            GenericConfig config = new GenericConfig();
            config.setConfFile(tempFilePath.toString());
            // Throws IllegalArgumentException if the JSON is malformed
            // or if reflection fails (e.g., non-existent agent type)
            config.create();

            Graph graph = new Graph();
            graph.createFromTopics();

            if (graph.hasCycles()) {
                // Rollback the corrupted state to prevent memory leaks or deadlocks
                TopicManagerSingleton.get().clear();
                throw new HTTPException(
                    HTTPStatus.BAD_REQUEST, 
                    "The current configuration has cycles. Please provide a graph without cycles."
                );
            }

            return sendJsonResponse(JsonGraphWriter.getGraphJSON(graph));

        } catch (IllegalArgumentException e) {
            throw new HTTPException(HTTPStatus.BAD_REQUEST, "Invalid JSON format or schema: " + e.getMessage(), e);
            
        } catch (IOException e) {
            throw new HTTPException(HTTPStatus.INTERNAL_SERVER_ERROR, "Failed to process configuration file", e);
            
        } finally {
            if (tempFilePath != null) {
                try {
                    Files.deleteIfExists(tempFilePath);
                } catch (IOException ignored) {}
            }
        }
    }

    @Override
    public void close() throws IOException {}
}