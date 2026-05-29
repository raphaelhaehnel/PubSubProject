package servlets;

import java.io.IOException;
import java.io.OutputStream;

import server.RequestParser.RequestInfo;

/**
 * Handles one HTTP endpoint. Implementations write the full HTTP
 * response (status line + headers + body) to {@code toClient}.
 */
public interface Servlet {
    void handle(RequestInfo ri, OutputStream toClient) throws IOException;
    void close() throws IOException;
}
