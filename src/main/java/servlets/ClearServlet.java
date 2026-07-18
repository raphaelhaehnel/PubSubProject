package servlets;

import graph.TopicManagerSingleton;
import server.dtos.HTTPRequest;
import server.dtos.HTTPResponse;
import server.exceptions.HTTPException;
import java.io.IOException;

/**
 * {@code POST /clear} : removes every topic and agent from the system by clearing the
 * {@link TopicManagerSingleton}, then returns an empty graph so the front-end resets its view.
 */
public class ClearServlet extends BaseServlet {

    /**
     * {@inheritDoc}
     * <p>
     * Clears all topics and returns an empty graph ({@code {"nodes": [], "edges": []}}).
     */
    @Override
    public HTTPResponse handle(HTTPRequest request) throws HTTPException {
        TopicManagerSingleton.get().clear();
        return sendJsonResponse("{\"nodes\": [], \"edges\": []}");
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {}
}