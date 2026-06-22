package servlets;

import graph.TopicManagerSingleton;
import server.dtos.HTTPRequest;
import server.dtos.HTTPResponse;
import server.exceptions.HTTPException;
import java.io.IOException;

public class ClearServlet extends BaseServlet {
    
    @Override
    public HTTPResponse handle(HTTPRequest request) throws HTTPException {
        TopicManagerSingleton.get().clear();
        return sendJsonResponse("{\"nodes\": [], \"edges\": []}");
    }

    @Override
    public void close() throws IOException {}
}