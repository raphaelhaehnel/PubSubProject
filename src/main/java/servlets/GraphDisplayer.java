package servlets;

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

    @Override
    public HTTPResponse handle(HTTPRequest request) throws HTTPException {
        try {
            Graph graph = new Graph();
            graph.createFromTopics();
            
            // Safety check: Ensure the current state of the graph is actually valid
            if (graph.hasCycles()) {
                throw new HTTPException(
                    HTTPStatus.INTERNAL_SERVER_ERROR, 
                    "Invalid graph state detected: The current topic configuration contains cycles."
                );
            }

            return sendJsonResponse(JsonGraphWriter.getGraphJSON(graph));

        } catch (Exception e) {
            // Wrap any unexpected runtime errors (like JSON parsing issues) into our API boundary
            throw new HTTPException(
                HTTPStatus.INTERNAL_SERVER_ERROR, 
                "An unexpected error occurred while generating the graph data.", 
                e
            );
        }
    }

    @Override
    public void close() {}
}