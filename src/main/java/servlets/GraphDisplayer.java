package servlets;

import graph.Graph;
import server.dtos.HTTPRequest;
import server.dtos.HTTPResponse;
import server.exceptions.HTTPException;
import view.HtmlGraphWriter;


/**
 * GET /graph : returns the current graph (topics + agents + edges +
 * latest values) as JSON for the front-end to render.
 */
public class GraphDisplayer extends BaseServlet {

    @Override
    public HTTPResponse handle(HTTPRequest request) throws HTTPException {
        Graph graph = new Graph();
        graph.createFromTopics();
        return sendJsonResponse(HtmlGraphWriter.getGraphJSON(graph));
    }

    @Override
    public void close() {}
}
