package servlets;

import java.io.IOException;

import server.dtos.HTTPRequest;
import server.dtos.HTTPResponse;
import server.exceptions.HTTPException;

/**
 * Handles one HTTP endpoint. Implementations return an internal HTTPResponse object.
 */
public interface Servlet {
    /**
     * Processes one HTTP request and produces the response to send back.
     *
     * @param request the parsed incoming request
     * @return the response to write to the client
     * @throws HTTPException to signal an HTTP error status (e.g. 400, 404) to the server
     */
    HTTPResponse handle(HTTPRequest request) throws HTTPException;

    /**
     * Releases any resources held by this servlet.
     *
     * @throws IOException if closing an underlying resource fails
     */
    void close() throws IOException;
}
