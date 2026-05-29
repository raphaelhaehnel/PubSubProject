package servlets;

import graph.Graph;
import server.RequestParser;
import view.HtmlGraphWriter;

import java.io.IOException;
import java.io.OutputStream;

/**
 * GET /graph : returns the current graph (topics + agents + edges +
 * latest values) as JSON for the front-end to render.
 */
public class GraphDisplayer extends BaseServlet {

    @Override
    public void handle(RequestParser.RequestInfo ri, OutputStream toClient) throws IOException {
        try {
            Graph graph = new Graph();
            graph.createFromTopics();

            sendJsonResponse(toClient, HtmlGraphWriter.getGraphJSON(graph));

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(toClient, 500, "<html><body>Error loading graph</body></html>");
        }
    }

    @Override
    public void close() {}
}
