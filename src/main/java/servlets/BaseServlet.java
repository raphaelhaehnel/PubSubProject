package servlets;

import java.nio.charset.StandardCharsets;

import server.enums.HTTPStatus;
import server.dtos.HTTPResponse;
import server.enums.ContentType;

/**
 * Shared HTTP response helpers to generate common HTTPResponse DTOs.
 */
public abstract class BaseServlet implements Servlet {

    protected HTTPResponse sendResponse(HTTPStatus status, String body) {
        return new HTTPResponse(
            status,
            ContentType.HTML.mimeType(), 
            body.getBytes(StandardCharsets.UTF_8)
        );
    }

    // Overloaded convenience method for default (empty) 200 OK responses
    protected HTTPResponse sendResponse(String body) {
        return sendResponse(HTTPStatus.OK, body);
    }

    protected HTTPResponse sendJsonResponse(HTTPStatus status, String body) {
        return new HTTPResponse(
            status,
            ContentType.JSON.mimeType(), 
            body.getBytes(StandardCharsets.UTF_8)
        );
    }

    // Overloaded convenience method for default (empty) 200 OK JSON responses
    protected HTTPResponse sendJsonResponse(String body) {
        return sendJsonResponse(HTTPStatus.OK, body);
    }
}