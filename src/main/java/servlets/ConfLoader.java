package servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import graph.GenericConfig;
import graph.Graph;
import graph.TopicManagerSingleton;
import servlets.dtos.Config;
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
 * field, strictly validates it against ConfigDTO, rebuilds the agent graph, 
 * and returns the new graph as JSON. Cyclic configurations are rejected.
 */
public class ConfLoader extends BaseServlet {

    // Instantiate Jackson mapper once per servlet lifecycle
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * {@inheritDoc}
     * <p>
     * Reads the {@code config} form field, validates it against {@link Config}, rebuilds the
     * agent graph, and returns the new graph as JSON.
     *
     * @throws HTTPException {@code 400} if the config is missing, malformed, fails validation,
     *         or introduces a cycle; {@code 500} if the temporary config file cannot be written
     */
    @Override
    public HTTPResponse handle(HTTPRequest request) throws HTTPException {
        String configText = request.getParameters().get("config");

        if (configText == null || configText.isEmpty()) {
            throw new HTTPException(HTTPStatus.BAD_REQUEST, "No config provided");
        }

        String decodedText = URLDecoder.decode(configText, StandardCharsets.UTF_8);

        try {
            // This triggers all @JsonCreator constructors and throws exceptions on bad schema
            mapper.readValue(decodedText, Config.class);
            
        } catch (MismatchedInputException e) {
            throw new HTTPException(HTTPStatus.BAD_REQUEST, "Invalid schema: Missing required fields or incorrect data types. " + e.getMessage());
        } catch (ValueInstantiationException e) {
            Throwable originalException = e.getCause();
            String message = (originalException != null) ? originalException.getMessage() : e.getMessage();
            throw new HTTPException(HTTPStatus.BAD_REQUEST, "Validation error: " + message);
        } catch (Exception e) {
            throw new HTTPException(HTTPStatus.BAD_REQUEST, "Malformed JSON syntax.");
        }

        Path tempFilePath = null;

        try {
            tempFilePath = Files.createTempFile("graph_config_", ".json");
            Files.writeString(tempFilePath, decodedText);

            GenericConfig config = new GenericConfig();
            config.setConfFile(tempFilePath.toString());
            config.create();

            Graph graph = new Graph();
            graph.createFromTopics();

            if (graph.hasCycles()) {
                TopicManagerSingleton.get().clear();
                throw new HTTPException(
                    HTTPStatus.BAD_REQUEST, 
                    "The current configuration has cycles. Please provide a graph without cycles."
                );
            }

            return sendJsonResponse(JsonGraphWriter.getGraphJSON(graph));

        } catch (IllegalArgumentException e) {
            throw new HTTPException(HTTPStatus.BAD_REQUEST, "Invalid graph creation: " + e.getMessage(), e);
            
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

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {}
}