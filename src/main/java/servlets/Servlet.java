package servlets;

import java.io.IOException;

import server.dtos.HTTPRequest;
import server.dtos.HTTPResponse;
import server.exceptions.HTTPException;

/**
 * Handles one HTTP endpoint. Implementations return an internal HTTPResponse object.
 */
public interface Servlet {
    HTTPResponse handle(HTTPRequest request) throws HTTPException;
    void close() throws IOException;
}
