package servlets;

import java.nio.charset.StandardCharsets;

import server.enums.HTTPStatus;
import server.dtos.HTTPResponse;
import server.enums.ContentType;

/**
 * Shared HTTP response helpers to generate common HTTPResponse DTOs.
 */
public abstract class BaseServlet implements Servlet {

    /**
     * Builds an HTML ({@code text/html}) response with the given status.
     *
     * @param status the HTTP status
     * @param body   the response body
     * @return the assembled response
     */
    protected HTTPResponse sendResponse(HTTPStatus status, String body) {
        return new HTTPResponse(
            status,
            ContentType.HTML.mimeType(),
            body.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Builds a {@code 200 OK} HTML response.
     *
     * @param body the response body
     * @return the assembled response
     */
    protected HTTPResponse sendResponse(String body) {
        return sendResponse(HTTPStatus.OK, body);
    }

    /**
     * Builds a JSON ({@code application/json}) response with the given status.
     *
     * @param status the HTTP status
     * @param body   the JSON response body
     * @return the assembled response
     * @throws NullPointerException if {@code body} is {@code null}
     */
    protected HTTPResponse sendJsonResponse(HTTPStatus status, String body) throws NullPointerException {
        return new HTTPResponse(
            status,
            ContentType.JSON.mimeType(),
            body.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Builds a {@code 200 OK} JSON response.
     *
     * @param body the JSON response body
     * @return the assembled response
     */
    protected HTTPResponse sendJsonResponse(String body) {
        return sendJsonResponse(HTTPStatus.OK, body);
    }
}