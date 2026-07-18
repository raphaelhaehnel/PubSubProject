package servlets;

import com.fasterxml.jackson.core.JsonProcessingException;
import graph.Graph;
import server.dtos.HTTPRequest;
import server.dtos.HTTPResponse;
import server.enums.HTTPStatus;
import server.exceptions.HTTPException;
import view.JsonGraphWriter;

/**
 * GET /graph : returns the current graph (topics + agents + edges +
 * latest values) as JSON for the front-end to render.
 */
public class GraphDisplayer extends BaseServlet {

    /**
     * {@inheritDoc}
     * <p>
     * Builds the current graph from the topic manager and returns it as JSON.
     *
     * @throws HTTPException {@code 500} if the current topic configuration contains a cycle
     */
    @Override
    public HTTPResponse handle(HTTPRequest request) throws HTTPException {
        Graph graph = new Graph();
        graph.createFromTopics();
        
        // Safety check: Ensure the current state of the graph is valid.
        if (graph.hasCycles()) {
            throw new HTTPException(
                HTTPStatus.INTERNAL_SERVER_ERROR, 
                "Invalid graph state detected: The current topic configuration contains cycles."
            );
        }

        // Generate JSON and return 200 OK.
        // StandardCharsets.UTF_8 is safe. Any unexpected serialization errors 
        // will bubble up and be handled by the server's top-level 500 error handler.
        return sendJsonResponse(JsonGraphWriter.getGraphJSON(graph));
    }

    /** {@inheritDoc} */
    @Override
    public void close() {}
}